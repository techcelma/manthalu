package com.manthalu.ai.data.repository

import android.content.Context
import com.manthalu.ai.data.local.AppDatabase
import com.manthalu.ai.data.local.TaskEntity
import com.manthalu.ai.data.model.TaskItem
import com.manthalu.ai.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class TaskRepository(context: Context) {
    private val dao = AppDatabase.get(context).taskDao()

    val allTasks: Flow<List<TaskItem>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun add(title: String, priority: Int = 3): TaskItem {
        val now = System.currentTimeMillis()
        val entity = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            status = "todo",
            priority = priority.coerceIn(1, 5),
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun toggleDone(task: TaskItem) {
        val now = System.currentTimeMillis()
        val newStatus = if (task.status == TaskStatus.DONE) "todo" else "done"
        dao.upsert(
            TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                status = newStatus,
                priority = task.priority,
                dueAt = task.dueAt?.toEpochMilli(),
                completedAt = if (newStatus == "done") now else null,
                createdAt = task.createdAt.toEpochMilli(),
                updatedAt = now
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)

    private fun TaskEntity.toDomain() = TaskItem(
        id = id,
        title = title,
        description = description,
        status = when (status) {
            "done" -> TaskStatus.DONE
            "in_progress" -> TaskStatus.IN_PROGRESS
            "cancelled" -> TaskStatus.CANCELLED
            else -> TaskStatus.TODO
        },
        priority = priority,
        dueAt = dueAt?.let { Instant.ofEpochMilli(it) },
        completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
        createdAt = Instant.ofEpochMilli(createdAt)
    )
}
