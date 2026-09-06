package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/** 空三引号串 """""" 的长度；短于它的三引号形态必为未闭合 opener */
private const val CLOSED_TRIPLE_QUOTE_LEN = 6

/**
 * 词法分析器接口，定义代码分词的核心方法。
 * 对外公开，支持外部注入自定义 Lexer 实现。
 */
public interface Lexer {
    /**
     * 对代码字符串进行词法分析，返回 Token 列表。
     * 空输入返回空列表，不抛出异常。
     * 所有 Token 的 range 覆盖原始字符串完整范围（无遗漏字符）。
     *
     * @param code 待分析的代码字符串
     * @return Token 列表
     */
    public fun tokenize(code: String): List<CodeToken>

    /**
     * 带全文偏移的词法分析，供增量引擎以 token 边界重启解析时提供上下文
     * （如行首敏感判定）。返回 Token 的 range 仍相对 [code] 从 0 计，调用方自行偏移。
     * 默认实现忽略 startOffset，仅行首敏感词法器需覆写。
     */
    public fun tokenize(code: String, startOffset: Int): List<CodeToken> = tokenize(code)

    /**
     * 该 Token 是否可能继续吸收后续字符（未闭合的多行结构）。
     * 增量引擎用它决定是否从该 Token 起点重解析；短前缀（如单个 `"`）由引擎的
     * 邻近 Token 回退兜底，无需在此穷举。
     * 契约宽容性：假阳性导致多回退、假阴性回退全量重解析，均不破坏正确性。
     */
    public fun isExtendableToken(token: CodeToken): Boolean {
        val t = token.text
        return when (token.type) {
            TokenType.COMMENT -> t.length >= 2 && t.startsWith("/*") && !t.endsWith("*/")
            TokenType.STRING -> when {
                t.startsWith("\"\"\"") -> t.length < CLOSED_TRIPLE_QUOTE_LEN || !t.endsWith("\"\"\"")
                t.startsWith("'''") -> t.length < CLOSED_TRIPLE_QUOTE_LEN || !t.endsWith("'''")
                t.length >= 2 && t.startsWith("\"") && !t.endsWith("\"") -> true
                t.length >= 2 && t.startsWith("'") && !t.endsWith("'") -> true
                t.length >= 2 && t.startsWith("`") && !t.endsWith("`") -> true
                else -> false
            }
            else -> false
        }
    }
}
