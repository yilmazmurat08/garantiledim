package com.garantiledim.app.ui.detail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.R
import com.garantiledim.app.domain.Attachment
import com.garantiledim.app.domain.Deadline
import com.garantiledim.app.domain.Product
import com.garantiledim.app.domain.TrDates
import com.garantiledim.app.domain.Urgency
import com.garantiledim.app.domain.daysRemaining
import com.garantiledim.app.domain.deadlines
import com.garantiledim.app.domain.elapsedDescription
import com.garantiledim.app.domain.elapsedFraction
import com.garantiledim.app.domain.formatFileSize
import com.garantiledim.app.domain.isExpired
import com.garantiledim.app.domain.remainingLabel
import com.garantiledim.app.domain.urgencyOf
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.BackTopBar
import com.garantiledim.app.ui.components.DashedActionButton
import com.garantiledim.app.ui.components.EmptyMessage
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.ProductVisual
import com.garantiledim.app.ui.components.ReceiptThumbnail
import com.garantiledim.app.ui.components.StatusBadge
import com.garantiledim.app.ui.components.color
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeadlineUi(
    val deadline: Deadline,
    val urgency: Urgency,
    val label: String,
    val fraction: Float,
    val description: String,
)

data class DetailUiState(
    val loaded: Boolean = false,
    val product: Product? = null,
    val expired: Boolean = false,
    val deadlines: List<DeadlineUi> = emptyList(),
    val message: String? = null,
)

class ProductDetailViewModel(
    private val container: AppContainer,
    handle: SavedStateHandle,
) : ViewModel() {
    private val id: Long = checkNotNull(handle.get<Long>("id"))
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<DetailUiState> = combine(
        container.products.product(id),
        container.today.today,
        message,
    ) { product, today, msg ->
        DetailUiState(
            loaded = true,
            product = product,
            expired = product?.isExpired(today) == true,
            deadlines = product?.deadlines().orEmpty()
                .sortedBy { it.endDate }
                .map { d ->
                    DeadlineUi(
                        deadline = d,
                        urgency = urgencyOf(daysRemaining(d.endDate, today)),
                        label = remainingLabel(d.endDate, today),
                        fraction = elapsedFraction(d, today),
                        description = elapsedDescription(d, today),
                    )
                },
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun download() {
        val receipt = state.value.product?.receipt ?: return
        viewModelScope.launch {
            message.value = try {
                container.files.exportToDownloads(receipt)
                "Fatura İndirilenler klasörüne kaydedildi"
            } catch (e: Exception) {
                "Dosya indirilemedi"
            }
        }
    }

    fun receiptViewIntent(receipt: Attachment): Intent =
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(container.files.uriFor(receipt.path), receipt.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    fun showMessage(text: String) {
        message.value = text
    }

    fun messageShown() {
        message.value = null
    }
}

@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ProductDetailViewModel = viewModel(factory = appViewModelFactory { c, h -> ProductDetailViewModel(c, h) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.download() else viewModel.showMessage("İndirmek için depolama izni gerekiyor")
    }
    val onDownload = {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.download()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val product = state.product
            BackTopBar(title = "Ürün Detayı", onBack = onBack, centered = true) {
                if (product != null) {
                    IconButton(onClick = { onEdit(product.id) }) {
                        GIcon(R.drawable.ic_edit, tint = GColors.TextSecondary, size = 20.dp, contentDescription = "Düzenle")
                    }
                }
            }
            if (!state.loaded) return@Column
            if (product == null) {
                EmptyMessage(
                    title = "Ürün bulunamadı",
                    body = "Bu ürün silinmiş olabilir.",
                    modifier = Modifier.padding(20.dp),
                )
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductVisual(
                    category = product.category,
                    thumbPath = product.photo?.path,
                    iconSize = 64.dp,
                    shape = GShapes.Header,
                    dimmed = state.expired,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
                Column {
                    Text(product.name, style = GType.ScreenTitle, color = GColors.Text)
                    Text(
                        listOf(product.store, product.category.label).filter { it.isNotBlank() }.joinToString(" · "),
                        style = GType.Caption.copy(fontSize = GType.Caption.fontSize * 1.08f),
                        color = GColors.TextSecondary,
                    )
                }

                state.deadlines.forEach { DeadlineCard(it) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoBox("Satın alma", TrDates.numeric(product.purchaseDate), Modifier.weight(1f))
                    InfoBox("Teslim", TrDates.numeric(product.deliveryDate), Modifier.weight(1f))
                }

                val receipt = product.receipt
                if (receipt != null) {
                    ReceiptCard(
                        receipt = receipt,
                        onOpen = {
                            try {
                                context.startActivity(viewModel.receiptViewIntent(receipt))
                            } catch (e: ActivityNotFoundException) {
                                viewModel.showMessage("Bu dosyayı açacak uygulama bulunamadı")
                            }
                        },
                        onDownload = onDownload,
                    )
                } else {
                    DashedActionButton(
                        text = "Fiş / fatura ekle",
                        icon = R.drawable.ic_plus,
                        onClick = { onEdit(product.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun DeadlineCard(ui: DeadlineUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .surfaceCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(ui.deadline.type.endLabel, style = GType.Caption, color = GColors.TextSecondary)
                Text(
                    TrDates.long(ui.deadline.endDate),
                    style = GType.SectionTitle.copy(lineHeight = GType.SectionTitle.fontSize * 1.45f),
                    color = GColors.Text,
                )
            }
            StatusBadge(ui.label, ui.urgency, large = true)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(GShapes.Pill)
                .background(GColors.Outline)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ui.fraction)
                    .fillMaxHeight()
                    .clip(GShapes.Pill)
                    .background(ui.urgency.color())
            )
        }
        Text(ui.description, style = GType.Small, color = GColors.TextMuted)
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.surfaceCard(GShapes.Field).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, style = GType.Small, color = GColors.TextSecondary)
        Text(value, style = GType.BodyMedium, color = GColors.Text)
    }
}

@Composable
private fun ReceiptCard(receipt: Attachment, onOpen: () -> Unit, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surfaceCard()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReceiptThumbnail(receipt, Modifier.clickable(onClickLabel = "Faturayı aç", onClick = onOpen))
        Column(Modifier.weight(1f)) {
            Text(receipt.fileName, style = GType.BodyMedium, color = GColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (receipt.isPdf) "PDF" else "Görsel"} · ${formatFileSize(receipt.sizeBytes)} · orijinal",
                style = GType.Small,
                color = GColors.TextSecondary,
            )
        }
        OutlinedButton(
            onClick = onDownload,
            shape = GShapes.Inner,
            border = BorderStroke(1.dp, GColors.Pink),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GColors.Pink),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(44.dp),
        ) {
            GIcon(R.drawable.ic_download, tint = GColors.Pink, size = 18.dp)
            Text(" İndir", style = GType.Label.copy(fontWeight = GType.Badge.fontWeight))
        }
    }
}
