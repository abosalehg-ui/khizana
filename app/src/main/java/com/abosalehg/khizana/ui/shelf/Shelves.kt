package com.abosalehg.khizana.ui.shelf

import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.Topic
import com.abosalehg.khizana.util.manualThenNaturalComparator

enum class ShelfKind {
    /** Synthetic: most recently read unfinished books. Never a drop target. */
    CONTINUE_READING,

    /** Built-in: books without a topic. */
    NEW,

    /** User-created topic shelf. */
    TOPIC
}

/** One rendered shelf. topicId/name are null except for TOPIC shelves. */
data class Shelf(
    val kind: ShelfKind,
    val topicId: Long?,
    val name: String?,
    val books: List<Book>
)

/** How many books the Continue Reading shelf shows at most. */
const val CONTINUE_READING_LIMIT = 10

/**
 * Restricts shelves to the given book ids (a tag filter). Null means no
 * filter. Shelf structure is preserved so drop targets stay visible.
 */
fun filterShelvesByBookIds(shelves: List<Shelf>, bookIds: Set<String>?): List<Shelf> =
    if (bookIds == null) shelves
    else shelves.map { shelf -> shelf.copy(books = shelf.books.filter { it.id in bookIds }) }

/**
 * Pure grouping logic: Continue Reading first (only when non-empty), then
 * the New shelf, then every topic in order — including empty ones, so they
 * remain visible drop targets. Within a shelf, manually ordered books come
 * first by position, the rest in natural title order (vol 2 before vol 10).
 */
fun buildShelves(topics: List<Topic>, books: List<Book>): List<Shelf> {
    val ordering = manualThenNaturalComparator<Book>({ it.manualOrder }, { it.title })
    val byTopic = books.groupBy { it.topicId }
    val knownIds = topics.mapTo(HashSet()) { it.id }
    // Books pointing at a deleted/unknown topic fall back to the New shelf.
    val newShelfBooks = books.filter { it.topicId == null || it.topicId !in knownIds }
    val continueBooks = books
        .filter { it.progress > 0f && it.progress < 1f && it.lastReadAt != null }
        .sortedByDescending { it.lastReadAt }
        .take(CONTINUE_READING_LIMIT)
    return buildList {
        if (continueBooks.isNotEmpty()) {
            add(Shelf(ShelfKind.CONTINUE_READING, null, null, continueBooks))
        }
        add(Shelf(ShelfKind.NEW, null, null, newShelfBooks.sortedWith(ordering)))
        topics.forEach { topic ->
            add(
                Shelf(
                    kind = ShelfKind.TOPIC,
                    topicId = topic.id,
                    name = topic.name,
                    books = byTopic[topic.id].orEmpty().sortedWith(ordering)
                )
            )
        }
    }
}
