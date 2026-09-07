package com.hrm.codehigh.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.hrm.codehigh.ast.TokenType

/**
 * 代码主题接口，定义代码高亮的颜色方案。
 * 对外公开，支持自定义主题实现。
 * 实现方必须保持无状态/不可变——Compose 编译器信任 @Immutable 承诺，可变实现会导致重组缺失。
 */
@Immutable
interface CodeTheme {
    /** 按 Token 类型返回对应颜色 */
    fun colorFor(type: TokenType): Color

    /** 代码块背景色 */
    val background: Color

    /** 是否为暗色主题 */
    val isDark: Boolean

    val highlightedLineBackground: Color
        get() = if (isDark) Color(0x22FFFFFF) else Color(0x12000000)

    val diffAddedLineBackground: Color
        get() = if (isDark) Color(0x3340A15F) else Color(0x2234D058)

    val diffRemovedLineBackground: Color
        get() = if (isDark) Color(0x33A84C57) else Color(0x22FF6B6B)

    val diffMetaLineBackground: Color
        get() = if (isDark) Color(0x224B5563) else Color(0x140A66A1)

    /** diff 标记（+/-）前景色 */
    fun diffMarkerColor(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_ADDED -> if (isDark) Color(0xFF9BE9A8) else Color(0xFF1F7A38)
        CodeLineKind.DIFF_REMOVED -> if (isDark) Color(0xFFFFA8B5) else Color(0xFFB42318)
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFF9CDCFE) else Color(0xFF0958D9)
        else -> colorFor(TokenType.PLAIN)
    }

    /** diff 标记背景色 */
    fun diffMarkerBackground(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_ADDED -> if (isDark) Color(0xFF224D35) else Color(0xFFD9F5E0)
        CodeLineKind.DIFF_REMOVED -> if (isDark) Color(0xFF5A2730) else Color(0xFFFADADD)
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFF1F4B70) else Color(0xFFDCEEFF)
        else -> Color.Transparent
    }

    /** diff 元信息行正文颜色 */
    fun diffTextColor(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFFBFE3FF) else Color(0xFF0B4F8A)
        else -> colorFor(TokenType.PLAIN)
    }
}

/**
 * 安全获取颜色，缺失类型时回退到 PLAIN 颜色。
 * 标记为 internal，仅供模块内部使用。
 */
internal fun CodeTheme.safeColorFor(type: TokenType): Color {
    return try {
        colorFor(type)
    } catch (_: Exception) {
        colorFor(TokenType.PLAIN)
    }
}
