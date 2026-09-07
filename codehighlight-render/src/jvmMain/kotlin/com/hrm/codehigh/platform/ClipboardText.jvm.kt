package com.hrm.codehigh.platform

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.datatransfer.StringSelection

/**
 * JVM/Desktop：构造为 ClipEntry(nativeClipEntry: Any)，内部以 as? Transferable 取值，
 * 传入 StringSelection（Transferable 实现）才能落到系统剪贴板；直接传其他类型会被置空。
 * 签名来源：本地缓存 ui-desktop-1.10.3.jar javap 反编译。
 */
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun textClipEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))
