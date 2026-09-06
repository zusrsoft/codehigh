package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * YAML 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object YamlLexer : BaseLexer() {

    private val boolValues = setOf("true", "false", "yes", "no", "on", "off", "True", "False", "Yes", "No", "On", "Off", "TRUE", "FALSE", "YES", "NO", "ON", "OFF")
    private val nullValues = setOf("null", "~", "Null", "NULL")

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 文档分隔符 --- 或 ...
            if ((code.startsWith("---", pos) || code.startsWith("...", pos)) &&
                (pos == 0 || code[pos - 1] == '\n')) {
                val start = pos
                pos += 3
                tokens.add(CodeToken(TokenType.KEYWORD, start until pos, code))
                continue
            }

            // 注释
            if (c == '#') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }

            // 锚点 &anchor
            if (c == '&') {
                val start = pos
                pos++
                while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.ANNOTATION, start until pos, code))
                continue
            }

            // 引用 *alias
            if (c == '*') {
                val start = pos
                pos++
                while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.VARIABLE, start until pos, code))
                continue
            }

            // 标签 !tag
            if (c == '!') {
                val start = pos
                pos++
                while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.ANNOTATION, start until pos, code))
                continue
            }

            // 双引号字符串
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"') {
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
                while (pos < code.length && !(code[pos] == '\'' && (pos + 1 >= code.length || code[pos + 1] != '\''))) {
                    if (code[pos] == '\'' && pos + 1 < code.length && code[pos + 1] == '\'') {
                        pos += 2
                    } else {
                        pos++
                    }
                }
                if (pos < code.length && code[pos] == '\'') pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 数字
            if (c.isDigit() || (c == '-' && pos + 1 < code.length && code[pos + 1].isDigit())) {
                val start = pos
                if (c == '-') pos++
                while (pos < code.length && code[pos].isDigit()) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                    pos++
                    if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                continue
            }

            // 标识符（键名、布尔值、null）
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_' || code[pos] == '-')) pos++
                val word = code.substring(start, pos)
                // 检查是否是键（后面跟冒号）
                var lookAhead = pos
                while (lookAhead < code.length && code[lookAhead] == ' ') lookAhead++
                val isKey = lookAhead < code.length && code[lookAhead] == ':'
                val type = when {
                    word in boolValues -> TokenType.BUILTIN
                    word in nullValues -> TokenType.BUILTIN
                    isKey -> TokenType.KEYWORD
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, start until pos, code))
                continue
            }

            // 结构符号
            if (c in "{}[]|>:,-") {
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
