package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * Bash/Shell 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object BashLexer : BaseLexer() {

    private val keywords = setOf(
        "if", "then", "else", "elif", "fi", "for", "do", "done", "while", "until",
        "case", "esac", "function", "return", "exit", "break", "continue",
        "local", "export", "readonly", "declare", "typeset", "source", ".",
        "in", "select", "time", "coproc"
    )

    private val builtins = setOf(
        "echo", "cd", "ls", "mkdir", "rm", "cp", "mv", "grep", "sed", "awk",
        "cat", "chmod", "chown", "pwd", "touch", "find", "sort", "uniq",
        "head", "tail", "wc", "cut", "tr", "xargs", "tee", "read", "printf",
        "test", "true", "false", "set", "unset", "shift", "eval", "exec",
        "trap", "wait", "jobs", "kill", "bg", "fg", "type", "which", "alias",
        "unalias", "history", "help", "let", "expr", "getopts", "basename",
        "dirname", "realpath", "readlink", "stat", "file", "env", "printenv"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // Shebang 和注释
            if (c == '#') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }

            // 单引号字符串（字面量，不展开变量）
            if (c == '\'') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '\'') pos++
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 双引号字符串（支持变量展开）
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"') {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++
                    pos++
                }
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 反引号命令替换
            if (c == '`') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '`') {
                    if (code[pos] == '\\' && pos + 1 < code.length) pos++
                    pos++
                }
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }

            // 变量 $VAR, ${VAR}, $1-$9, $@, $#, $?, $$
            if (c == '$') {
                val start = pos
                pos++
                when {
                    pos < code.length && code[pos] == '{' -> {
                        pos++
                        while (pos < code.length && code[pos] != '}') pos++
                        if (pos < code.length) pos++
                    }
                    pos < code.length && code[pos] == '(' -> {
                        pos++
                        var depth = 1
                        while (pos < code.length && depth > 0) {
                            when (code[pos]) {
                                '(' -> depth++
                                ')' -> depth--
                            }
                            pos++
                        }
                    }
                    pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_') -> {
                        while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                    }
                    pos < code.length && code[pos] in "@#?$!-*" -> pos++
                }
                tokens.add(CodeToken(TokenType.VARIABLE, start until pos, code))
                continue
            }

            // 数字
            if (c.isDigit()) {
                val start = pos
                while (pos < code.length && code[pos].isDigit()) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                tokens.add(CodeToken(TokenType.NUMBER, start until pos, code))
                continue
            }

            // 标识符、关键字、内置命令
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_' || code[pos] == '-')) pos++
                val word = code.substring(start, pos)
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtins -> TokenType.BUILTIN
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, start until pos, code))
                continue
            }

            // 运算符
            if (c in "+-*/%=!<>&|^~") {
                val start = pos
                val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
                when {
                    twoChar in setOf("==", "!=", "<=", ">=", "&&", "||", ">>", "<<", "+=", "-=", "*=", "/=") -> pos += 2
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
}
