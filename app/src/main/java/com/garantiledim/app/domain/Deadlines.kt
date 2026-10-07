package com.garantiledim.app.domain

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

/** "2 gün kaldı", "1 yıl 3 ay kaldı", "Bugün son gün", "Süre doldu" */
fun remainingLabel(endDate: LocalDate, today: LocalDate): String {
    val days = daysRemaining(endDate, today)
    return when {
        days < 0 -> "Süre doldu"
        days == 0L -> "Bugün son gün"
        else -> "${remainingAmount(endDate, today)} kaldı"
    }
}

/** Listelerdeki kısa rozet: "2 gün", "2 ay", "Bugün", "Süre doldu" */
fun shortRemainingLabel(endDate: LocalDate, today: LocalDate): String {
    val days = daysRemaining(endDate, today)
    return when {
        days < 0 -> "Süre doldu"
        days == 0L -> "Bugün"
        else -> remainingAmount(endDate, today)
    }
}

private fun remainingAmount(endDate: LocalDate, today: LocalDate): String {
    val days = daysRemaining(endDate, today)
    if (days <= 60) return "$days gün"
    val p = Period.between(today, endDate)
    return when {
        p.years > 0 && p.months > 0 -> "${p.years} yıl ${p.months} ay"
        p.years > 0 -> "${p.years} yıl"
        else -> "${p.months} ay"
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
fun elapsedDescription(deadline: Deadline, today: LocalDate): String {
    val manualNote = if (deadline.isManual) " Tarih elle girildi." else ""
    if (deadline.endDate.isBefore(today)) {
        return "Süre ${TrDates.long(deadline.endDate)} tarihinde doldu.$manualNote"
    }
    if (today.isBefore(deadline.startDate)) {
        return "Süre teslim tarihinde (${TrDates.short(deadline.startDate)}) başlayacak.$manualNote"
    }
    val elapsed = ChronoUnit.DAYS.between(deadline.startDate, today)
    val period = Period.between(deadline.startDate, deadline.endDate)
    val totalText = when {
        period.months == 0 && period.days == 0 && period.years > 0 -> "${period.years} yıllık"
        else -> "${ChronoUnit.DAYS.between(deadline.startDate, deadline.endDate)} günlük"
    }
    val subject = when (deadline.type) {
        DurationType.IADE -> "iade süresinin"
        DurationType.GARANTI -> "garantinin"
    }
    val body = if (elapsed == 0L) {
        "$totalText $subject ilk günü."
    } else {
        "$totalText $subject $elapsed günü geçti."
    }
    return body.replaceFirstChar { it.uppercase() } + manualNote
}
