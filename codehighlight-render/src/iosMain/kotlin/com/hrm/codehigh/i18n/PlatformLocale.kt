package com.hrm.codehigh.i18n

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

internal actual fun platformLanguageTag(): String {
    // 优先用户首选语言，回退系统区域
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
    return preferred ?: NSLocale.currentLocale.languageCode
}
