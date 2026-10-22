package com.garantiledim.app.data

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import com.garantiledim.app.BuildConfig
import com.garantiledim.app.domain.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Yedek dosyası okunamadı ya da Garantiledim yedeği değil. */
class InvalidBackupException(message: String) : IOException(message)

/**
 * Premium yedekleme: tüm ürünler, fiş/faturalar, ürün fotoğrafları ve hatırlatma ayarları tek bir
 * .zip dosyasına yazılır. Kullanıcı dosyayı istediği yere (telefon, Google Drive vb.) kaydeder ve
 * yeni telefonda geri yükler. Uygulamanın sunucusu yoktur; dosya yalnızca kullanıcının seçtiği yere gider.
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val dao get() = database.productDao()

    /** Yedeği [target] adresine yazar; yedeklenen ürün sayısını döner. */
    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        val products = dao.getAll()
        val s = settings.settings.first()
        val files = mutableListOf<Pair<String, File>>()

        fun ref(path: String?): String? {
            val file = path?.let(::File)?.takeIf { it.isFile } ?: return null
            val dir = file.parentFile?.name ?: return null
            if (dir !in FILE_DIRS) return null
            val name = "$dir/${file.name}"
            if (files.none { it.first == name }) files += name to file
            return name
        }

        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", LocalDateTime.now().toString())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put(
                "settings",
                JSONObject()
                    .put("name", s.name)
                    .put("reminderLeadDays", s.reminderLeadDays)
                    .put("notifyHour", s.notifyHour)
                    .put("notifyMinute", s.notifyMinute),
            )
            .put("products", JSONArray().apply { products.forEach { put(it.toJson(::ref)) } })

        // "wt" var olan dosyanın içeriğini siler; desteklemeyen sağlayıcılarda (bazı bulut depoları) "w" kullanılır.
        val output = runCatching { context.contentResolver.openOutputStream(target, "wt") }.getOrNull()
            ?: context.contentResolver.openOutputStream(target, "w")
            ?: throw IOException("Dosya açılamadı")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toString(2).toByteArray())
            zip.closeEntry()
            files.forEach { (name, file) ->
                zip.putNextEntry(ZipEntry("files/$name"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        products.size
    }

    /**
     * [source] yedeğindeki ürünleri geri yükler. Telefondaki mevcut ürünler yedektekilerle
     * değiştirilir (çağıran taraf kullanıcıdan onay alır). Geri yüklenen ürün sayısını döner.
     */
    suspend fun restore(source: Uri): Int = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            var json: JSONObject? = null
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("Dosya açılamadı")
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> Unit
                        entry.name == JSON_ENTRY -> json = runCatching { JSONObject(zip.readBytes().decodeToString()) }
                            .getOrElse { throw InvalidBackupException("Yedek dosyası bozuk") }
                        // Yalnızca beklenen klasörlerdeki düz dosya adları kabul edilir (../ gibi yollar atlanır).
                        FILE_ENTRY.matches(entry.name) -> {
                            val rel = entry.name.removePrefix("files/")
                            val dest = File(staging, rel).apply { parentFile?.mkdirs() }
                            dest.outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
            val data = json ?: throw InvalidBackupException("Bu dosya bir Garantiledim yedeği değil")
            if (data.optString("format") != FORMAT) throw InvalidBackupException("Bu dosya bir Garantiledim yedeği değil")
            if (data.optInt("version") > VERSION) {
                throw InvalidBackupException("Bu yedek daha yeni bir sürümle alınmış; önce uygulamayı güncelle")
            }

            // Yedekteki dosyalar yeni adlarla uygulamanın klasörlerine taşınır.
            val moved = mutableMapOf<String, String>()
            fun place(rel: String?): String? {
                if (rel == null) return null
                moved[rel]?.let { return it }
                val staged = File(staging, rel).takeIf { it.isFile } ?: return null
                val dir = File(context.filesDir, rel.substringBefore('/')).apply { mkdirs() }
                val ext = rel.substringAfterLast('.', "").takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
                val dest = File(dir, "${UUID.randomUUID()}$ext")
                if (!staged.renameTo(dest)) staged.copyTo(dest, overwrite = true)
                return dest.path.also { moved[rel] = it }
            }

            val array = data.optJSONArray("products") ?: JSONArray()
            val restored = (0 until array.length()).map { i ->
                runCatching { array.getJSONObject(i).toEntity(::place) }
                    .getOrElse { throw InvalidBackupException("Yedek dosyası bozuk") }
            }

            val old = dao.getAll()
            database.withTransaction {
                dao.deleteAll()
                restored.forEach { dao.insert(it) }
            }
            // Eski ürünlerin dosyaları ve bildirimleri temizlenir.
            old.flatMap { listOfNotNull(it.receiptPath, it.receiptThumbPath, it.photoPath, it.photoThumbPath) }
                .forEach { runCatching { File(it).delete() } }
            NotificationManagerCompat.from(context).cancelAll()

            data.optJSONObject("settings")?.let { s ->
                if (s.has("name")) settings.setName(s.optString("name"))
                s.optInt("reminderLeadDays", 0).takeIf { it in 1..30 }?.let { settings.setReminderLeadDays(it) }
                val hour = s.optInt("notifyHour", -1)
                val minute = s.optInt("notifyMinute", -1)
                if (hour in 0..23 && minute in 0..59) settings.setNotifyTime(hour, minute)
            }
            restored.size
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun ProductEntity.toJson(ref: (String?) -> String?): JSONObject = JSONObject()
        .put("name", name)
        .put("store", store)
        .put("category", category.name)
        .put("purchaseDate", purchaseDate.toString())
        .put("deliveryDate", deliveryDate.toString())
        .put("tracksReturn", tracksReturn)
        .put("tracksWarranty", tracksWarranty)
        .putOpt("returnEndOverride", returnEndOverride?.toString())
        .putOpt("warrantyEndOverride", warrantyEndOverride?.toString())
        .putOpt(
            "receipt",
            ref(receiptPath)?.let { file ->
                JSONObject()
                    .put("file", file)
                    .putOpt("thumb", ref(receiptThumbPath))
                    .putOpt("fileName", receiptFileName)
                    .putOpt("mimeType", receiptMimeType)
                    .putOpt("sizeBytes", receiptSizeBytes)
            },
        )
        .putOpt(
            "photo",
            ref(photoPath)?.let { file -> JSONObject().put("file", file).putOpt("thumb", ref(photoThumbPath)) },
        )
        .put("createdAt", createdAt)
        .putOpt("returnLastReminder", returnLastReminder?.toString())
        .put("returnExpiredNotified", returnExpiredNotified)
        .putOpt("warrantyLastReminder", warrantyLastReminder?.toString())
        .put("warrantyExpiredNotified", warrantyExpiredNotified)

    private fun JSONObject.toEntity(place: (String?) -> String?): ProductEntity {
        fun date(key: String): LocalDate? = optString(key).takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
        val receipt = optJSONObject("receipt")
        val receiptPath = place(receipt?.optString("file"))
        val photo = optJSONObject("photo")
        val photoPath = place(photo?.optString("file"))
        return ProductEntity(
            name = getString("name"),
            store = optString("store"),
            category = Category.entries.firstOrNull { it.name == optString("category") } ?: Category.DIGER,
            purchaseDate = date("purchaseDate")!!,
            deliveryDate = date("deliveryDate")!!,
            tracksReturn = optBoolean("tracksReturn", true),
            tracksWarranty = optBoolean("tracksWarranty", true),
            returnEndOverride = date("returnEndOverride"),
            warrantyEndOverride = date("warrantyEndOverride"),
            receiptPath = receiptPath,
            receiptThumbPath = receiptPath?.let { place(receipt?.optString("thumb")?.takeIf(String::isNotEmpty)) },
            receiptFileName = receiptPath?.let { receipt?.optString("fileName")?.takeIf(String::isNotEmpty) },
            receiptMimeType = receiptPath?.let { receipt?.optString("mimeType")?.takeIf(String::isNotEmpty) },
            receiptSizeBytes = receiptPath?.let { File(it).length() },
            photoPath = photoPath,
            photoThumbPath = photoPath?.let { place(photo?.optString("thumb")?.takeIf(String::isNotEmpty)) },
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            returnLastReminder = date("returnLastReminder"),
            returnExpiredNotified = optBoolean("returnExpiredNotified"),
            warrantyLastReminder = date("warrantyLastReminder"),
            warrantyExpiredNotified = optBoolean("warrantyExpiredNotified"),
        )
    }

    private companion object {
        const val FORMAT = "garantiledim-yedek"
        const val VERSION = 1
        const val JSON_ENTRY = "garantiledim.json"
        val FILE_DIRS = setOf("receipts", "photos", "thumbs")
        val FILE_ENTRY = Regex("files/(receipts|photos|thumbs)/[A-Za-z0-9_-][A-Za-z0-9._-]{0,119}")
    }
}
