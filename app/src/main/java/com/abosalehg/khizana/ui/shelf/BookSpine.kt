package com.abosalehg.khizana.ui.shelf

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookStatus
import com.abosalehg.khizana.ui.theme.LocalWoodTokens

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
    val openLabel = stringResource(R.string.action_open_book, book.title)

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
            .dropHighlight(hovered, radius = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coverModifier = Modifier
            .width(metrics.coverWidth)
            .height(metrics.coverHeight)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))

        Box {
            Box(
                // Opening a book is the whole point of this screen, so it is a
                // real click, not a raw pointer gesture: `clickable` is what
                // puts it in the semantics tree, gives it a Button role, makes
                // it focusable and activatable from a keyboard, and draws the
                // ripple. `detectTapGestures` did none of that, which left
                // TalkBack and keyboard users able to move, share and delete a
                // book from the overflow menu — but never to read one.
                // The long press stays on the drag source, where it belongs.
                modifier = Modifier
                    .dragAndDropSource {
                        detectTapGestures(
                            onLongPress = {
                                startTransfer(
                                    DragAndDropTransferData(
                                        ClipData.newPlainText("bookId", book.id)
                                    )
                                )
                            }
                        )
                    }
                    .clickable(onClickLabel = openLabel, onClick = onOpen)
            ) {
                BookCover(book = book, modifier = coverModifier)
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
                    contentDescription = stringResource(R.string.book_options, book.title),
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .background(tokens.coverScrim, CircleShape)
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
                    color = tokens.progressFill,
                    // A visible groove: without one there is nothing to read the
                    // filled part against, so "a third of the way in" and "all
                    // but done" looked alike.
                    trackColor = tokens.progressTrack,
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
 *
 * The glyph is not decoration: colour alone is not a usable signal, and the
 * ribbon is small enough that its red reads as "some badge" long before it
 * reads as "something is wrong".
 */
@Composable
private fun StatusRibbon(status: BookStatus, modifier: Modifier = Modifier) {
    val labelRes = when (status) {
        BookStatus.PROTECTED -> R.string.status_protected
        BookStatus.CORRUPT -> R.string.status_corrupt
        else -> return
    }
    val glyph: ImageVector =
        if (status == BookStatus.PROTECTED) Icons.Default.Lock else Icons.Default.Warning
    val label = stringResource(labelRes)
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
