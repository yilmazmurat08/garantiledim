package com.garantiledim.app.domain

/** Ücretsiz sürümde takip edilebilen en fazla ürün sayısı (SPEC.md madde 10). */
const val FREE_PRODUCT_LIMIT = 3

/** Ücretsiz sürümde yeni ürün eklenebilir mi. */
fun canAddProduct(productCount: Int, premium: Boolean): Boolean = premium || productCount < FREE_PRODUCT_LIMIT

/**
 * Premium yokken sınırı aşan ürünler (en son eklenenler) kilitlenir: silinmez, görüntülenir ve
 * düzenlenebilir ama hatırlatma gönderilmez. Premium alınınca kendiliğinden açılır. Normalde
 * yalnızca satın alma iade edildiğinde ortaya çıkar; kilit veritabanında tutulmaz, her seferinde
 * eklenme sırasına göre hesaplanır.
 */
fun lockedProductIds(products: List<Product>, premium: Boolean): Set<Long> {
    if (premium || products.size <= FREE_PRODUCT_LIMIT) return emptySet()
    return products
        .sortedWith(compareBy<Product> { it.createdAt }.thenBy { it.id })
        .drop(FREE_PRODUCT_LIMIT)
        .mapTo(mutableSetOf()) { it.id }
}
