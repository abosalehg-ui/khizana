package com.abosalehg.khizana.ui.reader

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abosalehg.khizana.R
import com.abosalehg.khizana.reader.buildSpreads
import com.abosalehg.khizana.reader.spreadIndexOfPage
import com.abosalehg.khizana.ui.reader.ReaderUiState.Ready

/**
 * Reader: single-page paging in portrait, two-page spreads in landscape,
 * pinch/double-tap zoom, RTL/LTR direction, position persistence.
 */
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141210))
    ) {
        when (val s = state) {
            ReaderUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ReaderUiState.Protected -> ReaderError(R.string.reader_protected, onBack)
            ReaderUiState.Corrupt -> ReaderError(R.string.reader_corrupt, onBack)
            ReaderUiState.Missing -> ReaderError(R.string.reader_missing, onBack)
            is Ready -> ReaderContent(s, viewModel, onBack)
        }
    }
}

@Composable
private fun ReaderContent(
    ready: Ready,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useSpreads = isLandscape && ready.pageCount > 1
    var chromeVisible by remember { mutableStateOf(true) }
    var displayedPage by remember { mutableIntStateOf(viewModel.currentPage) }
    val onPageChanged: (Int) -> Unit = { page ->
        displayedPage = page
        viewModel.onPageSettled(page)
    }
    val onTap = { chromeVisible = !chromeVisible }

    // The pager itself flips direction so page order matches the book.
    CompositionLocalProvider(
        LocalLayoutDirection provides
            if (ready.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        if (useSpreads) {
            SpreadPager(ready, viewModel, onPageChanged, onTap)
        } else {
            SinglePager(ready, viewModel, onPageChanged, onTap)
        }
    }

    AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC141210))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.action_back), color = Color.White)
            }
            Text(
                text = ready.book.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            // Always western digits, per spec.
            Text(
                text = "${displayedPage + 1} / ${ready.pageCount}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SinglePager(
    ready: Ready,
    viewModel: ReaderViewModel,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = viewModel.currentPage) { ready.pageCount }
    LaunchedEffect(pagerState.settledPage) { onPageChanged(pagerState.settledPage) }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        ZoomableBox(resetKey = page, onTap = onTap) {
            PageImage(
                page = page,
                render = viewModel::renderPage,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SpreadPager(
    ready: Ready,
    viewModel: ReaderViewModel,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit
) {
    val spreads = remember(ready.pageCount) { buildSpreads(ready.pageCount) }
    val pagerState = rememberPagerState(
        initialPage = spreadIndexOfPage(viewModel.currentPage).coerceAtMost(spreads.lastIndex)
    ) { spreads.size }
    LaunchedEffect(pagerState.settledPage) { onPageChanged(spreads[pagerState.settledPage].first) }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { index ->
        val spread = spreads[index]
        ZoomableBox(resetKey = index, onTap = onTap) {
            // Row start = right in RTL, left in LTR — reading order for free.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center
            ) {
                PageImage(
                    page = spread.first,
                    render = viewModel::renderPage,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
                if (spread.second != null) {
                    PageImage(
                        page = spread.second,
                        render = viewModel::renderPage,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Pinch to zoom (1x–5x), one-finger pan while zoomed, double-tap toggle. */
@Composable
private fun ZoomableBox(
    resetKey: Any?,
    onTap: () -> Unit,
    content: @Composable () -> Unit
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }

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
            content()
        }
    }
}

@Composable
private fun PageImage(
    page: Int,
    render: suspend (page: Int, targetWidth: Int) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val widthPx = constraints.maxWidth.coerceAtLeast(1)
        var failed by remember(page) { mutableStateOf(false) }
        var bitmap by remember(page, widthPx) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(page, widthPx) {
            val rendered = render(page, widthPx)
            bitmap = rendered
            if (rendered == null) failed = true
        }
        val current = bitmap
        when {
            current != null -> Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            failed -> Text(
                text = stringResource(R.string.reader_page_failed),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
private fun ReaderError(messageRes: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(messageRes),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.padding(12.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}
