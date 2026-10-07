package com.garantiledim.app

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.Product
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Uygulamayı baştan sona gezer, her ekranın görüntüsünü alır ve
 * kaydetme akışının çalıştığını doğrular. Görüntüler CI'da cihazdan çekilir.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTourTest {

    @get:Rule(order = 0)
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private val app: GarantiledimApp get() = ApplicationProvider.getApplicationContext()
    private val outDir: File by lazy { File(app.filesDir, "screenshots").apply { mkdirs() } }

    private fun waitForText(text: String, substring: Boolean = false) {
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(600)
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun seed() = runBlocking {
        val today = LocalDate.now()
        val repo = app.container.products
        app.container.settings.setName("Elif")
        fun product(name: String, store: String, category: Category, delivery: LocalDate, ret: Boolean, war: Boolean) =
            Product(
                name = name,
                store = store,
                category = category,
                purchaseDate = delivery.minusDays(2),
                deliveryDate = delivery,
                tracksReturn = ret,
                tracksWarranty = war,
            )
        val receipt = app.container.files.importReceipt(
            TestFiles.uri(app, TestFiles.pdf(app, "hepsiburada-fatura.pdf"))
        )
        repo.save(
            product("Kablosuz Kulaklık", "Hepsiburada", Category.ELEKTRONIK, today.minusDays(12), ret = true, war = true)
                .copy(receipt = receipt)
        )
        repo.save(product("Kışlık Mont", "Trendyol", Category.GIYIM, today.minusDays(7), ret = true, war = false))
        repo.save(product("Akıllı Saat", "Teknosa", Category.ELEKTRONIK, today.plusDays(70).minusYears(2), ret = false, war = true))
        repo.save(product("Akıllı Telefon", "n11", Category.ELEKTRONIK, today.plusMonths(9).minusYears(2), ret = false, war = true))
        repo.save(product("Buzdolabı", "Arçelik Bayi", Category.BEYAZ_ESYA, today.minusMonths(4), ret = false, war = true))
        repo.save(product("Kahve Makinesi", "MediaMarkt", Category.BEYAZ_ESYA, today.minusDays(8).minusYears(2), ret = false, war = true))
    }

    @Test
    fun uygulamayiGez() {
        waitForText("İlk ürününü ekle")
        shot("00-ilk-acilis")

        seed()
        waitForText("Günün Garantileri")
        shot("01-ana-sayfa")

        rule.onNodeWithText("Garanti Belgelerim").performClick()
        waitForText("ürün takip ediliyor", substring = true)
        shot("02-belgelerim")

        rule.onNodeWithText("Kablosuz Kulaklık").performClick()
        waitForText("İade son günü")
        shot("03-detay")
        rule.onNodeWithText("İndir", substring = true).performScrollTo().performClick()
        waitForText("Fatura İndirilenler klasörüne kaydedildi")
        shot("03b-fatura-indirildi")

        rule.onNodeWithContentDescription("Düzenle").performClick()
        waitForText("Ürünü Düzenle")
        shot("04-duzenle")
        rule.onNodeWithText("Ürünü sil", substring = true).performScrollTo()
        shot("05-duzenle-alt")

        rule.onNodeWithContentDescription("Geri").performClick()
        waitForText("İade son günü")
        rule.onNodeWithContentDescription("Geri").performClick()
        waitForText("ürün takip ediliyor", substring = true)

        rule.onNodeWithText("Ajanda ve Hatırlatıcılar").performClick()
        waitForText("Hatırlatma ayarı")
        shot("06-ajanda")

        rule.onNodeWithText("Profil & Ayarlar").performClick()
        waitForText("Hatırlatma başlangıcı")
        shot("07-profil")

        rule.onNodeWithText("Gizlilik Politikası").performScrollTo().performClick()
        waitForText("Kısaca")
        shot("07b-gizlilik-politikasi")
        rule.onNodeWithContentDescription("Geri").performClick()
        waitForText("Hatırlatma başlangıcı")
        rule.onNodeWithText("Kullanım Koşulları").performScrollTo().performClick()
        waitForText("Hukuki tavsiye değildir")
        shot("07c-kullanim-kosullari")
        rule.onNodeWithContentDescription("Geri").performClick()
        waitForText("Hatırlatma başlangıcı")

        rule.onNodeWithText("Ana Sayfa").performClick()
        waitForText("Günün Garantileri")
        rule.onNodeWithText("Yeni Garanti Ekle").performClick()
        waitForText("Ürün adı *")
        shot("08-yeni-urun")

        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Oyun Konsolu")
        rule.onNodeWithText("Kaydet").performClick()
        waitForText("Garanti bitişi")
        waitForText("Oyun Konsolu")
        shot("09-kaydedilen-urun")
    }
}
