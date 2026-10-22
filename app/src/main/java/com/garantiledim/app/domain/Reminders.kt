package com.garantiledim.app.domain

import com.garantiledim.app.i18n.Strings
import java.time.LocalDate

/** Bir süre için bugün yapılacak bildirim işlemi. */
sealed interface ReminderAction {
    /** Bitişe [days] gün kaldı (1..hatırlatma başlangıcı). */
    data class DaysLeft(val days: Long) : ReminderAction

    /** Süre bugün doluyor. */
    data object DueToday : ReminderAction

    /**
     * Süre doldu. [notify] false ise süre çok önce dolmuştur (örneğin eski bir ürün yeni eklendi);
     * bildirim gönderilmeden yalnızca işaretlenir.
     */
    data class Expired(val notify: Boolean) : ReminderAction
}

/** Süre dolduktan sonra en fazla bu kadar gün içinde "süresi doldu" bildirimi gönderilir. */
const val EXPIRED_NOTIFY_WINDOW_DAYS = 3L

/**
 * SPEC.md madde 7: bitişe [leadDays] gün kala her gün hatırlatma, son gün "bugün doluyor",
 * dolduktan sonra bir kez "süresi doldu". Aynı gün iki kez hatırlatma gönderilmez.
 */
fun reminderAction(
    endDate: LocalDate,
    today: LocalDate,
    leadDays: Int,
    lastReminder: LocalDate?,
    expiredNotified: Boolean,
): ReminderAction? {
    val days = daysRemaining(endDate, today)
    return when {
        days < 0 -> if (expiredNotified) null else ReminderAction.Expired(notify = -days <= EXPIRED_NOTIFY_WINDOW_DAYS)
        lastReminder == today -> null
        days == 0L -> ReminderAction.DueToday
        days <= leadDays -> ReminderAction.DaysLeft(days)
        else -> null
    }
}

data class ReminderText(val title: String, val body: String)

fun ReminderAction.text(productName: String, type: DurationType, s: Strings): ReminderText = when (this) {
    is ReminderAction.DaysLeft -> ReminderText(
        title = s.reminderDaysLeftTitle(type),
        body = s.reminderDaysLeftBody(productName, type, days),
    )
    ReminderAction.DueToday -> ReminderText(
        title = s.reminderDueTodayTitle(type),
        body = s.reminderDueTodayBody(productName, type),
    )
    is ReminderAction.Expired -> ReminderText(
        title = s.reminderExpiredTitle(type),
        body = s.reminderExpiredBody(productName, type),
    )
}
