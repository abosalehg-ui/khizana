package com.abosalehg.khizana.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Bookmark
import com.abosalehg.khizana.domain.model.MAX_NOTE_LENGTH
import com.abosalehg.khizana.ui.format.formatCount

/**
 * Adds a bookmark on the current page or edits the one already there.
 *
 * The note is optional: confirming with an empty field is a plain "remember
 * this page", which is what most bookmarks are.
 */
@Composable
internal fun BookmarkEditDialog(
    page: Int,
    existing: Bookmark?,
    onSave: (note: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    // Keyed on the bookmark so reopening the dialog on another page starts
    // from that page's note rather than the previous one's.
    var note by remember(existing?.id, page) { mutableStateOf(existing?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (existing == null) R.string.bookmark_add else R.string.bookmark_edit
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.bookmark_page, formatCount(page + 1)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = note,
                    // Capping input beats truncating on save: what the field
                    // shows is what gets stored.
                    onValueChange = { note = it.take(MAX_NOTE_LENGTH) },
                    label = { Text(stringResource(R.string.bookmark_note_hint)) },
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.bookmark_note_length,
                                formatCount(note.length),
                                formatCount(MAX_NOTE_LENGTH)
                            )
                        )
                    },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

/** The book's bookmarks: tap one to jump to its page, or delete it here. */
@Composable
internal fun BookmarkListDialog(
    bookmarks: List<Bookmark>,
    onJump: (page: Int) -> Unit,
    onDelete: (id: Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bookmarks_title)) },
        text = {
            if (bookmarks.isEmpty()) {
                Text(
                    text = stringResource(R.string.bookmarks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJump(bookmark.page) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        R.string.bookmark_page,
                                        formatCount(bookmark.page + 1)
                                    ),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val note = bookmark.note
                                if (note != null) {
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { onDelete(bookmark.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_back)) }
        }
    )
}
