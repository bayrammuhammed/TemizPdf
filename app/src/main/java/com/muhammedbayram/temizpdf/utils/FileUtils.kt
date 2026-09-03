package com.muhammedbayram.temizpdf.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            result = it.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            try {
                result = uri.lastPathSegment ?: uri.path
                val cut = result?.lastIndexOf('/') ?: -1
                if (cut != -1) {
                    result = result?.substring(cut + 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result ?: "Adsiz_Belge.pdf"
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            try {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.SIZE)
                        if (index >= 0) {
                            return it.getLong(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (uri.scheme == "file") {
            try {
                val file = File(uri.path ?: "")
                if (file.exists()) return file.length()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return 0L
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Bilinmiyor"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun copyUriToTempFile(context: Context, uri: Uri): File {
        val safeName = getFileName(context, uri).replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val fileName = "temp_" + System.currentTimeMillis() + "_" + safeName
        val tempFile = File(context.cacheDir, fileName)
        try {
            if (uri.scheme == "file") {
                val srcFile = File(uri.path ?: "")
                if (srcFile.exists()) {
                    srcFile.inputStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tempFile
    }

    fun createOutputPdfFile(context: Context, prefix: String = "TemizPDF"): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TemizPDF")
        if (!dir.exists()) dir.mkdirs()
        val timestamp = System.currentTimeMillis()
        return File(dir, "${prefix}_$timestamp.pdf")
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "PDF Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
