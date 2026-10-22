package com.garantiledim.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Compose ekranlarının kullandığı metinler (MainActivity seçili dile göre sağlar). */
val LocalStrings = staticCompositionLocalOf<Strings> { TrStrings }

/**
 * Dil seçimi. Etkinlik açılırken eşzamanlı okunabilsin diye SharedPreferences'ta tutulur.
 * "Sistem dili" seçiliyse telefon dili Türkçeyse Türkçe, değilse İngilizce kullanılır.
 */
class LanguageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("language", Context.MODE_PRIVATE)

    private val _choice = MutableStateFlow(readChoice())
    val choice: StateFlow<LanguageChoice> = _choice.asStateFlow()

    private val _strings = MutableStateFlow(resolve(_choice.value).strings)

    /** Etkin dilin metinleri. */
    val strings: StateFlow<Strings> = _strings.asStateFlow()

    val current: Strings get() = _strings.value

    fun set(choice: LanguageChoice) {
        prefs.edit().putString(KEY, choice.name).apply()
        _choice.value = choice
        refresh()
    }

    /** Telefon dili değişmiş olabilir ("Sistem dili" seçiliyken); etkinlik her açıldığında çağrılır. */
    fun refresh() {
        _strings.value = resolve(_choice.value).strings
    }

    /** Etkinliğin bağlamını seçili dile çevirir (tarih seçici gibi sistem bileşenleri de bu dilde görünsün). */
    fun wrap(base: Context): Context {
        val locale = resolve(readChoice()).locale
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    private fun readChoice(): LanguageChoice =
        prefs.getString(KEY, null)?.let { name -> LanguageChoice.entries.firstOrNull { it.name == name } }
            ?: LanguageChoice.SYSTEM

    private fun resolve(choice: LanguageChoice): AppLanguage = choice.resolve(systemLocale())

    private fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()

    private companion object {
        const val KEY = "choice"
    }
}
