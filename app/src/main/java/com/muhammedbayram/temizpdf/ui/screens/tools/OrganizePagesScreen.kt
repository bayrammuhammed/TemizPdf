package com.muhammedbayram.temizpdf.ui.screens.tools

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
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
fun OrganizePagesScreen(
    recentStore: RecentPdfStore,
    onNavigateBack: () -> Unit,
    onSaveCompleted: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    val pageOrder = remember { mutableStateListOf<Int>() }
    val pageRotations = remember { mutableStateMapOf<Int, Int>() } // originalIndex -> rotation degrees
    var isProcessing by remember { mutableStateOf(false) }

    val helper = remember { PdfRendererHelper(context) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            pageOrder.clear()
            pageRotations.clear()
            scope.launch {
                withContext(Dispatchers.IO) {
                    if (helper.open(it)) {
                        val count = helper.pageCount
                        for (i in 0 until count) {
                            pageOrder.add(i)
                            pageRotations[i] = 0
                        }
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
                title = { Text("Sayfa Düzenle & Döndür", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedPdfUri != null && pageOrder.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                isProcessing = true
                                scope.launch {
                                    val tempFile = FileUtils.copyUriToTempFile(context, selectedPdfUri!!)
                                    val outputFile = FileUtils.createOutputPdfFile(context, "Duzenlenmis_Belge")
                                    val success = PdfBoxHelper.reorderAndRotatePages(
                                        tempFile,
                                        pageOrder.toList(),
                                        pageRotations.toMap(),
                                        outputFile
                                    )
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
                                        Toast.makeText(context, "Sayfalar güncellendi ve kaydedildi!", Toast.LENGTH_SHORT).show()
                                        onSaveCompleted(uri)
                                    } else {
                                        Toast.makeText(context, "Kayıt sırasında hata oluştu", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kaydediliyor...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Değişiklikleri Kaydet (${pageOrder.size} Sayfa)")
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
                            Icons.Default.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = RedPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Düzenlenecek PDF Seçin", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Sayfaların sırasını değiştirmek, sayfaları 90° döndürmek veya istenmeyen sayfaları silmek için PDF seçin.",
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
                            Text("PDF Seç")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pageOrder) { listIndex, origPageIndex ->
                        var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        val currentRotation = pageRotations[origPageIndex] ?: 0

                        LaunchedEffect(origPageIndex) {
                            thumbBitmap = helper.renderPage(origPageIndex, scale = 0.4f, isNightMode = false)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbBitmap != null) {
                                        Image(
                                            bitmap = thumbBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(currentRotation.toFloat())
                                                .padding(6.dp)
                                        )
                                    }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            "${listIndex + 1}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Eylem çubuğu (Döndür, Sırala, Sil)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 90 derece döndür
                                    IconButton(
                                        onClick = {
                                            pageRotations[origPageIndex] = (currentRotation + 90) % 360
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "Döndür", modifier = Modifier.size(20.dp))
                                    }

                                    // Sola / Yukarı taşı
                                    if (listIndex > 0) {
                                        IconButton(
                                            onClick = {
                                                val prev = pageOrder[listIndex - 1]
                                                pageOrder[listIndex - 1] = origPageIndex
                                                pageOrder[listIndex] = prev
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Öne Al", modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // Sağa / Aşağı taşı
                                    if (listIndex < pageOrder.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val next = pageOrder[listIndex + 1]
                                                pageOrder[listIndex + 1] = origPageIndex
                                                pageOrder[listIndex] = next
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Sona Al", modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // Sayfayı sil
                                    IconButton(
                                        onClick = {
                                            pageOrder.removeAt(listIndex)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
