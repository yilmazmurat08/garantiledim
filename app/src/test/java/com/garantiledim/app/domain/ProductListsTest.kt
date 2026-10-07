package com.garantiledim.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProductListsTest {

    private val today = LocalDate.of(2026, 10, 3)

    private fun product(
        id: Long,
        name: String,
        delivery: LocalDate,
        tracksReturn: Boolean = false,
        tracksWarranty: Boolean = true,
        store: String = "Mağaza",
    ) = Product(
        id = id,
        name = name,
        store = store,
        category = Category.ELEKTRONIK,
        purchaseDate = delivery,
        deliveryDate = delivery,
        tracksReturn = tracksReturn,
        tracksWarranty = tracksWarranty,
    )

    // İade 5 Eki 2026 (2 gün), garanti 21 Eyl 2028
    private val kulaklik = product(1, "Kablosuz Kulaklık", LocalDate.of(2026, 9, 21), tracksReturn = true, store = "Hepsiburada")
    // Sadece iade, 10 Eki 2026 (7 gün)
    private val mont = product(2, "Kışlık Mont", LocalDate.of(2026, 9, 26), tracksReturn = true, tracksWarranty = false, store = "Trendyol")
    // Garanti 12 Ara 2026
    private val saat = product(3, "Akıllı Saat", LocalDate.of(2024, 12, 12))
    // Garanti 25 Eyl 2026 doldu (8 gün önce)
    private val kahve = product(4, "Kahve Makinesi", LocalDate.of(2024, 9, 25))
    // Garanti 1 Oca 2026 doldu (çok önce)
    private val eski = product(5, "Eski Ütü", LocalDate.of(2024, 1, 1))

    private val all = listOf(eski, saat, kahve, mont, kulaklik)

    @Test
    fun `liste sıralaması`() {
        val sorted = sortProducts(all, today)
        assertEquals(listOf("Kablosuz Kulaklık", "Kışlık Mont", "Akıllı Saat"), sorted.active.map { it.product.name })
        assertEquals(listOf("Kahve Makinesi", "Eski Ütü"), sorted.expired.map { it.product.name })
    }

    @Test
    fun `özet öne çıkan süreyi kullanır`() {
        val s = summarize(kulaklik, today)
        assertEquals(DurationType.IADE, s.primary!!.type)
        assertEquals(Urgency.DANGER, s.urgency)
        assertEquals("2 gün kaldı", s.label)
        assertEquals("2 gün", s.shortLabel)
        assertFalse(s.expired)
        assertTrue(summarize(kahve, today).expired)
    }

    @Test
    fun `filtreler ve arama`() {
        val sorted = sortProducts(all, today)
        fun names(f: ProductFilter, q: String = "") =
            filterProducts(sorted, f, q, today).let { it.active + it.expired }.map { it.product.name }

        assertEquals(5, names(ProductFilter.ALL).size)
        assertEquals(listOf("Kablosuz Kulaklık", "Kışlık Mont"), names(ProductFilter.UPCOMING))
        assertEquals(listOf("Kablosuz Kulaklık", "Kışlık Mont", "Akıllı Saat"), names(ProductFilter.ACTIVE))
        assertEquals(listOf("Kahve Makinesi", "Eski Ütü"), names(ProductFilter.EXPIRED))
        // Türkçe büyük/küçük harf: "İ" ve "ı"
        assertEquals(listOf("Kışlık Mont"), names(ProductFilter.ALL, "KIŞLIK"))
        assertEquals(listOf("Kablosuz Kulaklık"), names(ProductFilter.ALL, "hepsi"))
    }

    @Test
    fun `günün garantileri yeni dolanları sona ekler, eskileri almaz`() {
        val names = homeHighlights(all, today).map { it.product.name }
        assertEquals(listOf("Kablosuz Kulaklık", "Kışlık Mont", "Akıllı Saat", "Kahve Makinesi"), names)
        assertEquals(2, homeHighlights(all, today, limit = 2).size)
    }

    @Test
    fun `ajanda iki süreli ürünü iki kez gösterir`() {
        val groups = agendaGroups(all, today)
        assertEquals(listOf("Bu hafta", "Önümüzdeki 30 gün", "Daha sonra"), groups.map { it.title })
        val week = groups[0].entries
        assertEquals(listOf("Kablosuz Kulaklık"), week.map { it.productName })
        assertEquals("İade son günü · Pazartesi", week[0].subtitle)
        assertEquals(listOf("Kışlık Mont"), groups[1].entries.map { it.productName })
        val later = groups[2].entries
        assertEquals(listOf("Akıllı Saat", "Kablosuz Kulaklık"), later.map { it.productName })
        assertEquals(DurationType.GARANTI, later[1].deadline.type)
        assertEquals("Garanti bitişi · 2028", later[1].subtitle)
    }

    @Test
    fun `zil uyarısı`() {
        assertTrue(hasAlert(listOf(kulaklik), today))
        assertTrue(hasAlert(listOf(kahve), today.minusDays(1))) // 7 gün önce dolmuş
        assertFalse(hasAlert(listOf(saat, eski), today))
    }
}
