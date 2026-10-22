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
        listOf("Kısaca", "İzinler ve neden kullanıldıkları", "Premium satın alma", "Yedek dosyası (Premium)", "Verilerin saklanması ve silinmesi", "İletişim")
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
        listOf("Hukuki tavsiye değildir", "Ücretli özellikler", "Sorumluluğun sınırlandırılması", "Uygulanacak hukuk", "İletişim")
            .forEach { assertTrue("Eksik bölüm: $it", it in headings) }
        assertTrue(text.contains(CONTACT_EMAIL))
        assertTrue(text.contains("$DEVELOPER_NAME tarafından geliştirilen"))
        assertFalse("Geliştirici adı Garantiledim olarak kalmamalı", text.contains("Garantiledim sorumlu"))
        assertTrue("Ücretsiz sınır metinde kodla aynı olmalı", text.contains("en fazla $FREE_PRODUCT_LIMIT ürün"))
        assertTrue(text.contains("abonelik değildir"))
    }

    @Test
    fun `ingilizce metinler eksiksiz`() {
        val privacy = asset("en/privacy-policy.md")
        assertEquals(LegalBlock.Title("Privacy Policy"), parseLegal(privacy).first())
        val privacyHeadings = parseLegal(privacy).filterIsInstance<LegalBlock.Heading>().map { it.text }
        listOf("Summary", "Permissions and why they are used", "Premium purchase", "Backup file (Premium)", "Data retention and deletion", "Contact")
            .forEach { assertTrue("Missing section: $it", it in privacyHeadings) }
        assertTrue(privacy.contains(CONTACT_EMAIL))
        assertTrue(privacy.contains("developed by $DEVELOPER_NAME"))

        val terms = asset("en/terms-of-use.md")
        assertEquals(LegalBlock.Title("Terms of Use"), parseLegal(terms).first())
        val termsHeadings = parseLegal(terms).filterIsInstance<LegalBlock.Heading>().map { it.text }
        listOf("Not legal advice", "Paid features", "Limitation of liability", "Governing law", "Contact")
            .forEach { assertTrue("Missing section: $it", it in termsHeadings) }
        assertTrue(terms.contains("up to $FREE_PRODUCT_LIMIT products"))
        assertTrue(terms.contains("not a subscription"))

        // İki dilde aynı sayıda bölüm olmalı (bir dilde bölüm unutulmasın).
        val trPrivacy = parseLegal(asset("gizlilik-politikasi.md")).count { it is LegalBlock.Heading }
        val trTerms = parseLegal(asset("kullanim-kosullari.md")).count { it is LegalBlock.Heading }
        assertEquals(trPrivacy, privacyHeadings.size)
        assertEquals(trTerms, termsHeadings.size)
    }
}
