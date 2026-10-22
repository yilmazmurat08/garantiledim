package com.garantiledim.app

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.garantiledim.app.data.InvalidBackupException
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.Product
import com.garantiledim.app.domain.canAddProduct
import com.garantiledim.app.domain.lockedProductIds
import com.garantiledim.app.notifications.ReminderWorker
import com.garantiledim.app.ui.edit.EditProductViewModel
import com.garantiledim.app.ui.edit.PickTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/** Fiş/fatura dosya işlemleri ve bildirimlerin gerçek cihazdaki davranışı. */
@RunWith(AndroidJUnit4::class)
class FeaturesTest {

    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val app: GarantiledimApp get() = ApplicationProvider.getApplicationContext()

    @Test
    fun pdfFaturaKopyalanirOnizlenirVeIndirilenlereKaydedilir() = runBlocking {
        val pdf = TestFiles.pdf(app, "test-fatura.pdf")
        val attachment = app.container.files.importReceipt(TestFiles.uri(app, pdf))

        assertEquals("application/pdf", attachment.mimeType)
        assertEquals("test-fatura.pdf", attachment.fileName)
        assertEquals(pdf.length(), File(attachment.path).length())
        assertNotNull("PDF önizlemesi üretilmeli", attachment.thumbPath)
        val thumb = File(attachment.thumbPath!!)
        assertTrue("PDF önizlemesi üretilmeli", thumb.exists() && thumb.length() > 0)

        app.container.files.exportToDownloads(attachment)
        val cursor = app.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("test-fatura%"),
            null,
        )
        cursor.use {
            assertTrue("Fatura İndirilenler'de olmalı", it != null && it.moveToFirst())
            assertEquals(pdf.length(), it!!.getLong(1))
        }
    }

    @Test
    fun kameraFisiOrijinalBoyuttaSaklanirOnizlemeKucuktur() = runBlocking {
        val photo = TestFiles.jpeg(app, "kamera.jpg")
        val originalSize = photo.length()
        val attachment = app.container.files.importCameraReceipt(photo)

        assertEquals(originalSize, File(attachment.path).length())
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(attachment.path, bounds)
        assertEquals(2400, bounds.outWidth)

        val thumbBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(attachment.thumbPath, thumbBounds)
        assertTrue("Önizleme küçük olmalı", thumbBounds.outWidth in 1..1200)
    }

    @Test
    fun formVeKameraFotografiUygulamaKapanincaKorunur() {
        val handle = SavedStateHandle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var restored: EditProductViewModel
        instrumentation.runOnMainSync {
            val vm = EditProductViewModel(app.container, handle)
            assertFalse(vm.hasUnsavedChanges())
            vm.onName("Robot Süpürge")
            vm.onStore("Teknosa")
            vm.onCategory(Category.BEYAZ_ESYA)
            vm.onTrack(DurationType.IADE, false)
            vm.cameraTarget(PickTarget.RECEIPT)
            // Kamera açıkken Android süreci kapatır; yeni ViewModel kayıtlı durumdan kurulur.
            restored = EditProductViewModel(app.container, handle)
        }
        val form = restored.state.value
        assertEquals("Robot Süpürge", form.name)
        assertEquals("Teknosa", form.store)
        assertEquals(Category.BEYAZ_ESYA, form.category)
        assertFalse(form.tracksReturn)
        assertTrue(restored.hasUnsavedChanges())

        // Kameranın yazdığı fotoğraf, yeniden kurulan forma fiş olarak eklenir.
        val cameraFile = File(handle.get<Bundle>("draft")!!.getString("cameraFile")!!)
        TestFiles.jpeg(app, "kamera-donus.jpg").copyTo(cameraFile, overwrite = true)
        instrumentation.runOnMainSync { restored.onCameraResult(true) }
        val deadline = System.currentTimeMillis() + 10_000
        while (restored.state.value.receipt == null && System.currentTimeMillis() < deadline) Thread.sleep(100)
        val receipt = restored.state.value.receipt
        assertNotNull("Kamera fotoğrafı fiş olarak eklenmeli", receipt)
        assertTrue(File(receipt!!.path).length() > 0)
    }

    @Test
    fun yedekAlinirVeGeriYuklenir() = runBlocking {
        val repo = app.container.products
        val dao = app.container.database.productDao()
        dao.getAll().forEach { repo.delete(it.id) }
        val today = LocalDate.now()
        val receipt = app.container.files.importReceipt(TestFiles.uri(app, TestFiles.pdf(app, "yedek-fatura.pdf")))
        repo.save(
            Product(
                name = "Yedek Kulaklık", store = "Mağaza", category = Category.ELEKTRONIK,
                purchaseDate = today.minusDays(3), deliveryDate = today.minusDays(2),
                tracksReturn = true, tracksWarranty = true, warrantyEndOverride = today.plusYears(3),
                receipt = receipt,
            )
        )
        repo.save(
            Product(
                name = "Yedek Mont", store = "", category = Category.GIYIM,
                purchaseDate = today, deliveryDate = today, tracksReturn = true, tracksWarranty = false,
            )
        )
        app.container.settings.setReminderLeadDays(5)

        val backupFile = File(app.cacheDir, "camera/garantiledim-yedek.zip").apply { delete(); createNewFile() }
        assertEquals(2, app.container.backup.export(TestFiles.uri(app, backupFile)))
        assertTrue(backupFile.length() > receipt.sizeBytes)

        // Yedekten sonra veriler değişir; geri yükleme yedekteki hale döndürür.
        dao.getAll().forEach { repo.delete(it.id) }
        repo.save(
            Product(
                name = "Sonradan Eklenen", store = "", category = Category.DIGER,
                purchaseDate = today, deliveryDate = today, tracksReturn = true, tracksWarranty = true,
            )
        )
        app.container.settings.setReminderLeadDays(1)

        assertEquals(2, app.container.backup.restore(TestFiles.uri(app, backupFile)))
        val restored = repo.products.first().sortedBy { it.name }
        assertEquals(listOf("Yedek Kulaklık", "Yedek Mont"), restored.map { it.name })
        val kulaklik = restored.first()
        assertEquals(today.plusYears(3), kulaklik.warrantyEndOverride)
        val restoredReceipt = kulaklik.receipt
        assertNotNull("Fatura geri yüklenmeli", restoredReceipt)
        assertEquals("yedek-fatura.pdf", restoredReceipt!!.fileName)
        assertEquals(receipt.sizeBytes, File(restoredReceipt.path).length())
        assertTrue(File(restoredReceipt.thumbPath!!).exists())
        assertEquals(5, app.container.settings.settings.first().reminderLeadDays)

        // Garantiledim yedeği olmayan dosya reddedilir, mevcut veriler korunur.
        val notBackup = TestFiles.pdf(app, "yedek-degil.pdf")
        val error = runCatching { app.container.backup.restore(TestFiles.uri(app, notBackup)) }.exceptionOrNull()
        assertTrue(error is InvalidBackupException)
        assertEquals(2, repo.products.first().size)
        app.container.settings.setReminderLeadDays(3)
    }

    @Test
    fun ucretsizSurumdeUcUrunSiniriVeKilit() = runBlocking {
        val repo = app.container.products
        val dao = app.container.database.productDao()
        dao.getAll().forEach { repo.delete(it.id) }
        val premium = app.container.premium
        premium.setTestState(owned = false)
        val today = LocalDate.now()
        repeat(4) { i ->
            repo.save(
                Product(
                    name = "Sınır $i", store = "", category = Category.DIGER,
                    purchaseDate = today, deliveryDate = today, tracksReturn = true, tracksWarranty = true,
                )
            )
            Thread.sleep(5)
        }
        val products = repo.products.first()
        assertFalse(canAddProduct(products.size, premium = false))
        val locked = lockedProductIds(products, premium = false)
        assertEquals(listOf("Sınır 3"), products.filter { it.id in locked }.map { it.name })

        premium.setTestState(owned = true)
        assertTrue(premium.isPremiumNow())
        assertTrue(lockedProductIds(products, premium = true).isEmpty())
        dao.getAll().forEach { repo.delete(it.id) }
    }

    @Test
    fun hatirlatmaBildirimiGonderilirAyniGunTekrarlanmaz() = runBlocking {
        // Uygulamanın açılışta başlattığı "kaçırılan kontrol" işi bu testle aynı anda çalışmasın.
        WorkManager.getInstance(app).cancelAllWork().result.get()
        app.container.premium.setTestState(owned = true)
        val manager = app.getSystemService(NotificationManager::class.java)
        manager.cancelAll()

        val today = LocalDate.now()
        val id = app.container.products.save(
            Product(
                name = "Bildirim Testi",
                store = "Test",
                category = Category.DIGER,
                purchaseDate = today.minusDays(12),
                deliveryDate = today.minusDays(12),
                tracksReturn = true,
                tracksWarranty = false,
            )
        )
        fun ourTexts() = manager.activeNotifications.mapNotNull {
            it.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        }.filter { it.startsWith("Bildirim Testi") }

        // Android bildirimleri sıraya alıp ayrı bir iş parçacığında işler; görünene kadar bekle.
        fun awaitTexts(count: Int): List<String> {
            val deadline = System.currentTimeMillis() + 5_000
            while (ourTexts().size != count && System.currentTimeMillis() < deadline) Thread.sleep(100)
            return ourTexts()
        }

        val first = TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertEquals(ListenableWorker.Result.success(), first)
        assertEquals(listOf("Bildirim Testi için İade hakkı süresinin bitmesine 2 gün kaldı"), awaitTexts(1))
        assertEquals(today, app.container.database.productDao().getById(id)!!.returnLastReminder)

        manager.cancelAll()
        awaitTexts(0)
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        Thread.sleep(1_500)
        assertTrue("Aynı gün ikinci kez bildirim gönderilmemeli", ourTexts().isEmpty())

        // CI'da bildirim çekmecesinin görüntüsü için bildirimi yeniden göster
        val entity = app.container.database.productDao().getById(id)!!
        app.container.database.productDao().update(entity.copy(returnLastReminder = null))
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertEquals(1, awaitTexts(1).size)
    }
}
