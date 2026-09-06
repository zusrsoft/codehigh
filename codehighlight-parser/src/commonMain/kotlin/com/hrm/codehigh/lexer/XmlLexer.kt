package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * XML 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object XmlLexer : BaseLexer() {

    override fun tokenize(code: String): List<CodeToken> = tokenizeXml(code, false)
}

/**
 * HTML 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object HtmlLexer : BaseLexer() {

    override fun tokenize(code: String): List<CodeToken> = tokenizeXml(code, true)
}

/**
 * XML/HTML 通用词法分析实现。
 */
private fun tokenizeXml(code: String, isHtml: Boolean): List<CodeToken> {
    if (code.isEmpty()) return emptyList()
    val tokens = mutableListOf<CodeToken>()
    var pos = 0

    while (pos < code.length) {
        val c = code[pos]

        // XML/HTML 注释 <!-- -->
        if (code.startsWith("<!--", pos)) {
            val start = pos
            pos += 4
            while (pos + 2 < code.length && !code.startsWith("-->", pos)) pos++
            if (pos + 2 < code.length) {
                pos += 3
            } else {
                pos = code.length
            }
            tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
            continue
        }

        // CDATA <![CDATA[...]]>
        if (code.startsWith("<![CDATA[", pos)) {
            val start = pos
            pos += 9
            while (pos + 2 < code.length && !code.startsWith("]]>", pos)) pos++
            if (pos + 2 < code.length) {
                pos += 3
            } else {
                pos = code.length
            }
            tokens.add(CodeToken(TokenType.STRING, start until pos, code))
            continue
        }

        // DOCTYPE <!DOCTYPE ...>
        if (code.startsWith("<!DOCTYPE", pos) || code.startsWith("<!doctype", pos)) {
            val start = pos
            while (pos < code.length && code[pos] != '>') pos++
            if (pos < code.length) pos++
            tokens.add(CodeToken(TokenType.ANNOTATION, start until pos, code))
            continue
        }

        // 处理指令 <?...?>
        if (code.startsWith("<?", pos)) {
            val start = pos
            pos += 2
            while (pos + 1 < code.length && !code.startsWith("?>", pos)) pos++
            if (pos + 1 < code.length) {
                pos += 2
            } else {
                pos = code.length
            }
            tokens.add(CodeToken(TokenType.ANNOTATION, start until pos, code))
            continue
        }

        // 标签
        if (c == '<') {
            val start = pos
            pos++
            val isClosing = pos < code.length && code[pos] == '/'
            if (isClosing) pos++

            // 标签名
            val tagNameStart = pos
            while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '>' && code[pos] != '/') pos++
            val tagName = code.substring(tagNameStart, pos)

            if (tagName.isNotEmpty()) {
                tokens.add(CodeToken(TokenType.PUNCTUATION, start until tagNameStart, code))
                tokens.add(CodeToken(TokenType.FUNCTION, tagNameStart until pos, code))
            } else {
                tokens.add(CodeToken(TokenType.PUNCTUATION, start until pos, code))
            }

            // 属性
            while (pos < code.length && code[pos] != '>') {
                // 跳过空白
                if (code[pos].isWhitespace()) {
                    val wsStart = pos
                    while (pos < code.length && code[pos].isWhitespace()) pos++
                    tokens.add(CodeToken(TokenType.PLAIN, wsStart until pos, code))
                    continue
                }

                // 自闭合 />
                if (code[pos] == '/' && pos + 1 < code.length && code[pos + 1] == '>') {
                    tokens.add(CodeToken(TokenType.PUNCTUATION, pos until pos + 2, code))
                    pos += 2
                    break
                }

                // 属性名
                if (code[pos].isLetter() || code[pos] == '_' || code[pos] == ':') {
                    val attrStart = pos
                    while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '=' && code[pos] != '>' && code[pos] != '/') pos++
                    tokens.add(CodeToken(TokenType.IDENTIFIER, attrStart until pos, code))

                    // 跳过空白
                    while (pos < code.length && code[pos] == ' ') pos++

                    // 属性值
                    if (pos < code.length && code[pos] == '=') {
                        tokens.add(CodeToken(TokenType.OPERATOR, pos until pos + 1, code))
                        pos++
                        while (pos < code.length && code[pos] == ' ') pos++
                        if (pos < code.length && (code[pos] == '"' || code[pos] == '\'')) {
                            val quote = code[pos]
                            val valStart = pos
                            pos++
                            while (pos < code.length && code[pos] != quote) pos++
                            if (pos < code.length) pos++
                            tokens.add(CodeToken(TokenType.STRING, valStart until pos, code))
                        }
                    }
                    continue
                }

                // 其他字符
                tokens.add(CodeToken(TokenType.PLAIN, pos until pos + 1, code))
                pos++
            }

            // 闭合 >
            if (pos < code.length && code[pos] == '>') {
                tokens.add(CodeToken(TokenType.PUNCTUATION, pos until pos + 1, code))
                pos++
            }
            continue
        }

        // 文本内容
        val start = pos
        while (pos < code.length && code[pos] != '<') pos++
        if (start < pos) {
            tokens.add(CodeToken(TokenType.PLAIN, start until pos, code))
        }
    }

    return tokens
}
