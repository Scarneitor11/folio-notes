package com.folio.notes.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction

/**
 * A small hand-written SQLite store. Notes are few and text-only, so this keeps
 * the build free of annotation processors and code generated schemas.
 */
class FolioDatabase(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_NOTES)
        db.execSQL(CREATE_INDEX)
    }

    /**
     * Notes are the only copy of what someone wrote, so a version bump has to migrate
     * rather than start over. The table is rebuilt from the current schema and every
     * column the old table also had is carried across; columns that only exist in the
     * new schema take their declared defaults. Any column added later therefore needs
     * a default, or the copy will fail on rows that predate it.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.transaction {
            val existing = rawQuery("PRAGMA table_info($TABLE)", null).use { cursor ->
                val name = cursor.getColumnIndexOrThrow("name")
                buildSet { while (cursor.moveToNext()) add(cursor.getString(name)) }
            }
            val carried = COLUMNS.filter { it in existing }
            // The index travels with the renamed table, so drop it before rebuilding.
            execSQL("DROP INDEX IF EXISTS idx_notes_updated")
            execSQL("ALTER TABLE $TABLE RENAME TO ${TABLE}_old")
            execSQL(CREATE_NOTES)
            execSQL(CREATE_INDEX)
            if (carried.isNotEmpty()) {
                val columns = carried.joinToString(", ")
                execSQL("INSERT INTO $TABLE ($columns) SELECT $columns FROM ${TABLE}_old")
            }
            execSQL("DROP TABLE ${TABLE}_old")
        }
    }

    fun loadAll(): List<Note> {
        val out = mutableListOf<Note>()
        readableDatabase.rawQuery(
            "SELECT id, title, body, tint, pinned, archived, trashed, tags, created_at, updated_at FROM notes",
            null,
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val titleIdx = cursor.getColumnIndexOrThrow("title")
            val bodyIdx = cursor.getColumnIndexOrThrow("body")
            val tintIdx = cursor.getColumnIndexOrThrow("tint")
            val pinnedIdx = cursor.getColumnIndexOrThrow("pinned")
            val archivedIdx = cursor.getColumnIndexOrThrow("archived")
            val trashedIdx = cursor.getColumnIndexOrThrow("trashed")
            val tagsIdx = cursor.getColumnIndexOrThrow("tags")
            val createdIdx = cursor.getColumnIndexOrThrow("created_at")
            val updatedIdx = cursor.getColumnIndexOrThrow("updated_at")
            while (cursor.moveToNext()) {
                out += Note(
                    id = cursor.getString(idIdx),
                    title = cursor.getString(titleIdx) ?: "",
                    body = cursor.getString(bodyIdx) ?: "",
                    tint = cursor.getInt(tintIdx),
                    pinned = cursor.getInt(pinnedIdx) != 0,
                    archived = cursor.getInt(archivedIdx) != 0,
                    trashed = cursor.getInt(trashedIdx) != 0,
                    tags = decodeTags(cursor.getString(tagsIdx)),
                    createdAt = cursor.getLong(createdIdx),
                    updatedAt = cursor.getLong(updatedIdx),
                )
            }
        }
        return out
    }

    fun upsert(note: Note) {
        writableDatabase.insertWithOnConflict(TABLE, null, valuesOf(note), SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Batched import/restore path: one transaction instead of one fsync per note. */
    fun upsertAll(notes: List<Note>) {
        if (notes.isEmpty()) return
        writableDatabase.transaction {
            notes.forEach {
                insertWithOnConflict(TABLE, null, valuesOf(it), SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    private fun valuesOf(note: Note): ContentValues = ContentValues().apply {
        put("id", note.id)
        put("title", note.title)
        put("body", note.body)
        put("tint", note.tint)
        put("pinned", if (note.pinned) 1 else 0)
        put("archived", if (note.archived) 1 else 0)
        put("trashed", if (note.trashed) 1 else 0)
        put("tags", encodeTags(note.tags))
        put("created_at", note.createdAt)
        put("updated_at", note.updatedAt)
    }

    fun delete(id: String) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id))
    }

    fun deleteTrashed() {
        writableDatabase.delete(TABLE, "trashed = 1", null)
    }

    fun replaceAll(notes: List<Note>) {
        writableDatabase.transaction {
            delete(TABLE, null, null)
            notes.forEach {
                insertWithOnConflict(TABLE, null, valuesOf(it), SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    companion object {
        private const val NAME = "folio.db"
        private const val VERSION = 1
        private const val TABLE = "notes"

        private const val CREATE_INDEX = "CREATE INDEX idx_notes_updated ON notes(updated_at)"

        private val COLUMNS = listOf(
            "id", "title", "body", "tint", "pinned", "archived", "trashed",
            "tags", "created_at", "updated_at",
        )

        private const val CREATE_NOTES = """
            CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                body TEXT NOT NULL DEFAULT '',
                tint INTEGER NOT NULL DEFAULT -1,
                pinned INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0,
                trashed INTEGER NOT NULL DEFAULT 0,
                tags TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
        """

        fun encodeTags(tags: List<String>): String =
            tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("|")

        fun decodeTags(raw: String?): List<String> =
            raw.orEmpty().split('|').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
