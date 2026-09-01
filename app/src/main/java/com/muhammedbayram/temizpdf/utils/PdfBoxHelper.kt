package com.muhammedbayram.temizpdf.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfBoxHelper {

    suspend fun mergePdfs(inputFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val merger = PDFMergerUtility()
            merger.destinationFileName = outputFile.absolutePath
            for (file in inputFiles) {
                merger.addSource(file)
            }
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun splitPdf(inputFile: File, pageIndices: List<Int>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var srcDoc: PDDocument? = null
        var destDoc: PDDocument? = null
        try {
            srcDoc = PDDocument.load(inputFile)
            destDoc = PDDocument()
            val totalPages = srcDoc.numberOfPages
            for (pageIndex in pageIndices) {
                if (pageIndex in 0 until totalPages) {
                    val page = srcDoc.getPage(pageIndex)
                    destDoc.importPage(page)
                }
            }
            destDoc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { srcDoc?.close() } catch (e: Exception) {}
            try { destDoc?.close() } catch (e: Exception) {}
        }
    }

    suspend fun reorderAndRotatePages(
        inputFile: File,
        pageOrder: List<Int>,
        pageRotations: Map<Int, Int>, // pageIndex -> rotation degrees (0, 90, 180, 270)
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        var srcDoc: PDDocument? = null
        var destDoc: PDDocument? = null
        try {
            srcDoc = PDDocument.load(inputFile)
            destDoc = PDDocument()
            val totalPages = srcDoc.numberOfPages

            for (pageIndex in pageOrder) {
                if (pageIndex in 0 until totalPages) {
                    val page = srcDoc.getPage(pageIndex)
                    val currentRotation = page.rotation
                    val additionalRotation = pageRotations[pageIndex] ?: 0
                    val newRotation = (currentRotation + additionalRotation) % 360

                    val importedPage = destDoc.importPage(page)
                    importedPage.rotation = newRotation
                }
            }
            destDoc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { srcDoc?.close() } catch (e: Exception) {}
            try { destDoc?.close() } catch (e: Exception) {}
        }
    }

    suspend fun convertImagesToPdf(context: Context, imageUris: List<Uri>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            document = PDDocument()
            for (uri in imageUris) {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val page = PDPage(PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat()))
                    document.addPage(page)

                    val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.85f)
                    val contentStream = PDPageContentStream(document, page)
                    contentStream.drawImage(pdImage, 0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                    contentStream.close()
                }
            }
            document.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { document?.close() } catch (e: Exception) {}
        }
    }
}
