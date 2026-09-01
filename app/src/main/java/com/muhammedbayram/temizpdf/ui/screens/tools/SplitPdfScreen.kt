package com.muhammedbayram.temizpdf.ui.screens.tools

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import com.muhammedbayram.temizpdf.ui.theme.RedPrimary
import com.muhammedbayram.temizpdf.utils.FileUtils
import com.muhammedbayram.temizpdf.utils.PdfBoxHelper
import com.muhammedbayram.temizpdf.utils.PdfRendererHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    recentStore: RecentPdfStore,
    onNavigateBack: () -> Unit,
    onSplitCompleted: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var outputName by remember { mutableStateOf("Bolunmus_Belge") }
    var isProcessing by remember { mutableStateOf(false) }

    val helper = remember { PdfRendererHelper(context) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            selectedPages.clear()
            scope.launch {
                withContext(Dispatchers.IO) {
                    if (helper.open(it)) {
                        pageCount = helper.pageCount
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { helper.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Böl / Sayfa Çıkar", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedPdfUri != null && pageCount > 0) {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                if (selectedPages.isEmpty()) {
                                    Toast.makeText(context, "Lütfen en az bir sayfa seçin", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessing = true
                                scope.launch {
                                    val tempFile = FileUtils.copyUriToTempFile(context, selectedPdfUri!!)
                                    val outputFile = FileUtils.createOutputPdfFile(context, outputName.trim().ifEmpty { "Bolunmus" })
                                    val success = PdfBoxHelper.splitPdf(tempFile, selectedPages.sorted(), outputFile)
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
                                        Toast.makeText(context, "Seçilen sayfalar yeni PDF olarak kaydedildi!", Toast.LENGTH_SHORT).show()
                                        onSplitCompleted(uri)
                                    } else {
                                        Toast.makeText(context, "Bölme işlemi sırasında hata oluştu", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            enabled = selectedPages.isNotEmpty() && !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ayrıştırılıyor...")
                            } else {
                                Icon(Icons.Default.CallSplit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seçili ${selectedPages.size} Sayfayı Yeni PDF Yap")
                            }
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
            if (selectedPdfUri == null) {
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
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = RedPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bölünecek PDF Seçin", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "İçerisinden belirli sayfaları ayıklamak veya tek bir sayfayı kaydetmek istediğiniz PDF belgesini seçin.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { filePicker.launch(arrayOf("application/pdf")) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF Dosyası Seç")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            FileUtils.getFileName(context, selectedPdfUri!!),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            "Toplam $pageCount sayfa • ${selectedPages.size} seçildi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    TextButton(onClick = {
                        if (selectedPages.size == pageCount) {
                            selectedPages.clear()
                        } else {
                            selectedPages.clear()
                            for (i in 0 until pageCount) selectedPages.add(i)
                        }
                    }) {
                        Text(if (selectedPages.size == pageCount) "Seçimi Kaldır" else "Tümünü Seç")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Yeni PDF Dosya Adı") },
                    trailingIcon = { Text(".pdf", modifier = Modifier.padding(end = 12.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pageCount) { idx ->
                        var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        val isSelected = selectedPages.contains(idx)

                        LaunchedEffect(idx) {
                            thumbBitmap = helper.renderPage(idx, scale = 0.35f, isNightMode = false)
                        }

                        Card(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isSelected) selectedPages.remove(idx) else selectedPages.add(idx)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) RedPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, RedPrimary) else null
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (thumbBitmap != null) {
                                    Image(
                                        bitmap = thumbBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPages.add(idx) else selectedPages.remove(idx)
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    colors = CheckboxDefaults.colors(checkedColor = RedPrimary)
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(topEnd = 8.dp),
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        "Sayfa ${idx + 1}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
