package com.abosalehg.khizana.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.ui.format.formatCount
import com.abosalehg.khizana.ui.theme.LocalWoodTokens

/**
 * One shelf: a header row, the books standing on it and the wooden plank.
 *
 * The book row deliberately has **no** fixed height — the previous 120 dp cap
 * was shorter than a spine carrying a status badge, and shorter still at large
 * font scales, so it clipped the top of the covers.
 */
@Composable
internal fun ShelfSection(
    shelf: Shelf,
    metrics: ShelfMetrics,
    onMoveBook: (bookId: String, topicId: Long?) -> Unit,
    onDropOnBook: (draggedId: String, target: Book) -> Unit,
    onOpenBook: (Book) -> Unit,
    onRenameShelf: (Shelf) -> Unit,
    onDeleteShelf: (Shelf) -> Unit,
    onHideBook: (Book) -> Unit,
    onMoveBookRequest: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    onTagBook: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit
) {
    // The synthetic Continue Reading shelf is never a drop target.
    val acceptsDrops = shelf.kind != ShelfKind.CONTINUE_READING
    val tokens = LocalWoodTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (shelf.kind) {
                    ShelfKind.CONTINUE_READING -> stringResource(R.string.continue_reading)
                    ShelfKind.NEW -> stringResource(R.string.shelf_new)
                    ShelfKind.TOPIC -> shelf.name.orEmpty()
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCount(shelf.books.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The New shelf is built-in: no rename/delete.
                if (shelf.topicId != null) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.shelf_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename_shelf)) },
                                onClick = {
                                    menuOpen = false
                                    onRenameShelf(shelf)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_shelf)) },
                                onClick = {
                                    menuOpen = false
                                    onDeleteShelf(shelf)
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (shelf.books.isEmpty()) {
            var hovered by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.emptyShelfHeight)
                    .then(
                        if (acceptsDrops) {
                            dropTargetModifier(
                                key = shelf.topicId,
                                onDropId = { id -> onMoveBook(id, shelf.topicId) },
                                onHoverChanged = { hovered = it }
                            )
                        } else Modifier
                    )
                    .dropHighlight(hovered),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_shelf_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // The whole row is an append target, so a book can be dropped on
            // any empty stretch of a populated shelf — between books, in the
            // padding, or after the last one — not only onto another book or
            // a narrow trailing slot. Book spines register their own targets
            // on top and keep the precise insert-before behaviour: with
            // nested drag-and-drop targets, the innermost one under the
            // pointer receives the drop.
            var rowHovered by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (acceptsDrops) {
                            dropTargetModifier(
                                key = "row:${shelf.topicId}",
                                onDropId = { id -> onMoveBook(id, shelf.topicId) },
                                onHoverChanged = { rowHovered = it }
                            )
                        } else Modifier
                    )
                    .dropHighlight(rowHovered)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(shelf.books, key = { it.id }) { book ->
                        BookSpine(
                            book = book,
                            metrics = metrics,
                            acceptDrops = acceptsDrops,
                            onDroppedOn = { draggedId -> onDropOnBook(draggedId, book) },
                            onOpen = { onOpenBook(book) },
                            onHide = { onHideBook(book) },
                            onMove = { onMoveBookRequest(book) },
                            onShare = { onShareBook(book) },
                            onTags = { onTagBook(book) },
                            onDelete = { onDeleteBook(book) }
                        )
                    }
                }
            }
        }
        ShelfPlank(
            modifier = Modifier
                .fillMaxWidth()
                .height(tokens.plankHeight)
        )
    }
}

/**
 * A single wooden plank — the shelf surface books stand on.
 *
 * Drawn through `drawWithCache`, not `Canvas`: the grain is a fixed number of
 * lines across the width, and nothing about it changes between frames, so
 * recomputing the loop on every recomposition of a scrolling list was work
 * spent arriving at the identical picture. The cache block re-runs only when
 * the size or the tokens change.
 */
@Composable
private fun ShelfPlank(modifier: Modifier = Modifier) {
    val tokens = LocalWoodTokens.current
    Spacer(
        modifier.drawWithCache {
            val gradient = Brush.verticalGradient(
                colors = listOf(tokens.plankLight, tokens.plankBase, tokens.plankDeep)
            )
            val step = 34.dp.toPx()
            // Held in a local: inside buildList, `size` is the list's own count.
            val plankWidth = size.width
            val grainLines = buildList {
                var x = step / 2f
                var index = 0
                while (x < plankWidth) {
                    add(
                        GrainLine(
                            x = x,
                            color = if (index % 2 == 0) {
                                tokens.plankGrainDark
                            } else {
                                tokens.plankGrainLight
                            },
                            strokeWidth = if (index % 2 == 0) 2f else 1f
                        )
                    )
                    x += step
                    index++
                }
            }
            onDrawBehind {
                drawRect(brush = gradient)
                grainLines.forEach { line ->
                    drawLine(
                        color = line.color,
                        start = Offset(line.x, 0f),
                        end = Offset(line.x, size.height),
                        strokeWidth = line.strokeWidth
                    )
                }
                drawLine(
                    color = tokens.goldSoft.copy(alpha = 0.6f),
                    start = Offset(0f, 1f),
                    end = Offset(size.width, 1f),
                    strokeWidth = 2f
                )
            }
        }
    )
}

/** One vertical grain stroke, precomputed once per plank size. */
private data class GrainLine(val x: Float, val color: Color, val strokeWidth: Float)
