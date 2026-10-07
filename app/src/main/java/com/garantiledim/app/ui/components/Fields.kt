package com.garantiledim.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.garantiledim.app.R
import com.garantiledim.app.domain.Attachment
import com.garantiledim.app.domain.TrDates
import com.garantiledim.app.domain.formatFileSize
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import java.io.File
import java.time.LocalDate

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = GType.Label, color = GColors.TextSecondary, modifier = modifier)
}

@Composable
fun HelpText(text: String, modifier: Modifier = Modifier, color: Color = GColors.TextMuted) {
    Text(text, style = GType.Small, color = color, modifier = modifier)
}

/** Üstünde etiket olan metin alanı. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = error != null,
            textStyle = GType.Body,
            shape = GShapes.Field,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GColors.Surface,
                unfocusedContainerColor = GColors.Surface,
                errorContainerColor = GColors.Surface,
                focusedBorderColor = GColors.Pink,
                unfocusedBorderColor = GColors.Outline,
                errorBorderColor = GColors.Danger,
                cursorColor = GColors.Pink,
                focusedTextColor = GColors.Text,
                unfocusedTextColor = GColors.Text,
                errorTextColor = GColors.Text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) HelpText(error, color = GColors.Danger)
    }
}

/** Tarih alanı: dokununca tarih seçici açılır. */
@Composable
fun DateField(label: String, date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(GShapes.Field)
                .background(GColors.Surface)
                .border(1.dp, GColors.Outline, GShapes.Field)
                .clickable(onClickLabel = "$label seç", role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(TrDates.numeric(date), style = GType.Body, color = GColors.Text, modifier = Modifier.weight(1f))
            GIcon(R.drawable.ic_calendar, tint = GColors.Lavender, size = 18.dp)
        }
    }
}

fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawBehind {
    val stroke = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(radius.toPx()))
}

/** Fiş/fotoğraf eklemek için kesik çizgili büyük buton. */
@Composable
fun DashedActionButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(GShapes.Field)
            .background(GColors.Surface)
            .dashedBorder(GColors.DashedOutline, 14.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        GIcon(icon, tint = GColors.PastelPink, size = 22.dp)
        Text(text, style = GType.Label, color = GColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Fiş/fatura önizlemesi: resimse küçük görsel, PDF ise belge ikonu. */
@Composable
fun ReceiptThumbnail(attachment: Attachment, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 64.dp)
            .clip(GShapes.Inner)
            .background(GColors.Text),
        contentAlignment = Alignment.Center,
    ) {
        if (attachment.thumbPath != null) {
            AsyncImage(
                model = File(attachment.thumbPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GIcon(R.drawable.ic_file, tint = Color(0xFF6A4E96), size = 22.dp)
                Text(if (attachment.isPdf) "PDF" else "DOSYA", style = GType.Badge.copy(fontSize = GType.Badge.fontSize * 0.8f), color = Color(0xFF6A4E96))
            }
        }
    }
}

/** Eklenmiş fiş/fatura satırı (ekleme ekranında, kaldır butonuyla). */
@Composable
fun AttachmentRow(attachment: Attachment, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .surfaceCard(GShapes.Field)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReceiptThumbnail(attachment)
        Column(Modifier.weight(1f)) {
            Text(attachment.fileName, style = GType.BodyMedium, color = GColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (attachment.isPdf) "PDF" else "Görsel"} · ${formatFileSize(attachment.sizeBytes)}",
                style = GType.Small,
                color = GColors.TextSecondary,
            )
        }
        IconButton(onClick = onRemove) {
            GIcon(R.drawable.ic_trash, tint = GColors.TextSecondary, size = 20.dp, contentDescription = "Fişi kaldır")
        }
    }
}
