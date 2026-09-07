package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

internal data class ConfigurableLexerSpec(
    val keywords: Set<String>,
    val builtins: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val fixedTokens: Map<String, TokenType> = emptyMap(),
    val lineComments: List<String> = emptyList(),
    val blockComments: List<Pair<String, String>> = emptyList(),
    val blockStrings: List<Pair<String, String>> = emptyList(),
    val stringQuotes: Set<Char> = setOf('"', '\''),
    val tripleStrings: Set<String> = emptySet(),
    val annotationPrefix: Char? = null,
    val variablePrefixes: List<String> = emptyList(),
    val caseInsensitiveWords: Boolean = false,
    val uppercaseIdentifiersAreTypes: Boolean = false,
    val extraWordStartChars: Set<Char> = emptySet(),
    val extraWordChars: Set<Char> = emptySet(),
    val operators: Set<String> = emptySet(),
    val punctuation: Set<Char> = setOf('{', '}', '(', ')', '[', ']', ';', ',', '.', ':')
) {
    // 匹配表预排序惰性缓存：同一 spec 的重复 tokenize 不再重排
    //（sortedByDescending 为稳定排序，与每次现排结果一致）
    val sortedFixedTokens: List<String> by lazy { fixedTokens.keys.sortedByDescending { it.length } }
    val sortedVariablePrefixes: List<String> by lazy { variablePrefixes.sortedByDescending { it.length } }
    val sortedLineComments: List<String> by lazy { lineComments.sortedByDescending { it.length } }
    val sortedBlockComments: List<Pair<String, String>> by lazy { blockComments.sortedByDescending { it.first.length } }
    val sortedBlockStrings: List<Pair<String, String>> by lazy { blockStrings.sortedByDescending { it.first.length } }
    val sortedTripleStrings: List<String> by lazy { tripleStrings.sortedByDescending { it.length } }
    val sortedOperators: List<String> by lazy { operators.sortedByDescending { it.length } }

    // 大小写归一惰性缓存：caseInsensitiveWords 语言（sql/dockerfile）的重复 tokenize 不再重算 lowercase+toSet
    val normalizedKeywords: Set<String> by lazy { normalizeWords(keywords, caseInsensitiveWords) }
    val normalizedBuiltins: Set<String> by lazy { normalizeWords(builtins, caseInsensitiveWords) }
    val normalizedTypes: Set<String> by lazy { normalizeWords(types, caseInsensitiveWords) }
}

