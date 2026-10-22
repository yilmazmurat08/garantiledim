package com.garantiledim.app.billing

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Google Play satın alma kaydının (Purchase.originalJson) Play'in uygulamaya özel anahtarıyla
 * imzalandığını doğrular. Sahte faturalandırma servisleri ve elle değiştirilmiş önbellek geçerli
 * imza üretemez. Anahtar Play Console > Para kazanma kurulumu > Lisanslama'daki genel anahtardır.
 */
class PurchaseVerifier(base64PublicKey: String) {

    private val key: PublicKey? = runCatching {
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey)))
    }.getOrNull()

    /** Anahtar yapılandırılmış mı; değilse hiçbir satın alma doğrulanmaz. */
    val isConfigured: Boolean get() = key != null

    fun isValid(signedData: String, signature: String): Boolean {
        val publicKey = key ?: return false
        if (signedData.isEmpty() || signature.isEmpty()) return false
        return runCatching {
            Signature.getInstance("SHA1withRSA").run {
                initVerify(publicKey)
                update(signedData.toByteArray(Charsets.UTF_8))
                verify(Base64.getDecoder().decode(signature))
            }
        }.getOrDefault(false)
    }
}
