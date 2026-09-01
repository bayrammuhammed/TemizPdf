package com.muhammedbayram.temizpdf.ui.screens.scanner

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

object DocumentScannerHelper {

    fun createScannerClient(activity: Activity): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        return GmsDocumentScanning.getClient(options)
    }

    fun startScan(
        activity: Activity,
        scannerClient: GmsDocumentScanner,
        onIntentReady: (IntentSenderRequest) -> Unit,
        onError: (Exception) -> Unit
    ) {
        scannerClient.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender: IntentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                onIntentReady(request)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
