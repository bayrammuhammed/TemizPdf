package com.muhammedbayram.temizpdf.ui.screens.tools

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import com.muhammedbayram.temizpdf.ui.theme.RedPrimary
import com.muhammedbayram.temizpdf.utils.FileUtils
import com.muhammedbayram.temizpdf.utils.PdfBoxHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    recentStore: RecentPdfStore,
    onNavigateBack: () -> Unit,
    onMergeCompleted: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedFiles = remember { mutableStateListOf<Uri>() }
    var outputName by remember { mutableStateOf("Birlestirilmis_Belge") }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedFiles.addAll(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Dosyalarını Birleştir", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (selectedFiles.size < 2) {
                                Toast.makeText(context, "En az 2 adet PDF seçmelisiniz", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isProcessing = true
                            scope.launch {
                                val tempFiles = selectedFiles.map { FileUtils.copyUriToTempFile(context, it) }
                                val outputFile = FileUtils.createOutputPdfFile(context, outputName.trim().ifEmpty { "Birlestirilmis" })
                                val success = PdfBoxHelper.mergePdfs(tempFiles, outputFile)
                                isProcessing = false

                                if (success) {
                                    val uri = Uri.fromFile(outputFile)
                                    recentStore.addOrUpdate(
                                        PdfDocumentItem(
                                            uri = uri.toString(),
                                            name = outputFile.name,
                                            sizeFormatted = FileUtils.formatFileSize(outputFile.length())
                                        )
                                    )
                                    Toast.makeText(context, "PDF başarıyla birleştirildi!", Toast.LENGTH_SHORT).show()
                                    onMergeCompleted(uri)
                                } else {
                                    Toast.makeText(context, "Birleştirme sırasında hata oluştu", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        enabled = selectedFiles.size >= 2 && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Birleştiriliyor...")
                        } else {
                            Icon(Icons.Default.CallMerge, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seçilen ${selectedFiles.size} PDF'i Birleştir")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Oluşturulacak Dosya Adı") },
                trailingIcon = { Text(".pdf", modifier = Modifier.padding(end = 12.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Birleştirilecek Belgeler (${selectedFiles.size})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Button(
                    onClick = { filePicker.launch(arrayOf("application/pdf")) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF Ekle", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedFiles.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CallMerge,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = RedPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Henüz dosya seçilmedi", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Yukarıdaki 'PDF Ekle' butonuna basarak birleştirmek istediğiniz PDF dosyalarını seçin.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedFiles) { index, uri ->
                        val fileName = FileUtils.getFileName(context, uri)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = RedPrimary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${index + 1}",
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    fileName,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                // Sıralama kontrolleri
                                if (index > 0) {
                                    IconButton(onClick = {
                                        val temp = selectedFiles[index]
                                        selectedFiles[index] = selectedFiles[index - 1]
                                        selectedFiles[index - 1] = temp
                                    }) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Yukarı Taşı")
                                    }
                                }
                                if (index < selectedFiles.size - 1) {
                                    IconButton(onClick = {
                                        val temp = selectedFiles[index]
                                        selectedFiles[index] = selectedFiles[index + 1]
                                        selectedFiles[index + 1] = temp
                                    }) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Aşağı Taşı")
                                    }
                                }
                                IconButton(onClick = { selectedFiles.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
