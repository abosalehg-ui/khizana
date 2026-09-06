package com.abosalehg.khizana.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.data.repo.LibraryRepository
import com.abosalehg.khizana.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
