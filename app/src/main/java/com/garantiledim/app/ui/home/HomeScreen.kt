package com.garantiledim.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.R
import com.garantiledim.app.domain.ProductSummary
import com.garantiledim.app.domain.TrDates
import com.garantiledim.app.domain.hasAlert
import com.garantiledim.app.domain.homeHighlights
import com.garantiledim.app.ui.Tab
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.Avatar
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.ProductVisual
import com.garantiledim.app.ui.components.StatusBadge
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loaded: Boolean = false,
    val name: String = "",
    val photoPath: String? = null,
    val hasAlert: Boolean = false,
    val highlights: List<ProductSummary> = emptyList(),
    val hasProducts: Boolean = false,
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        container.products.products,
        container.settings.settings,
        container.today.today,
    ) { products, settings, today ->
        HomeUiState(
            loaded = true,
            name = settings.name,
            photoPath = settings.photo?.thumbPath ?: settings.photo?.path,
            hasAlert = hasAlert(products, today, settings.reminderLeadDays),
            highlights = homeHighlights(products, today),
            hasProducts = products.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

@Composable
fun HomeScreen(
    onAddProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenTab: (Tab) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = appViewModelFactory { c, _ -> HomeViewModel(c) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.loaded) return

    if (!state.hasProducts) {
        FirstRun(onAddProduct)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        GreetingHeader(
            name = state.name,
            photoPath = state.photoPath,
            hasAlert = state.hasAlert,
            onBell = { onOpenTab(Tab.AGENDA) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Önerilen İşlemler",
                style = GType.SectionTitle,
                color = GColors.Text,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionCard(
                    title = "Yeni Garanti Ekle",
                    description = "Hızlıca yeni ürün ve belgesini kaydet.",
                    icon = R.drawable.ic_camera,
                    badgeIcon = R.drawable.ic_camera,
                    colors = ActionColors(GColors.PastelPink, GColors.PastelPinkInner, GColors.PastelPinkBadge, GColors.PastelPinkIcon, GColors.PastelPinkText),
                    onClick = onAddProduct,
                )
                ActionCard(
                    title = "Hatırlatıcı Ayarla",
                    description = "Yaklaşan bitiş tarihleri için alarm",
                    icon = R.drawable.ic_calendar_check,
                    badgeIcon = R.drawable.ic_bell,
                    colors = ActionColors(GColors.PastelLavender, GColors.PastelLavenderInner, GColors.PastelLavenderBadge, GColors.PastelLavenderIcon, GColors.PastelLavenderText),
                    onClick = { onOpenTab(Tab.AGENDA) },
                )
                ActionCard(
                    title = "Belgeleri Düzenle",
                    description = "Tüm kayıtları kolayca filtrele",
                    icon = R.drawable.ic_search,
                    badgeIcon = R.drawable.ic_clock,
                    colors = ActionColors(GColors.PastelPeach, GColors.PastelPeachInner, GColors.PastelPeachBadge, GColors.PastelPeachIcon, GColors.PastelPeachText),
                    onClick = { onOpenTab(Tab.LIST) },
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Günün Garantileri", style = GType.SectionTitle, color = GColors.Text, modifier = Modifier.weight(1f))
                TextButton(onClick = { onOpenTab(Tab.LIST) }) {
                    Text("Tümü", style = GType.Label, color = GColors.Pink)
                }
            }
            state.highlights.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { summary ->
                        HighlightCard(
                            summary = summary,
                            onClick = { onOpenProduct(summary.product.id) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (state.highlights.isEmpty()) {
                Text(
                    "Yaklaşan ya da yeni dolan bir süre yok. Tüm ürünlerin Garanti Belgelerim'de.",
                    style = GType.Caption,
                    color = GColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun GreetingHeader(
    name: String,
    photoPath: String?,
    hasAlert: Boolean,
    onBell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(GShapes.Header)
            .background(GColors.Surface)
            .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(name = name, photoPath = photoPath, size = 54.dp)
        Column(Modifier.weight(1f)) {
            Text(
                if (name.isBlank()) "Merhaba!" else "Merhaba, $name!",
                style = GType.Greeting,
                color = GColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Garanti sürelerini takipte kal!", style = GType.Caption.copy(fontSize = GType.Caption.fontSize * 1.08f), color = GColors.TextSecondary)
        }
        IconButton(onClick = onBell) {
            Box {
                GIcon(
                    R.drawable.ic_bell,
                    tint = GColors.TextSecondary,
                    size = 22.dp,
                    contentDescription = if (hasAlert) "Bildirimler, acil hatırlatma var" else "Bildirimler",
                )
                if (hasAlert) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 1.dp, y = (-1).dp)
                            .size(9.dp)
                            .background(GColors.Surface, CircleShape)
                            .padding(1.5.dp)
                            .background(GColors.Danger, CircleShape)
                    )
                }
            }
        }
    }
}

private data class ActionColors(
    val background: Color,
    val inner: Color,
    val badge: Color,
    val icon: Color,
    val description: Color,
)

@Composable
private fun ActionCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    @DrawableRes badgeIcon: Int,
    colors: ActionColors,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(136.dp)
            .fillMaxHeight()
            .padding(top = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clip(GShapes.Card)
                .background(colors.background)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 12.dp, top = 26.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .clip(GShapes.Inner)
                    .background(colors.inner),
                contentAlignment = Alignment.Center,
            ) {
                GIcon(icon, tint = colors.icon, size = 32.dp)
            }
            Text(title, style = GType.ActionTitle, color = GColors.OnPastel)
            Text(description, style = GType.Small.copy(lineHeight = GType.Small.fontSize * 1.36f), color = colors.description)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(36.dp)
                .background(GColors.Background, CircleShape)
                .padding(3.dp)
                .background(colors.badge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GIcon(badgeIcon, tint = colors.icon, size = 16.dp)
        }
    }
}

@Composable
private fun HighlightCard(summary: ProductSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val product = summary.product
    val storeOrCategory = product.store.ifBlank { product.category.label }
    Column(
        modifier = modifier
            .surfaceCard(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(GColors.Outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    storeOrCategory.first().uppercaseChar().toString(),
                    style = GType.Badge.copy(fontSize = GType.Badge.fontSize * 0.9f),
                    color = GColors.Text,
                )
            }
            Text(storeOrCategory, style = GType.Small, color = GColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ProductVisual(
            category = product.category,
            thumbPath = product.photo?.thumbPath ?: product.photo?.path,
            iconSize = 44.dp,
            dimmed = summary.expired,
            modifier = Modifier.fillMaxWidth().height(92.dp),
        )
        Text(
            product.name,
            style = GType.BodyMedium.copy(fontWeight = GType.CardTitle.fontWeight),
            color = GColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        StatusBadge(summary.label, summary.urgency)
        summary.primary?.let { d ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                GIcon(R.drawable.ic_clock, tint = GColors.TextSecondary, size = 14.dp)
                Text(
                    "${d.type.shortLabel} · ${TrDates.short(d.endDate)}",
                    style = GType.Small,
                    color = GColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** İlk açılış: henüz ürün yokken logo ve ekleme çağrısı. */
@Composable
private fun FirstRun(onAddProduct: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_badge),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text("Garantiledim", style = GType.Wordmark, color = GColors.Text)
        Spacer(Modifier.height(6.dp))
        Text(
            "Garanti sürelerini takipte kal!",
            style = GType.Body,
            color = GColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddProduct,
            shape = GShapes.Field,
            colors = ButtonDefaults.buttonColors(containerColor = GColors.Pink, contentColor = GColors.OnPastel),
            modifier = Modifier.height(54.dp),
        ) {
            Text("İlk ürününü ekle", style = GType.CardTitle.copy(fontSize = GType.Body.fontSize * 1.14f))
        }
    }
}
