package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken

internal object LuaLexer : BaseLexer() {
    private val spec = ConfigurableLexerSpec(
        keywords = setOf(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return",
            "then", "true", "until", "while"
        ),
        builtins = setOf(
            "require", "print", "pairs", "ipairs", "next", "tonumber", "tostring",
            "table", "string", "math", "coroutine"
        ),
        lineComments = listOf("--"),
        blockComments = listOf("--[[" to "]]"),
        blockStrings = listOf("[[" to "]]"),
        stringQuotes = setOf('"', '\''),
        extraWordChars = setOf('.'),
        operators = setOf("==", "~=", "<=", ">=", "..", "...", "=", "+", "-", "*", "/", "%", "<", ">", "#"),
        punctuation = setOf('{', '}', '(', ')', '[', ']', ';', ',', '.', ':')
    )

    override fun tokenize(code: String): List<CodeToken> = tokenizeWithSpec(code, spec)

    // 长字符串 [[...]] 与块注释 --[[...]] 走 spec 定界符判定，其余回退默认实现
    override fun isExtendableToken(token: CodeToken): Boolean =
        isExtendableTokenWithSpec(token, spec) ?: super.isExtendableToken(token)
}
