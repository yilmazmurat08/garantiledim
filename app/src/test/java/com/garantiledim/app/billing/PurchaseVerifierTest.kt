package com.garantiledim.app.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PurchaseVerifierTest {

    private val play = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val verifier = PurchaseVerifier(Base64.getEncoder().encodeToString(play.public.encoded))
    private val json = """{"orderId":"GPA.1","packageName":"com.garantiledim.app","productId":"garantiledim_premium","purchaseState":0}"""

    private fun sign(data: String, key: java.security.PrivateKey): String =
        Base64.getEncoder().encodeToString(
            Signature.getInstance("SHA1withRSA").run {
                initSign(key)
                update(data.toByteArray())
                sign()
            }
        )

    @Test
    fun `Play anahtarıyla imzalanan satın alma geçerli`() {
        assertTrue(verifier.isConfigured)
        assertTrue(verifier.isValid(json, sign(json, play.private)))
    }

    @Test
    fun `sahte ya da değiştirilmiş satın alma reddedilir`() {
        // Başka anahtarla imzalanmış (sahte faturalandırma servisi)
        assertFalse(verifier.isValid(json, sign(json, other.private)))
        // İmzadan sonra içeriği değiştirilmiş kayıt
        val signature = sign(json, play.private)
        assertFalse(verifier.isValid(json.replace("GPA.1", "GPA.2"), signature))
        // Boş ya da bozuk imza
        assertFalse(verifier.isValid(json, ""))
        assertFalse(verifier.isValid(json, "bozuk-imza"))
    }

    @Test
    fun `anahtar yoksa hiçbir satın alma kabul edilmez`() {
        val empty = PurchaseVerifier("")
        assertFalse(empty.isConfigured)
        assertFalse(empty.isValid(json, sign(json, play.private)))
    }
}
