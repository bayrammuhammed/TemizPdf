package com.muhammedbayram.temizpdf.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererHelper(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempFile: File? = null
    private val renderMutex = Mutex()

    // LRU Cache for rendered page bitmaps (up to 30 MB)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 10)
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
                try {
                    if (file.exists() && file.length() > 0) {
                        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                } catch (e: Exception) {
                    fileDescriptor = null
                }
                if (fileDescriptor == null) {
                    tempFile = FileUtils.copyUriToTempFile(context, uri)
                    tempFile?.let {
                        if (it.exists() && it.length() > 0) {
                            fileDescriptor = ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                    }
                }
            } else {
                try {
                    fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                } catch (e: Exception) {
                    fileDescriptor = null
                }
                if (fileDescriptor == null) {
                    tempFile = FileUtils.copyUriToTempFile(context, uri)
                    tempFile?.let {
                        if (it.exists() && it.length() > 0) {
                            fileDescriptor = ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                    }
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
            if (file.exists() && file.length() > 0) {
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                fileDescriptor?.let {
                    pdfRenderer = PdfRenderer(it)
                    pageCount = pdfRenderer?.pageCount ?: 0
                    true
                } ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renderPage(pageIndex: Int, scale: Float = 1.8f, isNightMode: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        // Fast path: memory cache hit
        bitmapCache.get(pageIndex)?.let { return@withContext it }

        // Mutex protects PdfRenderer which is strictly single-threaded and allows only 1 open page
        renderMutex.withLock {
            // Double check cache inside lock
            bitmapCache.get(pageIndex)?.let { return@withLock it }

            val renderer = pdfRenderer ?: return@withLock null
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)

                // PdfRenderer strictly requires ARGB_8888 format
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                bitmapCache.put(pageIndex, bitmap)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try { page?.close() } catch (e: Exception) {}
            }
        }
    }

    suspend fun generateThumbnail(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val bitmap = renderPage(0, scale = 0.5f) ?: return@withContext false
        try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
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
        try {
            tempFile?.delete()
        } catch (e: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
        tempFile = null
        bitmapCache.evictAll()
        pageCount = 0
    }
}
