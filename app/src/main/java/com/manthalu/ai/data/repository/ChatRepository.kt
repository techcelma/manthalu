package com.manthalu.ai.data.repository

import android.content.Context
import com.manthalu.ai.data.local.AppDatabase
import com.manthalu.ai.data.local.ConversationEntity
import com.manthalu.ai.data.local.MessageEntity
import com.manthalu.ai.data.local.SecureConfigStore
import com.manthalu.ai.data.model.AiMode
import com.manthalu.ai.data.model.Message
import com.manthalu.ai.data.model.MessageRole
import com.manthalu.ai.data.remote.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val config = SecureConfigStore(context)
    private val gemini = GeminiClient(config)

    val conversations: Flow<List<ConversationEntity>> = db.conversationDao().observeAll()

    fun messagesFor(conversationId: String): Flow<List<Message>> =
        db.messageDao().observeForConversation(conversationId).map { list ->
            list.map {
                Message(
                    id = it.id,
                    conversationId = it.conversationId,
                    role = if (it.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
                    content = it.content,
                    model = it.model,
                    createdAt = java.time.Instant.ofEpochMilli(it.createdAt)
                )
            }
        }

    suspend fun createConversation(mode: AiMode = AiMode.GENERAL): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.conversationDao().upsert(
            ConversationEntity(
                id = id,
                title = null,
                mode = mode.systemKey,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun sendMessage(
        conversationId: String,
        userText: String,
        mode: AiMode
    ): Result<String> {
        val now = System.currentTimeMillis()

        // Persist user message
        db.messageDao().insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "user",
                content = userText,
                createdAt = now
            )
        )

        // Short conversation history
        val history = db.messageDao().getForConversation(conversationId)
            .map { it.role to it.content }

        // Pull relevant memories (simple keyword + importance ranking)
        val relevantMemories = if (config.memoryEnabled) {
            selectRelevantMemories(userText)
        } else {
            emptyList()
        }

        // Call Gemini with memories injected into system prompt
        val result = gemini.generate(
            userMessage = userText,
            mode = mode,
            conversationHistory = history,
            relevantMemories = relevantMemories
        )

        result.onSuccess { reply ->
            db.messageDao().insert(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "assistant",
                    content = reply,
                    model = config.geminiModelChat,
                    createdAt = System.currentTimeMillis()
                )
            )
            val conv = db.conversationDao().getById(conversationId)
            if (conv != null && conv.title.isNullOrBlank()) {
                val title = userText.take(48).let { if (userText.length > 48) "$it…" else it }
                db.conversationDao().upsert(conv.copy(title = title, updatedAt = System.currentTimeMillis()))
            } else {
                db.conversationDao().touch(conversationId, System.currentTimeMillis())
            }
        }

        return result
    }

    /**
     * Very simple relevance: prefer pinned + high-importance memories whose
     * content shares words with the user message. Good enough for personal use.
     */
    private suspend fun selectRelevantMemories(userText: String): List<String> {
        val all = try {
            // Use search with a broad query then rank
            val words = userText.lowercase()
                .split(Regex("\\W+"))
                .filter { it.length > 3 }
                .distinct()
                .take(6)

            if (words.isEmpty()) {
                db.memoryDao().observeAll() // fallback not ideal in suspend; use search
                emptyList()
            } else {
                // Pull all and rank in memory (personal collections stay small)
                val entities = db.memoryDao().search(words.first())
                // Also try other words and merge
                val extra = words.drop(1).flatMap { w ->
                    try { db.memoryDao().search(w) } catch (_: Exception) { emptyList() }
                }
                (entities + extra).distinctBy { it.id }
            }
        } catch (_: Exception) {
            emptyList()
        }

        val candidates = if (all.isEmpty()) {
            try { db.memoryDao().getAllOnce() } catch (_: Exception) { emptyList() }
        } else all

        return candidates
            .sortedWith(
                compareByDescending<com.manthalu.ai.data.local.MemoryEntity> { it.isPinned }
                    .thenByDescending { it.importance }
                    .thenByDescending { it.updatedAt }
            )
            .take(8)
            .map { it.content }
    }

    suspend fun deleteConversation(id: String) {
        db.messageDao().deleteForConversation(id)
        db.conversationDao().delete(id)
    }

    suspend fun getConversation(id: String) = db.conversationDao().getById(id)
}
