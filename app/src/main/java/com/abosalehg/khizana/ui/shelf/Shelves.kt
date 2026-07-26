package com.abosalehg.khizana.ui.shelf

import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.Topic

/**
 * One rendered shelf. topicId/name are null for the special "New ⭐" shelf,
 * which always comes first and holds every book without a topic.
 */
data class Shelf(
    val topicId: Long?,
    val name: String?,
    val books: List<Book>
)

/**
 * Pure grouping logic: the New shelf first, then every topic in the given
 * order — including empty ones, so they remain visible drop targets.
 */
fun buildShelves(topics: List<Topic>, books: List<Book>): List<Shelf> {
    val byTopic = books.groupBy { it.topicId }
    val knownIds = topics.mapTo(HashSet()) { it.id }
    // Books pointing at a deleted/unknown topic fall back to the New shelf.
    val newShelfBooks = books.filter { it.topicId == null || it.topicId !in knownIds }
    return buildList {
        add(Shelf(topicId = null, name = null, books = newShelfBooks))
        topics.forEach { topic ->
            add(Shelf(topicId = topic.id, name = topic.name, books = byTopic[topic.id].orEmpty()))
        }
    }
}
