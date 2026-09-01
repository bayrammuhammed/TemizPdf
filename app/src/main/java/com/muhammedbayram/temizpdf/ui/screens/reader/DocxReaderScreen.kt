package com.muhammedbayram.temizpdf.ui.screens.reader

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import com.muhammedbayram.temizpdf.ui.theme.RedPrimary
import com.muhammedbayram.temizpdf.utils.DocConverterHelper
import com.muhammedbayram.temizpdf.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxReaderScreen(
    fileUri: Uri,
    recentStore: RecentPdfStore,
    onNavigateBack: () -> Unit,
    onOpenConvertedPdf: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fileName = remember(fileUri) { FileUtils.getFileName(context, fileUri) }
    val isDocx = fileName.endsWith(".docx", ignoreCase = true)

    var fileContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isNightMode by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(15) }

    LaunchedEffect(fileUri) {
        isLoading = true
        fileContent = if (isDocx) {
            DocConverterHelper.extractTextFromDocx(context, fileUri)
        } else {
            DocConverterHelper.readTextFile(context, fileUri)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (isDocx) "Word Belgesi" else "Metin Belgesi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Gece Modu
                    IconButton(onClick = { isNightMode = !isNightMode }) {
                        Icon(
                            if (isNightMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Gece Modu",
                            tint = if (isNightMode) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // PDF'e Dönüştür Butonu
                    IconButton(
                        onClick = {
                            if (fileContent.isBlank()) {
                                Toast.makeText(context, "Dönüştürülecek metin yok", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            isConverting = true
                            scope.launch {
                                val baseName = fileName.substringBeforeLast(".")
                                val outputFile = FileUtils.createOutputPdfFile(context, baseName)
                                val success = DocConverterHelper.convertTextToPdf(baseName, fileContent, outputFile)
                                isConverting = false

                                if (success) {
                                    val uri = Uri.fromFile(outputFile)
                                    recentStore.addOrUpdate(
                                        PdfDocumentItem(
                                            uri = uri.toString(),
                                            name = outputFile.name,
                                            sizeFormatted = FileUtils.formatFileSize(outputFile.length())
                                        )
                                    )
                                    Toast.makeText(context, "PDF olarak kaydedildi!", Toast.LENGTH_SHORT).show()
                                    onOpenConvertedPdf(uri)
                                } else {
                                    Toast.makeText(context, "PDF oluşturulamadı", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Yap", tint = RedPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isNightMode) Color(0xFF121212) else Color(0xFFF8FAFC))
        ) {
            if (isLoading || isConverting) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = RedPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (isConverting) "PDF Oluşturuluyor..." else "Belge Açılıyor...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (fileContent.isBlank()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Belge içeriği okunamadı veya dosya boş.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isNightMode) Color(0xFF1E1E1E) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = fileContent,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.5).sp,
                            color = if (isNightMode) Color(0xFFE0E0E0) else Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }
}
