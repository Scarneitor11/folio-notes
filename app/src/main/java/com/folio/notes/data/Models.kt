package com.folio.notes.data

/** A single note. Stored in SQLite; tags are joined with '|'. */
data class Note(
    val id: String,
    val title: String = "",
    val body: String = "",
    val tint: Int = -1,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()
}

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class SortMode { UPDATED_DESC, UPDATED_ASC, CREATED_DESC, TITLE_ASC, TITLE_DESC }

enum class Feed { ALL, PINNED, ARCHIVED, TRASH }
