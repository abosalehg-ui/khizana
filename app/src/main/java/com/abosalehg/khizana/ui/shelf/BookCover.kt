package com.abosalehg.khizana.ui.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.abosalehg.khizana.domain.model.Book
import java.io.File

/**
 * A book's cover: the generated JPEG when there is one, the title's first
 * letter when there is not.
 *
 * Shared by the shelves and the hidden-books list, which drew the same two
 * branches separately — and it is the single place the designed fallback cover
 * on the roadmap will replace.
 *
 * The fallback carries the title in [semantics] because a lone letter tells a
 * screen reader nothing; pass `describe = false` where a visible title label
 * sits next to it, so the same words are not announced twice.
 */
@Composable
internal fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    describe: Boolean = true
) {
    val description = book.title.takeIf { describe }
    if (book.coverPath != null) {
        AsyncImage(
            model = File(book.coverPath),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (description != null) {
                        Modifier.semantics { contentDescription = description }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
