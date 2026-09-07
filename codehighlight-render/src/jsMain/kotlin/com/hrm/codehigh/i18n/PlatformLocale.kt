package com.hrm.codehigh.i18n

internal actual fun platformLanguageTag(): String = js("window.navigator.language || 'en'") as String
