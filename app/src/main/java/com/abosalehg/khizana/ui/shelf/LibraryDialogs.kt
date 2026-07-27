package com.abosalehg.khizana.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.Tag

@Composable
internal fun AddTopicDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
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

@Composable
internal fun RenameTopicDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_shelf)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.topic_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/**
 * Keyboard- and screen-reader-reachable equivalent of dragging a book onto
 * another shelf. Drag and drop stays as the fast path, but it is no longer the
 * only way to organise a library.
 */
@Composable
internal fun MoveToShelfDialog(
    book: Book,
    shelves: List<Shelf>,
    onMove: (topicId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val targets = shelves.filter { it.kind != ShelfKind.CONTINUE_READING }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.move_to_shelf_title, book.title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(targets, key = { "${it.kind.name}:${it.topicId ?: -1L}" }) { shelf ->
                    val selected = shelf.topicId == book.topicId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = { onMove(shelf.topicId) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = { onMove(shelf.topicId) })
                        Text(
                            text = when (shelf.kind) {
                                ShelfKind.NEW -> stringResource(R.string.shelf_new)
                                else -> shelf.name.orEmpty()
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
internal fun TagsDialog(
    book: Book,
    allTags: List<Tag>,
    loadSelected: suspend (String) -> Set<Long>,
    onSave: (tagIds: Set<Long>, newTagNames: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var loaded by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf(setOf<Long>()) }
    val newNames = remember { mutableListOf<String>().toMutableStateList() }
    var newTagText by remember { mutableStateOf("") }

    LaunchedEffect(book.id) {
        selected.value = loadSelected(book.id)
        loaded = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_dialog_title)) },
        text = {
            Column {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allTags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = tag.id in selected.value,
                            onClick = {
                                selected.value =
                                    if (tag.id in selected.value) selected.value - tag.id
                                    else selected.value + tag.id
                            },
                            label = { Text(tag.name) }
                        )
                    }
                    items(newNames.size) { index ->
                        FilterChip(
                            selected = true,
                            onClick = { newNames.removeAt(index) },
                            label = { Text(newNames[index]) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text(stringResource(R.string.new_tag_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (newTagText.isNotBlank()) {
                                newNames.add(newTagText.trim())
                                newTagText = ""
                            }
                        },
                        enabled = newTagText.isNotBlank()
                    ) { Text(stringResource(R.string.action_add)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selected.value, newNames.toList()) },
                enabled = loaded
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
