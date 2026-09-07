package com.hrm.codehigh.i18n

internal actual fun platformLanguageTag(): String = java.util.Locale.getDefault().toLanguageTag()
