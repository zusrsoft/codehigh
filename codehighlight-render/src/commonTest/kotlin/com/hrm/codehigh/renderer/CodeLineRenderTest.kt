package com.hrm.codehigh.renderer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.theme.CodeLineKind
import com.hrm.codehigh.theme.OneDarkProTheme
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeLineRenderTest {

    @Test
    fun should_markHighlightedLines_when_highlightedLinesProvided() {
        val src = "fun hello()\nprintln(\"ok\")\nreturn"
        val renders = buildLineRenders(
            sourceLines = listOf("fun hello()", "println(\"ok\")", "return"),
            tokens = listOf(
                CodeToken(TokenType.KEYWORD, 0 until 3, src),
                CodeToken(TokenType.PLAIN, 3 until 11, src),
                CodeToken(TokenType.PLAIN, 11 until 12, src),
                CodeToken(TokenType.FUNCTION, 12 until 19, src),
                CodeToken(TokenType.PLAIN, 19 until 25, src),
                CodeToken(TokenType.PLAIN, 25 until 26, src),
                CodeToken(TokenType.KEYWORD, 26 until 32, src)
            ),
            theme = OneDarkProTheme,
            language = "kotlin",
            highlightedLines = setOf(2)
        )

        assertEquals(CodeLineKind.NORMAL, renders[0].kind)
        assertEquals(CodeLineKind.HIGHLIGHTED, renders[1].kind)
        assertEquals(CodeLineKind.NORMAL, renders[2].kind)
    }

    @Test
    fun should_markDiffLines_when_diffLanguageProvided() {
        assertEquals(CodeLineKind.DIFF_META_HEADER, resolveLineKind(0, "diff --git a/a.kt b/a.kt", "diff", emptySet()))
        assertEquals(CodeLineKind.DIFF_META_HUNK, resolveLineKind(1, "@@ -1,2 +1,2 @@", "diff", emptySet()))
        assertEquals(CodeLineKind.DIFF_REMOVED, resolveLineKind(2, "-old line", "diff", emptySet()))
        assertEquals(CodeLineKind.DIFF_ADDED, resolveLineKind(3, "+new line", "diff", emptySet()))
        assertEquals(CodeLineKind.NORMAL, resolveLineKind(4, " unchanged", "diff", emptySet()))
    }
}
