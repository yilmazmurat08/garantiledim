package com.garantiledim.app.data

import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.Product
import com.garantiledim.app.domain.deadline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val dao: ProductDao,
    private val files: FileStore,
) {
    val products: Flow<List<Product>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun product(id: Long): Flow<Product?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun get(id: Long): Product? = dao.getById(id)?.toDomain()

    /** Yeni ürünü ekler ya da mevcut ürünü günceller; ürünün id'sini döner. */
    suspend fun save(product: Product): Long {
        if (product.id == 0L) {
            return dao.insert(product.copy(createdAt = System.currentTimeMillis()).toEntity())
        }
        val old = dao.getById(product.id)
        val updated = product.toEntity()
        if (old == null) {
            dao.update(updated)
            return product.id
        }
        // Bitiş tarihi değişmeyen sürenin bildirim durumu korunur, değişeninki sıfırlanır.
        val before = old.toDomain()
        val returnSame = sameEnd(before, product, DurationType.IADE)
        val warrantySame = sameEnd(before, product, DurationType.GARANTI)
        dao.update(
            updated.copy(
                createdAt = old.createdAt,
                returnLastReminder = if (returnSame) old.returnLastReminder else null,
                returnExpiredNotified = returnSame && old.returnExpiredNotified,
                warrantyLastReminder = if (warrantySame) old.warrantyLastReminder else null,
                warrantyExpiredNotified = warrantySame && old.warrantyExpiredNotified,
            )
        )
        return product.id
    }

    /** Ürünü ve ona ait dosyaları siler. */
    suspend fun delete(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.deleteById(id)
        files.delete(
            listOfNotNull(
                entity.receiptPath,
                entity.receiptThumbPath,
                entity.photoPath,
                entity.photoThumbPath,
            )
        )
    }

    private fun sameEnd(a: Product, b: Product, type: DurationType): Boolean {
        val endA = a.deadline(type)?.endDate ?: return false
        return endA == b.deadline(type)?.endDate
    }
}
