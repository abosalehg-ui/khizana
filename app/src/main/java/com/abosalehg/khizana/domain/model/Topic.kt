package com.abosalehg.khizana.domain.model

/** A user-defined shelf/topic. Books with topicId = null live on the "New ⭐" shelf. */
data class Topic(
    val id: Long,
    val name: String,
    val order: Int
)

/** A free-form label attachable to any number of books. */
data class Tag(
    val id: Long,
    val name: String
)
