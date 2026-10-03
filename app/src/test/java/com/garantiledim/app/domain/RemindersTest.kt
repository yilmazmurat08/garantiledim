package com.garantiledim.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RemindersTest {

    private val today = LocalDate.of(2026, 10, 3)

    private fun action(
        daysLeft: Long,
        lastReminder: LocalDate? = null,
        expiredNotified: Boolean = false,
        leadDays: Int = 3,
    ) = reminderAction(today.plusDays(daysLeft), today, leadDays, lastReminder, expiredNotified)

    @Test
    fun `hatırlatma bitişe 3 gün kala başlar`() {
        assertNull(action(4))
        assertEquals(ReminderAction.DaysLeft(3), action(3))
        assertEquals(ReminderAction.DaysLeft(1), action(1))
        assertEquals(ReminderAction.DaysLeft(5), action(5, leadDays = 7))
    }

    @Test
    fun `son gün bugün doluyor bildirimi`() {
        assertEquals(ReminderAction.DueToday, action(0))
    }

    @Test
    fun `aynı gün iki kez hatırlatılmaz`() {
        assertNull(action(2, lastReminder = today))
        assertNull(action(0, lastReminder = today))
        assertEquals(ReminderAction.DaysLeft(2), action(2, lastReminder = today.minusDays(1)))
    }

    @Test
    fun `süresi doldu bildirimi bir kez gönderilir`() {
        assertEquals(ReminderAction.Expired(notify = true), action(-1, lastReminder = today))
        assertNull(action(-1, expiredNotified = true))
    }

    @Test
    fun `çok önce dolmuş süre sessizce işaretlenir`() {
        assertEquals(ReminderAction.Expired(notify = true), action(-3))
        assertEquals(ReminderAction.Expired(notify = false), action(-4))
    }

    @Test
    fun `bildirim metinleri`() {
        assertEquals(
            "Kablosuz Kulaklık için İade hakkı süresinin bitmesine 2 gün kaldı",
            ReminderAction.DaysLeft(2).text("Kablosuz Kulaklık", DurationType.IADE).body,
        )
        assertEquals(
            "Akıllı Saat için Garanti süresi bugün doluyor",
            ReminderAction.DueToday.text("Akıllı Saat", DurationType.GARANTI).body,
        )
        assertEquals(
            "Akıllı Saat için Garanti süresi doldu",
            ReminderAction.Expired(true).text("Akıllı Saat", DurationType.GARANTI).body,
        )
    }
}
