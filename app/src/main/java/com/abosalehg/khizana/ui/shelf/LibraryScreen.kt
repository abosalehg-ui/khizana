package com.abosalehg.khizana.ui.shelf

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.abosalehg.khizana.R
import com.abosalehg.khizana.data.scanner.StoragePermission
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.domain.model.ThemeMode
import com.abosalehg.khizana.ui.theme.LocalWoodTokens
import java.io.File

/**
 * M1 library screen: permission gate, manual scan and a plain list of found
 * books. Replaced by the real wooden shelves in M3.
 */
@Composable
fun LibraryScreen(
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    // The grant happens in system settings — re-check whenever we come back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onCycleThemeMode) {
                    Text(
                        text = stringResource(
                            R.string.theme_mode_label,
                            stringResource(themeMode.labelRes())
                        )
                    )
                }
            }
            ShelfPlank(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )
            Spacer(Modifier.height(20.dp))

            if (!permissionGranted) {
                PermissionCard(onGranted = viewModel::refreshPermission)
            } else {
                ScanSection(
                    scanState = scanState,
                    bookCount = books.size,
                    onScanClick = viewModel::startScan
                )
                Spacer(Modifier.height(12.dp))
                if (books.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_library),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    BookList(books)
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onGranted: () -> Unit) {
    val context = LocalContext.current
    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onGranted() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_rationale),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (StoragePermission.needsAllFilesAccess) {
                    context.startActivity(StoragePermission.allFilesAccessIntent(context))
                } else {
                    legacyLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }) {
                Text(stringResource(R.string.permission_grant))
            }
        }
    }
}

@Composable
private fun ScanSection(
    scanState: ScanUiState,
    bookCount: Int,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.books_count, bookCount),
            style = MaterialTheme.typography.titleMedium
        )
        Button(onClick = onScanClick, enabled = !scanState.running) {
            Text(stringResource(R.string.scan_now))
        }
    }
    if (scanState.running) {
        Spacer(Modifier.height(8.dp))
        if (scanState.total > 0) {
            LinearProgressIndicator(
                progress = { scanState.processed.toFloat() / scanState.total },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.scanning_progress,
                    scanState.processed,
                    scanState.total
                ),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    } else if (scanState.lastScanned != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.scan_report,
                scanState.lastScanned,
                scanState.lastAdded,
                scanState.lastRelocated,
                scanState.lastMissing
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BookList(books: List<Book>) {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(books, key = { it.id }) { book ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverThumb(book)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        val pages = if (book.pageCount > 0) {
                            stringResource(R.string.pages_count, book.pageCount) + " · "
                        } else ""
                        Text(
                            text = "${book.format.name} · " + pages +
                                Formatter.formatShortFileSize(context, book.fileSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusBadge(book.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BookStatus) {
    val label = when (status) {
        BookStatus.PROTECTED -> stringResource(R.string.status_protected)
        BookStatus.CORRUPT -> stringResource(R.string.status_corrupt)
        else -> return
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun CoverThumb(book: Book) {
    val modifier = Modifier
        .width(48.dp)
        .height(64.dp)
        .clip(RoundedCornerShape(4.dp))
    if (book.coverPath != null) {
        AsyncImage(
            model = File(book.coverPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Placeholder until CoverWorker gets to this book (or if it failed).
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}

/** A single wooden plank — identity element kept from M0. */
@Composable
private fun ShelfPlank(modifier: Modifier = Modifier) {
    val tokens = LocalWoodTokens.current
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(tokens.plankLight, tokens.plankBase, tokens.plankDeep)
            )
        )
        val step = 34.dp.toPx()
        var x = step / 2f
        var index = 0
        while (x < size.width) {
            val color = if (index % 2 == 0) tokens.plankGrainDark else tokens.plankGrainLight
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = if (index % 2 == 0) 2f else 1f
            )
            x += step
            index++
        }
        drawLine(
            color = tokens.goldSoft.copy(alpha = 0.6f),
            start = Offset(0f, 1f),
            end = Offset(size.width, 1f),
            strokeWidth = 2f
        )
    }
}