internal fun tokenizeWithSpec(code: String, spec: ConfigurableLexerSpec): List<CodeToken> {
    if (code.isEmpty()) return emptyList()

    val tokens = mutableListOf<CodeToken>()
    var pos = 0

    val fixedTokens = spec.sortedFixedTokens
    val variablePrefixes = spec.sortedVariablePrefixes
    val lineComments = spec.sortedLineComments
    val blockComments = spec.sortedBlockComments
    val blockStrings = spec.sortedBlockStrings
    val tripleStrings = spec.sortedTripleStrings
    val operators = spec.sortedOperators

    val keywords = spec.normalizedKeywords
    val builtins = spec.normalizedBuiltins
    val types = spec.normalizedTypes

    while (pos < code.length) {
        val fixedToken = fixedTokens.firstOrNull { code.startsWith(it, pos) }
        if (fixedToken != null) {
            val end = pos + fixedToken.length
            tokens.add(CodeToken(spec.fixedTokens.getValue(fixedToken), pos until end, code))
            pos = end
            continue
        }

        val blockComment = blockComments.firstOrNull { code.startsWith(it.first, pos) }
        if (blockComment != null) {
            val start = pos
            val end = findDelimitedEnd(code, pos + blockComment.first.length, blockComment.second)
            pos = end
            tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
            continue
        }

        val lineComment = lineComments.firstOrNull { code.startsWith(it, pos) }
        if (lineComment != null) {
            val start = pos
            while (pos < code.length && code[pos] != '\n') pos++
            tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
            continue
        }

        val blockString = blockStrings.firstOrNull { code.startsWith(it.first, pos) }
        if (blockString != null) {
            val start = pos
            val end = findDelimitedEnd(code, pos + blockString.first.length, blockString.second)
            pos = end
            tokens.add(CodeToken(TokenType.STRING, start until pos, code))
            continue
        }

        val tripleString = tripleStrings.firstOrNull { code.startsWith(it, pos) }
        if (tripleString != null) {
            val start = pos
            val end = findDelimitedEnd(code, pos + tripleString.length, tripleString)
            pos = end
            tokens.add(CodeToken(TokenType.STRING, start until pos, code))
            continue
        }

        val annotationPrefix = spec.annotationPrefix
        if (annotationPrefix != null && code[pos] == annotationPrefix && pos + 1 < code.length && isWordStart(code[pos + 1], spec)) {
            val start = pos
            pos++
            while (pos < code.length && isWordPart(code[pos], spec)) pos++
            tokens.add(CodeToken(TokenType.ANNOTATION, start until pos, code))
            continue
        }

        val variablePrefix = variablePrefixes.firstOrNull { code.startsWith(it, pos) }
        if (variablePrefix != null) {
            val start = pos
            pos += variablePrefix.length
            when {
                pos < code.length && code[pos] == '{' -> {
                    pos++
                    var depth = 1
                    while (pos < code.length && depth > 0) {
                        when (code[pos]) {
                            '{' -> depth++
                            '}' -> depth--
                        }
                        pos++
                    }
                }
                pos < code.length && isWordStart(code[pos], spec) -> {
                    while (pos < code.length && isWordPart(code[pos], spec)) pos++
                }
                pos < code.length && code[pos].isDigit() -> {
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
            }
            tokens.add(CodeToken(TokenType.VARIABLE, start until pos, code))
            continue
        }

        val current = code[pos]

        if (current in spec.stringQuotes) {
            val start = pos
            val quote = current
            pos++
            while (pos < code.length && code[pos] != quote && code[pos] != '\n') {
                if (code[pos] == '\\' && pos + 1 < code.length) pos++
                pos++
            }
            if (pos < code.length && code[pos] == quote) pos++
            tokens.add(CodeToken(TokenType.STRING, start until pos, code))
            continue
        }

        if (current.isDigit()) {
            val start = pos
            if (current == '0' && pos + 1 < code.length && (code[pos + 1] == 'x' || code[pos + 1] == 'X')) {
                pos += 2
                while (pos < code.length && (code[pos].isDigit() || code[pos] in 'a'..'f' || code[pos] in 'A'..'F' || code[pos] == '_')) pos++
            } else if (current == '0' && pos + 1 < code.length && (code[pos + 1] == 'b' || code[pos + 1] == 'B')) {
                pos += 2
                while (pos < code.length && (code[pos] == '0' || code[pos] == '1' || code[pos] == '_')) pos++
            } else if (current == '0' && pos + 1 < code.length && (code[pos + 1] == 'o' || code[pos + 1] == 'O')) {
                pos += 2
                while (pos < code.length && (code[pos] in '0'..'7' || code[pos] == '_')) pos++
            } else {
                while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                }
                if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                    pos++
                    if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
            }
            tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
            continue
        }

        if (isWordStart(current, spec)) {
            val start = pos
            pos++
            while (pos < code.length && isWordPart(code[pos], spec)) pos++
            val word = code.substring(start, pos)
            val normalized = if (spec.caseInsensitiveWords) word.lowercase() else word
            val type = when {
                normalized in keywords -> TokenType.KEYWORD
                normalized in builtins -> TokenType.BUILTIN
                normalized in types -> TokenType.TYPE
                spec.uppercaseIdentifiersAreTypes && word.firstOrNull()?.isUpperCase() == true -> TokenType.TYPE
                pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                else -> TokenType.IDENTIFIER
            }
            tokens.add(CodeToken(type, start until pos, code))
            continue
        }

        val operator = operators.firstOrNull { code.startsWith(it, pos) }
        if (operator != null) {
            val end = pos + operator.length
            tokens.add(CodeToken(TokenType.OPERATOR, pos until end, code))
            pos = end
            continue
        }

        if (current in spec.punctuation) {
            tokens.add(CodeToken(TokenType.PUNCTUATION, pos until pos + 1, code))
            pos++
            continue
        }

        // 其他字符：连续空白合并为单个 PLAIN，其余逐字符兜底
        if (current.isWhitespace()) {
            val start = pos
            while (pos < code.length && code[pos].isWhitespace()) pos++
            tokens.add(CodeToken(TokenType.PLAIN, start until pos, code))
            continue
        }
        tokens.add(CodeToken(TokenType.PLAIN, pos until pos + 1, code))
        pos++
    }

    return tokens
}

/**
 * 基于 spec 块定界符（块注释/块字符串）判定 Token 是否可扩展（未闭合）。
 * Token 未命中任何定界符起始前缀时返回 null，调用方回退 Lexer 默认实现。
 * 闭合判定与 tokenizeWithSpec 的 findDelimitedEnd 语义对齐：
 * 短于「起始+结束定界符」长度总和的形态必为未闭合 opener。
 *
 * 边界：STRING 分支不查 tripleStrings（现有语言三引号定界均由默认实现覆盖）；
 * 未来若定义非标三引号定界需扩展此函数。
 */
internal fun isExtendableTokenWithSpec(token: CodeToken, spec: ConfigurableLexerSpec): Boolean? {
    val text = token.text
    return when (token.type) {
        TokenType.COMMENT -> extendableDelimited(text, spec.sortedBlockComments)
        TokenType.STRING -> extendableDelimited(text, spec.sortedBlockStrings)
        else -> null
    }
}

private fun extendableDelimited(text: String, pairs: List<Pair<String, String>>): Boolean? {
    val pair = pairs.firstOrNull { text.startsWith(it.first) } ?: return null
    return text.length < pair.first.length + pair.second.length || !text.endsWith(pair.second)
}

private fun normalizeWords(words: Set<String>, caseInsensitive: Boolean): Set<String> {
    return if (caseInsensitive) words.map { it.lowercase() }.toSet() else words
}

private fun findDelimitedEnd(code: String, from: Int, endDelimiter: String): Int {
    val endIndex = code.indexOf(endDelimiter, from)
    return if (endIndex >= 0) endIndex + endDelimiter.length else code.length
}

private fun isWordStart(char: Char, spec: ConfigurableLexerSpec): Boolean {
    return char.isLetter() || char == '_' || char in spec.extraWordStartChars
}

private fun isWordPart(char: Char, spec: ConfigurableLexerSpec): Boolean {
    return char.isLetterOrDigit() || char == '_' || char in spec.extraWordChars || char in spec.extraWordStartChars
}
