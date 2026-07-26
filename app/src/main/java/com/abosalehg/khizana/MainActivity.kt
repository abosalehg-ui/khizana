package com.abosalehg.khizana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abosalehg.khizana.ui.reader.ReaderScreen
import com.abosalehg.khizana.ui.shelf.LibraryScreen
import com.abosalehg.khizana.ui.theme.KhizanaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KhizanaRoot()
        }
    }
}

@Composable
private fun KhizanaRoot(viewModel: MainViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    KhizanaTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "library") {
            composable("library") {
                LibraryScreen(
                    themeMode = themeMode,
                    onCycleThemeMode = viewModel::cycleThemeMode,
                    onOpenBook = { book -> navController.navigate("reader/${book.id}") }
                )
            }
            composable("reader/{bookId}") {
                ReaderScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
