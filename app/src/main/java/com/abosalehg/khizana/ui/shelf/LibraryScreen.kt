package com.abosalehg.khizana.ui.shelf

import android.content.ClipData
import android.content.ClipDescription
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
 * M3 library screen: wooden shelves grouped by topic with the "New ⭐" shelf
 * first, drag & drop to move books between shelves, and shelf creation.
 */
@Composable
fun LibraryScreen(
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit,
    onOpenBook: (Book) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    var showAddTopic by remember { mutableStateOf(false) }

    // The grant happens in system settings — re-check whenever we come back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    if (showAddTopic) {
        AddTopicDialog(
            onConfirm = { name ->
                viewModel.addTopic(name)
                showAddTopic = false
            },
            onDismiss = { showAddTopic = false }
        )
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item(key = "header") {
                Column(Modifier.padding(horizontal = 20.dp)) {
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
                }
            }

            if (!permissionGranted) {
                item(key = "permission") {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        PermissionCard(onGranted = viewModel::refreshPermission)
                    }
                }
            } else {
                item(key = "scan") {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ScanSection(
                            scanState = scanState,
                            bookCount = shelves.sumOf { it.books.size },
                            onScanClick = viewModel::startScan,
                            onAddTopicClick = { showAddTopic = true }
                        )
                    }
                }
                items(shelves, key = { it.topicId ?: -1L }) { shelf ->
                    ShelfSection(
                        shelf = shelf,
                        onMoveBook = viewModel::moveBook,
                        onOpenBook = onOpenBook
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTopicDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_topic)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.topic_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfSection(
    shelf: Shelf,
    onMoveBook: (bookId: String, topicId: Long?) -> Unit,
    onOpenBook: (Book) -> Unit
) {
    val tokens = LocalWoodTokens.current
    var isHovered by remember { mutableStateOf(false) }
    val dropTarget = remember(shelf.topicId) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isHovered = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isHovered = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isHovered = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isHovered = false
                val clip = event.toAndroidDragEvent().clipData ?: return false
                val bookId = (0 until clip.itemCount)
                    .firstNotNullOfOrNull { clip.getItemAt(it).text?.toString() }
                    ?: return false
                onMoveBook(bookId, shelf.topicId)
                return true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = dropTarget
            )
            .then(
                if (isHovered) {
                    Modifier.border(2.dp, tokens.goldSoft, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shelf.name ?: stringResource(R.string.shelf_new),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = shelf.books.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        if (shelf.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_shelf_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(120.dp)
            ) {
                items(shelf.books, key = { it.id }) { book ->
                    BookSpine(book, onOpen = { onOpenBook(book) })
                }
            }
        }
        ShelfPlank(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookSpine(book: Book, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .dragAndDropSource {
                detectTapGestures(
                    onTap = { onOpen() },
                    onLongPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                ClipData.newPlainText("bookId", book.id)
                            )
                        )
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coverModifier = Modifier
            .width(72.dp)
            .height(98.dp)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
        if (book.coverPath != null) {
            AsyncImage(
                model = File(book.coverPath),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = coverModifier
            )
        } else {
            Box(
                modifier = coverModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.title.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (book.progress > 0f) {
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .width(72.dp)
                    .height(3.dp)
            )
        }
        StatusBadge(book.status)
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        color = MaterialTheme.colorScheme.error,
        maxLines = 1
    )
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
    onScanClick: () -> Unit,
    onAddTopicClick: () -> Unit
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onAddTopicClick) {
                Text(stringResource(R.string.add_topic))
            }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onScanClick, enabled = !scanState.running) {
                Text(stringResource(R.string.scan_now))
            }
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

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}

/** A single wooden plank — the shelf surface books stand on. */
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
