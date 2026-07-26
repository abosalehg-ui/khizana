package com.abosalehg.khizana.domain.model

/** A user-defined shelf/topic. Books with topicId = null live on the "New ⭐" shelf. */
data class Topic(
    val id: Long,
    val name: String,
    val order: Int
)
