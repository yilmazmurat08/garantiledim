package com.garantiledim.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegalDocumentTest {

    private fun asset(name: String) = File("src/main/assets/legal/$name").readText()

    @Test
    fun `biçim ayrıştırılır`() {
        val blocks = parseLegal(
            """
            # Başlık

            Birinci satır
            ikinci satır

            ## Bölüm
            - madde bir
            - madde iki
            Son paragraf
            """.trimIndent()
        )
        assertEquals(
            listOf(
                LegalBlock.Title("Başlık"),
                LegalBlock.Paragraph("Birinci satır\nikinci satır"),
                LegalBlock.Heading("Bölüm"),
                LegalBlock.Bullet("madde bir"),
                LegalBlock.Bullet("madde iki"),
                LegalBlock.Paragraph("Son paragraf"),
            ),
            blocks,
        )
    }

    @Test
    fun `gizlilik politikası eksiksiz`() {
        val text = asset("gizlilik-politikasi.md")
        val blocks = parseLegal(text)
        assertEquals(LegalBlock.Title("Gizlilik Politikası"), blocks.first())
        val headings = blocks.filterIsInstance<LegalBlock.Heading>().map { it.text }
        listOf("Kısaca", "İzinler ve neden kullanıldıkları", "Verilerin saklanması ve silinmesi", "İletişim")
            .forEach { assertTrue("Eksik bölüm: $it", it in headings) }
        assertTrue(text.contains(CONTACT_EMAIL))
        assertTrue(text.contains("$DEVELOPER_NAME tarafından geliştirilmiştir"))
        assertFalse(text.contains("{{"))
    }

    @Test
    fun `kullanım koşulları eksiksiz`() {
        val text = asset("kullanim-kosullari.md")
        val blocks = parseLegal(text)
        assertEquals(LegalBlock.Title("Kullanım Koşulları"), blocks.first())
        val headings = blocks.filterIsInstance<LegalBlock.Heading>().map { it.text }
        listOf("Hukuki tavsiye değildir", "Sorumluluğun sınırlandırılması", "Uygulanacak hukuk", "İletişim")
            .forEach { assertTrue("Eksik bölüm: $it", it in headings) }
        assertTrue(text.contains(CONTACT_EMAIL))
        assertTrue(text.contains("$DEVELOPER_NAME tarafından geliştirilen"))
        assertFalse("Geliştirici adı Garantiledim olarak kalmamalı", text.contains("Garantiledim sorumlu"))
    }
}
