package com.hrm.codehigh.lexer

/**
 * 词法分析器基础工具类。
 * 标记为 internal，仅供模块内部使用。
 */
internal abstract class BaseLexer : Lexer {

    /** 返回 [pos] 起连续空白后的首个非空白位置 */
    protected fun whitespaceEnd(code: String, pos: Int): Int {
        var i = pos
        while (i < code.length && code[i].isWhitespace()) i++
        return i
    }
}
