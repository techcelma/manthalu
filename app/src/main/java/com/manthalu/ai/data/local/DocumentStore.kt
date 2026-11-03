package com.manthalu.ai.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

data class LocalDocument(
    val id: String,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val textPreview: String,
    val fullText: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Reads user-selected documents via Storage Access Framework.
 * Supports plain text, markdown, and basic extraction from other text-like files.
 * PDF binary extraction is limited without a dedicated PDF library.
 */
class DocumentStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("manthalu_docs_meta", Context.MODE_PRIVATE)
    private val texts = context.getSharedPreferences("manthalu_docs_text", Context.MODE_PRIVATE)

    fun list(): List<LocalDocument> {
        val ids = prefs.getStringSet("ids", emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            val name = prefs.getString("$id.name", null) ?: return@mapNotNull null
            val mime = prefs.getString("$id.mime", null)
            val size = prefs.getLong("$id.size", 0L)
            val added = prefs.getLong("$id.added", 0L)
            val full = texts.getString(id, "") ?: ""
            LocalDocument(
                id = id,
                name = name,
                mimeType = mime,
                sizeBytes = size,
                textPreview = full.take(200),
                fullText = full,
                addedAt = added
            )
        }.sortedByDescending { it.addedAt }
    }

    fun importFromUri(uri: Uri): Result<LocalDocument> {
        return try {
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
                mime?.startsWith("text/") == true ||
                name.endsWith(".txt", true) ||
                name.endsWith(".md", true) ||
                name.endsWith(".markdown", true) ||
                name.endsWith(".json", true) ||
                name.endsWith(".csv", true) ||
                name.endsWith(".kt", true) ||
                name.endsWith(".java", true) ||
                name.endsWith(".ts", true) ||
                name.endsWith(".js", true) ||
                name.endsWith(".py", true) -> {
                    resolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).readText()
                    } ?: ""
                }
                mime == "application/pdf" || name.endsWith(".pdf", true) -> {
                    // Without a PDF library we store a note; user can paste text for now
                    "[PDF selected: $name. Full PDF text extraction requires a PDF library. Paste key sections into Chat or Memory for now.]"
                }
                else -> {
                    // Best-effort: try as text
                    try {
                        resolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream)).readText().take(500_000)
                        } ?: "[Could not read file as text]"
                    } catch (_: Exception) {
                        "[Unsupported or binary format: $name]"
                    }
                }
            }

            val id = java.util.UUID.randomUUID().toString()
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

            Result.success(
                LocalDocument(
                    id = id,
                    name = name,
                    mimeType = mime,
                    sizeBytes = size,
                    textPreview = text.take(200),
                    fullText = text
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun delete(id: String) {
        val ids = prefs.getStringSet("ids", emptySet())?.toMutableSet() ?: return
        ids.remove(id)
        prefs.edit()
            .putStringSet("ids", ids)
            .remove("$id.name")
            .remove("$id.mime")
            .remove("$id.size")
            .remove("$id.added")
            .apply()
        texts.edit().remove(id).apply()
    }

    fun getText(id: String): String = texts.getString(id, "") ?: ""
}
