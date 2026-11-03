package com.manthalu.ai.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

 data class LocalDocument(
    val id: String,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val textPreview: String,
    val fullText: String,
    val addedAt: Long = System.currentTimeMillis()
)

/** Reads user-selected documents through Android's Storage Access Framework. */
class DocumentStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("manthalu_docs_meta", Context.MODE_PRIVATE)
    private val texts = context.getSharedPreferences("manthalu_docs_text", Context.MODE_PRIVATE)

    fun list(): List<LocalDocument> {
        val ids = prefs.getStringSet("ids", emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            val name = prefs.getString("$id.name", null) ?: return@mapNotNull null
            val full = texts.getString(id, "") ?: ""
            LocalDocument(
                id = id,
                name = name,
                mimeType = prefs.getString("$id.mime", null),
                sizeBytes = prefs.getLong("$id.size", 0L),
                textPreview = full.take(200),
                fullText = full,
                addedAt = prefs.getLong("$id.added", 0L)
            )
        }.sortedByDescending { it.addedAt }
    }

    fun importFromUri(uri: Uri): Result<LocalDocument> = try {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        var name = "document"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val text = when {
            mime?.startsWith("text/") == true || name.endsWithAny(".txt", ".md", ".markdown", ".json", ".csv", ".kt", ".java", ".ts", ".js", ".py", ".xml") -> {
                resolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText().take(MAX_TEXT_CHARS)
                }.orEmpty()
            }
            mime == "application/pdf" || name.endsWith(".pdf", true) ->
                "[PDF selected: $name. This build cannot extract PDF text yet; attach a text-based file or paste the relevant passage.]"
            else -> "[File selected: $name. This build can attach text-based files; binary file contents are not extracted.]"
        }

        val id = UUID.randomUUID().toString()
        val ids = prefs.getStringSet("ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(id)
        prefs.edit()
            .putStringSet("ids", ids)
            .putString("$id.name", name)
            .putString("$id.mime", mime)
            .putLong("$id.size", size)
            .putLong("$id.added", System.currentTimeMillis())
            .apply()
        texts.edit().putString(id, text).apply()
        Result.success(LocalDocument(id, name, mime, size, text.take(200), text))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun delete(id: String) {
        val ids = prefs.getStringSet("ids", emptySet())?.toMutableSet() ?: return
        ids.remove(id)
        prefs.edit().putStringSet("ids", ids).remove("$id.name").remove("$id.mime")
            .remove("$id.size").remove("$id.added").apply()
        texts.edit().remove(id).apply()
    }

    fun getText(id: String): String = texts.getString(id, "") ?: ""

    private fun String.endsWithAny(vararg suffixes: String): Boolean = suffixes.any { endsWith(it, true) }

    private companion object {
        const val MAX_TEXT_CHARS = 100_000
    }
}
