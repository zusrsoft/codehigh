package com.hrm.codehigh.i18n

import androidx.compose.runtime.compositionLocalOf

/** 宿主注入自定义代码块文案的入口；注入实例应保持稳定（如 remember 持有），避免每次重组触发全量读取点重组 */
val LocalCodeBlockStrings = compositionLocalOf<CodeBlockStrings> { DefaultCodeBlockStrings }
