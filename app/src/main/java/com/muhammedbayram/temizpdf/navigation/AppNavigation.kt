package com.muhammedbayram.temizpdf.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muhammedbayram.temizpdf.data.db.RecentPdfStore
import com.muhammedbayram.temizpdf.ui.screens.converter.DocumentConverterScreen
import com.muhammedbayram.temizpdf.ui.screens.home.HomeScreen
import com.muhammedbayram.temizpdf.ui.screens.reader.DocxReaderScreen
import com.muhammedbayram.temizpdf.ui.screens.reader.PdfReaderScreen
import com.muhammedbayram.temizpdf.ui.screens.tools.ImagesToPdfScreen
import com.muhammedbayram.temizpdf.ui.screens.tools.MergePdfScreen
import com.muhammedbayram.temizpdf.ui.screens.tools.OrganizePagesScreen
import com.muhammedbayram.temizpdf.ui.screens.tools.SplitPdfScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reader : Screen("reader/{uri}") {
        fun createRoute(uri: Uri): String {
            val encoded = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
            return "reader/$encoded"
        }
    }
    object DocxReader : Screen("docx_reader/{uri}") {
        fun createRoute(uri: Uri): String {
            val encoded = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
            return "docx_reader/$encoded"
        }
    }
    object Converter : Screen("converter")
    object Merge : Screen("merge")
    object Split : Screen("split")
    object Organize : Screen("organize")
    object ImagesToPdf : Screen("images_to_pdf")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    recentStore: RecentPdfStore,
    themeMode: com.muhammedbayram.temizpdf.ui.theme.AppThemeMode,
    onToggleThemeMode: () -> Unit,
    onStartScan: () -> Unit,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                recentStore = recentStore,
                themeMode = themeMode,
                onToggleThemeMode = onToggleThemeMode,
                onOpenPdf = { uri ->
                    navController.navigate(Screen.Reader.createRoute(uri))
                },
                onOpenDocx = { uri ->
                    navController.navigate(Screen.DocxReader.createRoute(uri))
                },
                onStartScan = onStartScan,
                onNavigateToConverter = { navController.navigate(Screen.Converter.route) },
                onNavigateToMerge = { navController.navigate(Screen.Merge.route) },
                onNavigateToSplit = { navController.navigate(Screen.Split.route) },
                onNavigateToOrganize = { navController.navigate(Screen.Organize.route) },
                onNavigateToImagesToPdf = { navController.navigate(Screen.ImagesToPdf.route) }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse(decodedUri)

            PdfReaderScreen(
                pdfUri = uri,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DocxReader.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse(decodedUri)

            DocxReaderScreen(
                fileUri = uri,
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() },
                onOpenConvertedPdf = { pdfUri ->
                    navController.navigate(Screen.Reader.createRoute(pdfUri)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Converter.route) {
            DocumentConverterScreen(
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() },
                onOpenPdf = { uri ->
                    navController.navigate(Screen.Reader.createRoute(uri)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Merge.route) {
            MergePdfScreen(
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() },
                onMergeCompleted = { uri ->
                    navController.navigate(Screen.Reader.createRoute(uri)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Split.route) {
            SplitPdfScreen(
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() },
                onSplitCompleted = { uri ->
                    navController.navigate(Screen.Reader.createRoute(uri)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Organize.route) {
            OrganizePagesScreen(
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() },
                onSaveCompleted = { uri ->
                    navController.navigate(Screen.Reader.createRoute(uri)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.ImagesToPdf.route) {
            ImagesToPdfScreen(
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() },
                onPdfCreated = { uri ->
                    navController.navigate(Screen.Reader.createRoute(uri)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
    }
}
