package com.hrm.codehigh.platform

import androidx.compose.ui.platform.ClipEntry

/**
 * wasmJs：actual 构造为 ClipEntry(JsArray&lt;ClipboardItem&gt;)，无法直接从文本构造，
 * 使用 Companion.withPlainText 文本工厂。
 * 签名来源：本地缓存 ui-wasm-js-1.10.3.klib 经 klib dump-metadata-signatures 确认。
 */
internal actual fun textClipEntry(text: String): ClipEntry =
    ClipEntry.withPlainText(text)
