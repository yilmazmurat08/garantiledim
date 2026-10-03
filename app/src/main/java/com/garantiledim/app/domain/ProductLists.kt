package com.garantiledim.app.domain

import java.time.LocalDate

/** Bir ürünün kartta gösterilecek hali: öne çıkan süre ve rozet bilgileri. */
data class ProductSummary(
    val product: Product,
    val primary: Deadline?,
    val urgency: Urgency,
    val label: String,
    val shortLabel: String,
    val expired: Boolean,
)

fun summarize(product: Product, today: LocalDate): ProductSummary {
    val primary = product.primaryDeadline(today)
    val days = primary?.let { daysRemaining(it.endDate, today) }
    return ProductSummary(
        product = product,
        primary = primary,
        urgency = days?.let(::urgencyOf) ?: Urgency.SUCCESS,
        label = primary?.let { remainingLabel(it.endDate, today) } ?: "",
        shortLabel = primary?.let { shortRemainingLabel(it.endDate, today) } ?: "",
        expired = product.isExpired(today),
    )
}

data class SortedProducts(
    val active: List<ProductSummary>,
    val expired: List<ProductSummary>,
)

/** Devam edenler bitişi en yakından uzağa, süresi dolanlar en yeni dolandan eskiye. */
fun sortProducts(products: List<Product>, today: LocalDate): SortedProducts {
    val (expired, active) = products.map { summarize(it, today) }.partition { it.expired }
    return SortedProducts(
        active = active.sortedWith(compareBy({ it.primary?.endDate }, { it.product.name })),
        expired = expired.sortedWith(
            compareByDescending<ProductSummary> { it.primary?.endDate }.thenBy { it.product.name }
        ),
    )
}

enum class ProductFilter(val label: String) {
    ALL("Tümü"),
    UPCOMING("Yaklaşan"),
    ACTIVE("Aktif"),
    EXPIRED("Süresi dolan"),
}

fun ProductSummary.matches(filter: ProductFilter, today: LocalDate): Boolean = when (filter) {
    ProductFilter.ALL -> true
    ProductFilter.ACTIVE -> !expired
    ProductFilter.EXPIRED -> expired
    ProductFilter.UPCOMING -> !expired && primary != null &&
        daysRemaining(primary.endDate, today) in 0..13
}

fun ProductSummary.matches(query: String): Boolean {
    val q = query.trim().lowercase(TURKISH)
    if (q.isEmpty()) return true
    return product.name.lowercase(TURKISH).contains(q) ||
        product.store.lowercase(TURKISH).contains(q)
}

fun filterProducts(
    sorted: SortedProducts,
    filter: ProductFilter,
    query: String,
    today: LocalDate,
): SortedProducts = SortedProducts(
    active = sorted.active.filter { it.matches(filter, today) && it.matches(query) },
    expired = sorted.expired.filter { it.matches(filter, today) && it.matches(query) },
)

/**
 * Ana sayfadaki "Günün Garantileri": önce süresi devam edenler (en yakın bitiş önce),
 * ardından son [expiredWindowDays] günde süresi dolanlar.
 */
fun homeHighlights(
    products: List<Product>,
    today: LocalDate,
    limit: Int = 6,
    expiredWindowDays: Long = 30,
): List<ProductSummary> {
    val sorted = sortProducts(products, today)
    val recentlyExpired = sorted.expired.filter { s ->
        val end = s.primary?.endDate ?: return@filter false
        daysRemaining(end, today) >= -expiredWindowDays
    }
    return (sorted.active + recentlyExpired).take(limit)
}

/** Zil üzerindeki nokta: son günlerdeki ya da yeni dolmuş bir süre varsa. */
fun hasAlert(products: List<Product>, today: LocalDate, leadDays: Int = 3): Boolean =
    products.any { product ->
        product.deadlines().any { d ->
            daysRemaining(d.endDate, today) in -7L..leadDays.toLong()
        }
    }

data class AgendaEntry(
    val productId: Long,
    val productName: String,
    val deadline: Deadline,
    val urgency: Urgency,
    val shortLabel: String,
    val subtitle: String,
)

data class AgendaGroup(val title: String, val entries: List<AgendaEntry>)

/** Süresi devam eden tüm süreler; iki süresi olan ürün iki kez yer alır. */
fun agendaGroups(products: List<Product>, today: LocalDate): List<AgendaGroup> {
    val entries = products.flatMap { product ->
        product.deadlines()
            .filter { !it.endDate.isBefore(today) }
            .map { d ->
                val days = daysRemaining(d.endDate, today)
                AgendaEntry(
                    productId = product.id,
                    productName = product.name,
                    deadline = d,
                    urgency = urgencyOf(days),
                    shortLabel = shortRemainingLabel(d.endDate, today),
                    subtitle = if (days <= 30) {
                        "${d.type.endLabel} · ${TrDates.weekday(d.endDate)}"
                    } else {
                        "${d.type.endLabel} · ${d.endDate.year}"
                    },
                )
            }
    }.sortedWith(compareBy({ it.deadline.endDate }, { it.productName }))

    fun group(title: String, range: LongRange) = AgendaGroup(
        title,
        entries.filter { daysRemaining(it.deadline.endDate, today) in range },
    )

    return listOf(
        group("Bu hafta", 0L..6L),
        group("Önümüzdeki 30 gün", 7L..30L),
        group("Daha sonra", 31L..Long.MAX_VALUE),
    ).filter { it.entries.isNotEmpty() }
}
