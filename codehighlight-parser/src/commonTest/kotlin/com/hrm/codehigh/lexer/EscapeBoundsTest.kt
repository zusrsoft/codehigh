package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EscapeBoundsTest {

    /** 反斜杠结尾的未闭合字符串：历史 bug 为 IndexOutOfBoundsException */
    private val trailingBackslashCases = listOf("\"a\\", "'a\\", "\"\\", "'\\", "\"C:\\")

    private val languagesWithQuotes = listOf(
        "kotlin", "java", "python", "javascript", "typescript", "go", "rust",
        "swift", "c", "cpp", "css", "json", "yaml", "bash"
    )

    @Test
    fun should_notThrow_when_trailingBackslashInUnclosedString() {
        for (lang in languagesWithQuotes) {
            for (case in trailingBackslashCases) {
                val tokens = LanguageRegistry.getOrPlain(lang).tokenize(case)
                assertEquals(case, tokens.joinToString("") { it.text }, "lang=$lang")
            }
        }
    }

    @Test
    fun should_mergeWhitespace_when_plainFallbackRuns() {
        val tokens = KotlinLexer.tokenize("fun a() {}\n\n\nval x = 1")
        val whitespaceTokens = tokens.filter { it.type == TokenType.PLAIN && it.text.all { c -> c.isWhitespace() } }
        assertTrue(whitespaceTokens.any { it.text.length > 1 }, "应存在合并后的多字符空白 Token")
    }

    @Test
    fun should_mergeCrlf_when_windowsLineEndings() {
        val tokens = KotlinLexer.tokenize("val a = 1\r\nval b = 2")
        assertTrue(tokens.any { it.type == TokenType.PLAIN && it.text == "\r\n" }, "\\r\\n 应合并为单个 PLAIN Token")
    }
}
