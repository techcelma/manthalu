package com.manthalu.ai.data.repository

import android.content.Context
import com.manthalu.ai.data.local.AppDatabase
import com.manthalu.ai.data.local.MemoryEntity
import com.manthalu.ai.data.model.Memory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class MemoryRepository(context: Context) {
    private val dao = AppDatabase.get(context).memoryDao()

    val allMemories: Flow<List<Memory>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun add(
        content: String,
        category: String = "other",
        importance: Int = 3,
        isPinned: Boolean = false
    ): Memory {
        val now = System.currentTimeMillis()
        val entity = MemoryEntity(
            id = UUID.randomUUID().toString(),
            category = category,
            content = content,
            importance = importance.coerceIn(1, 5),
            isPinned = isPinned,
            sourceConversationId = null,
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun update(memory: Memory) {
        dao.upsert(
            MemoryEntity(
                id = memory.id,
                category = memory.category,
                content = memory.content,
                importance = memory.importance,
                isPinned = memory.isPinned,
                sourceConversationId = memory.sourceConversationId,
                createdAt = memory.createdAt.toEpochMilli(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun togglePin(memory: Memory) {
        update(memory.copy(isPinned = !memory.isPinned))
    }

    private fun MemoryEntity.toDomain() = Memory(
        id = id,
        category = category,
        content = content,
        importance = importance,
        isPinned = isPinned,
        sourceConversationId = sourceConversationId,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )
}
