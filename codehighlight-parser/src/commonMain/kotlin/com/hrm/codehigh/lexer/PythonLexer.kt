package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * Python 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object PythonLexer : BaseLexer() {

    private val keywords = setOf(
        "def", "class", "if", "elif", "else", "for", "while", "return", "import",
        "from", "as", "with", "try", "except", "finally", "raise", "lambda",
        "yield", "async", "await", "pass", "break", "continue", "del", "global",
        "nonlocal", "assert", "in", "is", "not", "and", "or", "True", "False", "None"
    )

    private val builtins = setOf(
        "print", "len", "range", "type", "isinstance", "enumerate", "zip", "map",
        "filter", "sorted", "reversed", "list", "dict", "set", "tuple", "str",
        "int", "float", "bool", "bytes", "bytearray", "memoryview", "complex",
        "abs", "all", "any", "bin", "chr", "dir", "divmod", "eval", "exec",
        "format", "getattr", "globals", "hasattr", "hash", "help", "hex",
        "id", "input", "iter", "locals", "max", "min", "next", "object",
        "oct", "open", "ord", "pow", "repr", "round", "setattr", "slice",
        "staticmethod", "classmethod", "property", "super", "vars", "zip",
        "Exception", "ValueError", "TypeError", "KeyError", "IndexError",
        "AttributeError", "RuntimeError", "StopIteration", "NotImplementedError"
    )

    private val stringPrefixes = setOf("r", "b", "u", "f", "rb", "br", "rf", "fr")

    /** 返回 [pos] 起合法字符串前缀长度（后随引号才算），否则 0 */
    private fun stringPrefixLengthAt(code: String, pos: Int): Int {
        for (len in 2 downTo 1) {
            if (pos + len >= code.length) continue
            if (code.substring(pos, pos + len).lowercase() in stringPrefixes) {
                val next = code[pos + len]
                if (next == '"' || next == '\'') return len
            }
        }
        return 0
    }

    private val threeCharOps = setOf("**=", "//=", ">>=", "<<=")
    private val twoCharOps = setOf(
        "==", "!=", "<=", ">=", "**", "//", "+=", "-=", "*=", "/=", "%=",
        "&=", "|=", "^=", "->", "<<", ">>"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 单行注释
            if (c == '#') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }

            // 装饰器
            if (c == '@') {
                val start = pos
                pos++
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_' || code[pos] == '.')) pos++
                tokens.add(CodeToken(TokenType.DECORATOR, start until pos, code))
                continue
            }

            // 三引号字符串（双引号）
            if (pos + 2 < code.length && code[pos] == '"' && code[pos + 1] == '"' && code[pos + 2] == '"') {
                val start = pos
                pos += 3
                while (pos + 2 < code.length && !(code[pos] == '"' && code[pos + 1] == '"' && code[pos + 2] == '"')) pos++
            if (pos + 2 < code.length) {
                pos += 3
            } else {
                pos = code.length
            }
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 三引号字符串（单引号）
            if (pos + 2 < code.length && code[pos] == '\'' && code[pos + 1] == '\'' && code[pos + 2] == '\'') {
                val start = pos
                pos += 3
                while (pos + 2 < code.length && !(code[pos] == '\'' && code[pos + 1] == '\'' && code[pos + 2] == '\'')) pos++
            if (pos + 2 < code.length) {
                pos += 3
            } else {
                pos = code.length
            }
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 字符串前缀（f/r/b/u 及 rb、br、rf、fr 组合，支持三引号）
            val prefixLen = stringPrefixLengthAt(code, pos)
            if (prefixLen > 0) {
                val start = pos
                pos += prefixLen
                val quote = code[pos]
                if (pos + 2 < code.length && code[pos + 1] == quote && code[pos + 2] == quote) {
                    pos += 3
                    while (pos + 2 < code.length &&
                        !(code[pos] == quote && code[pos + 1] == quote && code[pos + 2] == quote)
                    ) {
                        if (code[pos] == '\\' && pos + 1 < code.length) pos++
                        pos++
                    }
                    if (pos + 2 < code.length) pos += 3 else pos = code.length
                } else {
                    pos++
                    while (pos < code.length && code[pos] != quote && code[pos] != '\n') {
                        if (code[pos] == '\\' && pos + 1 < code.length) pos++
                        pos++
                    }
                    if (pos < code.length && code[pos] == quote) pos++
                }
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 双引号字符串
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"' && code[pos] != '\n') {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++
                    pos++
                }
                if (pos < code.length && code[pos] == '"') pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 单引号字符串
            if (c == '\'') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '\'' && code[pos] != '\n') {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++
                    pos++
                }
                if (pos < code.length && code[pos] == '\'') pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 数字字面量
            if (c.isDigit()) {
                val start = pos
                if (c == '0' && pos + 1 < code.length && (code[pos + 1] == 'x' || code[pos + 1] == 'X')) {
                    pos += 2
                    while (pos < code.length && (code[pos].isDigit() || code[pos] in 'a'..'f' || code[pos] in 'A'..'F' || code[pos] == '_')) pos++
                } else if (c == '0' && pos + 1 < code.length && (code[pos + 1] == 'b' || code[pos + 1] == 'B')) {
                    pos += 2
                    while (pos < code.length && (code[pos] == '0' || code[pos] == '1' || code[pos] == '_')) pos++
                } else if (c == '0' && pos + 1 < code.length && (code[pos + 1] == 'o' || code[pos + 1] == 'O')) {
                    pos += 2
                    while (pos < code.length && (code[pos] in '0'..'7' || code[pos] == '_')) pos++
                } else {
                    while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                    if (pos < code.length && code[pos] == '.') {
                        pos++
                        while (pos < code.length && code[pos].isDigit()) pos++
                    }
                    if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                        pos++
                        if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                        while (pos < code.length && code[pos].isDigit()) pos++
                    }
                    if (pos < code.length && (code[pos] == 'j' || code[pos] == 'J')) pos++
                }
                tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                continue
            }

            // 标识符、关键字、内置函数
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                val word = code.substring(start, pos)
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtins -> TokenType.BUILTIN
                    word[0].isUpperCase() -> TokenType.TYPE
                    pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, start until pos, code))
                continue
            }

            // 运算符
            if (c in "+-*/%=!<>&|^~") {
                val start = pos
                val c1 = if (pos + 1 < code.length) code[pos + 1] else ' '
                val c2 = if (pos + 2 < code.length) code[pos + 2] else ' '
                when {
                    "$c$c1$c2" in threeCharOps -> pos += 3
                    "$c$c1" in twoCharOps -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, start until pos, code))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.:") {
                tokens.add(CodeToken(TokenType.PUNCTUATION, pos until pos + 1, code))
                pos++
                continue
            }

            // 其他字符：连续空白合并为单个 PLAIN，其余逐字符兜底
            if (c.isWhitespace()) {
                val start = pos
                pos = whitespaceEnd(code, pos)
                tokens.add(CodeToken(TokenType.PLAIN, start until pos, code))
                continue
            }
            tokens.add(CodeToken(TokenType.PLAIN, pos until pos + 1, code))
            pos++
        }

        return tokens
    }

    override fun isExtendableToken(token: CodeToken): Boolean {
        if (token.type != TokenType.STRING) return super.isExtendableToken(token)
        val t = token.text
        if (t.length >= 2 && t[0] != '"' && t[0] != '\'' && t[0] != '`') {
            // 带前缀字符串（f/r/b/u 及组合）：按前缀后的引号判定
            val q = t.indexOfFirst { it == '"' || it == '\'' }
            if (q in 1..2) {
                return if (t.length > q + 1) t.last() != t[q] else true
            }
        }
        return super.isExtendableToken(token)
    }
}
