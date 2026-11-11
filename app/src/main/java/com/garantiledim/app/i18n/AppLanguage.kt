package com.garantiledim.app.i18n

import java.util.Locale

/** Uygulamanın desteklediği diller. */
enum class AppLanguage(val locale: Locale) {
    TR(Locale.forLanguageTag("tr-TR")),
    EN(Locale.forLanguageTag("en-US"));

    val strings: Strings
        get() = when (this) {
            TR -> TrStrings
            EN -> EnStrings
        }

    companion object {
        /** Telefon dili Türkçeyse Türkçe, diğer tüm dillerde İngilizce. */
        fun forLocale(locale: Locale): AppLanguage = if (locale.language == "tr") TR else EN
    }
}

/** Kullanıcının Profil & Ayarlar'daki dil seçimi. */
enum class LanguageChoice {
    SYSTEM,
    TR,
    EN;

    fun resolve(systemLocale: Locale): AppLanguage = when (this) {
        SYSTEM -> AppLanguage.forLocale(systemLocale)
        TR -> AppLanguage.TR
        EN -> AppLanguage.EN
    }
}
