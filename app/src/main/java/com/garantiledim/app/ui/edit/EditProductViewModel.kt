package com.garantiledim.app.ui.edit

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garantiledim.app.AppContainer
import com.garantiledim.app.domain.Attachment
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.FREE_PRODUCT_LIMIT
import com.garantiledim.app.domain.Photo
import com.garantiledim.app.domain.Product
import com.garantiledim.app.domain.canAddProduct
import com.garantiledim.app.domain.deadline
import com.garantiledim.app.notifications.Reminders
import com.garantiledim.app.ui.NEW_PRODUCT_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

sealed interface EditResult {
    data class Created(val id: Long) : EditResult
    data object Saved : EditResult
    data object Deleted : EditResult
}

enum class PickTarget { RECEIPT, PHOTO }

data class EditUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val name: String = "",
    val store: String = "",
    val category: Category = Category.ELEKTRONIK,
    val purchaseDate: LocalDate = LocalDate.now(),
    val deliveryDate: LocalDate = LocalDate.now(),
    val tracksReturn: Boolean = true,
    val tracksWarranty: Boolean = true,
    val returnOverride: LocalDate? = null,
    val warrantyOverride: LocalDate? = null,
    val receipt: Attachment? = null,
    val photo: Photo? = null,
    val nameError: String? = null,
    val dateError: String? = null,
    val periodError: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val result: EditResult? = null,
) {
    /** Formdaki değerlerle oluşturulan ürün; bitiş tarihlerini göstermek için de kullanılır. */
    fun toProduct(id: Long = 0): Product = Product(
        id = id,
        name = name.trim(),
        store = store.trim(),
        category = category,
        purchaseDate = purchaseDate,
        deliveryDate = deliveryDate,
        tracksReturn = tracksReturn,
        tracksWarranty = tracksWarranty,
        returnEndOverride = returnOverride.takeIf { tracksReturn },
        warrantyEndOverride = warrantyOverride.takeIf { tracksWarranty },
        receipt = receipt,
        photo = photo,
    )

    fun endDate(type: DurationType): LocalDate {
        val draft = toProduct().copy(tracksReturn = true, tracksWarranty = true)
        return draft.deadline(type)!!.endDate
    }

    fun isManual(type: DurationType): Boolean = when (type) {
        DurationType.IADE -> returnOverride != null
        DurationType.GARANTI -> warrantyOverride != null
    }

    fun isTracked(type: DurationType): Boolean = when (type) {
        DurationType.IADE -> tracksReturn
        DurationType.GARANTI -> tracksWarranty
    }
}

class EditProductViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val productId: Long = handle.get<Long>("id") ?: NEW_PRODUCT_ID
    private val isEdit = productId != NEW_PRODUCT_ID

    private val _state = MutableStateFlow(EditUiState(isEdit = isEdit, loaded = !isEdit))
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    /** Bu oturumda kopyalanan, kaydedilmezse silinecek dosyalar. */
    private val importedPaths = mutableListOf<String>()

    /** Kaydedilen üründen kaldırılan, kayıttan sonra silinecek dosyalar. */
    private val replacedPaths = mutableListOf<String>()

    private var originalCreatedAt = 0L
    private var deliveryTouched = false
    private var saved = false
    private var pendingCamera: Pair<File, PickTarget>? = null

    /** Formun açıldığı andaki hali; çıkarken kaydedilmemiş değişiklik var mı diye bakılır. */
    private var initialForm: EditUiState? = if (isEdit) null else _state.value.formOnly()

    init {
        val draft = handle.get<Bundle>(KEY_DRAFT)
        if (draft != null) {
            // Kamera ya da galeri açıkken Android uygulamayı kapattıysa form kaldığı yerden sürer.
            restore(draft)
        } else if (isEdit) {
            viewModelScope.launch {
                val product = container.products.get(productId)
                if (product == null) {
                    _state.update { it.copy(loaded = true, message = "Ürün bulunamadı") }
                    return@launch
                }
                originalCreatedAt = product.createdAt
                deliveryTouched = true
                _state.value = EditUiState(
                    loaded = true,
                    isEdit = true,
                    name = product.name,
                    store = product.store,
                    category = product.category,
                    purchaseDate = product.purchaseDate,
                    deliveryDate = product.deliveryDate,
                    tracksReturn = product.tracksReturn,
                    tracksWarranty = product.tracksWarranty,
                    returnOverride = product.returnEndOverride,
                    warrantyOverride = product.warrantyEndOverride,
                    receipt = product.receipt,
                    photo = product.photo,
                )
                initialForm = _state.value.formOnly()
            }
        }
        viewModelScope.launch {
            _state.collect { if (it.loaded && !saved) persist() }
        }
    }

    /** Kaydedilmemiş değişiklik var mı (geri tuşunda onay sormak için). */
    fun hasUnsavedChanges(): Boolean {
        val initial = initialForm ?: return false
        return _state.value.formOnly() != initial
    }

    fun onName(value: String) = _state.update { it.copy(name = value, nameError = null) }

    fun onStore(value: String) = _state.update { it.copy(store = value) }

    fun onCategory(value: Category) = _state.update { it.copy(category = value) }

    fun onPurchaseDate(date: LocalDate) = _state.update {
        val delivery = if (!deliveryTouched || it.deliveryDate.isBefore(date)) date else it.deliveryDate
        it.copy(purchaseDate = date, deliveryDate = delivery, dateError = null)
    }

    fun onDeliveryDate(date: LocalDate) {
        deliveryTouched = true
        _state.update { it.copy(deliveryDate = date, dateError = null, periodError = null) }
    }

    fun onTrack(type: DurationType, enabled: Boolean) = _state.update {
        when (type) {
            DurationType.IADE -> it.copy(tracksReturn = enabled, periodError = null)
            DurationType.GARANTI -> it.copy(tracksWarranty = enabled, periodError = null)
        }
    }

    /** Elle bitiş tarihi; null otomatik hesaba döner. */
    fun onOverride(type: DurationType, date: LocalDate?) = _state.update {
        when (type) {
            DurationType.IADE -> it.copy(returnOverride = date, periodError = null)
            DurationType.GARANTI -> it.copy(warrantyOverride = date, periodError = null)
        }
    }

    fun onPicked(target: PickTarget, uri: Uri) = importFile(target) {
        when (target) {
            PickTarget.RECEIPT -> container.files.importReceipt(uri)
            PickTarget.PHOTO -> container.files.importPhoto(uri)
        }
    }

    /** Kameraya verilecek hedef URI. */
    fun cameraTarget(target: PickTarget): Uri {
        val (file, uri) = container.files.createCameraTarget()
        pendingCamera = file to target
        persist()
        return uri
    }

    fun onCameraResult(success: Boolean) {
        val (file, target) = pendingCamera ?: return
        pendingCamera = null
        if (!success) {
            file.delete()
            return
        }
        importFile(target) {
            when (target) {
                PickTarget.RECEIPT -> container.files.importCameraReceipt(file)
                PickTarget.PHOTO -> container.files.importCameraPhoto(file)
            }
        }
    }

    fun onCameraUnavailable() {
        pendingCamera?.first?.delete()
        pendingCamera = null
        showMessage("Kamera uygulaması bulunamadı")
    }

    fun removeReceipt() {
        val current = _state.value.receipt ?: return
        discard(listOfNotNull(current.path, current.thumbPath))
        _state.update { it.copy(receipt = null) }
    }

    fun removePhoto() {
        val current = _state.value.photo ?: return
        discard(listOfNotNull(current.path, current.thumbPath))
        _state.update { it.copy(photo = null) }
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun messageShown() = _state.update { it.copy(message = null) }

    fun save() {
        val s = _state.value
        if (s.busy) return
        val nameError = if (s.name.isBlank()) "Ürün adı boş olamaz" else null
        val dateError = if (s.deliveryDate.isBefore(s.purchaseDate)) {
            "Teslim tarihi satın alma tarihinden önce olamaz"
        } else {
            null
        }
        val overrideBeforeDelivery = listOfNotNull(
            s.returnOverride.takeIf { s.tracksReturn },
            s.warrantyOverride.takeIf { s.tracksWarranty },
        ).any { it.isBefore(s.deliveryDate) }
        val periodError = when {
            !s.tracksReturn && !s.tracksWarranty -> "En az bir süre takip edilmeli"
            overrideBeforeDelivery -> "Bitiş tarihi teslim tarihinden önce olamaz"
            else -> null
        }
        if (nameError != null || dateError != null || periodError != null) {
            _state.update { it.copy(nameError = nameError, dateError = dateError, periodError = periodError) }
            return
        }

        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                if (!isEdit && !canAddProduct(container.products.count(), container.premium.isPremium.value)) {
                    _state.update {
                        it.copy(busy = false, message = "Ücretsiz sürümde en fazla $FREE_PRODUCT_LIMIT ürün eklenebilir")
                    }
                    return@launch
                }
                val product = s.toProduct(id = if (isEdit) productId else 0)
                    .copy(createdAt = originalCreatedAt)
                val id = container.products.save(product)
                saved = true
                handle.remove<Bundle>(KEY_DRAFT)
                importedPaths.clear()
                container.files.delete(replacedPaths.toList())
                replacedPaths.clear()
                _state.update {
                    it.copy(busy = false, result = if (isEdit) EditResult.Saved else EditResult.Created(id))
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = "Kaydedilemedi, tekrar dene") }
            }
        }
    }

    fun delete() {
        if (!isEdit) return
        viewModelScope.launch {
            container.products.delete(productId)
            Reminders.cancel(container.appContext, productId)
            saved = true
            handle.remove<Bundle>(KEY_DRAFT)
            container.files.delete(importedPaths.toList())
            importedPaths.clear()
            _state.update { it.copy(result = EditResult.Deleted) }
        }
    }

    override fun onCleared() {
        if (!saved && importedPaths.isNotEmpty()) {
            val paths = importedPaths.toList()
            container.appScope.launch { container.files.delete(paths) }
        }
    }

    private fun importFile(target: PickTarget, block: suspend () -> Any) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    is Attachment -> {
                        _state.value.receipt?.let { discard(listOfNotNull(it.path, it.thumbPath)) }
                        importedPaths += listOfNotNull(result.path, result.thumbPath)
                        _state.update { it.copy(receipt = result, busy = false) }
                    }
                    is Photo -> {
                        _state.value.photo?.let { discard(listOfNotNull(it.path, it.thumbPath)) }
                        importedPaths += listOfNotNull(result.path, result.thumbPath)
                        _state.update { it.copy(photo = result, busy = false) }
                    }
                    else -> _state.update { it.copy(busy = false) }
                }
            } catch (e: Exception) {
                val what = if (target == PickTarget.RECEIPT) "Fiş/fatura" else "Fotoğraf"
                _state.update { it.copy(busy = false, message = "$what eklenemedi") }
            }
        }
    }

    private fun persist() {
        handle[KEY_DRAFT] = _state.value.toFormBundle().apply {
            putLong("createdAt", originalCreatedAt)
            putBoolean("deliveryTouched", deliveryTouched)
            putStringArrayList("imported", ArrayList(importedPaths))
            putStringArrayList("replaced", ArrayList(replacedPaths))
            pendingCamera?.let { (file, target) ->
                putString("cameraFile", file.path)
                putString("cameraTarget", target.name)
            }
            initialForm?.let { putBundle("initial", it.toFormBundle()) }
        }
    }

    private fun restore(b: Bundle) {
        _state.value = b.toForm().copy(loaded = true, isEdit = isEdit)
        originalCreatedAt = b.getLong("createdAt")
        deliveryTouched = b.getBoolean("deliveryTouched")
        importedPaths += b.getStringArrayList("imported").orEmpty()
        replacedPaths += b.getStringArrayList("replaced").orEmpty()
        val cameraFile = b.getString("cameraFile")
        val cameraTarget = b.getString("cameraTarget")?.let { name -> PickTarget.entries.firstOrNull { it.name == name } }
        if (cameraFile != null && cameraTarget != null) pendingCamera = File(cameraFile) to cameraTarget
        initialForm = b.getBundle("initial")?.toForm()?.formOnly()
    }

    /**
     * Formdan çıkarılan dosya: bu oturumda eklendiyse hemen silinir,
     * kayıtlı üründense kayıttan sonra silinmek üzere bekletilir.
     */
    private fun discard(paths: List<String>) {
        val fresh = paths.filter { it in importedPaths }
        importedPaths.removeAll(fresh)
        if (fresh.isNotEmpty()) viewModelScope.launch { container.files.delete(fresh) }
        replacedPaths += paths - fresh.toSet()
    }

    private companion object {
        const val KEY_DRAFT = "draft"
    }
}

