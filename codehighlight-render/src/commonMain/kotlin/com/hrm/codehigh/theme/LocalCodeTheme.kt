package com.hrm.codehigh.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal，支持全局主题注入。
 * 默认使用 OneDarkProTheme 暗色主题。
 * 对外公开，供调用方注入自定义主题。
 * 主题可随系统暗色模式动态切换，读取处正确追踪依赖。
 */
val LocalCodeTheme = compositionLocalOf<CodeTheme> {
    OneDarkProTheme
}
