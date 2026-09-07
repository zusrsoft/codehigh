package com.hrm.codehigh.theme

import androidx.compose.ui.graphics.Color
import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class CodeThemeTest {

    private val allThemes = listOf(OneDarkProTheme, GithubLightTheme, DraculaProTheme, SolarizedLightTheme)

    @Test
    fun should_returnColor_when_validTokenType() {
        val theme = OneDarkProTheme
        val color = theme.colorFor(TokenType.KEYWORD)
        assertNotNull(color)
        assertNotEquals(Color.Unspecified, color)
    }

    @Test
    fun should_coverAllTokenTypes_when_builtinThemes() {
        for (theme in allThemes) {
            for (type in TokenType.entries) {
                assertNotEquals(Color.Unspecified, theme.colorFor(type), "${theme::class.simpleName} $type")
            }
        }
    }

    @Test
    fun should_fallbackToPlainColor_when_themeThrowsForType() {
        val plain = OneDarkProTheme.colorFor(TokenType.PLAIN)
        val broken = object : CodeTheme by OneDarkProTheme {
            override fun colorFor(type: TokenType): Color =
                if (type == TokenType.KEYWORD) error("not implemented") else OneDarkProTheme.colorFor(type)
        }
        assertEquals(plain, broken.safeColorFor(TokenType.KEYWORD))
    }

    @Test
    fun should_provideDiffColors_when_builtinThemes() {
        for (theme in allThemes) {
            assertNotEquals(Color.Unspecified, theme.diffMarkerColor(CodeLineKind.DIFF_ADDED))
            assertNotEquals(Color.Unspecified, theme.diffMarkerBackground(CodeLineKind.DIFF_REMOVED))
            assertNotEquals(Color.Unspecified, theme.diffTextColor(CodeLineKind.DIFF_META_HUNK))
        }
    }

    @Test
    fun should_beDarkOrLight_when_builtinThemes() {
        assertEquals(true, OneDarkProTheme.isDark)
        assertEquals(true, DraculaProTheme.isDark)
        assertEquals(false, GithubLightTheme.isDark)
        assertEquals(false, SolarizedLightTheme.isDark)
    }

    @Test
    fun should_provideLineBackgroundColors_when_themeUsedForAdvancedRendering() {
        val theme = OneDarkProTheme
        assertNotEquals(Color.Unspecified, theme.highlightedLineBackground)
        assertNotEquals(Color.Unspecified, theme.diffAddedLineBackground)
        assertNotEquals(Color.Unspecified, theme.diffRemovedLineBackground)
        assertNotEquals(Color.Unspecified, theme.diffMetaLineBackground)
    }
}
