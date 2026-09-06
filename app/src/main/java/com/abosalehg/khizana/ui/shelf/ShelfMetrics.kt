package com.abosalehg.khizana.ui.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.ui.theme.LocalWoodTokens

/**
 * Shelf dimensions for the current window size. Everything used to be a fixed
 * 72x98 dp, which is both the wrong aspect ratio (the design tokens call for
 * 2:3) and far too small on a tablet.
 */
@Immutable
data class ShelfMetrics(
    val coverWidth: Dp,
    val coverHeight: Dp,
    val spineWidth: Dp,
    val emptyShelfHeight: Dp
)

/**
 * Metrics for a container [widthDp] wide.
 *
 * Pure, and it takes the width rather than reading it, because the number that
 * matters is how wide the shelves actually are — not how wide the display is.
 * `LocalConfiguration.screenWidthDp` answers the second question: in split
 * screen, in a freeform window, or on the narrow half of a folded device it
 * reports the whole screen while the app owns a fraction of it, and 112 dp
 * covers then overflowed a 400 dp row.
 */
fun shelfMetricsFor(widthDp: Int, aspectRatio: Float): ShelfMetrics {
    // Material window size classes: compact < 600 <= medium < 840 <= expanded.
    val coverWidth = when {
        widthDp >= 840 -> 112.dp
        widthDp >= 600 -> 96.dp
        else -> 72.dp
    }
    val coverHeight = coverWidth / aspectRatio
    return ShelfMetrics(
        coverWidth = coverWidth,
        coverHeight = coverHeight,
        spineWidth = coverWidth + 4.dp,
        emptyShelfHeight = coverHeight
    )
}

@Composable
fun rememberShelfMetrics(widthDp: Int): ShelfMetrics {
    val aspectRatio = LocalWoodTokens.current.coverAspectRatio
    return androidx.compose.runtime.remember(widthDp, aspectRatio) {
        shelfMetricsFor(widthDp, aspectRatio)
    }
}
