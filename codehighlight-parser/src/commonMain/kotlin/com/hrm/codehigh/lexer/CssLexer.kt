package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * CSS 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object CssLexer : BaseLexer() {

    private val atRules = setOf(
        "media", "keyframes", "import", "font-face", "supports", "charset",
        "namespace", "page", "layer", "container", "property", "counter-style"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 注释
            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                val start = pos
                pos += 2
                while (pos + 1 < code.length && !(code[pos] == '*' && code[pos + 1] == '/')) pos++
                if (pos + 1 < code.length) {
                    pos += 2
                } else {
                    pos = code.length
                }
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }

            // At 规则 @media, @keyframes 等
            if (c == '@') {
                val start = pos
                pos++
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '-')) pos++
                tokens.add(CodeToken(TokenType.KEYWORD, start until pos, code))
                continue
            }

            // 字符串
            if (c == '"' || c == '\'') {
                val quote = c
                val start = pos
                pos++
                while (pos < code.length && code[pos] != quote) {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++
                    pos++
                }
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 颜色值 #rrggbb
            if (c == '#') {
                val start = pos
                pos++
                while (pos < code.length && (code[pos].isDigit() || code[pos] in 'a'..'f' || code[pos] in 'A'..'F')) pos++
                if (pos > start + 1) {
                    tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                } else {
                    // 可能是 ID 选择器
                    while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '-' || code[pos] == '_')) pos++
                    tokens.add(CodeToken(TokenType.FUNCTION, start until pos, code))
                }
                continue
            }

            // 数字（包括单位）
            if (c.isDigit() || (c == '.' && pos + 1 < code.length && code[pos + 1].isDigit()) ||
                (c == '-' && pos + 1 < code.length && (code[pos + 1].isDigit() || code[pos + 1] == '.'))) {
                val start = pos
                if (c == '-') pos++
                while (pos < code.length && code[pos].isDigit()) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                // 单位
                while (pos < code.length && (code[pos].isLetter() || code[pos] == '%')) pos++
                tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                continue
            }

            // 标识符（属性名、选择器、值）
            if (c.isLetter() || c == '_' || c == '-') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '-' || code[pos] == '_')) pos++
                val word = code.substring(start, pos)
                // 判断是否是属性名（后面跟冒号）
                var lookAhead = pos
                while (lookAhead < code.length && code[lookAhead] == ' ') lookAhead++
                val isProperty = lookAhead < code.length && code[lookAhead] == ':'
                val type = when {
                    isProperty -> TokenType.IDENTIFIER
                    word.startsWith("--") -> TokenType.VARIABLE // CSS 自定义属性
                    else -> TokenType.PLAIN
                }
                tokens.add(CodeToken(type, start until pos, code))
                continue
            }

            // 伪类/伪元素选择器 :hover, ::before
            if (c == ':') {
                val start = pos
                pos++
                if (pos < code.length && code[pos] == ':') pos++ // ::
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '-')) pos++
                if (pos > start + 1) {
                    tokens.add(CodeToken(TokenType.KEYWORD, start until pos, code))
                } else {
                    tokens.add(CodeToken(TokenType.PUNCTUATION, start until start + 1, code))
                }
                continue
            }

            // 类选择器 .class
            if (c == '.') {
                val start = pos
                pos++
                if (pos < code.length && (code[pos].isLetter() || code[pos] == '_' || code[pos] == '-')) {
                    while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '-' || code[pos] == '_')) pos++
                    tokens.add(CodeToken(TokenType.FUNCTION, start until pos, code))
                } else {
                    tokens.add(CodeToken(TokenType.PUNCTUATION, start until start + 1, code))
                }
                continue
            }

            // 属性选择器 [attr]
            if (c == '[') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != ']') pos++
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.FUNCTION, start until pos, code))
                continue
            }

            // 运算符
            if (c in "=!<>~|^$*+") {
                val start = pos
                val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
                when {
                    twoChar in setOf("~=", "|=", "^=", "$=", "*=", "!=") -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, start until pos, code))
                continue
            }

            // 标点符号
            if (c in "{}();,>+~") {
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
}
