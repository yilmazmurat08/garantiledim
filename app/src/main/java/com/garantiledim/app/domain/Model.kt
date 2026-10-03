package com.garantiledim.app.domain

import java.time.LocalDate

enum class Category(val label: String) {
    ELEKTRONIK("Elektronik"),
    BEYAZ_ESYA("Beyaz eşya"),
    GIYIM("Giyim"),
    DIGER("Diğer"),
}

enum class DurationType(
    val label: String,
    val shortLabel: String,
    val endLabel: String,
) {
    IADE("İade hakkı", "İade", "İade son günü"),
    GARANTI("Garanti", "Garanti", "Garanti bitişi"),
}

enum class Urgency { SUCCESS, WARNING, DANGER, EXPIRED }

/** Fiş / fatura. Orijinal dosya ve önizlemesi uygulamanın kendi dizininde tutulur. */
data class Attachment(
    val path: String,
    val thumbPath: String?,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
) {
    val isPdf: Boolean get() = mimeType == "application/pdf"
}

data class Photo(val path: String, val thumbPath: String?)

data class Product(
    val id: Long = 0,
    val name: String,
    val store: String,
    val category: Category,
    val purchaseDate: LocalDate,
    val deliveryDate: LocalDate,
    val tracksReturn: Boolean,
    val tracksWarranty: Boolean,
    val returnEndOverride: LocalDate? = null,
    val warrantyEndOverride: LocalDate? = null,
    val receipt: Attachment? = null,
    val photo: Photo? = null,
    val createdAt: Long = 0,
)

/** Bir ürünün takip edilen tek bir süresi (iade hakkı ya da garanti). */
data class Deadline(
    val type: DurationType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isManual: Boolean,
)
