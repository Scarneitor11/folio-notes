package com.folio.notes.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Single source of truth for notes. The in-memory flow updates immediately so the
 * UI never waits on disk; SQLite writes happen off the main thread.
 */
class NotesRepository(
    private val database: FolioDatabase,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /**
     * Writes are serialized. Dispatchers.IO is a pool, so two saves launched in order
     * can otherwise reach SQLite out of order and leave an older revision on disk.
     */
    private val writeLock = Mutex()

    fun load() {
        scope.launch {
            val stored = withContext(io) { database.loadAll() }
            _notes.value = stored
            _loaded.value = true
        }
    }

    fun note(id: String): Note? = _notes.value.firstOrNull { it.id == id }

    fun create(tint: Int = -1): Note {
        val now = clock()
        val note = Note(
            id = UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now,
            tint = tint,
        )
        _notes.value = listOf(note) + _notes.value
        persist(note)
        return note
    }

    fun save(note: Note) {
        val stamped = note.copy(updatedAt = clock())
        replaceInMemory(stamped)
        persist(stamped)
    }

    fun mutate(id: String, block: (Note) -> Note) {
        val existing = note(id) ?: return
        save(block(existing))
    }

    fun trash(id: String) = mutate(id) { it.copy(trashed = true, pinned = false, updatedAt = clock()) }
    fun restore(id: String) = mutate(id) { it.copy(trashed = false, updatedAt = clock()) }
    fun archive(id: String) = mutate(id) { it.copy(archived = true, pinned = false, updatedAt = clock()) }
    fun unarchive(id: String) = mutate(id) { it.copy(archived = false, updatedAt = clock()) }
    fun togglePin(id: String) = mutate(id) { it.copy(pinned = !it.pinned, updatedAt = clock()) }
    fun setTint(id: String, tint: Int) = mutate(id) { it.copy(tint = tint) }

    fun deleteForever(id: String) {
        _notes.value = _notes.value.filterNot { it.id == id }
        scope.launch { write { database.delete(id) } }
    }

    fun emptyTrash() {
        _notes.value = _notes.value.filterNot { it.trashed }
        scope.launch { write { database.deleteTrashed() } }
    }

    fun addTag(id: String, tag: String) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isEmpty()) return
        mutate(id) { note ->
            if (note.tags.any { it.equals(clean, ignoreCase = true) }) note
            else note.copy(tags = note.tags + clean, updatedAt = clock())
        }
    }

    fun removeTag(id: String, tag: String) = mutate(id) { note ->
        note.copy(tags = note.tags.filterNot { it.equals(tag, ignoreCase = true) }, updatedAt = clock())
    }

    fun renameTag(from: String, to: String) {
        val clean = to.trim().removePrefix("#")
        if (clean.isEmpty()) return
        val changed = mutableListOf<Note>()
        _notes.value = _notes.value.map { note ->
            if (note.tags.none { it.equals(from, ignoreCase = true) }) return@map note
            note.copy(
                tags = note.tags.map { if (it.equals(from, ignoreCase = true)) clean else it }
                    .distinctBy { it.lowercase() },
                updatedAt = clock(),
            ).also { changed += it }
        }
        changed.forEach { persist(it) }
    }

    fun deleteTag(tag: String) {
        val changed = mutableListOf<Note>()
        _notes.value = _notes.value.map { note ->
            if (note.tags.none { it.equals(tag, ignoreCase = true) }) return@map note
            note.copy(
                tags = note.tags.filterNot { it.equals(tag, ignoreCase = true) },
                updatedAt = clock(),
            ).also { changed += it }
        }
        changed.forEach { persist(it) }
    }

    /** Replaces the whole store — used by import and by restoring a backup. */
    fun replaceAll(notes: List<Note>) {
        _notes.value = notes
        scope.launch { write { database.replaceAll(notes) } }
    }

    fun appendAll(notes: List<Note>) {
        val existingIds = _notes.value.map { it.id }.toSet()
        val incoming = notes.filter { it.id !in existingIds }
        if (incoming.isEmpty()) return
        val merged = _notes.value + incoming
        _notes.value = merged
        scope.launch { write { incoming.forEach { database.upsert(it) } } }
    }

    private fun replaceInMemory(note: Note) {
        val current = _notes.value
        val index = current.indexOfFirst { it.id == note.id }
        _notes.value = if (index >= 0) {
            current.toMutableList().also { it[index] = note }
        } else {
            listOf(note) + current
        }
    }

    private fun persist(note: Note) {
        scope.launch { write { database.upsert(note) } }
    }

    /**
     * Runs one database mutation at a time, in the order it was launched. Failures are
     * logged rather than thrown: losing a write should not take the app down, but it
     * must not disappear silently either.
     */
    private suspend fun write(block: suspend () -> Unit) {
        writeLock.withLock {
            withContext(io) {
                runCatching { block() }.onFailure { Log.e(TAG, "Note write failed", it) }
            }
        }
    }

    private companion object {
        const val TAG = "NotesRepository"
    }
}
