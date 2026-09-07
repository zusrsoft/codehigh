package com.hrm.codehigh.platform

import androidx.compose.ui.platform.ClipEntry

/**
 * 从纯文本创建平台 ClipEntry。
 * 各平台 actual 构造签名不同（JVM 需 Transferable、Android 需 ClipData、
 * js/wasmJs 需 ClipboardItem 数组、iOS 需 NSData），common 层无统一文本构造，
 * 故以 expect/actual 工厂收口。
 */
internal expect fun textClipEntry(text: String): ClipEntry
