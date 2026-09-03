package com.muhammedbayram.temizpdf

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import com.muhammedbayram.temizpdf.navigation.AppNavigation
import com.muhammedbayram.temizpdf.navigation.Screen
import com.muhammedbayram.temizpdf.ui.screens.scanner.DocumentScannerHelper
import com.muhammedbayram.temizpdf.ui.theme.AppThemeMode
import com.muhammedbayram.temizpdf.ui.theme.TemizPdfTheme
import com.muhammedbayram.temizpdf.utils.FileUtils

class MainActivity : ComponentActivity() {

    private lateinit var recentStore: RecentPdfStore
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var pendingScanCallback: ((Uri) -> Unit)? = null

    private var navHostController: androidx.navigation.NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        recentStore = RecentPdfStore(applicationContext)

        // Initialize Google ML Kit Scanner Launcher
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanResult?.pdf?.uri?.let { pdfUri ->
                    val fileName = "Tarama_" + System.currentTimeMillis() + ".pdf"
                    recentStore.addOrUpdate(
                        PdfDocumentItem(
                            uri = pdfUri.toString(),
                            name = fileName,
                            pageCount = scanResult.pages?.size ?: 1,
                            sizeFormatted = FileUtils.formatFileSize(FileUtils.getFileSize(this, pdfUri))
                        )
                    )
                    pendingScanCallback?.invoke(pdfUri)
                }
            }
        }

        // Check if opened via intent (e.g. user clicked a PDF file in WhatsApp / Files)
        val initialUri = handleIncomingUri(extractPdfUriFromIntent(intent))

        setContent {
            val themeModeState = remember { mutableStateOf(AppThemeMode.SYSTEM) }

            TemizPdfTheme(themeMode = themeModeState.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    navHostController = navController

                    val startDestination = if (initialUri != null) {
                        val fileName = FileUtils.getFileName(this@MainActivity, initialUri)
                        if (fileName.endsWith(".docx", true) || fileName.endsWith(".doc", true) || fileName.endsWith(".txt", true)) {
                            Screen.DocxReader.createRoute(initialUri)
                        } else {
                            Screen.Reader.createRoute(initialUri)
                        }
                    } else {
                        Screen.Home.route
                    }

                    AppNavigation(
                        navController = navController,
                        recentStore = recentStore,
                        themeMode = themeModeState.value,
                        onToggleThemeMode = {
                            themeModeState.value = when (themeModeState.value) {
                                AppThemeMode.SYSTEM -> AppThemeMode.DARK
                                AppThemeMode.DARK -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.DARK
                            }
                        },
                        onStartScan = {
                            val client = DocumentScannerHelper.createScannerClient(this@MainActivity)
                            pendingScanCallback = { uri ->
                                navController.navigate(Screen.Reader.createRoute(uri))
                            }
                            DocumentScannerHelper.startScan(
                                activity = this@MainActivity,
                                scannerClient = client,
                                onIntentReady = { request ->
                                    scannerLauncher.launch(request)
                                },
                                onError = { e ->
                                    Toast.makeText(this@MainActivity, "Tarayıcı açılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = handleIncomingUri(extractPdfUriFromIntent(intent))
        if (uri != null) {
            val fileName = FileUtils.getFileName(this, uri)
            val route = if (fileName.endsWith(".docx", true) || fileName.endsWith(".doc", true) || fileName.endsWith(".txt", true)) {
                Screen.DocxReader.createRoute(uri)
            } else {
                Screen.Reader.createRoute(uri)
            }
            navHostController?.navigate(route)
        }
    }

    private fun handleIncomingUri(rawUri: Uri?): Uri? {
        if (rawUri == null) return null
        return try {
            if (rawUri.scheme == "content") {
                try {
                    contentResolver.takePersistableUriPermission(
                        rawUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Not persistable (e.g. WhatsApp, temporary stream)
                }
            }

            val authority = rawUri.authority ?: ""
            val isWhatsAppOrTemp = rawUri.scheme == "content" &&
                (authority.contains("whatsapp", ignoreCase = true) || authority.contains("mail", ignoreCase = true))

            val finalUri = if (isWhatsAppOrTemp) {
                val copied = FileUtils.copyUriToTempFile(this, rawUri)
                if (copied.exists() && copied.length() > 0) {
                    Uri.fromFile(copied)
                } else {
                    rawUri
                }
            } else {
                rawUri
            }

            val fileName = FileUtils.getFileName(this, finalUri)
            val fileSize = FileUtils.getFileSize(this, finalUri)

            recentStore.addOrUpdate(
                PdfDocumentItem(
                    uri = finalUri.toString(),
                    name = fileName,
                    sizeFormatted = FileUtils.formatFileSize(fileSize)
                )
            )
            finalUri
        } catch (e: Exception) {
            e.printStackTrace()
            rawUri
        }
    }

    private fun extractPdfUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        val action = intent.action
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) {
            return intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return null
    }
}
