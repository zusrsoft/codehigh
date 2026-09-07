package com.hrm.codehigh.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

/**
 * Android：构造收 ClipData。
 * 签名来源：本地缓存 androidx.compose.ui ui-android 1.10.5 AAR javap 反编译。
 */
internal actual fun textClipEntry(text: String): ClipEntry =
    ClipEntry(ClipData.newPlainText("code", text))
