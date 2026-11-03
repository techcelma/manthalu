package com.manthalu.ai.data.model

import java.time.Instant
import java.util.UUID

/**
 * Core domain models used across the app.
 * These are the single source of truth for UI and network layers.
 */

enum class AiMode(val displayName: String, val systemKey: String) {
    GENERAL("General", "general"),
    STUDY("Study", "study"),
    DEVELOPER("Developer", "developer"),
    REFLECTION("Reflection", "reflection"),
    PRODUCTIVITY("Productivity", "productivity"),
    RESEARCH("Research", "research");

    companion object {
        fun fromKey(key: String): AiMode =
            entries.find { it.systemKey == key } ?: GENERAL
    }
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String? = null,
    val mode: AiMode = AiMode.GENERAL,
    val summary: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val messageCount: Int = 0
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val model: String? = null,
    val createdAt: Instant = Instant.now(),
    val isStreaming: Boolean = false,
    val error: String? = null
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, TOOL
}

data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "other",
    val content: String,
    val importance: Int = 3,          // 1–5
    val sourceConversationId: String? = null,
    val isPinned: Boolean = false,
    val expiresAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: Int = 3,
    val dueAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)

enum class TaskStatus {
    TODO, IN_PROGRESS, DONE, CANCELLED
}

data class DocumentMeta(
    val id: String = UUID.randomUUID().toString(),
    val filename: String,
    val mimeType: String? = null,
    val sizeBytes: Long = 0,
    val status: DocumentStatus = DocumentStatus.PENDING,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now()
)

enum class DocumentStatus {
    PENDING, PROCESSING, READY, FAILED
}

/** Local draft that has not yet been sent to the backend */
data class ChatDraft(
    val conversationId: String?,
    val text: String,
    val mode: AiMode = AiMode.GENERAL
)
