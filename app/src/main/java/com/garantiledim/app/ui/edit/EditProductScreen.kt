package com.garantiledim.app.ui.edit

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.R
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.TrDates
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.AttachmentRow
import com.garantiledim.app.ui.components.BackTopBar
import com.garantiledim.app.ui.components.ConfirmDialog
import com.garantiledim.app.ui.components.DashedActionButton
import com.garantiledim.app.ui.components.DateField
import com.garantiledim.app.ui.components.DatePickerSheet
import com.garantiledim.app.ui.components.FieldLabel
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.HelpText
import com.garantiledim.app.ui.components.LabeledTextField
import com.garantiledim.app.ui.components.Pill
import com.garantiledim.app.ui.components.ProductVisual
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType

private enum class DateTarget { PURCHASE, DELIVERY, RETURN_END, WARRANTY_END }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProductScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditProductViewModel = viewModel(factory = appViewModelFactory { c, h -> EditProductViewModel(c, h) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var datePicker by rememberSaveable { mutableStateOf<DateTarget?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var photoMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.result) {
        when (val result = state.result) {
            is EditResult.Created -> onCreated(result.id)
            EditResult.Saved -> onSaved()
            EditResult.Deleted -> onDeleted()
            null -> Unit
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onPicked(PickTarget.RECEIPT, it) }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPicked(PickTarget.PHOTO, it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        viewModel.onCameraResult(ok)
    }
    fun openCamera(target: PickTarget) {
        try {
            camera.launch(viewModel.cameraTarget(target))
        } catch (e: ActivityNotFoundException) {
            viewModel.onCameraUnavailable()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            BackTopBar(
                title = if (state.isEdit) "Ürünü Düzenle" else "Yeni Garanti Ekle",
                onBack = onBack,
            )
            if (!state.loaded) return@Column

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LabeledTextField(
                    label = "Ürün adı *",
                    value = state.name,
                    onValueChange = viewModel::onName,
                    error = state.nameError,
                )
                LabeledTextField(label = "Mağaza", value = state.store, onValueChange = viewModel::onStore)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateField(
                            label = "Satın alma tarihi",
                            date = state.purchaseDate,
                            onClick = { datePicker = DateTarget.PURCHASE },
                            modifier = Modifier.weight(1f),
                        )
                        DateField(
                            label = "Teslim tarihi",
                            date = state.deliveryDate,
                            onClick = { datePicker = DateTarget.DELIVERY },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    state.dateError?.let { HelpText(it, color = GColors.Danger) }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Kategori")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Category.entries.forEach { c ->
                            Pill(
                                text = c.label,
                                selected = c == state.category,
                                onClick = { viewModel.onCategory(c) },
                                selectedColor = GColors.Lavender,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Takip edilecek süreler")
                    DurationType.entries.forEach { type ->
                        PeriodRow(
                            type = type,
                            tracked = state.isTracked(type),
                            endText = TrDates.numeric(state.endDate(type)),
                            manual = state.isManual(type),
                            onToggle = { viewModel.onTrack(type, it) },
                            onPickDate = {
                                datePicker = if (type == DurationType.IADE) DateTarget.RETURN_END else DateTarget.WARRANTY_END
                            },
                            onReset = { viewModel.onOverride(type, null) },
                        )
                    }
                    state.periodError?.let { HelpText(it, color = GColors.Danger) }
                    HelpText(
                        "Mağazadan aldıysan iade hakkını kapatabilir ya da mağazanın süresini girebilirsin. " +
                            "Ek garanti varsa garanti tarihini değiştir."
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Fiş / fatura")
                    val receipt = state.receipt
                    if (receipt == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashedActionButton(
                                text = "Galeriden / dosyadan",
                                icon = R.drawable.ic_image,
                                enabled = !state.busy,
                                onClick = { receiptPicker.launch(arrayOf("image/*", "application/pdf")) },
                                modifier = Modifier.weight(1f),
                            )
                            DashedActionButton(
                                text = "Kamera ile çek",
                                icon = R.drawable.ic_camera,
                                enabled = !state.busy,
                                onClick = { openCamera(PickTarget.RECEIPT) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        AttachmentRow(receipt, onRemove = viewModel::removeReceipt)
                    }
                    HelpText("Fotoğraf veya PDF fatura eklenebilir. Orijinal dosya bozulmadan saklanır.")
                }

                val photo = state.photo
                if (photo == null) {
                    Box {
                        OutlinedButton(
                            onClick = { photoMenu = true },
                            enabled = !state.busy,
                            shape = GShapes.Field,
                            border = BorderStroke(1.dp, GColors.Outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GColors.Text),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            GIcon(R.drawable.ic_plus, tint = GColors.Lavender, size = 20.dp)
                            Text("  Ürün fotoğrafı ekle (opsiyonel)", style = GType.BodyMedium)
                        }
                        DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Galeriden seç", style = GType.Body) },
                                leadingIcon = { GIcon(R.drawable.ic_image, tint = GColors.PastelPink, size = 20.dp) },
                                onClick = {
                                    photoMenu = false
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Kamera ile çek", style = GType.Body) },
                                leadingIcon = { GIcon(R.drawable.ic_camera, tint = GColors.PastelPink, size = 20.dp) },
                                onClick = {
                                    photoMenu = false
                                    openCamera(PickTarget.PHOTO)
                                },
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .surfaceCard(GShapes.Field)
                            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProductVisual(
                            category = state.category,
                            thumbPath = photo.thumbPath ?: photo.path,
                            modifier = Modifier.size(52.dp),
                        )
                        Text("Ürün fotoğrafı", style = GType.BodyMedium, color = GColors.Text, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::removePhoto) {
                            GIcon(R.drawable.ic_trash, tint = GColors.TextSecondary, size = 20.dp, contentDescription = "Fotoğrafı kaldır")
                        }
                    }
                }

                if (state.isEdit) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        GIcon(R.drawable.ic_trash, tint = GColors.Danger, size = 18.dp)
                        Text("  Ürünü sil", style = GType.Label, color = GColors.Danger)
                    }
                }
            }

            Box(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.busy,
                    shape = GShapes.Field,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GColors.Pink,
                        contentColor = GColors.OnPastel,
                        disabledContainerColor = GColors.SurfaceHigh,
                        disabledContentColor = GColors.TextMuted,
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(color = GColors.Pink, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Kaydet", style = GType.CardTitle.copy(fontSize = GType.Body.fontSize * 1.14f))
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
        )
    }

    datePicker?.let { target ->
        val (initial, min) = when (target) {
            DateTarget.PURCHASE -> state.purchaseDate to null
            DateTarget.DELIVERY -> state.deliveryDate to state.purchaseDate
            DateTarget.RETURN_END -> state.endDate(DurationType.IADE) to state.deliveryDate
            DateTarget.WARRANTY_END -> state.endDate(DurationType.GARANTI) to state.deliveryDate
        }
        DatePickerSheet(
            initial = initial,
            minDate = min,
            onDismiss = { datePicker = null },
            onPick = { date ->
                when (target) {
                    DateTarget.PURCHASE -> viewModel.onPurchaseDate(date)
                    DateTarget.DELIVERY -> viewModel.onDeliveryDate(date)
                    DateTarget.RETURN_END -> viewModel.onOverride(DurationType.IADE, date)
                    DateTarget.WARRANTY_END -> viewModel.onOverride(DurationType.GARANTI, date)
                }
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Ürün silinsin mi?",
            text = "\"${state.name}\" ve eklenen fiş/fatura kalıcı olarak silinecek.",
            confirmLabel = "Sil",
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PeriodRow(
    type: DurationType,
    tracked: Boolean,
    endText: String,
    manual: Boolean,
    onToggle: (Boolean) -> Unit,
    onPickDate: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surfaceCard(GShapes.Field)
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Switch(
            checked = tracked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GColors.OnPastel,
                checkedTrackColor = GColors.Pink,
                checkedBorderColor = GColors.Pink,
                uncheckedThumbColor = GColors.TextMuted,
                uncheckedTrackColor = GColors.SurfaceHigh,
                uncheckedBorderColor = GColors.Outline,
            ),
        )
        Column(Modifier.weight(1f)) {
            Text(type.label, style = GType.BodyMedium.copy(fontWeight = GType.CardTitle.fontWeight), color = GColors.Text)
            if (manual && tracked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Elle girildi · ", style = GType.Small, color = GColors.TextMuted)
                    Text(
                        "Sıfırla",
                        style = GType.Badge,
                        color = GColors.Pink,
                        modifier = Modifier
                            .clip(GShapes.Pill)
                            .clickable(onClick = onReset)
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                }
            } else {
                Text(
                    if (type == DurationType.IADE) "Teslimden itibaren 14 gün" else "Teslimden itibaren 2 yıl",
                    style = GType.Small,
                    color = GColors.TextMuted,
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(GShapes.Inner)
                .border(1.dp, if (manual && tracked) GColors.Lavender else GColors.Outline, GShapes.Inner)
                .clickable(enabled = tracked, onClick = onPickDate)
                .height(44.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                endText,
                style = GType.Label,
                color = if (tracked) GColors.Text else GColors.TextMuted.copy(alpha = 0.5f),
            )
        }
    }
}
