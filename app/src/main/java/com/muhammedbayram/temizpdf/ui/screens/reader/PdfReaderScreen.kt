package com.muhammedbayram.temizpdf.ui.screens.reader

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.muhammedbayram.temizpdf.ui.theme.RedPrimary
import com.muhammedbayram.temizpdf.utils.FileUtils
import com.muhammedbayram.temizpdf.utils.PdfRendererHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdfUri: Uri,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fileName = remember(pdfUri) { FileUtils.getFileName(context, pdfUri) }
    val helper = remember { PdfRendererHelper(context) }

    var isLoaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    var isNightMode by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()

    // Initialize & Load PDF
    LaunchedEffect(pdfUri) {
        loadError = null
        isLoaded = false
        withContext(Dispatchers.IO) {
            if (helper.open(pdfUri)) {
                totalPages = helper.pageCount
                isLoaded = true
            } else {
                loadError = "Belge açılamadı veya dosya silinmiş/erişilemez olabilir."
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.close()
        }
    }

    // Track current visible page
    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentPage = listState.firstVisibleItemIndex
    }

    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            val extraWidth = (scale - 1) * 500
            val extraHeight = (scale - 1) * 500
            val maxX = extraWidth / 2
            val maxY = extraHeight / 2
            offset = Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
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
                        if (totalPages > 0) {
                            Text(
                                "Sayfa ${currentPage + 1} / $totalPages",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Gece modu aç / kapat
                    IconButton(onClick = { isNightMode = !isNightMode }) {
                        Icon(
                            if (isNightMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Gece Modu",
                            tint = if (isNightMode) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Sayfa Listesi / Grid
                    IconButton(onClick = { showGridDialog = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Sayfalar")
                    }
                    // Paylaş
                    IconButton(onClick = {
                        try {
                            val tempFile = FileUtils.copyUriToTempFile(context, pdfUri)
                            FileUtils.sharePdf(context, tempFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Paylaş")
                    }
                }
            )
        },
        bottomBar = {
            if (totalPages > 1) {
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${currentPage + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { value ->
                                val targetPage = value.toInt()
                                currentPage = targetPage
                                scope.launch {
                                    listState.scrollToItem(targetPage)
                                }
                            },
                            valueRange = 0f..(totalPages - 1).toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = RedPrimary,
                                activeTrackColor = RedPrimary
                            )
                        )
                        Text(
                            "$totalPages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isNightMode) Color(0xFF121212) else Color(0xFFECEFF1))
        ) {
            if (loadError != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        loadError ?: "Belge açılamadı",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Dosya silinmiş, taşınmış veya erişim izni sona ermiş olabilir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Geri Dön")
                    }
                }
            } else if (!isLoaded) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = RedPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PDF Yükleniyor...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = transformState),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(totalPages) { pageIndex ->
                        PdfPageCard(
                            helper = helper,
                            pageIndex = pageIndex,
                            isNightMode = isNightMode
                        )
                    }
                }

                // Reset zoom button if zoomed in
                if (scale > 1.05f) {
                    FloatingActionButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = "Yakınlaştırmayı Sıfırla")
                    }
                }
            }
        }
    }

    // Grid Sayfa Seçici Dialog
    if (showGridDialog && totalPages > 0) {
        Dialog(onDismissRequest = { showGridDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sayfaya Git", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showGridDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(totalPages) { idx ->
                            var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
                            LaunchedEffect(idx) {
                                thumbBitmap = helper.renderPage(idx, scale = 0.35f)
                            }

                            Card(
                                modifier = Modifier
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showGridDialog = false
                                        scope.launch { listState.scrollToItem(idx) }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentPage == idx) RedPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (currentPage == idx) androidx.compose.foundation.BorderStroke(2.dp, RedPrimary) else null
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (thumbBitmap != null) {
                                        Image(
                                            bitmap = thumbBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(bottomStart = 8.dp),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            color = Color.White,
                                            fontSize = 11.sp,
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
}

@Composable
fun PdfPageCard(
    helper: PdfRendererHelper,
    pageIndex: Int,
    isNightMode: Boolean
) {
    var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        pageBitmap = helper.renderPage(pageIndex, scale = 1.8f)
    }

    val nightColorFilter = remember(isNightMode) {
        if (isNightMode) {
            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix(
                    floatArrayOf(
                        -1f,  0f,  0f,  0f, 255f,
                         0f, -1f,  0f,  0f, 255f,
                         0f,  0f, -1f,  0f, 255f,
                         0f,  0f,  0f,  1f,   0f
                    )
                )
            )
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = if (isNightMode) Color(0xFF1E1E1E) else Color.White)
    ) {
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "Sayfa ${pageIndex + 1}",
                colorFilter = nightColorFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp), color = RedPrimary)
            }
        }
    }
}
