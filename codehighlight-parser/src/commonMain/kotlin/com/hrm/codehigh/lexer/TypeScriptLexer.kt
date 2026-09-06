package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * TypeScript 词法分析器，继承 JavaScript 全部规则并扩展类型系统。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object TypeScriptLexer : BaseLexer() {

    private val keywords = setOf(
        // JavaScript 关键字
        "function", "class", "const", "let", "var", "if", "else", "for", "while",
        "return", "import", "export", "default", "async", "await", "new", "this",
        "typeof", "instanceof", "try", "catch", "throw", "finally", "switch",
        "case", "break", "continue", "do", "in", "of", "delete", "void",
        "yield", "from", "as", "extends", "super", "static", "get", "set",
        "debugger", "with",
        // TypeScript 扩展关键字
        "interface", "type", "enum", "namespace", "declare", "readonly", "abstract",
        "implements", "module", "require", "keyof", "infer", "never", "unknown",
        "any", "is", "asserts", "override", "satisfies", "using", "accessor"
    )

    private val builtins = setOf(
        "console", "window", "document", "undefined", "null", "true", "false",
        "NaN", "Infinity", "Math", "JSON", "Date", "Array", "Object", "String",
        "Number", "Boolean", "RegExp", "Error", "Promise", "Map", "Set",
        "WeakMap", "WeakSet", "Symbol", "Proxy", "Reflect", "globalThis",
        "setTimeout", "setInterval", "clearTimeout", "clearInterval",
        "parseInt", "parseFloat", "isNaN", "isFinite",
        "fetch", "alert", "confirm", "prompt", "require", "module", "exports",
        "process", "__dirname", "__filename"
    )

    private val builtinTypes = setOf(
        "string", "number", "boolean", "object", "symbol", "bigint", "never",
        "unknown", "any", "void", "null", "undefined",
        "Array", "Promise", "Record", "Partial", "Required", "Readonly",
        "Pick", "Omit", "Exclude", "Extract", "NonNullable", "ReturnType",
        "InstanceType", "Parameters", "ConstructorParameters", "Awaited",
        "Uppercase", "Lowercase", "Capitalize", "Uncapitalize"
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

            // 多行注释
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

            // 装饰器
            if (c == '@') {
                val start = pos
                pos++
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                tokens.add(CodeToken(TokenType.DECORATOR, start until pos, code))
                continue
            }

            // 模板字符串
            if (c == '`') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '`') {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++
                    pos++
                }
                if (pos < code.length && code[pos] == '`') pos++
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
                    if (pos < code.length && code[pos] == 'n') pos++ // BigInt
                }
                tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                continue
            }

            // 标识符、关键字、内置
            if (c.isLetter() || c == '_' || c == '$') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_' || code[pos] == '$')) pos++
                val word = code.substring(start, pos)
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtins -> TokenType.BUILTIN
                    word in builtinTypes -> TokenType.TYPE
                    word[0].isUpperCase() -> TokenType.TYPE
                    pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, start until pos, code))
                continue
            }

            // 运算符
            if (c in "+-*/%=!<>&|^~?:") {
                val start = pos
                val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
                val threeChar = if (pos + 2 < code.length) code.substring(pos, pos + 3) else ""
                when {
                    threeChar in setOf("===", "!==", "**=", "&&=", "||=", "??=") -> pos += 3
                    twoChar in setOf("==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=", "**", ">>", "<<", "??", "?.", "=>", "->") -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, start until pos, code))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.$") {
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
