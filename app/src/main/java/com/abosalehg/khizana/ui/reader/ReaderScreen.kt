package com.abosalehg.khizana.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abosalehg.khizana.R
import com.abosalehg.khizana.ui.reader.ReaderUiState.Ready

/**
 * M4 reader: full-screen page view with swipe paging, RTL/LTR direction,
 * position persistence, and a tap-toggled top bar.
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
    val pagerState = rememberPagerState(initialPage = ready.initialPage) { ready.pageCount }
    var chromeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.settledPage) {
        viewModel.onPageSettled(pagerState.settledPage)
    }

    // The pager itself flips direction so page order matches the book.
    CompositionLocalProvider(
        LocalLayoutDirection provides
            if (ready.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ReaderPage(
                page = page,
                render = viewModel::renderPage,
                onTap = { chromeVisible = !chromeVisible }
            )
        }
    }

    AnimatedVisibility(
        visible = chromeVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
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
                text = "${pagerState.currentPage + 1} / ${ready.pageCount}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ReaderPage(
    page: Int,
    render: suspend (page: Int, targetWidth: Int) -> Bitmap?,
    onTap: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth
        var failed by remember(page) { mutableStateOf(false) }
        val bitmap by produceState<Bitmap?>(initialValue = null, page, widthPx) {
            value = render(page, widthPx)
            if (value == null) failed = true
        }
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(),
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
        androidx.compose.foundation.layout.Spacer(Modifier.padding(12.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}
