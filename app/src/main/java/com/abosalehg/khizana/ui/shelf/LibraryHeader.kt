package com.abosalehg.khizana.ui.shelf

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.R
import com.abosalehg.khizana.data.scanner.StoragePermission
import com.abosalehg.khizana.domain.model.ShelfSort
import com.abosalehg.khizana.ui.format.formatCount

@Composable
internal fun PermissionCard(onGranted: () -> Unit) {
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
internal fun ScanSection(
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
            text = pluralStringResource(
                R.plurals.books_count,
                bookCount,
                formatCount(bookCount)
            ),
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
                    formatCount(scanState.processed),
                    formatCount(scanState.total)
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
                formatCount(scanState.lastScanned),
                formatCount(scanState.lastAdded),
                formatCount(scanState.lastRelocated),
                formatCount(scanState.lastMissing)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Within-shelf ordering picker. It sits with the search field rather than in
 * Settings: it changes what the reader is looking at right now, and it is the
 * kind of thing you flip back and forth while hunting for a book.
 */
@Composable
internal fun SortRow(current: ShelfSort, onSelect: (ShelfSort) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Text(stringResource(R.string.sort_label, stringResource(sortLabel(current))))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                ShelfSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sortLabel(sort))) },
                        leadingIcon = {
                            // onClick = null: the row itself handles the tap,
                            // so the radio stays a state marker, not a
                            // second target the screen reader announces.
                            RadioButton(selected = sort == current, onClick = null)
                        },
                        onClick = {
                            menuOpen = false
                            onSelect(sort)
                        }
                    )
                }
            }
        }
    }
}

@StringRes
private fun sortLabel(sort: ShelfSort): Int = when (sort) {
    ShelfSort.MANUAL -> R.string.sort_manual
    ShelfSort.TITLE -> R.string.sort_title
    ShelfSort.DATE_ADDED -> R.string.sort_date
    ShelfSort.SIZE -> R.string.sort_size
}

/** Shown instead of the shelves when there is genuinely nothing to show. */
@Composable
internal fun LibraryPlaceholder(messageRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
