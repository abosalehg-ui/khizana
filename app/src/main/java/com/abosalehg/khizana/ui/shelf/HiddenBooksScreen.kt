package com.abosalehg.khizana.ui.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.repo.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HiddenBooksViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {

    val hiddenBooks: StateFlow<List<Book>> = repository.hiddenBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unhide(bookId: String) {
        viewModelScope.launch { repository.setBookHidden(bookId, false) }
    }
}

/** Management screen for hidden books: list them, unhide on demand. */
@Composable
fun HiddenBooksScreen(
    onBack: () -> Unit,
    viewModel: HiddenBooksViewModel = hiltViewModel()
) {
    val books by viewModel.hiddenBooks.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hidden_books),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.action_back))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (books.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_hidden_books),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(books, key = { it.id }) { book ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val coverModifier = Modifier
                                    .width(40.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                if (book.coverPath != null) {
                                    AsyncImage(
                                        model = File(book.coverPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = coverModifier
                                    )
                                } else {
                                    Box(
                                        modifier = coverModifier.background(
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.unhide(book.id) }) {
                                    Text(stringResource(R.string.action_unhide))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
