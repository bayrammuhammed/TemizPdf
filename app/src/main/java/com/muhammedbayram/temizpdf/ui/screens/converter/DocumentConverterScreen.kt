package com.muhammedbayram.temizpdf.ui.screens.converter

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import com.muhammedbayram.temizpdf.ui.theme.*
import com.muhammedbayram.temizpdf.utils.DocConverterHelper
import com.muhammedbayram.temizpdf.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentConverterScreen(
    recentStore: RecentPdfStore,
    onNavigateBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var resultDialogTitle by remember { mutableStateOf<String?>(null) }
    var resultDialogMessage by remember { mutableStateOf("") }
    var lastProducedFile by remember { mutableStateOf<File?>(null) }

    // 1. DOCX -> PDF
    val docxPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            processingMessage = "Word belgesi okunuyor ve PDF oluşturuluyor..."
            scope.launch {
                val fileName = FileUtils.getFileName(context, it).substringBeforeLast(".")
                val text = DocConverterHelper.extractTextFromDocx(context, it)
                if (text.isNotBlank()) {
                    val outputFile = FileUtils.createOutputPdfFile(context, fileName)
                    val success = DocConverterHelper.convertTextToPdf(fileName, text, outputFile)
                    isProcessing = false
                    if (success) {
                        lastProducedFile = outputFile
                        recentStore.addOrUpdate(
                            PdfDocumentItem(
                                uri = Uri.fromFile(outputFile).toString(),
                                name = outputFile.name,
                                sizeFormatted = FileUtils.formatFileSize(outputFile.length())
                            )
                        )
                        resultDialogTitle = "Word Belgesi PDF Yapıldı! 🎉"
                        resultDialogMessage = "${outputFile.name} başarıyla oluşturuldu."
                    } else {
                        Toast.makeText(context, "PDF dönüştürme başarısız oldu", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isProcessing = false
                    Toast.makeText(context, "Word dosyasından metin okunamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 2. PDF -> TXT
    val pdfToTxtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            processingMessage = "PDF metinleri ayıklanıyor..."
            scope.launch {
                val tempPdf = FileUtils.copyUriToTempFile(context, it)
                val baseName = FileUtils.getFileName(context, it).substringBeforeLast(".")
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TemizPDF")
                if (!dir.exists()) dir.mkdirs()
                val outputFile = File(dir, "${baseName}_Metin_${System.currentTimeMillis()}.txt")

                val success = DocConverterHelper.convertPdfToText(tempPdf, outputFile)
                isProcessing = false
                if (success) {
                    lastProducedFile = outputFile
                    resultDialogTitle = "PDF Metne Dönüştürüldü! 📄"
                    resultDialogMessage = "Metin dosyası kaydedildi:\n${outputFile.name}"
                } else {
                    Toast.makeText(context, "Metin ayıklama başarısız oldu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 3. TXT -> PDF
    val txtToPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            processingMessage = "Metin dosyası PDF yapılıyor..."
            scope.launch {
                val fileName = FileUtils.getFileName(context, it).substringBeforeLast(".")
                val text = DocConverterHelper.readTextFile(context, it)
                if (text.isNotBlank()) {
                    val outputFile = FileUtils.createOutputPdfFile(context, fileName)
                    val success = DocConverterHelper.convertTextToPdf(fileName, text, outputFile)
                    isProcessing = false
                    if (success) {
                        lastProducedFile = outputFile
                        recentStore.addOrUpdate(
                            PdfDocumentItem(
                                uri = Uri.fromFile(outputFile).toString(),
                                name = outputFile.name,
                                sizeFormatted = FileUtils.formatFileSize(outputFile.length())
                            )
                        )
                        resultDialogTitle = "Metin PDF Yapıldı! 📑"
                        resultDialogMessage = "${outputFile.name} başarıyla oluşturuldu."
                    } else {
                        Toast.makeText(context, "PDF dönüştürme başarısız oldu", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isProcessing = false
                    Toast.makeText(context, "Metin dosyası boş veya okunamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 4. PDF -> JPG Görselleri
    val pdfToImagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            processingMessage = "PDF sayfaları fotoğrafa dönüştürülüyor..."
            scope.launch {
                val tempPdf = FileUtils.copyUriToTempFile(context, it)
                val baseName = FileUtils.getFileName(context, it).substringBeforeLast(".")
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "TemizPDF_$baseName")

                val imageFiles = DocConverterHelper.convertPdfToImages(context, tempPdf, dir)
                isProcessing = false
                if (imageFiles.isNotEmpty()) {
                    lastProducedFile = imageFiles.first()
                    resultDialogTitle = "PDF Görsellere Çevrildi! 🖼️"
                    resultDialogMessage = "Toplam ${imageFiles.size} sayfa JPEG olarak kaydedildi."
                } else {
                    Toast.makeText(context, "Görsele dönüştürme başarısız oldu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Belge Format Dönüştürücü", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Dönüştürme Seçenekleri",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "Telefonundaki belgeleri farklı formatlara çevir, reklamsız ve çevrimdışı paylaş.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    ConverterActionCard(
                        title = "Word (DOCX) ➔ PDF",
                        subtitle = "Word belgelerini biçimlendirilmiş PDF'e çevir",
                        icon = Icons.Default.Description,
                        color = AccentBlue,
                        badge = "Çok Popüler",
                        onClick = {
                            docxPicker.launch(arrayOf(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword",
                                "application/octet-stream"
                            ))
                        }
                    )
                }

                item {
                    ConverterActionCard(
                        title = "PDF ➔ Metin (TXT / Word)",
                        subtitle = "PDF içindeki tüm yazıları düzenlenebilir metin yap",
                        icon = Icons.Default.TextFields,
                        color = AccentTeal,
                        onClick = { pdfToTxtPicker.launch(arrayOf("application/pdf")) }
                    )
                }

                item {
                    ConverterActionCard(
                        title = "Metin (TXT) ➔ PDF",
                        subtitle = "Yazı ve notlarını A4 formatında PDF belgesine dönüştür",
                        icon = Icons.Default.FormatAlignLeft,
                        color = AccentAmber,
                        onClick = { txtToPdfPicker.launch(arrayOf("text/plain", "text/*")) }
                    )
                }

                item {
                    ConverterActionCard(
                        title = "PDF ➔ Fotoğraf (JPG/PNG)",
                        subtitle = "PDF sayfalarını yüksek çözünürlüklü görsellere dönüştür",
                        icon = Icons.Default.Image,
                        color = AccentPurple,
                        onClick = { pdfToImagesPicker.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            // İşleniyor Durumu
            if (isProcessing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = RedPrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(processingMessage, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Başarı Dialogu
    if (resultDialogTitle != null && lastProducedFile != null) {
        AlertDialog(
            onDismissRequest = { resultDialogTitle = null },
            title = { Text(resultDialogTitle!!, fontWeight = FontWeight.Bold) },
            text = { Text(resultDialogMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        val file = lastProducedFile!!
                        resultDialogTitle = null
                        if (file.name.endsWith(".pdf", ignoreCase = true)) {
                            onOpenPdf(Uri.fromFile(file))
                        } else {
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, if (file.name.endsWith(".txt")) "text/plain" else "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Dosya açılamadı", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Görüntüle")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val file = lastProducedFile!!
                        resultDialogTitle = null
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = if (file.name.endsWith(".pdf")) "application/pdf" else if (file.name.endsWith(".txt")) "text/plain" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Dosyayı Paylaş"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text("Paylaş")
                }
            }
        )
    }
}

@Composable
fun ConverterActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                badge,
                                color = color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
