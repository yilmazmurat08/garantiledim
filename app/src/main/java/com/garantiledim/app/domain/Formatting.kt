package com.garantiledim.app.domain

import java.time.LocalDate
import java.util.Locale

val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

/** Cihaz dilinden bağımsız Türkçe tarih biçimleri. */
object TrDates {
    private val months = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )
    private val monthsShort = listOf(
        "Oca", "Şub", "Mar", "Nis", "May", "Haz",
        "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara",
    )
    private val weekdays = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar",
    )

    /** 5 Eki 2026 */
    fun short(date: LocalDate): String =
        "${date.dayOfMonth} ${monthsShort[date.monthValue - 1]} ${date.year}"

    /** 5 Ekim 2026, Pazartesi */
    fun long(date: LocalDate): String =
        "${date.dayOfMonth} ${months[date.monthValue - 1]} ${date.year}, ${weekday(date)}"

    /** 05.10.2026 */
    fun numeric(date: LocalDate): String =
        String.format(Locale.ROOT, "%02d.%02d.%04d", date.dayOfMonth, date.monthValue, date.year)

    fun weekday(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]

    /** EKİ */
    fun monthShortUpper(date: LocalDate): String =
        monthsShort[date.monthValue - 1].uppercase(TURKISH)
}

/** 1,2 MB · 350 KB · 900 B */
fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(TURKISH, "%.1f MB", bytes / (1024.0 * 1024.0))
}
