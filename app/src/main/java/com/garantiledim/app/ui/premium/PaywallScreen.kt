package com.garantiledim.app.ui.premium

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.R
import com.garantiledim.app.billing.PremiumOffer
import com.garantiledim.app.domain.FREE_PRODUCT_LIMIT
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.BackTopBar
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.legal.LegalDoc
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Paywall'ın hangi işlemden açıldığı; üstteki açıklama buna göre değişir. */
enum class PaywallReason(val route: String) {
    GENERAL("genel"),
    LIMIT("sinir"),
    BACKUP("yedek"),
}

data class PaywallUiState(
    val owned: Boolean = false,
    val offer: PremiumOffer = PremiumOffer.Loading,
)

class PaywallViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<PaywallUiState> = combine(
        container.premium.isPremium,
        container.premium.offer,
    ) { owned, offer -> PaywallUiState(owned, offer) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallUiState())

    val messages: SharedFlow<String> = container.premium.messages

    init {
        container.premium.refresh()
    }

    fun purchase(activity: android.app.Activity) = container.premium.purchase(activity)

    fun restore() = container.premium.restore()

    fun retry() = container.premium.refresh()
}

/**
 * Garantiledim Premium satın alma ekranı (SPEC.md madde 10). Tek seferlik ödeme olduğu, fiyatı ve
 * neleri kapsadığı satın almadan önce açıkça gösterilir (Google Play ödeme şeffaflığı kuralları).
 */
@Composable
fun PaywallScreen(
    reason: PaywallReason,
    onClose: () -> Unit,
    onOpenLegal: (LegalDoc) -> Unit,
    viewModel: PaywallViewModel = viewModel(factory = appViewModelFactory { c, _ -> PaywallViewModel(c) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(title = "", onBack = onClose)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_badge),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Garantiledim Premium", style = GType.ScreenTitle.copy(fontSize = GType.ScreenTitle.fontSize * 1.1f), color = GColors.Text)
                    Text(
                        if (state.owned) "Premium aktif. Desteğin için teşekkürler!" else reason.subtitle(),
                        style = GType.Body,
                        color = GColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Column(Modifier.fillMaxWidth().surfaceCard().padding(vertical = 6.dp)) {
                    Benefit(R.drawable.ic_infinity, GColors.PastelPink, GColors.PastelPinkIcon, "Sınırsız ürün", "İstediğin kadar ürün, fiş ve fatura ekle")
                    HorizontalDivider(color = GColors.SurfaceHigh, modifier = Modifier.padding(horizontal = 14.dp))
                    Benefit(R.drawable.ic_bell, GColors.PastelLavender, GColors.PastelLavenderIcon, "Her ürüne hatırlatma", "Tüm iade ve garanti süreleri için bildirim")
                    HorizontalDivider(color = GColors.SurfaceHigh, modifier = Modifier.padding(horizontal = 14.dp))
                    Benefit(R.drawable.ic_upload, GColors.PastelPeach, GColors.PastelPeachIcon, "Yedekle ve taşı", "Ürünlerin, faturaların ve fotoğrafların tek dosyada; yeni telefona geri yükle")
                    HorizontalDivider(color = GColors.SurfaceHigh, modifier = Modifier.padding(horizontal = 14.dp))
                    Benefit(R.drawable.ic_star, GColors.PastelPink, GColors.PastelPinkIcon, "Tek seferlik ödeme", "Abonelik yok. Bir kez öde, hep senin")
                }

                Text(
                    "Ücretsiz sürümde $FREE_PRODUCT_LIMIT ürüne kadar tüm özellikler (yedekleme hariç) kullanılabilir.",
                    style = GType.Caption,
                    color = GColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    state.owned -> PrimaryButton(text = "Devam et", onClick = onClose)
                    else -> when (val offer = state.offer) {
                        PremiumOffer.Loading -> PrimaryButton(text = null, onClick = {}, enabled = false)
                        is PremiumOffer.Available -> PrimaryButton(
                            text = "Premium'u al · ${offer.price}",
                            onClick = { activity?.let(viewModel::purchase) },
                        )
                        PremiumOffer.Unavailable -> {
                            PrimaryButton(text = "Tekrar dene", onClick = viewModel::retry)
                            Text(
                                "Google Play'e bağlanılamadı. İnternet bağlantını ve Play Store hesabını kontrol et.",
                                style = GType.Small,
                                color = GColors.Warning,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                if (!state.owned) {
                    Text(
                        "Tek seferlik ödeme · Abonelik değildir, otomatik yenilenmez. Ödeme Google Play hesabından alınır.",
                        style = GType.Small,
                        color = GColors.TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = viewModel::restore) {
                        Text("Satın alımı geri yükle", style = GType.Label, color = GColors.Pink)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onOpenLegal(LegalDoc.TERMS) }) {
                        Text(LegalDoc.TERMS.title, style = GType.Small, color = GColors.TextSecondary)
                    }
                    Text("·", style = GType.Small, color = GColors.TextMuted)
                    TextButton(onClick = { onOpenLegal(LegalDoc.PRIVACY) }) {
                        Text(LegalDoc.PRIVACY.title, style = GType.Small, color = GColors.TextSecondary)
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 150.dp),
        )
    }
}

private fun PaywallReason.subtitle(): String = when (this) {
    PaywallReason.LIMIT ->
        "Ücretsiz sürümde en fazla $FREE_PRODUCT_LIMIT ürün takip edebilirsin. Premium ile sınır kalkar."
    PaywallReason.BACKUP ->
        "Yedekleme Premium'a dahil. Ürünlerini ve faturalarını yeni telefonuna güvenle taşı."
    PaywallReason.GENERAL ->
        "Tüm iade ve garanti sürelerin, faturaların tek yerde. Sınırsız."
}

@Composable
private fun Benefit(@DrawableRes icon: Int, background: Color, tint: Color, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(42.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            GIcon(icon, tint = tint, size = 22.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = GType.CardTitle, color = GColors.Text)
            Text(body, style = GType.Caption, color = GColors.TextSecondary)
        }
        GIcon(R.drawable.ic_check, tint = GColors.Success, size = 20.dp)
    }
}

@Composable
private fun PrimaryButton(text: String?, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = GShapes.Field,
        colors = ButtonDefaults.buttonColors(
            containerColor = GColors.Pink,
            contentColor = GColors.OnPastel,
            disabledContainerColor = GColors.SurfaceHigh,
            disabledContentColor = GColors.TextMuted,
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (text == null) {
            CircularProgressIndicator(color = GColors.Pink, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, style = GType.CardTitle.copy(fontSize = GType.Body.fontSize * 1.14f))
        }
    }
}