/** Yalnızca kullanıcının girdiği alanlar (hata, yükleniyor gibi geçici durumlar olmadan). */
private fun EditUiState.formOnly() = EditUiState(
    loaded = true,
    isEdit = isEdit,
    name = name,
    store = store,
    category = category,
    purchaseDate = purchaseDate,
    deliveryDate = deliveryDate,
    tracksReturn = tracksReturn,
    tracksWarranty = tracksWarranty,
    returnOverride = returnOverride,
    warrantyOverride = warrantyOverride,
    receipt = receipt,
    photo = photo,
)

private fun EditUiState.toFormBundle(): Bundle = Bundle().apply {
    putString("name", name)
    putString("store", store)
    putString("category", category.name)
    putLong("purchase", purchaseDate.toEpochDay())
    putLong("delivery", deliveryDate.toEpochDay())
    putBoolean("tracksReturn", tracksReturn)
    putBoolean("tracksWarranty", tracksWarranty)
    returnOverride?.let { putLong("returnOverride", it.toEpochDay()) }
    warrantyOverride?.let { putLong("warrantyOverride", it.toEpochDay()) }
    receipt?.let {
        putString("receiptPath", it.path)
        putString("receiptThumb", it.thumbPath)
        putString("receiptName", it.fileName)
        putString("receiptMime", it.mimeType)
        putLong("receiptSize", it.sizeBytes)
    }
    photo?.let {
        putString("photoPath", it.path)
        putString("photoThumb", it.thumbPath)
    }
}

private fun Bundle.toForm(): EditUiState {
    fun date(key: String): LocalDate? = if (containsKey(key)) LocalDate.ofEpochDay(getLong(key)) else null
    val receiptPath = getString("receiptPath")
    val photoPath = getString("photoPath")
    return EditUiState(
        loaded = true,
        name = getString("name").orEmpty(),
        store = getString("store").orEmpty(),
        category = Category.entries.firstOrNull { it.name == getString("category") } ?: Category.ELEKTRONIK,
        purchaseDate = date("purchase") ?: LocalDate.now(),
        deliveryDate = date("delivery") ?: LocalDate.now(),
        tracksReturn = getBoolean("tracksReturn", true),
        tracksWarranty = getBoolean("tracksWarranty", true),
        returnOverride = date("returnOverride"),
        warrantyOverride = date("warrantyOverride"),
        receipt = receiptPath?.let {
            Attachment(
                path = it,
                thumbPath = getString("receiptThumb"),
                fileName = getString("receiptName").orEmpty(),
                mimeType = getString("receiptMime") ?: "application/octet-stream",
                sizeBytes = getLong("receiptSize"),
            )
        },
        photo = photoPath?.let { Photo(it, getString("photoThumb")) },
    )
}
