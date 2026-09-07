package com.hrm.codehigh.renderer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.theme.OneDarkProTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightedStringTest {

    @Test
    fun should_mergeAdjacentSpans_when_sameStyle() {
        val src = "aaa bbb"
        val tokens = listOf(
            CodeToken(TokenType.PLAIN, 0 until 3, src),
            CodeToken(TokenType.PLAIN, 3 until 7, src), // 含空格，同为 PLAIN
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(src, s.text)
        assertEquals(1, s.spanStyles.size, "相邻同样式应合并为 1 个 span")
    }

    @Test
    fun should_keepDistinctSpans_when_styleChanges() {
        val src = "fun x"
        val tokens = listOf(
            CodeToken(TokenType.KEYWORD, 0 until 3, src),
            CodeToken(TokenType.PLAIN, 3 until 5, src),
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(2, s.spanStyles.size)
    }

    @Test
    fun should_reuseEqualStyle_when_typeRepeats() {
        val src = "a b c"
        val tokens = listOf(
            CodeToken(TokenType.IDENTIFIER, 0 until 1, src),
            CodeToken(TokenType.PLAIN, 1 until 3, src),
            CodeToken(TokenType.IDENTIFIER, 3 until 5, src),
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(3, s.spanStyles.size)
        // OneDarkPro 的 IDENTIFIER 与 PLAIN 同色同样式 → 3 段 span 的 item 应相等（复用缓存实例）
        assertEquals(s.spanStyles[0].item, s.spanStyles[2].item)
    }

    @Test
    fun should_coverAllChars_when_spansMerged() {
        val src = "val x = 1 // note"
        val tokens = com.hrm.codehigh.lexer.LanguageRegistry.getOrPlain("kotlin").tokenize(src)
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(src, s.text)
        // 合并后 span 数应显著少于 token 数（连续同色 token 合并）
        assertTrue(s.spanStyles.size <= tokens.size, "spans=${s.spanStyles.size} tokens=${tokens.size}")
    }
}
