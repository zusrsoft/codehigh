package com.hrm.codehigh.ast

/**
 * 代码 Token 数据结构，表示词法分析后的最小语义单元。
 * 惰性持有源文本引用，仅在访问 [text] 时切片，避免逐 Token 全量拷贝源码。
 *
 * 等值语义仅比较 [type] 与 [range]；text 需要时显式断言。
 *
 * @param type Token 类型
 * @param range Token 在源文本中的位置范围（相对 [source] 从 0 计）
 * @param source 源文本引用
 */
class CodeToken(
    val type: TokenType,
    val range: IntRange,
    val source: CharSequence,
) {
    /** Token 原始文本，惰性切片 */
    val text: String by lazy(LazyThreadSafetyMode.NONE) {
        source.subSequence(range.first, range.last + 1).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeToken) return false
        return type == other.type && range == other.range
    }

    override fun hashCode(): Int = 31 * type.hashCode() + range.hashCode()

    override fun toString(): String =
        "CodeToken(type=$type, text=$text, range=$range)"
}
