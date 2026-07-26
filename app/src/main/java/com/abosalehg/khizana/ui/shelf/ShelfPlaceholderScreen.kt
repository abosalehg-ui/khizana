package com.abosalehg.khizana.ui.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.ThemeMode
import com.abosalehg.khizana.ui.theme.LocalWoodTokens

/**
 * M0 placeholder for the shelves screen: brand identity only.
 * Replaced by the real shelf UI in M3.
 */
@Composable
fun ShelfPlaceholderScreen(
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.shelf_placeholder_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            ShelfPlankPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )
            Spacer(Modifier.height(40.dp))
            TextButton(onClick = onCycleThemeMode) {
                Text(
                    text = stringResource(
                        R.string.theme_mode_label,
                        stringResource(themeMode.labelRes())
                    )
                )
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}

/** A single wooden plank — first taste of the shelf identity. */
@Composable
private fun ShelfPlankPreview(modifier: Modifier = Modifier) {
    val tokens = LocalWoodTokens.current
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // Plank body: vertical gradient from light top to deep bottom.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(tokens.plankLight, tokens.plankBase, tokens.plankDeep)
                )
            )
            // Subtle wood grain: sparse translucent vertical lines.
            val step = 34.dp.toPx()
            var x = step / 2f
            var index = 0
            while (x < size.width) {
                val color = if (index % 2 == 0) tokens.plankGrainDark else tokens.plankGrainLight
                drawLine(
                    color = color,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (index % 2 == 0) 2f else 1f
                )
                x += step
                index++
            }
            // Gold trim line on the top edge.
            drawLine(
                color = tokens.goldSoft.copy(alpha = 0.6f),
                start = Offset(0f, 1f),
                end = Offset(size.width, 1f),
                strokeWidth = 2f
            )
        }
    }
}
