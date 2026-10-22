package com.garantiledim.app.domain

import com.garantiledim.app.i18n.Strings
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

const val RETURN_DAYS = 14L
const val WARRANTY_YEARS = 2L

fun Product.autoEndDate(type: DurationType): LocalDate = when (type) {
    DurationType.IADE -> deliveryDate.plusDays(RETURN_DAYS)
    DurationType.GARANTI -> deliveryDate.plusYears(WARRANTY_YEARS)
}

fun Product.deadline(type: DurationType): Deadline? {
    val tracked = when (type) {
        DurationType.IADE -> tracksReturn
        DurationType.GARANTI -> tracksWarranty
    }
    if (!tracked) return null
    val override = when (type) {
        DurationType.IADE -> returnEndOverride
        DurationType.GARANTI -> warrantyEndOverride
    }
    return Deadline(
        type = type,
        startDate = deliveryDate,
        endDate = override ?: autoEndDate(type),
        isManual = override != null,
    )
}

fun Product.deadlines(): List<Deadline> = DurationType.entries.mapNotNull { deadline(it) }

/**
 * Kartlarda gösterilen süre: devam edenlerden bitişi en yakın olanı,
 * hepsi dolmuşsa en son dolanı.
 */
fun Product.primaryDeadline(today: LocalDate): Deadline? {
    val all = deadlines()
    return all.filter { !it.endDate.isBefore(today) }.minByOrNull { it.endDate }
        ?: all.maxByOrNull { it.endDate }
}

/** Takip edilen sürelerin hepsi dolduysa ürün süresi dolmuş sayılır. */
fun Product.isExpired(today: LocalDate): Boolean {
    val all = deadlines()
    return all.isNotEmpty() && all.all { it.endDate.isBefore(today) }
}

fun daysRemaining(endDate: LocalDate, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, endDate)

fun urgencyOf(days: Long): Urgency = when {
    days < 0 -> Urgency.EXPIRED
    days <= 3 -> Urgency.DANGER
    days < 14 -> Urgency.WARNING
    else -> Urgency.SUCCESS
}

/** "2 gün kaldı", "1 yıl 3 ay kaldı", "Bugün son gün", "Süre doldu" (ve İngilizce karşılıkları) */
fun remainingLabel(endDate: LocalDate, today: LocalDate, s: Strings): String {
    val days = daysRemaining(endDate, today)
    val p = Period.between(today, endDate)
    return when {
        days < 0 -> s.expired
        days == 0L -> s.lastDayToday
        else -> s.remaining(days, p.years, p.months)
    }
}

/** Listelerdeki kısa rozet: "2 gün", "2 ay", "Bugün", "Süre doldu" */
fun shortRemainingLabel(endDate: LocalDate, today: LocalDate, s: Strings): String {
    val days = daysRemaining(endDate, today)
    val p = Period.between(today, endDate)
    return when {
        days < 0 -> s.expired
        days == 0L -> s.today
        else -> s.remainingShort(days, p.years, p.months)
    }
}

/** Geçen sürenin oranı (0..1), ilerleme çubuğu için. */
fun elapsedFraction(deadline: Deadline, today: LocalDate): Float {
    val total = ChronoUnit.DAYS.between(deadline.startDate, deadline.endDate)
    if (total <= 0) return 1f
    val elapsed = ChronoUnit.DAYS.between(deadline.startDate, today).coerceIn(0, total)
    return elapsed.toFloat() / total
}

/** "14 günlük iade süresinin 12 günü geçti." gibi açıklama. */
fun elapsedDescription(deadline: Deadline, today: LocalDate, s: Strings): String {
    val manualNote = if (deadline.isManual) s.manualDateNote else ""
    if (deadline.endDate.isBefore(today)) return s.elapsedExpired(deadline.endDate) + manualNote
    if (today.isBefore(deadline.startDate)) return s.elapsedNotStarted(deadline.startDate) + manualNote
    val elapsed = ChronoUnit.DAYS.between(deadline.startDate, today)
    val period = Period.between(deadline.startDate, deadline.endDate)
    val totalYears = period.years.takeIf { period.months == 0 && period.days == 0 && it > 0 }
    val totalDays = ChronoUnit.DAYS.between(deadline.startDate, deadline.endDate)
    return s.elapsedProgress(deadline.type, totalYears, totalDays, elapsed) + manualNote
}
