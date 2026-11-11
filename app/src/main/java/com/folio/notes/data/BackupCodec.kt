package com.folio.notes.data

import org.json.JSONArray
import org.json.JSONObject

/** JSON export / import. Plain org.json keeps the payload inspectable and portable. */
object BackupCodec {

    private const val VERSION = 1

    fun encode(notes: List<Note>, now: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("app", "Folio")
        root.put("version", VERSION)
        root.put("exportedAt", now)
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("tint", note.tint)
                    put("pinned", note.pinned)
                    put("archived", note.archived)
                    put("trashed", note.trashed)
                    put("tags", JSONArray(note.tags))
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                },
            )
        }
        root.put("notes", array)
        return root.toString(2)
    }

    fun decode(raw: String): List<Note> {
        val root = JSONObject(raw)
        val array = root.optJSONArray("notes") ?: JSONArray()
        val out = mutableListOf<Note>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val tags = mutableListOf<String>()
            item.optJSONArray("tags")?.let { tagArray ->
                for (tagIndex in 0 until tagArray.length()) {
                    val tag = tagArray.optString(tagIndex).trim()
                    if (tag.isNotEmpty()) tags += tag
                }
            }
            out += Note(
                id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                title = item.optString("title"),
                body = item.optString("body"),
                tint = item.optInt("tint", -1),
                pinned = item.optBoolean("pinned", false),
                archived = item.optBoolean("archived", false),
                trashed = item.optBoolean("trashed", false),
                tags = tags,
                createdAt = item.optLong("createdAt", 0L),
                updatedAt = item.optLong("updatedAt", 0L),
            )
        }
        return out
    }
}
