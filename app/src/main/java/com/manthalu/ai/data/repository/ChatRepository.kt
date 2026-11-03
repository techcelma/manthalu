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
import java.time.Instant
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
                    createdAt = Instant.ofEpochMilli(it.createdAt)
                )
            }
        }

    suspend fun createConversation(mode: AiMode = AiMode.GENERAL): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.conversationDao().upsert(
            ConversationEntity(id = id, title = null, mode = mode.systemKey, createdAt = now, updatedAt = now)
        )
        return id
    }

    suspend fun updateConversationMode(conversationId: String, mode: AiMode) {
        val conversation = db.conversationDao().getById(conversationId) ?: return
        db.conversationDao().upsert(conversation.copy(mode = mode.systemKey, updatedAt = System.currentTimeMillis()))
    }

    suspend fun sendMessage(
        conversationId: String,
        userText: String,
        mode: AiMode,
        attachmentContext: String? = null
    ): Result<String> {
        val now = System.currentTimeMillis()
        val attachmentLabel = attachmentContext?.substringBefore("\n")?.takeIf { it.isNotBlank() }
        val savedUserText = if (attachmentLabel != null) "$userText\n\n[Attached file: $attachmentLabel]" else userText
        db.messageDao().insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "user",
                content = savedUserText,
                createdAt = now
            )
        )

        // The current turn is passed separately; only send earlier turns as history.
        val history = db.messageDao().getForConversation(conversationId)
            .dropLast(1)
            .takeLast(24)
            .map { it.role to it.content.take(6_000) }

        val relevantMemories = if (config.memoryEnabled) selectRelevantMemories(userText) else emptyList()
        val prompt = if (attachmentContext.isNullOrBlank()) userText else
            "$userText\n\n--- Attached file context ---\n$attachmentContext\n--- End attached file context ---"

        val result = gemini.generate(
            userMessage = prompt,
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
                db.conversationDao().upsert(conv.copy(title = title, mode = mode.systemKey, updatedAt = System.currentTimeMillis()))
            } else {
                db.conversationDao().touch(conversationId, System.currentTimeMillis())
                updateConversationMode(conversationId, mode)
            }
        }
        return result
    }

    /** Rank approved memories by relevance first, then by pin and importance. */
    private suspend fun selectRelevantMemories(userText: String): List<String> {
        val candidates = try { db.memoryDao().getAllOnce() } catch (_: Exception) { emptyList() }
        val terms = userText.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        return candidates
            .sortedWith(
                compareByDescending<com.manthalu.ai.data.local.MemoryEntity> { memory ->
                    val words = (memory.content + " " + memory.category).lowercase()
                        .split(Regex("\\W+")).filter { it.length > 2 }.toSet()
                    terms.intersect(words).size
                }
                    .thenByDescending { it.isPinned }
                    .thenByDescending { it.importance }
                    .thenByDescending { it.updatedAt }
            )
            .take(10)
            .map { it.content }
    }

    suspend fun deleteConversation(id: String) {
        db.messageDao().deleteForConversation(id)
        db.conversationDao().delete(id)
    }

    suspend fun getConversation(id: String) = db.conversationDao().getById(id)
}
