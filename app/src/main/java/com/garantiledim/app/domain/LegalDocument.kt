package com.garantiledim.app.domain

/** Politikalarda geliştirici olarak görünen ad (uygulamanın adı Garantiledim). */
const val DEVELOPER_NAME = "Lusnika"

/** Kullanıcıların geliştiriciye ulaşacağı adres; politikalarda ve uygulama içinde kullanılır. */
const val CONTACT_EMAIL = "yilmazmurat08@gmail.com"

/** Gizlilik politikası ve kullanım koşulları metinlerinin blokları. */
sealed interface LegalBlock {
    data class Title(val text: String) : LegalBlock
    data class Heading(val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
    data class Bullet(val text: String) : LegalBlock
}

/**
 * assets/legal altındaki metinleri ayrıştırır. Desteklenen biçim: "# " başlık, "## " bölüm başlığı,
 * "- " madde; boş satırla ayrılan diğer satırlar paragraf olur (paragraf içi satır sonları korunur).
 */
fun parseLegal(text: String): List<LegalBlock> {
    val blocks = mutableListOf<LegalBlock>()
    val paragraph = mutableListOf<String>()

    fun flush() {
        if (paragraph.isNotEmpty()) {
            blocks += LegalBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }

    for (raw in text.lines()) {
        val line = raw.trim()
        when {
            line.isEmpty() -> flush()
            line.startsWith("## ") -> {
                flush()
                blocks += LegalBlock.Heading(line.removePrefix("## ").trim())
            }
            line.startsWith("# ") -> {
                flush()
                blocks += LegalBlock.Title(line.removePrefix("# ").trim())
            }
            line.startsWith("- ") -> {
                flush()
                blocks += LegalBlock.Bullet(line.removePrefix("- ").trim())
            }
            else -> paragraph += line
        }
    }
    flush()
    return blocks
}
