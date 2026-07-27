package com.abosalehg.khizana.ui.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.ui.theme.LocalWoodTokens

/**
 * Shelf dimensions for the current window size. Everything used to be a fixed
 * 72×98 dp, which is both the wrong aspect ratio (the design tokens call for
 * 2:3) and far too small on a tablet.
 */
@Immutable
data class ShelfMetrics(
    val coverWidth: Dp,
    val coverHeight: Dp,
    val spineWidth: Dp,
    val emptyShelfHeight: Dp
)

@Composable
fun rememberShelfMetrics(): ShelfMetrics {
    val aspectRatio = LocalWoodTokens.current.coverAspectRatio
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp, aspectRatio) {
        // Material window size classes: compact < 600 <= medium < 840 <= expanded.
        val coverWidth = when {
            screenWidthDp >= 840 -> 112.dp
            screenWidthDp >= 600 -> 96.dp
            else -> 72.dp
        }
        val coverHeight = coverWidth / aspectRatio
        ShelfMetrics(
            coverWidth = coverWidth,
            coverHeight = coverHeight,
            spineWidth = coverWidth + 4.dp,
            emptyShelfHeight = coverHeight
        )
    }
}
