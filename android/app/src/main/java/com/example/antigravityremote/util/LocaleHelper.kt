package com.example.antigravityremote.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    const val LANGUAGE_SYSTEM = ""
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_THAI = "th"

    fun setLanguage(languageCode: String) {
        val appLocale = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLanguage(): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        return if (!currentLocales.isEmpty) {
            currentLocales[0]?.language ?: LANGUAGE_SYSTEM
        } else {
            LANGUAGE_SYSTEM
        }
    }
}

