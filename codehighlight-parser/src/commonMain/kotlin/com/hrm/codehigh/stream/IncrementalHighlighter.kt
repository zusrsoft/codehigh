package com.hrm.codehigh.stream

import com.hrm.codehigh.ast.CodeAst
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.lexer.LanguageRegistry

/**
 * 增量高亮引擎，用于流式场景下的高效代码高亮更新。
 *
 * 核心策略：
 * 1. 稳定前缀 Token 直接复用，不重新解析
 * 2. 仅对尾部脏区域重新解析，重解析起点以 startOffset 传给词法器，
 *    保证行首等全文上下文判定正确
 * 3. 相同代码字符串和语言命中 AST 缓存，直接返回缓存结果
 *
 * [UpdateResult.firstChangedLine] / [UpdateResult.reparseStart] 为 -1 表示无变化。
 */
class IncrementalHighlighter {
    data class UpdateResult(
        val ast: CodeAst,
        /** 首个变更行号；-1 表示本次无变化（缓存命中） */
        val firstChangedLine: Int,
        /** 重解析起点；-1 表示本次无变化（缓存命中） */
        val reparseStart: Int,
    )

    private companion object {
        private const val CONTEXT_LOOKBACK_CHARS = 64
    }

    private var cachedAst: CodeAst? = null

    private var lastLanguage: String = ""

    fun update(code: String, language: String): CodeAst {
        return updateDetailed(code, language).ast
    }

    fun updateDetailed(code: String, language: String): UpdateResult {
        if (language != lastLanguage) {
            lastLanguage = language
            cachedAst = null
        }

        val cached = cachedAst
        // 长度短路，避免大字符串逐字符比较
        if (cached != null && cached.source.length == code.length && cached.source == code) {
            return UpdateResult(ast = cached, firstChangedLine = -1, reparseStart = -1)
        }

        val update = if (cached != null && code.startsWith(cached.source)) {
            incrementalParse(code, language, cached)
        } else {
            UpdateResult(ast = fullParse(code, language), firstChangedLine = 0, reparseStart = 0)
        }

        cachedAst = update.ast
        return update
    }

    private fun fullParse(code: String, language: String): CodeAst {
        val lexer = LanguageRegistry.getOrPlain(language)
        return CodeAst(lexer.tokenize(code), code, language)
    }

    private fun incrementalParse(newCode: String, language: String, oldAst: CodeAst): UpdateResult {
        val appendedStart = oldAst.source.length
        val reparseStart = determineReparseStart(oldAst, appendedStart)
        val lexer = LanguageRegistry.getOrPlain(language)
        // 按稳定 Token 数取前缀视图，O(1) 无拷贝
        val stableCount = oldAst.tokens.count { it.range.last < reparseStart }
        val stable = oldAst.tokens.subList(0, stableCount)

        // startOffset 让词法器以全文视角判断行首等上下文；返回 range 仍相对子串
        val dirtyCode = newCode.substring(reparseStart)
        val dirtyTokens = lexer.tokenize(dirtyCode, reparseStart).map { token ->
            CodeToken(
                type = token.type,
                range = (token.range.first + reparseStart)..(token.range.last + reparseStart),
                source = newCode,
            )
        }

        val allTokens = ArrayList<CodeToken>(stable.size + dirtyTokens.size)
        allTokens.addAll(stable)
        allTokens.addAll(dirtyTokens)

        return UpdateResult(
            ast = CodeAst(allTokens, newCode, language),
            firstChangedLine = newCode.countLinesBefore(reparseStart),
            reparseStart = reparseStart,
        )
    }

    private fun determineReparseStart(oldAst: CodeAst, appendedStart: Int): Int {
        if (appendedStart == 0) return 0
        val lexer = LanguageRegistry.getOrPlain(oldAst.language)

        // 未闭合多行结构：只检查"结束位置落在窗口内"的 Token（未闭合结构必延伸到
        // 文末或行尾，其 Token 起点可远在窗口之外——长注释/长字符串场景），
        // 从其起点重解析，避免每次追加全量回退
        val unfinishedTokenStart = oldAst.tokens
            .asReversed()
            .firstOrNull {
                it.range.last >= appendedStart - CONTEXT_LOOKBACK_CHARS &&
                    lexer.isExtendableToken(it)
            }
            ?.range
            ?.first

        // 邻近 Token 回退：吸收因追加而尾段变化的最后一个短 Token
        val nearbyTokenStart = oldAst.tokens
            .lastOrNull { it.range.last < appendedStart && appendedStart - it.range.first <= CONTEXT_LOOKBACK_CHARS }
            ?.range
            ?.first

        return unfinishedTokenStart ?: nearbyTokenStart ?: 0
    }

    /** 清除缓存，强制下次全量解析 */
    fun invalidate() {
        cachedAst = null
        lastLanguage = ""
    }
}

private fun String.countLinesBefore(charIndex: Int): Int {
    if (charIndex <= 0) return 0
    val end = charIndex.coerceAtMost(length)
    var count = 0
    for (i in 0 until end) if (this[i] == '\n') count++
    return count
}
