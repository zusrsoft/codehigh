package com.hrm.codehigh.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.theme.GithubLightTheme
import com.hrm.codehigh.theme.OneDarkProTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InlineCodeStyleTest {

    @Test
    fun should_embedGithubLikeDefaults_intoInlineStyle() {
        val style = InlineCodeDefaults.style(GithubLightTheme)

        assertEquals(GithubLightTheme, style.theme)
        assertEquals(GithubLightTheme.colorFor(TokenType.PLAIN), style.textStyle.color)
        assertEquals(13.sp, style.textStyle.fontSize)
        assertEquals(20.sp, style.textStyle.lineHeight)
    }

    @Test
    fun should_restoreDarkDefaults_withClearContrast() {
        val style = InlineCodeDefaults.style(OneDarkProTheme)

        assertEquals(OneDarkProTheme, style.theme)
        assertEquals(OneDarkProTheme.colorFor(TokenType.PLAIN), style.textStyle.color)
    }

    @Test
    fun should_adaptColors_when_themeBrightnessDiffers() {
        val dark = InlineCodeDefaults.style(OneDarkProTheme)
        val light = InlineCodeDefaults.style(GithubLightTheme)
        assertTrue(dark.isDarkStyle() && !light.isDarkStyle())
        assertNotEquals(dark.containerColor, light.containerColor)
        assertNotEquals(dark.borderColor, light.borderColor)
        assertTrue(dark.borderWidth > 0.dp && light.borderWidth > 0.dp)
    }

    @Test
    fun should_keepSingleStyleEntry_whenInlineStyleIsCustomized() {
        val baseStyle = InlineCodeDefaults.style(OneDarkProTheme)
        val style = baseStyle.copy(
            textStyle = baseStyle.textStyle.copy(fontSize = 14.sp),
            containerColor = Color.Magenta,
        )

        assertEquals(OneDarkProTheme, style.theme)
        assertEquals(OneDarkProTheme.colorFor(TokenType.PLAIN), style.textStyle.color)
        assertEquals(14.sp, style.textStyle.fontSize)
        assertEquals(Color.Magenta, style.containerColor)
    }

    private fun InlineCodeStyle.isDarkStyle() = theme.isDark
}
