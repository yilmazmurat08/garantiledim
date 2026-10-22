package com.garantiledim.app.billing

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
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
import com.garantiledim.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * Garantiledim Premium: Google Play Faturalandırma ile tek seferlik "ömür boyu" satın alma.
 * Sunucu yoktur; satın alma Google Play'den sorgulanır ve sonucu cihazda saklanır.
 */
class PremiumManager(
    context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Kullanıcı Premium sahibi mi (son doğrulanan durum, uygulama açılışında önbellekten gelir). */
    val isPremium: StateFlow<Boolean> = settings.premiumOwned
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _offer = MutableStateFlow<PremiumOffer>(PremiumOffer.Loading)
    val offer: StateFlow<PremiumOffer> = _offer.asStateFlow()

    /** Kullanıcıya gösterilecek kısa mesajlar (satın alma bekliyor, hata vb.). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var details: ProductDetails? = null
    private var testMode = false

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
            setOwned(true)
            return
        }
        val productDetails = details
        if (productDetails == null) {
            _messages.tryEmit("Google Play'e bağlanılamadı, biraz sonra tekrar dene")
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
            _messages.tryEmit("Google Play'e bağlanılıyor, biraz sonra tekrar dene")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach(::handle)
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> loadPurchases(userRequested = true)
            else -> _messages.tryEmit("Satın alma tamamlanamadı, tekrar dene")
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
                if (userRequested) _messages.tryEmit("Google Play'e bağlanılamadı, biraz sonra tekrar dene")
                return@queryPurchasesAsync
            }
            val premium = purchases.firstOrNull { PREMIUM_PRODUCT_ID in it.products }
            if (premium == null) {
                // Satın alma yok ya da iade edildi.
                setOwned(false)
                if (userRequested) _messages.tryEmit("Bu Google hesabında Premium satın alımı bulunamadı")
            } else {
                handle(premium)
                if (userRequested && premium.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _messages.tryEmit("Premium geri yüklendi")
                }
            }
        }
    }

    private fun handle(purchase: Purchase) {
        if (PREMIUM_PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                setOwned(true)
                // Onaylanmayan satın alma Google Play tarafından 3 gün içinde iade edilir.
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    client.acknowledgePurchase(params) { }
                }
            }
            Purchase.PurchaseState.PENDING ->
                _messages.tryEmit("Ödeme onay bekliyor; tamamlanınca Premium kendiliğinden açılır")
            else -> Unit
        }
    }

    private fun setOwned(owned: Boolean) {
        scope.launch { settings.setPremiumOwned(owned) }
    }

    /** Testlerde Google Play olmadan fiyat ve sahiplik durumunu ayarlar. */
    @VisibleForTesting
    suspend fun setTestState(owned: Boolean, price: String? = "₺149,99") {
        testMode = true
        _offer.value = price?.let { PremiumOffer.Available(it) } ?: PremiumOffer.Unavailable
        settings.setPremiumOwned(owned)
    }
}
