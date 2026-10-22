package com.garantiledim.app.domain

import com.garantiledim.app.i18n.Strings
import java.time.LocalDate
import java.util.Locale

/** Bir ürünün kartta gösterilecek hali: öne çıkan süre ve rozet bilgileri. */
data class ProductSummary(
    val product: Product,
    val primary: Deadline?,
    val urgency: Urgency,
    val label: String,
    val shortLabel: String,
    val expired: Boolean,
)

fun summarize(product: Product, today: LocalDate, s: Strings): ProductSummary {
    val primary = product.primaryDeadline(today)
    val days = primary?.let { daysRemaining(it.endDate, today) }
    return ProductSummary(
        product = product,
        primary = primary,
        urgency = days?.let(::urgencyOf) ?: Urgency.SUCCESS,
        label = primary?.let { remainingLabel(it.endDate, today, s) } ?: "",
        shortLabel = primary?.let { shortRemainingLabel(it.endDate, today, s) } ?: "",
        expired = product.isExpired(today),
    )
}

data class SortedProducts(
    val active: List<ProductSummary>,
    val expired: List<ProductSummary>,
)

/** Devam edenler bitişi en yakından uzağa, süresi dolanlar en yeni dolandan eskiye. */
fun sortProducts(products: List<Product>, today: LocalDate, s: Strings): SortedProducts {
    val (expired, active) = products.map { summarize(it, today, s) }.partition { it.expired }
    return SortedProducts(
        active = active.sortedWith(compareBy({ it.primary?.endDate }, { it.product.name })),
        expired = expired.sortedWith(
            compareByDescending<ProductSummary> { it.primary?.endDate }.thenBy { it.product.name }
        ),
    )
}

enum class ProductFilter { ALL, UPCOMING, ACTIVE, EXPIRED }

fun ProductSummary.matches(filter: ProductFilter, today: LocalDate): Boolean = when (filter) {
    ProductFilter.ALL -> true
    ProductFilter.ACTIVE -> !expired
    ProductFilter.EXPIRED -> expired
    ProductFilter.UPCOMING -> !expired && primary != null &&
        daysRemaining(primary.endDate, today) in 0..13
}

/** Ad ya da mağazada arar; büyük/küçük harf uygulama diline göre (Türkçede I/ı, İ/i). */
fun ProductSummary.matches(query: String, locale: Locale): Boolean {
    val q = query.trim().lowercase(locale)
    if (q.isEmpty()) return true
    return product.name.lowercase(locale).contains(q) ||
        product.store.lowercase(locale).contains(q)
}

fun filterProducts(
    sorted: SortedProducts,
    filter: ProductFilter,
    query: String,
    today: LocalDate,
    locale: Locale,
): SortedProducts = SortedProducts(
    active = sorted.active.filter { it.matches(filter, today) && it.matches(query, locale) },
    expired = sorted.expired.filter { it.matches(filter, today) && it.matches(query, locale) },
)

/**
 * Ana sayfadaki "Günün Garantileri": önce süresi devam edenler (en yakın bitiş önce),
 * ardından son [expiredWindowDays] günde süresi dolanlar.
 */
fun homeHighlights(
    products: List<Product>,
    today: LocalDate,
    s: Strings,
    limit: Int = 6,
    expiredWindowDays: Long = 30,
): List<ProductSummary> {
    val sorted = sortProducts(products, today, s)
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
fun agendaGroups(products: List<Product>, today: LocalDate, s: Strings): List<AgendaGroup> {
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
                    shortLabel = shortRemainingLabel(d.endDate, today, s),
                    subtitle = if (days <= 30) {
                        "${s.durationEnd(d.type)} · ${s.weekday(d.endDate)}"
                    } else {
                        "${s.durationEnd(d.type)} · ${d.endDate.year}"
                    },
                )
            }
    }.sortedWith(compareBy({ it.deadline.endDate }, { it.productName }))

    fun group(title: String, range: LongRange) = AgendaGroup(
        title,
        entries.filter { daysRemaining(it.deadline.endDate, today) in range },
    )

    return listOf(
        group(s.agendaThisWeek, 0L..6L),
        group(s.agendaNext30, 7L..30L),
        group(s.agendaLater, 31L..Long.MAX_VALUE),
    ).filter { it.entries.isNotEmpty() }
}
