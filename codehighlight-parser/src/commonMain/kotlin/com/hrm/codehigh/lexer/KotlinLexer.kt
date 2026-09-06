package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * Kotlin 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object KotlinLexer : BaseLexer() {

    private val keywords = setOf(
        "fun", "class", "val", "var", "if", "else", "when", "for", "while", "do",
        "return", "object", "interface", "data", "sealed", "suspend", "companion",
        "override", "internal", "private", "public", "protected", "abstract",
        "open", "final", "import", "package", "as", "in", "is", "!is", "!in",
        "null", "true", "false", "this", "super", "new", "throw", "try", "catch",
        "finally", "by", "where", "init", "constructor", "get", "set", "field",
        "it", "crossinline", "inline", "noinline", "reified", "vararg", "lateinit",
        "external", "expect", "actual", "typealias", "enum", "annotation", "break",
        "continue", "typeof", "dynamic"
    )

    private val builtinTypes = setOf(
        "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char",
        "String", "Unit", "Any", "Nothing", "Array", "List", "MutableList",
        "Map", "MutableMap", "Set", "MutableSet", "Pair", "Triple",
        "IntArray", "LongArray", "FloatArray", "DoubleArray", "BooleanArray",
        "CharArray", "ByteArray", "ShortArray", "Sequence", "Flow"
    )

    private val threeCharOps = setOf("..<")
    private val twoCharOps = setOf(
        "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=",
        "%=", "->", "=>", "::", "?.", "..", "?:"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 单行注释
            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '/') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }

            // 多行注释（Kotlin 支持嵌套）
            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                val start = pos
                pos += 2
                var depth = 1
                while (pos < code.length && depth > 0) {
                    if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                        depth++; pos += 2
                    } else if (pos + 1 < code.length && code[pos] == '*' && code[pos + 1] == '/') {
                        depth--; pos += 2
                    } else {
                        pos++
                    }
                }
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }

            // 注解
            if (c == '@') {
                val start = pos
                pos++
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                tokens.add(CodeToken(TokenType.ANNOTATION, start until pos, code))
                continue
            }

            // 三引号字符串
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

            // 双引号字符串
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"' && code[pos] != '\n') {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++ // 跳过转义字符
                    pos++
                }
                if (pos < code.length && code[pos] == '"') pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 字符字面量
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
                } else {
                    while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                    // 小数点仅在后继不是 '.' 时吸收，避免吞掉范围运算符 .. / ..< 的首点
                    if (pos < code.length && code[pos] == '.' && (pos + 1 >= code.length || code[pos + 1] != '.')) {
                        pos++
                        while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                    }
                    if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                        pos++
                        if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                        while (pos < code.length && code[pos].isDigit()) pos++
                    }
                }
                // 长整型后缀
                if (pos < code.length && (code[pos] == 'L' || code[pos] == 'f' || code[pos] == 'F')) pos++
                tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                continue
            }

            // 标识符、关键字、类型
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                val word = code.substring(start, pos)
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtinTypes -> TokenType.TYPE
                    word[0].isUpperCase() -> TokenType.TYPE
                    pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, start until pos, code))
                continue
            }

            // 运算符（. 纳入以识别 .. 与 ..<；单 . 作为成员访问标点回退）
            if (c in "+-*/%=!<>&|^~?:.") {
                val start = pos
                val c1 = if (pos + 1 < code.length) code[pos + 1] else ' '
                val c2 = if (pos + 2 < code.length) code[pos + 2] else ' '
                when {
                    "$c$c1$c2" in threeCharOps -> pos += 3
                    "$c$c1" in twoCharOps -> pos += 2
                    c == '.' -> {
                        tokens.add(CodeToken(TokenType.PUNCTUATION, start until start + 1, code))
                        pos++
                        continue
                    }
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, start until pos, code))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.") {
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
