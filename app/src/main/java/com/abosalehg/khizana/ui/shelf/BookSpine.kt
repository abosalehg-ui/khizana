package com.abosalehg.khizana.ui.shelf

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.ui.theme.LocalWoodTokens
import java.io.File

/**
 * One book standing on a shelf.
 *
 * The spine has a fixed height by construction — cover, a reserved
 * progress-bar strip and a two-line title — so a status badge can never push
 * the cover out of the row the way it did when the badge was a fourth stacked
 * element inside a fixed-height row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookSpine(
    book: Book,
    metrics: ShelfMetrics,
    acceptDrops: Boolean,
    onDroppedOn: (draggedId: String) -> Unit,
    onOpen: () -> Unit,
    onHide: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onTags: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(false) }
    val tokens = LocalWoodTokens.current

    Column(
        modifier = Modifier
            .width(metrics.spineWidth)
            .then(
                if (acceptDrops) {
                    dropTargetModifier(
                        key = book.id,
                        onDropId = onDroppedOn,
                        onHoverChanged = { hovered = it }
                    )
                } else Modifier
            )
            .then(
                if (hovered) {
                    Modifier.border(2.dp, tokens.goldSoft, RoundedCornerShape(4.dp))
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coverModifier = Modifier
            .width(metrics.coverWidth)
            .height(metrics.coverHeight)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))

        Box {
            Box(
                modifier = Modifier.dragAndDropSource {
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
                }
            ) {
                if (book.coverPath != null) {
                    AsyncImage(
                        model = File(book.coverPath),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = coverModifier
                    )
                } else {
                    Box(
                        modifier = coverModifier
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = book.title.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusRibbon(
                    status = book.status,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(metrics.coverWidth)
                )
            }
            // No explicit .size(): IconButton keeps its 48 dp touch target, and
            // the scrim behind the glyph keeps it visible on a pale cover.
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.book_options),
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0x99000000), CircleShape)
                        .padding(5.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_to_shelf)) },
                    onClick = {
                        menuOpen = false
                        onMove()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_book)) },
                    onClick = {
                        menuOpen = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_hide)) },
                    onClick = {
                        menuOpen = false
                        onHide()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_tags)) },
                    onClick = {
                        menuOpen = false
                        onTags()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete_book),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }

        // Always reserved so every spine is the same height.
        Box(
            modifier = Modifier
                .width(metrics.coverWidth)
                .height(tokens.progressBarHeight)
        ) {
            if (book.progress > 0f) {
                LinearProgressIndicator(
                    progress = { book.progress },
                    color = tokens.gold,
                    trackColor = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = book.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Protected/damaged marker, drawn as a ribbon across the bottom of the cover
 * instead of a fourth row under it.
 */
@Composable
private fun StatusRibbon(status: BookStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        BookStatus.PROTECTED -> stringResource(R.string.status_protected)
        BookStatus.CORRUPT -> stringResource(R.string.status_corrupt)
        else -> return
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onError,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    )
}
