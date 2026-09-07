package com.hrm.codehigh.i18n

import androidx.compose.runtime.compositionLocalOf

/** 宿主注入自定义代码块文案的入口 */
val LocalCodeBlockStrings = compositionLocalOf<CodeBlockStrings> { DefaultCodeBlockStrings }
