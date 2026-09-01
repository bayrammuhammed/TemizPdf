package com.muhammedbayram.temizpdf.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.util.zip.ZipInputStream

object DocConverterHelper {

    /**
     * DOCX (Word) dosyasından saf metni paragraflar halinde ayıklar (ZIP word/document.xml ayrıştırma).
     */
    suspend fun extractTextFromDocx(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        var inputStream: InputStream? = null
        var zipInputStream: ZipInputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
            zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(zipInputStream, "UTF-8")

                    var eventType = parser.eventType
                    var isInsideParagraph = false

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name) {
                                    "w:p" -> isInsideParagraph = true
                                    "w:t" -> {
                                        parser.next()
                                        if (parser.text != null) {
                                            stringBuilder.append(parser.text)
                                        }
                                    }
                                    "w:tab" -> stringBuilder.append("\t")
                                    "w:br" -> stringBuilder.append("\n")
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "w:p") {
                                    stringBuilder.append("\n\n")
                                    isInsideParagraph = false
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { zipInputStream?.close() } catch (e: Exception) {}
            try { inputStream?.close() } catch (e: Exception) {}
        }
        return@withContext stringBuilder.toString().trim()
    }

    /**
     * TXT dosyasını okur.
     */
    suspend fun readTextFile(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    return@withContext reader.readText()
                }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Metni (DOCX veya TXT içeriğini) standart A4 PDF belgesine dönüştürür.
     */
    suspend fun convertTextToPdf(
        title: String,
        text: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 genişlik (pt)
            val pageHeight = 842 // A4 yükseklik (pt)
            val margin = 50f
            val printableWidth = (pageWidth - (margin * 2)).toInt()
            val printableHeight = (pageHeight - (margin * 2)).toInt()

            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            val titlePaint = TextPaint().apply {
                color = Color.parseColor("#D32F2F")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val paragraphs = text.split("\n\n")
            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = margin

            // Başlık çizimi (1. Sayfa)
            if (title.isNotEmpty()) {
                val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(margin, currentY)
                titleLayout.draw(canvas)
                canvas.restore()
                currentY += titleLayout.height + 25f
            }

            for (paragraph in paragraphs) {
                if (paragraph.isBlank()) continue

                val layout = StaticLayout.Builder.obtain(paragraph, 0, paragraph.length, textPaint, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(4f, 1.15f)
                    .build()

                if (currentY + layout.height > pageHeight - margin) {
                    // Sayfa numarasını çiz
                    drawPageFooter(canvas, currentPageNumber, pageWidth, pageHeight, margin)
                    pdfDocument.finishPage(page)

                    // Yeni sayfa aç
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin
                }

                canvas.save()
                canvas.translate(margin, currentY)
                layout.draw(canvas)
                canvas.restore()
                currentY += layout.height + 14f
            }

            // Son sayfa alt bilgi
            drawPageFooter(canvas, currentPageNumber, pageWidth, pageHeight, margin)
            pdfDocument.finishPage(page)

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, pageWidth: Int, pageHeight: Int, margin: Float) {
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("- Sayfa $pageNumber -", pageWidth / 2f, pageHeight - (margin / 2f), footerPaint)
    }

    /**
     * PDF dosyasındaki metinleri ayıklar ve .txt dosyası olarak kaydeder.
     */
    suspend fun convertPdfToText(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            document = PDDocument.load(inputFile)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)

            FileOutputStream(outputFile).use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { document?.close() } catch (e: Exception) {}
        }
    }

    /**
     * PDF'in tüm sayfalarını yüksek çözünürlüklü JPEG/PNG görsellerine dönüştürür.
     */
    suspend fun convertPdfToImages(context: Context, inputFile: File, outputDir: File): List<File> = withContext(Dispatchers.IO) {
        val helper = PdfRendererHelper(context)
        val imageFiles = mutableListOf<File>()
        try {
            if (helper.openFile(inputFile)) {
                val total = helper.pageCount
                val baseName = inputFile.nameWithoutExtension
                if (!outputDir.exists()) outputDir.mkdirs()

                for (i in 0 until total) {
                    val bitmap = helper.renderPage(i, scale = 2.5f, isNightMode = false)
                    if (bitmap != null) {
                        val imgFile = File(outputDir, "${baseName}_sayfa_${i + 1}.jpg")
                        FileOutputStream(imgFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        imageFiles.add(imgFile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            helper.close()
        }
        return@withContext imageFiles
    }
}
