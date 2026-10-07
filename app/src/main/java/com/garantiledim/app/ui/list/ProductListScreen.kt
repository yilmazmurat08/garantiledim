package com.garantiledim.app.ui.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.R
import com.garantiledim.app.domain.ProductFilter
import com.garantiledim.app.domain.ProductSummary
import com.garantiledim.app.domain.TrDates
import com.garantiledim.app.domain.filterProducts
import com.garantiledim.app.domain.sortProducts
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.EmptyMessage
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.Pill
import com.garantiledim.app.ui.components.ProductVisual
import com.garantiledim.app.ui.components.SectionLabel
import com.garantiledim.app.ui.components.StatusBadge
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ListUiState(
    val loaded: Boolean = false,
    val total: Int = 0,
    val active: List<ProductSummary> = emptyList(),
    val expired: List<ProductSummary> = emptyList(),
)

class ProductListViewModel(container: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(ProductFilter.ALL)

    val state: StateFlow<ListUiState> = combine(
        container.products.products,
        container.today.today,
        query,
        filter,
    ) { products, today, q, f ->
        val filtered = filterProducts(sortProducts(products, today), f, q, today)
        ListUiState(loaded = true, total = products.size, active = filtered.active, expired = filtered.expired)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())
}

@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    viewModel: ProductListViewModel = viewModel(factory = appViewModelFactory { c, _ -> ProductListViewModel(c) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Column {
                    Text("Garanti Belgelerim", style = GType.ScreenTitle, color = GColors.Text)
                    Text("${state.total} ürün takip ediliyor", style = GType.Caption.copy(fontSize = GType.Caption.fontSize * 1.08f), color = GColors.TextSecondary)
                }
            }
            item(key = "search") {
                TextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    placeholder = { Text("Ürün veya mağaza ara", style = GType.Body) },
                    leadingIcon = { GIcon(R.drawable.ic_search, tint = GColors.TextMuted, size = 20.dp) },
                    singleLine = true,
                    textStyle = GType.Body,
                    shape = GShapes.Field,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GColors.Surface,
                        unfocusedContainerColor = GColors.Surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GColors.Pink,
                        focusedTextColor = GColors.Text,
                        unfocusedTextColor = GColors.Text,
                        focusedPlaceholderColor = GColors.TextMuted,
                        unfocusedPlaceholderColor = GColors.TextMuted,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            item(key = "filters") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProductFilter.entries.forEach { f ->
                        Pill(text = f.label, selected = f == filter, onClick = { viewModel.filter.value = f })
                    }
                }
            }

            if (state.loaded && state.total == 0) {
                item(key = "empty") {
                    EmptyMessage(
                        title = "Henüz ürün yok",
                        body = "Sağ alttaki + ile ilk ürününü ve faturasını ekle.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else if (state.loaded && state.active.isEmpty() && state.expired.isEmpty()) {
                item(key = "no-match") {
                    EmptyMessage(
                        title = "Sonuç yok",
                        body = "Bu filtreye uyan ürün yok.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            items(state.active, key = { it.product.id }) { summary ->
                ProductRow(summary, onClick = { onOpenProduct(summary.product.id) })
            }
            if (state.expired.isNotEmpty()) {
                item(key = "expired-header") {
                    SectionLabel("Süresi dolanlar", Modifier.padding(start = 4.dp, top = 6.dp))
                }
                items(state.expired, key = { it.product.id }) { summary ->
                    ProductRow(summary, onClick = { onOpenProduct(summary.product.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddProduct,
            shape = GShapes.Card,
            containerColor = GColors.Pink,
            contentColor = GColors.OnPastel,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
                .size(58.dp),
        ) {
            GIcon(R.drawable.ic_plus, tint = GColors.OnPastel, size = 26.dp, contentDescription = "Yeni garanti ekle")
        }
    }
}

@Composable
private fun ProductRow(summary: ProductSummary, onClick: () -> Unit) {
    val product = summary.product
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surfaceCard(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductVisual(
            category = product.category,
            thumbPath = product.photo?.thumbPath ?: product.photo?.path,
            shape = GShapes.Field,
            dimmed = summary.expired,
            modifier = Modifier.size(48.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(product.name, style = GType.CardTitle, color = GColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(product.store, product.category.label).filter { it.isNotBlank() }.joinToString(" · "),
                style = GType.Caption,
                color = GColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            summary.primary?.let { d ->
                Text(
                    "${d.type.label} · ${TrDates.short(d.endDate)}",
                    style = GType.Small.copy(fontWeight = GType.Label.fontWeight),
                    color = GColors.Lavender,
                )
            }
        }
        StatusBadge(summary.shortLabel, summary.urgency)
    }
}
