package com.hrm.codehigh.stream

import com.hrm.codehigh.ast.CodeAst
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
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
    ) {
        /** 本次更新是否产生变化（缓存命中时 false） */
        val hasChange: Boolean get() = firstChangedLine >= 0
    }

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
        // 按稳定 Token 数取前缀视图，O(1) 无拷贝；Token 有序，首个非稳定索引即稳定数
        val stableCount = oldAst.tokens.indexOfFirst { it.range.last >= reparseStart }
            .let { if (it < 0) oldAst.tokens.size else it }
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

        // 窗口：只考察结束位置落在窗口内的 Token（未闭合结构必延伸到文末/行尾），
        // 从尾部反向定位首个窗口外 Token，避免全列表扫描
        val windowStart = (appendedStart - CONTEXT_LOOKBACK_CHARS).coerceAtLeast(0)
        var firstWindowIdx = oldAst.tokens.size
        for (i in oldAst.tokens.indices.reversed()) {
            if (oldAst.tokens[i].range.last < windowStart) break
            firstWindowIdx = i
        }

        // 最后一个"连续可扩展 Token 游程"的最早起点：
        // 单个未闭合长结构（注释/三引号）起点可远在窗口外，需整游程回退；
        // 连续可合并序列（如 ---- 中相邻的多个 -）需回退到游程首
        var runStart: Int? = null
        var runStartIsDelimited = false
        var prevEnd = -2
        for (i in firstWindowIdx until oldAst.tokens.size) {
            val t = oldAst.tokens[i]
            if (lexer.isExtendableToken(t)) {
                if (runStart == null || t.range.first != prevEnd + 1) {
                    runStart = t.range.first
                    runStartIsDelimited = t.type == TokenType.COMMENT || t.type == TokenType.STRING
                }
            } else {
                runStart = null
                runStartIsDelimited = false
            }
            prevEnd = t.range.last
        }

        // 邻近 Token 回退：吸收因追加而尾段变化的最后一个短 Token
        val nearbyTokenStart = oldAst.tokens
            .lastOrNull { it.range.last < appendedStart && appendedStart - it.range.first <= CONTEXT_LOOKBACK_CHARS }
            ?.range
            ?.first

        val candidate = runStart ?: nearbyTokenStart ?: 0
        return retreatToLineContext(oldAst, candidate, fromDelimitedStructure = runStart != null && runStartIsDelimited)
    }

    /**
     * 重解析起点恰在真行首时（前一字符为 \n），把包含该 \n 的 Token 纳入脏区，
     * 让词法器在子串内回看一行首（YamlLexer 等按前一字符判行首）。
     * 回退目标保持 Token 边界，维持"从 Token 边界重解析"的不变式。
     *
     * 例外：未闭合定界结构（注释/字符串）的识别由定界符前缀驱动、不依赖行首
     * 上下文，从结构自身起点重解析即可，不回退。
     */
    private fun retreatToLineContext(oldAst: CodeAst, reparseStart: Int, fromDelimitedStructure: Boolean): Int {
        if (reparseStart <= 0 || fromDelimitedStructure) return reparseStart
        if (oldAst.source[reparseStart - 1] != '\n') return reparseStart
        return oldAst.tokens
            .lastOrNull { it.range.first < reparseStart && it.range.last >= reparseStart - 1 }
            ?.range?.first ?: reparseStart
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
