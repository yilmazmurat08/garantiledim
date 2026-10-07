package com.garantiledim.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeadlinesTest {

    private val today = LocalDate.of(2026, 10, 3)

    private fun product(
        delivery: LocalDate = LocalDate.of(2026, 9, 21),
        tracksReturn: Boolean = true,
        tracksWarranty: Boolean = true,
        returnOverride: LocalDate? = null,
        warrantyOverride: LocalDate? = null,
    ) = Product(
        name = "Kablosuz Kulaklık",
        store = "Hepsiburada",
        category = Category.ELEKTRONIK,
        purchaseDate = LocalDate.of(2026, 9, 19),
        deliveryDate = delivery,
        tracksReturn = tracksReturn,
        tracksWarranty = tracksWarranty,
        returnEndOverride = returnOverride,
        warrantyEndOverride = warrantyOverride,
    )

    @Test
    fun `süreler teslim tarihinden hesaplanır`() {
        val p = product()
        assertEquals(LocalDate.of(2026, 10, 5), p.deadline(DurationType.IADE)!!.endDate)
        assertEquals(LocalDate.of(2028, 9, 21), p.deadline(DurationType.GARANTI)!!.endDate)
    }

    @Test
    fun `elle girilen tarih otomatik hesabın yerine geçer`() {
        val p = product(warrantyOverride = LocalDate.of(2029, 9, 21))
        val d = p.deadline(DurationType.GARANTI)!!
        assertEquals(LocalDate.of(2029, 9, 21), d.endDate)
        assertTrue(d.isManual)
        assertFalse(p.deadline(DurationType.IADE)!!.isManual)
    }

    @Test
    fun `kapatılan süre takip edilmez`() {
        val p = product(tracksReturn = false)
        assertNull(p.deadline(DurationType.IADE))
        assertEquals(listOf(DurationType.GARANTI), p.deadlines().map { it.type })
    }

    @Test
    fun `öne çıkan süre devam edenlerden en yakın olanıdır`() {
        assertEquals(DurationType.IADE, product().primaryDeadline(today)!!.type)
        // İade dolduktan sonra garanti öne çıkar
        assertEquals(DurationType.GARANTI, product().primaryDeadline(LocalDate.of(2026, 10, 6))!!.type)
    }

    @Test
    fun `hepsi dolmuşsa en son dolan süre öne çıkar`() {
        val later = LocalDate.of(2030, 1, 1)
        val p = product()
        assertTrue(p.isExpired(later))
        assertEquals(DurationType.GARANTI, p.primaryDeadline(later)!!.type)
    }

    @Test
    fun `iade dolsa da garanti devam ediyorsa ürün dolmuş sayılmaz`() {
        assertFalse(product().isExpired(LocalDate.of(2026, 10, 6)))
    }

    @Test
    fun `aciliyet eşikleri`() {
        assertEquals(Urgency.EXPIRED, urgencyOf(-1))
        assertEquals(Urgency.DANGER, urgencyOf(0))
        assertEquals(Urgency.DANGER, urgencyOf(3))
        assertEquals(Urgency.WARNING, urgencyOf(4))
        assertEquals(Urgency.WARNING, urgencyOf(13))
        assertEquals(Urgency.SUCCESS, urgencyOf(14))
    }

    @Test
    fun `kalan süre metinleri`() {
        assertEquals("Süre doldu", remainingLabel(today.minusDays(1), today))
        assertEquals("Bugün son gün", remainingLabel(today, today))
        assertEquals("1 gün kaldı", remainingLabel(today.plusDays(1), today))
        assertEquals("60 gün kaldı", remainingLabel(today.plusDays(60), today))
        assertEquals("2 ay kaldı", remainingLabel(LocalDate.of(2026, 12, 12), today))
        assertEquals("1 yıl kaldı", remainingLabel(LocalDate.of(2027, 10, 3), today))
        assertEquals("1 yıl 11 ay kaldı", remainingLabel(LocalDate.of(2028, 9, 21), today))
        assertEquals("2 yıl 8 ay", shortRemainingLabel(LocalDate.of(2029, 6, 8), today))
        assertEquals("Bugün", shortRemainingLabel(today, today))
        assertEquals("2 gün", shortRemainingLabel(today.plusDays(2), today))
    }

    @Test
    fun `geçen süre açıklaması`() {
        val p = product()
        assertEquals(
            "14 günlük iade süresinin 12 günü geçti.",
            elapsedDescription(p.deadline(DurationType.IADE)!!, today),
        )
        assertEquals(
            "2 yıllık garantinin 12 günü geçti.",
            elapsedDescription(p.deadline(DurationType.GARANTI)!!, today),
        )
        val manual = product(warrantyOverride = LocalDate.of(2029, 9, 21))
        assertEquals(
            "3 yıllık garantinin 12 günü geçti. Tarih elle girildi.",
            elapsedDescription(manual.deadline(DurationType.GARANTI)!!, today),
        )
        assertEquals(
            "Süre 5 Ekim 2026, Pazartesi tarihinde doldu.",
            elapsedDescription(p.deadline(DurationType.IADE)!!, LocalDate.of(2026, 10, 7)),
        )
        assertEquals(12f / 14f, elapsedFraction(p.deadline(DurationType.IADE)!!, today), 0.0001f)
    }

    @Test
    fun `türkçe tarih biçimleri`() {
        val d = LocalDate.of(2026, 10, 5)
        assertEquals("5 Eki 2026", TrDates.short(d))
        assertEquals("5 Ekim 2026, Pazartesi", TrDates.long(d))
        assertEquals("05.10.2026", TrDates.numeric(d))
        assertEquals("EKİ", TrDates.monthShortUpper(d))
        assertEquals("Cumartesi", TrDates.weekday(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun `dosya boyutu`() {
        assertEquals("900 B", formatFileSize(900))
        assertEquals("350 KB", formatFileSize(350 * 1024))
        assertEquals("1,2 MB", formatFileSize((1.2 * 1024 * 1024).toLong()))
    }
}
