package com.hrm.codehigh.i18n

import kotlinx.browser.window

internal actual fun platformLanguageTag(): String = window.navigator.language
