package com.muhammedbayram.temizpdf.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererHelper(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempFile: File? = null

    // LRU Cache for rendered page bitmaps (up to 30 MB)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    var pageCount: Int = 0
        private set

    fun open(uri: Uri): Boolean {
        close()
        return try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (fileDescriptor == null) {
                    tempFile = FileUtils.copyUriToTempFile(context, uri)
                    fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                }
            }
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
                pageCount = pdfRenderer?.pageCount ?: 0
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openFile(file: File): Boolean {
        close()
        return try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
                pageCount = pdfRenderer?.pageCount ?: 0
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renderPage(pageIndex: Int, scale: Float = 2.0f, isNightMode: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        val cacheKey = pageIndex * 10 + (if (isNightMode) 1 else 0)
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        var page: PdfRenderer.Page? = null
        try {
            page = pdfRenderer?.openPage(pageIndex) ?: return@withContext null
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val finalBitmap = if (isNightMode) {
                invertBitmap(bitmap)
            } else {
                bitmap
            }

            bitmapCache.put(cacheKey, finalBitmap)
            return@withContext finalBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            try { page?.close() } catch (e: Exception) {}
        }
    }

    private fun invertBitmap(src: Bitmap): Bitmap {
        val inverted = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(inverted)
        val paint = Paint()
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f,  0f, 255f,
                 0f, -1f,  0f,  0f, 255f,
                 0f,  0f, -1f,  0f, 255f,
                 0f,  0f,  0f,  1f,   0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return inverted
    }

    suspend fun generateThumbnail(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val bitmap = renderPage(0, scale = 0.5f, isNightMode = false) ?: return@withContext false
        try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun close() {
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (e: Exception) {}
        tempFile?.delete()
        pdfRenderer = null
        fileDescriptor = null
        tempFile = null
        bitmapCache.evictAll()
        pageCount = 0
    }
}
