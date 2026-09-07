package com.hrm.codehigh.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

/**
 * iOS：actual 构造为无参 ClipEntry()，无法直接携带内容，
 * 使用 Companion.withPlainText 文本工厂（该工厂标注 @ExperimentalComposeUiApi，故此处 OptIn）。
 * 签名来源：本地缓存 ui-uikitArm64Main-1.10.3.klib 经 klib dump-metadata-signatures
 * 确认（<init>(){} 与 withPlainText(kotlin.String){}）。
 */
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun textClipEntry(text: String): ClipEntry =
    ClipEntry.withPlainText(text)
