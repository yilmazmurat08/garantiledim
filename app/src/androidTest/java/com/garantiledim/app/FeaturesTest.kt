package com.garantiledim.app

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.Product
import com.garantiledim.app.notifications.ReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun hatirlatmaBildirimiGonderilirAyniGunTekrarlanmaz() = runBlocking {
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
        val manager = app.getSystemService(NotificationManager::class.java)
        fun ourTexts() = manager.activeNotifications.mapNotNull {
            it.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        }.filter { it.startsWith("Bildirim Testi") }

        val first = TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertEquals(ListenableWorker.Result.success(), first)
        assertEquals(listOf("Bildirim Testi için İade hakkı süresinin bitmesine 2 gün kaldı"), ourTexts())
        assertEquals(today, app.container.database.productDao().getById(id)!!.returnLastReminder)

        manager.cancelAll()
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertTrue("Aynı gün ikinci kez bildirim gönderilmemeli", ourTexts().isEmpty())

        // CI'da bildirim çekmecesinin görüntüsü için bildirimi yeniden göster
        val entity = app.container.database.productDao().getById(id)!!
        app.container.database.productDao().update(entity.copy(returnLastReminder = null))
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertEquals(1, ourTexts().size)
    }
}
