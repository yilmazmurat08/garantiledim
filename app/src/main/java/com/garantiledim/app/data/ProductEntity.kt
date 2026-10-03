package com.garantiledim.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.garantiledim.app.domain.Attachment
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.Photo
import com.garantiledim.app.domain.Product
import java.io.File
import java.time.LocalDate

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val store: String,
    val category: Category,
    val purchaseDate: LocalDate,
    val deliveryDate: LocalDate,
    val tracksReturn: Boolean,
    val tracksWarranty: Boolean,
    val returnEndOverride: LocalDate?,
    val warrantyEndOverride: LocalDate?,
    val receiptPath: String?,
    val receiptThumbPath: String?,
    val receiptFileName: String?,
    val receiptMimeType: String?,
    val receiptSizeBytes: Long?,
    val photoPath: String?,
    val photoThumbPath: String?,
    // Bildirim durumu: süre başına son hatırlatma günü ve "süresi doldu" bildirimi
    val returnLastReminder: LocalDate? = null,
    val returnExpiredNotified: Boolean = false,
    val warrantyLastReminder: LocalDate? = null,
    val warrantyExpiredNotified: Boolean = false,
    val createdAt: Long,
)

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    store = store,
    category = category,
    purchaseDate = purchaseDate,
    deliveryDate = deliveryDate,
    tracksReturn = tracksReturn,
    tracksWarranty = tracksWarranty,
    returnEndOverride = returnEndOverride,
    warrantyEndOverride = warrantyEndOverride,
    receipt = receiptPath?.let { path ->
        Attachment(
            path = path,
            thumbPath = receiptThumbPath,
            fileName = receiptFileName ?: File(path).name,
            mimeType = receiptMimeType ?: "application/octet-stream",
            sizeBytes = receiptSizeBytes ?: 0,
        )
    },
    photo = photoPath?.let { Photo(it, photoThumbPath) },
    createdAt = createdAt,
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    store = store,
    category = category,
    purchaseDate = purchaseDate,
    deliveryDate = deliveryDate,
    tracksReturn = tracksReturn,
    tracksWarranty = tracksWarranty,
    returnEndOverride = returnEndOverride,
    warrantyEndOverride = warrantyEndOverride,
    receiptPath = receipt?.path,
    receiptThumbPath = receipt?.thumbPath,
    receiptFileName = receipt?.fileName,
    receiptMimeType = receipt?.mimeType,
    receiptSizeBytes = receipt?.sizeBytes,
    photoPath = photo?.path,
    photoThumbPath = photo?.thumbPath,
    createdAt = createdAt,
)
