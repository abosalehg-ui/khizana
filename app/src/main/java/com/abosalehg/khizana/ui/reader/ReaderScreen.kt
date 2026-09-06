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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.bookmarkAt
import com.abosalehg.khizana.reader.buildSpreads
import com.abosalehg.khizana.reader.spreadIndexOfPage
import com.abosalehg.khizana.ui.format.formatCount
import com.abosalehg.khizana.ui.reader.ReaderUiState.Ready
import com.abosalehg.khizana.ui.theme.LocalWoodTokens
import kotlin.math.roundToInt

/** Highest multiple of the viewport width we will re-render a page at. */
private const val MAX_RENDER_SCALE = 3

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
    val tokens = LocalWoodTokens.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.readerBackground)
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
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    // In a spread the settled page is the spread's first page, so this is the
    // bookmark of the page the reader would say they are on.
    val pageBookmark = bookmarkAt(bookmarks, displayedPage)
    var showBookmarkEditor by remember { mutableStateOf(false) }
    var showBookmarkList by remember { mutableStateOf(false) }
    val onPageChanged: (Int) -> Unit = { page ->
        displayedPage = page
        viewModel.onPageSettled(page)
    }
    val onTap = { chromeVisible = !chromeVisible }
    val bookDirection = if (ready.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val tokens = LocalWoodTokens.current

    // Reading a page is the one activity in this app that involves not
    // touching the screen, so the system's idle timer would dim and lock it
    // mid-page. Scoped to the reader and released on the way out, so it can
    // never leak into the shelves.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // The pager itself flips direction so page order matches the book. `key`
    // gives each mode its own saved pager state, so a rotation can never
    // restore a spread index into the single-page pager or vice versa.
    CompositionLocalProvider(LocalLayoutDirection provides bookDirection) {
        key(useSpreads) {
            if (useSpreads) {
                SpreadPager(ready, viewModel, onPageChanged, onTap)
            } else {
                SinglePager(ready, viewModel, onPageChanged, onTap)
            }
        }
    }

    // Deliberately outside the RTL provider above: dialogs follow the UI
    // language, not the book's reading direction.
    if (showBookmarkEditor) {
        BookmarkEditDialog(
            page = displayedPage,
            existing = pageBookmark,
            onSave = { note ->
                viewModel.saveBookmark(displayedPage, note)
                showBookmarkEditor = false
            },
            onDelete = {
                pageBookmark?.let { viewModel.deleteBookmark(it.id) }
                showBookmarkEditor = false
            },
            onDismiss = { showBookmarkEditor = false }
        )
    }
    if (showBookmarkList) {
        BookmarkListDialog(
            bookmarks = bookmarks,
            onJump = { page ->
                viewModel.requestPage(page)
                showBookmarkList = false
            },
            onDelete = viewModel::deleteBookmark,
            onDismiss = { showBookmarkList = false }
        )
    }

    AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.readerChrome)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.action_back), color = tokens.readerOnSurface)
                }
                Text(
                    text = ready.book.title,
                    color = tokens.readerOnSurface,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                IconButton(onClick = { showBookmarkEditor = true }) {
                    Icon(
                        // Filled vs outlined is the "is this page saved?"
                        // answer at a glance, without a second row of chrome.
                        imageVector = if (pageBookmark != null) {
                            Icons.Default.Bookmark
                        } else {
                            Icons.Default.BookmarkBorder
                        },
                        contentDescription = stringResource(
                            if (pageBookmark != null) R.string.bookmark_edit
                            else R.string.bookmark_add
                        ),
                        tint = tokens.readerOnSurface
                    )
                }
                IconButton(onClick = { showBookmarkList = true }) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = stringResource(R.string.bookmarks_title),
                        tint = tokens.readerOnSurface
                    )
                }
                // Always western digits, per spec.
                Text(
                    text = "${formatCount(displayedPage + 1)} / ${formatCount(ready.pageCount)}",
                    color = tokens.readerOnSurface,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            // Swiping is not the only way to move through a book: the slider
            // is reachable by keyboard and screen reader, and it makes long
            // scanned volumes navigable at all.
            if (ready.pageCount > 1) {
                var sliderValue by remember(displayedPage) {
                    mutableFloatStateOf(displayedPage.toFloat())
                }
                val sliderLabel = stringResource(R.string.reader_page_slider)
                // Matching the book's direction keeps "forward" on the slider
                // and "forward" in the pager the same way round.
                CompositionLocalProvider(LocalLayoutDirection provides bookDirection) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.requestPage(sliderValue.roundToInt())
                        },
                        valueRange = 0f..(ready.pageCount - 1).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = sliderLabel }
                    )
                }
            }
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
    SeekHandler(viewModel, pagerState) { it }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        ZoomableBox(resetKey = page, onTap = onTap) { renderScale ->
            PageImage(
                page = page,
                pageCount = ready.pageCount,
                renderScale = renderScale,
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
    SeekHandler(viewModel, pagerState) { page ->
        spreadIndexOfPage(page).coerceIn(0, spreads.lastIndex)
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { index ->
        val spread = spreads[index]
        ZoomableBox(resetKey = index, onTap = onTap) { renderScale ->
            // Row start = right in RTL, left in LTR — reading order for free.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center
            ) {
                PageImage(
                    page = spread.first,
                    pageCount = ready.pageCount,
                    renderScale = renderScale,
                    render = viewModel::renderPage,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
                if (spread.second != null) {
                    PageImage(
                        page = spread.second,
                        pageCount = ready.pageCount,
                        renderScale = renderScale,
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

/** Applies slider jumps to whichever pager is currently mounted. */
@Composable
private fun SeekHandler(
    viewModel: ReaderViewModel,
    pagerState: PagerState,
    toPagerIndex: (page: Int) -> Int
) {
    LaunchedEffect(pagerState) {
        viewModel.seekRequests.collect { page ->
            pagerState.animateScrollToPage(toPagerIndex(page))
        }
    }
}

/**
 * Pinch to zoom (1x–5x), one-finger pan while zoomed, double-tap toggle.
 *
 * [content] receives a quantized render scale: zooming used to only stretch
 * the viewport-width bitmap, so a 5x zoom on a scanned page showed nothing but
 * bigger blur. Quantizing to whole steps keeps it to at most two re-renders.
 */
@Composable
private fun ZoomableBox(
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
private fun PageImage(
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

@Composable
private fun ReaderError(messageRes: Int, onBack: () -> Unit) {
    val tokens = LocalWoodTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(messageRes),
            color = tokens.readerOnSurface,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.padding(12.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}
