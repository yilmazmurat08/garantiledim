package com.garantiledim.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Testlerde kullanılan örnek fatura dosyaları (FileProvider'ın paylaştığı önbellek klasöründe). */
object TestFiles {

    private fun dir(context: Context) = File(context.cacheDir, "camera").apply { mkdirs() }

    fun pdf(context: Context, name: String): File {
        val file = File(dir(context), name)
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val paint = Paint().apply { color = Color.BLACK; textSize = 28f }
        page.canvas.drawText("Garantiledim test faturası", 60f, 100f, paint)
        page.canvas.drawText("Kablosuz Kulaklık · 1.299,00 TL", 60f, 160f, paint)
        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    fun jpeg(context: Context, name: String, width: Int = 2400, height: Int = 1600): File {
        val file = File(dir(context), name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText("FİŞ", 100f, 200f, Paint().apply { color = Color.BLACK; textSize = 120f })
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    fun uri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
