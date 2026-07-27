package com.abosalehg.khizana.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.Tag
import kotlinx.coroutines.launch

/**
 * Library screen: wooden shelves grouped by topic with the "New ⭐" shelf
 * first, drag & drop plus a menu equivalent to move books between shelves,
 * and shelf creation.
 *
 * The pieces it composes live next to it: [ShelfSection], [BookSpine],
 * [ScanSection]/[PermissionCard] and the dialogs in `LibraryDialogs.kt`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenBook: (Book) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.libraryState.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddTopic by remember { mutableStateOf(false) }
    var shelfToRename by remember { mutableStateOf<Shelf?>(null) }
    var shelfToDelete by remember { mutableStateOf<Shelf?>(null) }
    var bookForTags by remember { mutableStateOf<Book?>(null) }
    var bookToMove by remember { mutableStateOf<Book?>(null) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    val context = LocalContext.current
    val metrics = rememberShelfMetrics()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // The grant happens in system settings — re-check whenever we come back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    if (showAddTopic) {
        AddTopicDialog(
            onConfirm = { name ->
                viewModel.addTopic(name)
                showAddTopic = false
            },
            onDismiss = { showAddTopic = false }
        )
    }
    shelfToRename?.let { shelf ->
        RenameTopicDialog(
            currentName = shelf.name.orEmpty(),
            onConfirm = { name ->
                shelf.topicId?.let { viewModel.renameTopic(it, name) }
                shelfToRename = null
            },
            onDismiss = { shelfToRename = null }
        )
    }
    shelfToDelete?.let { shelf ->
        AlertDialog(
            onDismissRequest = { shelfToDelete = null },
            title = { Text(stringResource(R.string.delete_shelf)) },
            text = { Text(stringResource(R.string.delete_shelf_message)) },
            confirmButton = {
                TextButton(onClick = {
                    shelf.topicId?.let { viewModel.deleteTopic(it) }
                    shelfToDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { shelfToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    bookToMove?.let { book ->
        MoveToShelfDialog(
            book = book,
            shelves = state.shelves,
            onMove = { topicId ->
                viewModel.moveBook(book.id, topicId)
                bookToMove = null
            },
            onDismiss = { bookToMove = null }
        )
    }
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text(stringResource(R.string.delete_book)) },
            text = { Text(stringResource(R.string.delete_book_message, book.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(book) { ok ->
                        if (!ok) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.delete_book_failed)
                                )
                            }
                        }
                    }
                    bookToDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    bookForTags?.let { book ->
        TagsDialog(
            book = book,
            allTags = tags,
            loadSelected = viewModel::tagIdsForBook,
            onSave = { tagIds, newNames ->
                viewModel.saveTags(book.id, tagIds, newNames)
                bookForTags = null
            },
            onDismiss = { bookForTags = null }
        )
    }

    /** Hiding is instant but reversible from the snackbar. */
    fun hideWithUndo(book: Book) {
        viewModel.hideBook(book.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.book_hidden, book.title),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.unhideBook(book.id)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!permissionGranted) {
                item(key = "permission") {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        PermissionCard(onGranted = viewModel::refreshPermission)
                    }
                }
                return@LazyColumn
            }

            item(key = "scan") {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    ScanSection(
                        scanState = scanState,
                        bookCount = state.bookCount,
                        onScanClick = viewModel::startScan,
                        onAddTopicClick = { showAddTopic = true }
                    )
                }
            }
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.search_clear)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            if (tags.isNotEmpty()) {
                item(key = "tags") {
                    TagFilterRow(
                        tags = tags,
                        selectedTagId = selectedTagId,
                        onSelect = viewModel::selectTag
                    )
                }
            }

            val nothingToShow = state.shelves.all { it.books.isEmpty() }
            when {
                state.loading -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                // A library with no books at all is a different message from a
                // filter that matched nothing; the old UI showed neither.
                nothingToShow && !state.hasAnyBook -> item(key = "empty") {
                    LibraryPlaceholder(R.string.empty_library)
                }
                nothingToShow -> item(key = "no-results") {
                    LibraryPlaceholder(R.string.no_results)
                }
                else -> items(
                    state.shelves,
                    key = { "${it.kind.name}:${it.topicId ?: -1L}" }
                ) { shelf ->
                    ShelfSection(
                        shelf = shelf,
                        metrics = metrics,
                        onMoveBook = viewModel::moveBook,
                        onDropOnBook = viewModel::dropOnBook,
                        onOpenBook = onOpenBook,
                        onRenameShelf = { shelfToRename = it },
                        onDeleteShelf = { shelfToDelete = it },
                        onHideBook = ::hideWithUndo,
                        onMoveBookRequest = { bookToMove = it },
                        onTagBook = { bookForTags = it },
                        onDeleteBook = { bookToDelete = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<Tag>,
    selectedTagId: Long?,
    onSelect: (Long?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.filter_all)) }
            )
        }
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelect(if (selectedTagId == tag.id) null else tag.id) },
                label = { Text(tag.name) }
            )
        }
    }
}
