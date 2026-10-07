package com.garantiledim.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.garantiledim.app.R
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.Urgency
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import java.io.File

fun Urgency.color(): Color = when (this) {
    Urgency.SUCCESS -> GColors.Success
    Urgency.WARNING -> GColors.Warning
    Urgency.DANGER, Urgency.EXPIRED -> GColors.Danger
}

@DrawableRes
fun Category.iconRes(): Int = when (this) {
    Category.ELEKTRONIK -> R.drawable.ic_cat_electronics
    Category.BEYAZ_ESYA -> R.drawable.ic_cat_appliance
    Category.GIYIM -> R.drawable.ic_cat_clothing
    Category.DIGER -> R.drawable.ic_cat_other
}

fun Category.tileColor(): Color = when (this) {
    Category.ELEKTRONIK -> Color(0xFF3A2360)
    Category.GIYIM -> Color(0xFF45203F)
    Category.BEYAZ_ESYA, Category.DIGER -> Color(0xFF452A2E)
}

fun Category.iconColor(): Color = when (this) {
    Category.ELEKTRONIK -> GColors.PastelLavender
    Category.GIYIM -> GColors.PastelPink
    Category.BEYAZ_ESYA, Category.DIGER -> GColors.PastelPeach
}

@Composable
fun GIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color = GColors.Text,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/** Renkli kalan süre rozeti: yazı durum renginde, zemin aynı rengin %16'sı. */
@Composable
fun StatusBadge(text: String, urgency: Urgency, modifier: Modifier = Modifier, large: Boolean = false) {
    val color = urgency.color()
    Text(
        text = text,
        style = if (large) GType.Badge.copy(fontSize = GType.Caption.fontSize) else GType.Badge,
        color = color,
        maxLines = 1,
        modifier = modifier
            .background(color.copy(alpha = 0.16f), GShapes.Pill)
            .padding(horizontal = if (large) 10.dp else 8.dp, vertical = if (large) 5.dp else 3.dp),
    )
}

/** Ürün fotoğrafı; yoksa kategori renginde ikonlu kutu. Süresi dolmuşsa soluk. */
@Composable
fun ProductVisual(
    category: Category,
    thumbPath: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    shape: Shape = GShapes.Inner,
    dimmed: Boolean = false,
) {
    Box(
        modifier = modifier
            .alpha(if (dimmed) 0.55f else 1f)
            .clip(shape)
            .background(category.tileColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbPath != null) {
            AsyncImage(
                model = File(thumbPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            GIcon(category.iconRes(), tint = category.iconColor(), size = iconSize)
        }
    }
}

/** Yuvarlak avatar: fotoğraf ya da adın baş harfi. */
@Composable
fun Avatar(name: String, photoPath: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .border(2.dp, GColors.Pink, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(GColors.Outline),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString()
            if (initial != null) {
                Text(initial, style = GType.Greeting.copy(fontSize = (size.value * 0.34f).sp), color = GColors.Text)
            } else {
                GIcon(R.drawable.ic_user, tint = GColors.TextSecondary, size = size * 0.42f)
            }
        }
    }
}

/** Seçilebilir hap şeklinde çip (filtreler, kategoriler). */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = GColors.Pink,
) {
    Box(
        modifier = modifier
            .clip(GShapes.Pill)
            .then(
                if (selected) Modifier.background(selectedColor)
                else Modifier.background(GColors.Surface).border(1.dp, GColors.Outline, GShapes.Pill)
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (selected) GType.Label.copy(fontWeight = GType.Badge.fontWeight) else GType.Label,
            color = if (selected) GColors.OnPastel else GColors.Text,
        )
    }
}

/** Alt menüsüz ekranların üst çubuğu: geri, başlık ve sağda isteğe bağlı eylemler. */
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            GIcon(R.drawable.ic_chevron_left, contentDescription = "Geri")
        }
        Text(
            text = title,
            style = if (centered) GType.CardTitle.copy(fontSize = 16.sp) else GType.Greeting,
            color = GColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        if (centered) {
            Row(Modifier.defaultMinSize(minWidth = 48.dp), content = actions)
        } else {
            actions()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = GType.Label.copy(fontWeight = GType.Badge.fontWeight), color = GColors.TextMuted, modifier = modifier)
}

/** Ekranın ortasında boş durum mesajı. */
@Composable
fun EmptyMessage(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GShapes.Card)
            .background(GColors.Surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = GType.CardTitle, color = GColors.Text)
        Text(
            body,
            style = GType.Caption,
            color = GColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun VerticalSpace(height: Dp) = Spacer(Modifier.height(height))

/** Tıklanabilir yüzey kartı. */
fun Modifier.surfaceCard(shape: Shape = GShapes.Card, onClick: (() -> Unit)? = null): Modifier =
    this
        .clip(shape)
        .background(GColors.Surface)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
