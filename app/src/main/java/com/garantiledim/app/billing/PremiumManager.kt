package com.garantiledim.app.billing

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.garantiledim.app.BuildConfig
import com.garantiledim.app.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Play Console'da tanımlanan tek seferlik (yönetilen) ürünün kimliği. */
const val PREMIUM_PRODUCT_ID = "garantiledim_premium"

/** Paywall'da gösterilen satın alma seçeneği. */
sealed interface PremiumOffer {
    data object Loading : PremiumOffer

    /** [price] Google Play'in kullanıcının ülkesine göre biçimlendirdiği fiyat (ör. "₺149,99"). */
    data class Available(val price: String) : PremiumOffer

    /** Google Play'e ulaşılamadı ya da ürün henüz Play Console'da yayında değil. */
    data object Unavailable : PremiumOffer
}

/** Doğrulanmış satın alma kaydı yalnızca bu dosyada tutulur; yedeklere ve telefon aktarımına girmez. */
private val Context.premiumStore: DataStore<Preferences> by preferencesDataStore(name = "premium")

private object PremiumKeys {
    val json = stringPreferencesKey("purchase_json")
    val signature = stringPreferencesKey("purchase_signature")
}

/**
 * Garantiledim Premium: Google Play Faturalandırma ile tek seferlik "ömür boyu" satın alma.
 *
 * Sunucu yoktur; güvenlik şu kurallarla sağlanır:
 * - Premium yalnızca Google Play'in uygulamaya özel anahtarıyla imzalanmış, bu pakete ve
 *   `garantiledim_premium` ürününe ait, ödemesi tamamlanmış satın alma kaydıyla açılır
 *   ([PurchaseVerifier]). Sahte faturalandırma servisleri geçerli imza üretemez.
 * - Cihazda "premium = true" gibi bir bayrak tutulmaz; Play'in imzalı kaydı saklanır ve her açılışta
 *   imzası yeniden doğrulanır. Kayıt elle değiştirilirse imza tutmaz.
 * - Kayıt yedeklere ve telefon aktarımına girmez (backup kuralları); başka cihaza kopyalanamaz.
 * - Süre ya da tarih kullanılmaz (deneme yok, ömür boyu); telefonun saatini değiştirmek bir şey açmaz.
 * - Play'e her ulaşıldığında satın almalar yeniden sorgulanır; iade edilen satın alma kaydı silinir.
 * - Bekleyen (PENDING) ödeme Premium açmaz.
 */
class PremiumManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val strings: () -> Strings,
    licenseKey: String = BuildConfig.PLAY_LICENSE_KEY,
) : PurchasesUpdatedListener {

    private val verifier = PurchaseVerifier(licenseKey)
    private val store = context.applicationContext.premiumStore

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Yalnızca testlerde (debug) kullanılan sahiplik durumu. */
    private val testOwned = MutableStateFlow<Boolean?>(null)

    private val storedOwned = store.data
        .catch { emit(emptyPreferences()) }
        .map { isValidRecord(it[PremiumKeys.json], it[PremiumKeys.signature]) }

    /** Kullanıcı Premium sahibi mi: saklanan imzalı satın alma kaydı her seferinde doğrulanır. */
    val isPremium: StateFlow<Boolean> = combine(storedOwned, testOwned) { stored, test -> test ?: stored }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Arka plan işleri için güncel durum (StateFlow'un ilk değerini beklemeden). */
    suspend fun isPremiumNow(): Boolean = testOwned.value ?: storedOwned.first()

    private val _offer = MutableStateFlow<PremiumOffer>(PremiumOffer.Loading)
    val offer: StateFlow<PremiumOffer> = _offer.asStateFlow()

    /** Kullanıcıya gösterilecek kısa mesajlar (satın alma bekliyor, hata vb.). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var details: ProductDetails? = null
    private val testMode get() = testOwned.value != null

    /** Play'e bağlanır, ürün fiyatını ve mevcut satın almaları yükler. Tekrar çağrılabilir. */
    fun refresh() {
        if (testMode) return
        if (client.isReady) {
            loadProduct()
            loadPurchases(userRequested = false)
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (testMode) return
                if (result.responseCode == BillingResponseCode.OK) {
                    loadProduct()
                    loadPurchases(userRequested = false)
                } else {
                    _offer.value = PremiumOffer.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection bir sonraki istekte yeniden bağlanır.
            }
        })
    }

    /** Google Play satın alma penceresini açar. */
    fun purchase(activity: Activity) {
        if (testMode) {
            testOwned.value = true
            return
        }
        val productDetails = details
        if (productDetails == null) {
            _messages.tryEmit(strings().playRetryLater)
            refresh()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build())
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) loadPurchases(userRequested = true)
    }

    /** "Satın alımı geri yükle": aynı Google hesabıyla yapılmış satın almayı yeniden sorgular. */
    fun restore() {
        if (testMode) return
        if (client.isReady) {
            loadPurchases(userRequested = true)
        } else {
            refresh()
            _messages.tryEmit(strings().playConnecting)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach(::handle)
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> loadPurchases(userRequested = true)
            else -> _messages.tryEmit(strings().purchaseFailed)
        }
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, productResult ->
            if (testMode) return@queryProductDetailsAsync
            val product = productResult.productDetailsList.firstOrNull()
            val price = product?.oneTimePurchaseOfferDetails?.formattedPrice
            if (result.responseCode == BillingResponseCode.OK && product != null && price != null) {
                details = product
                _offer.value = PremiumOffer.Available(price)
            } else {
                _offer.value = PremiumOffer.Unavailable
            }
        }
    }

    private fun loadPurchases(userRequested: Boolean) {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (testMode) return@queryPurchasesAsync
            if (result.responseCode != BillingResponseCode.OK) {
                // Play'e ulaşılamadı: son bilinen durum korunur.
                if (userRequested) _messages.tryEmit(strings().playRetryLater)
                return@queryPurchasesAsync
            }
            val premium = purchases.firstOrNull { PREMIUM_PRODUCT_ID in it.products && isAuthentic(it) }
            if (premium == null) {
                // Satın alma yok, iade edildi ya da doğrulanamadı: saklanan kayıt silinir.
                clearRecord()
                if (userRequested) _messages.tryEmit(strings().purchaseNotFound)
            } else {
                handle(premium)
                if (userRequested && premium.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _messages.tryEmit(strings().purchaseRestored)
                }
            }
        }
    }

    private fun handle(purchase: Purchase) {
        if (PREMIUM_PRODUCT_ID !in purchase.products) return
        if (!isAuthentic(purchase)) {
            _messages.tryEmit(strings().purchaseNotVerified)
            return
        }
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                scope.launch {
                    store.edit {
                        it[PremiumKeys.json] = purchase.originalJson
                        it[PremiumKeys.signature] = purchase.signature
                    }
                }
                // Onaylanmayan satın alma Google Play tarafından 3 gün içinde iade edilir.
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    client.acknowledgePurchase(params) { }
                }
            }
            Purchase.PurchaseState.PENDING ->
                _messages.tryEmit(strings().purchasePending)
            else -> Unit
        }
    }

    /** Play imzası geçerli ve bu uygulamaya ait mi. */
    private fun isAuthentic(purchase: Purchase): Boolean =
        purchase.packageName == context.packageName && verifier.isValid(purchase.originalJson, purchase.signature)

    /** Saklanan kayıt: imzası geçerli, bu uygulamanın Premium ürünü ve ödemesi tamamlanmış. */
    private fun isValidRecord(json: String?, signature: String?): Boolean {
        if (json == null || signature == null || !verifier.isValid(json, signature)) return false
        val purchase = runCatching { Purchase(json, signature) }.getOrNull() ?: return false
        return purchase.packageName == context.packageName &&
            PREMIUM_PRODUCT_ID in purchase.products &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

    private fun clearRecord() {
        scope.launch {
            store.edit {
                it.remove(PremiumKeys.json)
                it.remove(PremiumKeys.signature)
            }
        }
    }

    /** Testlerde Google Play olmadan fiyat ve sahiplik durumunu ayarlar. Yalnızca debug sürümde çalışır. */
    @VisibleForTesting
    fun setTestState(owned: Boolean, price: String? = "₺709,99") {
        check(BuildConfig.DEBUG) { "Test durumu yalnızca debug sürümde ayarlanabilir" }
        testOwned.value = owned
        _offer.value = price?.let { PremiumOffer.Available(it) } ?: PremiumOffer.Unavailable
    }
}
