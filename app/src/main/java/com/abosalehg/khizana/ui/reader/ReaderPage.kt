package com.abosalehg.khizana.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.abosalehg.khizana.R
import com.abosalehg.khizana.ui.format.formatCount
import com.abosalehg.khizana.ui.theme.LocalWoodTokens
import kotlin.math.roundToInt

/*
 * One page on screen: the zoom/pan surface and the rendered bitmap inside it.
 * Split out of ReaderScreen.kt, which keeps the screen, the chrome and the
 * two pagers.
 */

/** Highest multiple of the viewport width we will re-render a page at. */
private const val MAX_RENDER_SCALE = 3

/**
 * Pinch to zoom (1x–5x), one-finger pan while zoomed, double-tap toggle.
 *
 * [content] receives a quantized render scale: zooming used to only stretch
 * the viewport-width bitmap, so a 5x zoom on a scanned page showed nothing but
 * bigger blur. Quantizing to whole steps keeps it to at most two re-renders.
 */
@Composable
internal fun ZoomableBox(
    resetKey: Any?,
    onTap: () -> Unit,
    content: @Composable (renderScale: Float) -> Unit
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
    val renderScale = scale.roundToInt().coerceIn(1, MAX_RENDER_SCALE).toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(resetKey) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(resetKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.size > 1
                        if (pinching || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            val pan = event.calculatePan()
                            val maxX = size.width * (scale - 1f) / 2f
                            val maxY = size.height * (scale - 1f) / 2f
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                            // Keep the pager from paging while zoomed/pinching.
                            event.changes.forEach { it.consume() }
                        }
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            content(renderScale)
        }
    }
}

@Composable
internal fun PageImage(
    page: Int,
    pageCount: Int,
    renderScale: Float,
    render: suspend (page: Int, targetWidth: Int) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWoodTokens.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val baseWidth = constraints.maxWidth.coerceAtLeast(1)
        val targetWidth = (baseWidth * renderScale).toInt()
            .coerceIn(1, baseWidth * MAX_RENDER_SCALE)
        var failed by remember(page) { mutableStateOf(false) }
        // Keyed on the page only: a re-render at a higher zoom keeps showing
        // the previous bitmap instead of flashing a spinner.
        var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(page, targetWidth) {
            val rendered = render(page, targetWidth)
            if (rendered != null) {
                bitmap = rendered
                failed = false
            } else if (bitmap == null) {
                failed = true
            }
        }
        val current = bitmap
        val description = stringResource(
            R.string.reader_page_position,
            formatCount(page + 1),
            formatCount(pageCount)
        )
        when {
            current != null -> Image(
                bitmap = current.asImageBitmap(),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            failed -> Text(
                text = stringResource(R.string.reader_page_failed),
                color = tokens.readerOnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            else -> CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = description }
            )
        }
    }
}
