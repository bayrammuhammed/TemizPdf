package com.muhammedbayram.temizpdf.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import com.muhammedbayram.temizpdf.ui.theme.*
import com.muhammedbayram.temizpdf.utils.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentStore: RecentPdfStore,
    themeMode: AppThemeMode,
    onToggleThemeMode: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
    onStartScan: () -> Unit,
    onNavigateToMerge: () -> Unit,
    onNavigateToSplit: () -> Unit,
    onNavigateToOrganize: () -> Unit,
    onNavigateToImagesToPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentPdfs by recentStore.recentPdfs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tümü") } // "Tümü", "Favoriler"

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = FileUtils.getFileName(context, it)
            val fileSize = FileUtils.formatFileSize(FileUtils.getFileSize(context, it))
            recentStore.addOrUpdate(
                PdfDocumentItem(
                    uri = it.toString(),
                    name = fileName,
                    sizeFormatted = fileSize
                )
            )
            onOpenPdf(it)
        }
    }

    val filteredList = remember(recentPdfs, searchQuery, selectedFilter) {
        recentPdfs.filter { item ->
            val matchesQuery = item.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = if (selectedFilter == "Favoriler") item.isFavorite else true
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Temiz PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = RedPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Reklamsız",
                                    color = RedPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Hızlı, Çevrimdışı & Güvenli Belge Stüdyosu",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    // Gece / Gündüz modu geçişi
                    IconButton(onClick = onToggleThemeMode) {
                        Icon(
                            imageVector = when (themeMode) {
                                AppThemeMode.DARK -> Icons.Outlined.LightMode
                                AppThemeMode.LIGHT -> Icons.Outlined.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Outlined.DarkMode
                            },
                            contentDescription = "Tema Değiştir",
                            tint = if (themeMode == AppThemeMode.DARK) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "PDF Dosyası Seç", tint = RedPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartScan,
                icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                text = { Text("Belge Tara") },
                containerColor = RedPrimary,
                contentColor = Color.White
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                com.muhammedbayram.temizpdf.ui.components.AdBanner(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hızlı Eylemler (Quick Actions)
            item {
                Text(
                    "Hızlı Araçlar",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "PDF Aç",
                        icon = Icons.Default.FolderOpen,
                        color = AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                    QuickActionCard(
                        title = "Belge Tara",
                        icon = Icons.Default.CameraAlt,
                        color = AccentTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onStartScan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "PDF Birleştir",
                        icon = Icons.Default.CallMerge,
                        color = AccentPurple,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMerge
                    )
                    QuickActionCard(
                        title = "PDF Böl",
                        icon = Icons.Default.CallSplit,
                        color = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSplit
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Sayfa Düzenle",
                        icon = Icons.Default.GridView,
                        color = AccentGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToOrganize
                    )
                    QuickActionCard(
                        title = "Fotoğrafı PDF Yap",
                        icon = Icons.Default.AddPhotoAlternate,
                        color = RedPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToImagesToPdf
                    )
                }
            }

            // Arama ve Filtre
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("PDF ara veya filtrele...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "Tümü",
                        onClick = { selectedFilter = "Tümü" },
                        label = { Text("Tüm Belgeler (${recentPdfs.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "Favoriler",
                        onClick = { selectedFilter = "Favoriler" },
                        label = { Text("Favoriler") },
                        leadingIcon = {
                            Icon(
                                if (selectedFilter == "Favoriler") Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Liste Başlığı
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Son Belgeler",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (recentPdfs.isNotEmpty()) {
                        TextButton(onClick = { recentStore.clearAll() }) {
                            Text("Geçmişi Temizle", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Boş Durum (Empty State)
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Henüz belge bulunmuyor",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Yukarıdaki hızlı araçlardan bir PDF açabilir veya belge tarayabilirsiniz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // PDF Dosya Listesi
                items(filteredList, key = { it.uri }) { item ->
                    RecentPdfItemCard(
                        item = item,
                        onClick = { onOpenPdf(Uri.parse(item.uri)) },
                        onToggleFavorite = { recentStore.toggleFavorite(item.uri) },
                        onRemove = { recentStore.remove(item.uri) },
                        onShare = {
                            try {
                                val file = File(Uri.parse(item.uri).path ?: "")
                                if (file.exists()) {
                                    FileUtils.sharePdf(context, file)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecentPdfItemCard(
    item: PdfDocumentItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Küçük önizleme resmi veya simge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RedPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailPath != null && File(item.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(item.thumbnailPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.sizeFormatted.isNotEmpty()) {
                        Text(
                            item.sizeFormatted,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (item.pageCount > 0) {
                        Text(
                            " • ${item.pageCount} sayfa",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favori",
                    tint = if (item.isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Seçenekler",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Paylaş") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            expandedMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Listeden Kaldır") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expandedMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}
