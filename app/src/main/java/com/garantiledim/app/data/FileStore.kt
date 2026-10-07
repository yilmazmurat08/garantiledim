package com.garantiledim.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.garantiledim.app.domain.Attachment
import com.garantiledim.app.domain.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Fiş, fatura ve fotoğrafları uygulamanın kendi dizinine kopyalar.
 * Orijinal dosya hiç değiştirilmez; listelerde gösterilmek üzere ayrıca küçük bir önizleme üretilir.
 */
class FileStore(private val context: Context) {

    private val receiptsDir get() = File(context.filesDir, "receipts")
    private val photosDir get() = File(context.filesDir, "photos")
    private val thumbsDir get() = File(context.filesDir, "thumbs")
    private val cameraDir get() = File(context.cacheDir, "camera")

    /** Galeriden ya da dosyalardan seçilen fiş/faturayı (resim veya PDF) kopyalar. */
    suspend fun importReceipt(uri: Uri): Attachment = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val displayName = queryDisplayName(uri) ?: "fatura-${LocalDate.now()}.${extensionFor(mime, null)}"
        val dest = newFile(receiptsDir, extensionFor(mime, displayName))
        copyFromUri(uri, dest)
        Attachment(
            path = dest.path,
            thumbPath = createThumbnail(dest, mime),
            fileName = displayName,
            mimeType = mime,
            sizeBytes = dest.length(),
        )
    }

    /** Ürün ya da profil fotoğrafını kopyalar. */
    suspend fun importPhoto(uri: Uri): Photo = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val dest = newFile(photosDir, extensionFor(mime, queryDisplayName(uri)))
        copyFromUri(uri, dest)
        Photo(dest.path, createThumbnail(dest, mime))
    }

    /** Kamera için boş bir hedef dosya ve kameraya verilecek URI. */
    fun createCameraTarget(): Pair<File, Uri> {
        val file = newFile(cameraDir, "jpg")
        return file to uriFor(file)
    }

    suspend fun importCameraReceipt(file: File): Attachment = withContext(Dispatchers.IO) {
        val dest = moveFromCamera(file, receiptsDir)
        Attachment(
            path = dest.path,
            thumbPath = createThumbnail(dest, "image/jpeg"),
            fileName = "fis-${LocalDate.now()}.jpg",
            mimeType = "image/jpeg",
            sizeBytes = dest.length(),
        )
    }

    suspend fun importCameraPhoto(file: File): Photo = withContext(Dispatchers.IO) {
        val dest = moveFromCamera(file, photosDir)
        Photo(dest.path, createThumbnail(dest, "image/jpeg"))
    }

    suspend fun delete(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { runCatching { File(it).delete() } }
    }

    /** Dosyayı başka bir uygulamada açmak için paylaşılabilir URI. */
    fun uriFor(path: String): Uri = uriFor(File(path))

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Orijinal, tam çözünürlüklü dosyayı cihazın İndirilenler klasörüne kopyalar.
     * Android 9 ve öncesinde depolama izni çağıran tarafından alınmış olmalıdır.
     */
    suspend fun exportToDownloads(attachment: Attachment) = withContext(Dispatchers.IO) {
        val source = File(attachment.path)
        if (!source.exists()) throw FileNotFoundException(attachment.path)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, attachment.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, attachment.mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("İndirilenler klasörüne yazılamadı")
            try {
                val output = resolver.openOutputStream(target) ?: throw IOException("Dosya açılamadı")
                output.use { out -> source.inputStream().use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            } catch (e: Exception) {
                resolver.delete(target, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val target = uniqueFile(dir, attachment.fileName)
            source.copyTo(target)
            MediaScannerConnection.scanFile(context, arrayOf(target.path), arrayOf(attachment.mimeType), null)
        }
    }

    private fun copyFromUri(uri: Uri, dest: File) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Dosya açılamadı")
        try {
            input.use { src -> dest.outputStream().use { src.copyTo(it) } }
        } catch (e: Exception) {
            dest.delete()
            throw e
        }
    }

    private fun moveFromCamera(file: File, dir: File): File {
        if (!file.exists() || file.length() == 0L) throw IOException("Fotoğraf çekilemedi")
        val dest = newFile(dir, "jpg")
        file.copyTo(dest, overwrite = true)
        file.delete()
        return dest
    }

    private fun newFile(dir: File, extension: String): File {
        dir.mkdirs()
        return File(dir, "${UUID.randomUUID()}.$extension")
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        var index = 1
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext")
            index++
        }
        return candidate
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun extensionFor(mime: String, displayName: String?): String {
        val fromName = displayName?.substringAfterLast('.', "")?.lowercase()
        if (!fromName.isNullOrEmpty() && fromName.length <= 5) return fromName
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
    }

    private fun createThumbnail(source: File, mime: String): String? = try {
        val bitmap = when {
            mime == "application/pdf" -> renderPdfFirstPage(source)
            mime.startsWith("image/") -> decodeSampled(source)
            else -> null
        }
        bitmap?.let {
            thumbsDir.mkdirs()
            val out = File(thumbsDir, "${source.nameWithoutExtension}.jpg")
            out.outputStream().use { stream -> it.compress(Bitmap.CompressFormat.JPEG, 85, stream) }
            it.recycle()
            out.path
        }
    } catch (e: Exception) {
        null
    }

    private fun decodeSampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= THUMB_SIZE && bounds.outHeight / (sample * 2) >= THUMB_SIZE) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val rotation = when (
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return decoded
        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }

    private fun renderPdfFirstPage(file: File): Bitmap? {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            try {
                if (renderer.pageCount == 0) return null
                val page = renderer.openPage(0)
                try {
                    val scale = THUMB_SIZE.toFloat() / maxOf(page.width, page.height)
                    val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                    val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        }
    }

    private companion object {
        const val THUMB_SIZE = 480
    }
}
