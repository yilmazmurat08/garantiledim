package com.garantiledim.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PremiumTest {

    private fun product(id: Long, createdAt: Long) = Product(
        id = id,
        name = "Ürün $id",
        store = "",
        category = Category.DIGER,
        purchaseDate = LocalDate.of(2026, 10, 1),
        deliveryDate = LocalDate.of(2026, 10, 1),
        tracksReturn = true,
        tracksWarranty = true,
        createdAt = createdAt,
    )

    @Test
    fun `ücretsiz sürümde üç ürüne kadar eklenir`() {
        assertTrue(canAddProduct(0, premium = false))
        assertTrue(canAddProduct(2, premium = false))
        assertFalse(canAddProduct(3, premium = false))
        assertTrue(canAddProduct(3, premium = true))
        assertTrue(canAddProduct(250, premium = true))
    }

    @Test
    fun `sınırı aşan en son eklenen ürünler kilitlenir`() {
        val products = listOf(product(5, 500), product(1, 100), product(3, 300), product(2, 200), product(4, 400))
        assertEquals(setOf(4L, 5L), lockedProductIds(products, premium = false))
        assertEquals(emptySet<Long>(), lockedProductIds(products, premium = true))
        assertEquals(emptySet<Long>(), lockedProductIds(products.take(3), premium = false))
    }
}
