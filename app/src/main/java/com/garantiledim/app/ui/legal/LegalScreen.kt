package com.garantiledim.app.ui.legal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.garantiledim.app.R
import com.garantiledim.app.domain.CONTACT_EMAIL
import com.garantiledim.app.domain.LegalBlock
import com.garantiledim.app.domain.parseLegal
import com.garantiledim.app.i18n.AppLanguage
import com.garantiledim.app.i18n.LocalStrings
import com.garantiledim.app.i18n.Strings
import com.garantiledim.app.ui.components.BackTopBar
import com.garantiledim.app.ui.components.EmptyMessage
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LegalDoc(val route: String, private val trAsset: String, private val enAsset: String) {
    PRIVACY("gizlilik", "legal/gizlilik-politikasi.md", "legal/en/privacy-policy.md"),
    TERMS("kosullar", "legal/kullanim-kosullari.md", "legal/en/terms-of-use.md");

    fun asset(language: AppLanguage): String = if (language == AppLanguage.TR) trAsset else enAsset

    fun title(s: Strings): String = if (this == PRIVACY) s.privacyPolicy else s.termsOfUse
}

fun sendContactEmail(context: Context, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL"))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // E-posta uygulaması yok; adres metnin içinde de yazılı.
    }
}

@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val language = LocalStrings.current.language
    val blocks by produceState<List<LegalBlock>?>(initialValue = null, doc, language) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(doc.asset(language)).bufferedReader().use { it.readText() } }
                .map(::parseLegal)
                .getOrDefault(emptyList())
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(title = doc.title(s), onBack = onBack, centered = true)
        val content = blocks ?: return@Column
        if (content.isEmpty()) {
            EmptyMessage(
                title = s.legalLoadErrorTitle,
                body = s.legalLoadErrorBody(CONTACT_EMAIL),
                modifier = Modifier.padding(20.dp),
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(content) { index, block ->
                when (block) {
                    // Başlık üst çubukta gösteriliyor.
                    is LegalBlock.Title -> Unit
                    is LegalBlock.Heading -> Text(
                        block.text,
                        style = GType.CardTitle.copy(fontSize = GType.Body.fontSize * 1.14f),
                        color = GColors.Text,
                        modifier = Modifier.padding(top = if (index <= 2) 4.dp else 14.dp),
                    )
                    is LegalBlock.Paragraph -> Text(
                        block.text,
                        style = GType.Body.copy(lineHeight = GType.Body.fontSize * 1.55f),
                        color = GColors.TextSecondary,
                    )
                    is LegalBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("•", style = GType.Body, color = GColors.Pink)
                        Text(
                            block.text,
                            style = GType.Body.copy(lineHeight = GType.Body.fontSize * 1.55f),
                            color = GColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { sendContactEmail(context, "Garantiledim - ${doc.title(s)}") },
                    shape = GShapes.Field,
                    border = BorderStroke(1.dp, GColors.Outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GColors.Text),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                ) {
                    GIcon(R.drawable.ic_document, tint = GColors.Lavender, size = 20.dp)
                    Text("  " + s.askQuestion(CONTACT_EMAIL), style = GType.Label)
                }
            }
        }
    }
}
