package com.garantiledim.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garantiledim.app.R

/** SPEC.md madde 2.1 ve 2.2'deki renkler. */
object GColors {
    val Background = Color(0xFF1C0D33)
    val Surface = Color(0xFF2A1747)
    val SurfaceHigh = Color(0xFF362057)
    val Outline = Color(0xFF4A3270)
    val DashedOutline = Color(0xFF6A4E96)
    val Text = Color(0xFFF7F2FF)
    val TextSecondary = Color(0xFFC7B6E6)
    val TextMuted = Color(0xFFB9A6DC)
    val Pink = Color(0xFFFF7AB8)
    val Lavender = Color(0xFFA78BFA)
    val OnPastel = Color(0xFF2A0F3D)

    val PastelPink = Color(0xFFF9B4D6)
    val PastelPinkInner = Color(0xFFFCD3E7)
    val PastelPinkBadge = Color(0xFFFFE3F1)
    val PastelPinkIcon = Color(0xFFB8336F)
    val PastelPinkText = Color(0xFF4A2350)

    val PastelLavender = Color(0xFFC4AAFA)
    val PastelLavenderInner = Color(0xFFD9C9FC)
    val PastelLavenderBadge = Color(0xFFECE2FF)
    val PastelLavenderIcon = Color(0xFF6B3FD1)
    val PastelLavenderText = Color(0xFF3E2266)

    val PastelPeach = Color(0xFFF7B48E)
    val PastelPeachInner = Color(0xFFFACBB0)
    val PastelPeachBadge = Color(0xFFFFE6D8)
    val PastelPeachIcon = Color(0xFFA2471F)
    val PastelPeachText = Color(0xFF5A2A16)

    val Success = Color(0xFF5EDB9F)
    val Warning = Color(0xFFFFC266)
    val Danger = Color(0xFFFF6B8B)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/** SPEC.md madde 2.3'teki yazı stilleri. */
object GType {
    private fun style(size: Int, line: Int, weight: FontWeight) =
        TextStyle(fontFamily = Poppins, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

    val ScreenTitle = style(22, 30, FontWeight.SemiBold)
    val Greeting = style(20, 28, FontWeight.SemiBold)
    val SectionTitle = style(18, 24, FontWeight.SemiBold)
    val CardTitle = style(15, 20, FontWeight.SemiBold)
    val ActionTitle = style(15, 19, FontWeight.Bold)
    val Body = style(14, 20, FontWeight.Normal)
    val BodyMedium = style(14, 20, FontWeight.Medium)
    val Caption = style(12, 16, FontWeight.Normal)
    val Label = style(12, 16, FontWeight.Medium)
    val Small = style(11, 16, FontWeight.Normal)
    val Badge = style(11, 16, FontWeight.SemiBold)
    val NavLabel = style(11, 13, FontWeight.Medium)
    val Wordmark = style(34, 42, FontWeight.Bold)
}

/** SPEC.md madde 2.4'teki köşe yarıçapları. */
object GShapes {
    val Nav = RoundedCornerShape(24.dp)
    val Header = RoundedCornerShape(20.dp)
    val Card = RoundedCornerShape(18.dp)
    val Field = RoundedCornerShape(14.dp)
    val Inner = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

private val colorScheme = darkColorScheme(
    primary = GColors.Pink,
    onPrimary = GColors.OnPastel,
    primaryContainer = GColors.SurfaceHigh,
    onPrimaryContainer = GColors.Text,
    secondary = GColors.Lavender,
    onSecondary = GColors.OnPastel,
    secondaryContainer = GColors.SurfaceHigh,
    onSecondaryContainer = GColors.Text,
    tertiary = GColors.PastelPeach,
    onTertiary = GColors.OnPastel,
    background = GColors.Background,
    onBackground = GColors.Text,
    surface = GColors.Surface,
    onSurface = GColors.Text,
    surfaceVariant = GColors.SurfaceHigh,
    onSurfaceVariant = GColors.TextSecondary,
    surfaceContainerLowest = GColors.Background,
    surfaceContainerLow = GColors.Surface,
    surfaceContainer = GColors.Surface,
    surfaceContainerHigh = GColors.Surface,
    surfaceContainerHighest = GColors.SurfaceHigh,
    // Snackbar gibi bilgi mesajları: koyu yüzey, açık metin, pembe eylem
    inverseSurface = GColors.SurfaceHigh,
    inverseOnSurface = GColors.Text,
    inversePrimary = GColors.Pink,
    outline = GColors.Outline,
    outlineVariant = GColors.Outline,
    error = GColors.Danger,
    onError = GColors.OnPastel,
)

private val typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Poppins),
        displayMedium = base.displayMedium.copy(fontFamily = Poppins),
        displaySmall = base.displaySmall.copy(fontFamily = Poppins),
        headlineLarge = base.headlineLarge.copy(fontFamily = Poppins),
        headlineMedium = base.headlineMedium.copy(fontFamily = Poppins),
        headlineSmall = base.headlineSmall.copy(fontFamily = Poppins),
        titleLarge = base.titleLarge.copy(fontFamily = Poppins),
        titleMedium = base.titleMedium.copy(fontFamily = Poppins),
        titleSmall = base.titleSmall.copy(fontFamily = Poppins),
        bodyLarge = base.bodyLarge.copy(fontFamily = Poppins),
        bodyMedium = base.bodyMedium.copy(fontFamily = Poppins),
        bodySmall = base.bodySmall.copy(fontFamily = Poppins),
        labelLarge = base.labelLarge.copy(fontFamily = Poppins),
        labelMedium = base.labelMedium.copy(fontFamily = Poppins),
        labelSmall = base.labelSmall.copy(fontFamily = Poppins),
    )
}

@Composable
fun GarantiledimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
