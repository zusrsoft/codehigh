package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

internal object DiffLexer : BaseLexer() {
    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()

        val tokens = mutableListOf<CodeToken>()
        var offset = 0
        val lines = code.split("\n")

        lines.forEachIndexed { index, line ->
            val lineLength = line.length
            val rangeEnd = offset + lineLength

            if (line.isNotEmpty()) {
                when {
                    line.startsWith("diff ") || line.startsWith("index ") -> {
                        tokens.add(CodeToken(TokenType.ANNOTATION, offset until rangeEnd, code))
                    }
                    line.startsWith("@@") -> {
                        tokens.add(CodeToken(TokenType.FUNCTION, offset until rangeEnd, code))
                    }
                    line.startsWith("+++") || line.startsWith("---") -> {
                        tokens.add(CodeToken(TokenType.TYPE, offset until rangeEnd, code))
                    }
                    line.startsWith("+") || line.startsWith("-") -> {
                        tokens.add(CodeToken(TokenType.OPERATOR, offset until offset + 1, code))
                        if (lineLength > 1) {
                            tokens.add(CodeToken(TokenType.PLAIN, offset + 1 until rangeEnd, code))
                        }
                    }
                    else -> {
                        tokens.add(CodeToken(TokenType.PLAIN, offset until rangeEnd, code))
                    }
                }
            }

            if (index < lines.lastIndex) {
                tokens.add(CodeToken(TokenType.PLAIN, rangeEnd until rangeEnd + 1, code))
                offset = rangeEnd + 1
            } else {
                offset = rangeEnd
            }
        }

        return tokens
    }
}
