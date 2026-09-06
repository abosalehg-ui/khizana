package com.abosalehg.khizana.ui.shelf

import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.ui.theme.LocalWoodTokens

/**
 * Shared drop-target modifier: extracts the dragged book id from the clip
 * data and reports hover state for the gold highlight.
 *
 * Drag and drop is a convenience, never the only route — every move a drag can
 * make is also reachable from the book's overflow menu, which is what keyboard
 * and screen-reader users have.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun dropTargetModifier(
    key: Any?,
    onDropId: (String) -> Unit,
    onHoverChanged: (Boolean) -> Unit
): Modifier {
    val currentOnDrop by rememberUpdatedState(onDropId)
    val currentHover by rememberUpdatedState(onHoverChanged)
    val target = remember(key) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                currentHover(true)
            }

            override fun onExited(event: DragAndDropEvent) {
                currentHover(false)
            }

            override fun onEnded(event: DragAndDropEvent) {
                currentHover(false)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentHover(false)
                val clip = event.toAndroidDragEvent().clipData ?: return false
                val id = (0 until clip.itemCount)
                    .firstNotNullOfOrNull { clip.getItemAt(it).text?.toString() }
                    ?: return false
                currentOnDrop(id)
                return true
            }
        }
    }
    return Modifier.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
        },
        target = target
    )
}

/**
 * The "a book would land here" outline, in one place instead of three.
 *
 * It reads [WoodTokens.dropHighlight] rather than the identity gold: this is
 * the only feedback a drag gives, so it has to clear the 3:1 that WCAG asks of
 * a functional non-text element in both themes.
 */
@Composable
internal fun Modifier.dropHighlight(active: Boolean, radius: Dp = 8.dp): Modifier {
    val color = LocalWoodTokens.current.dropHighlight
    return if (active) this.border(2.dp, color, RoundedCornerShape(radius)) else this
}
