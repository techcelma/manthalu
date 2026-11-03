package com.manthalu.ai.features.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manthalu.ai.data.model.TaskItem
import com.manthalu.ai.data.model.TaskStatus
import com.manthalu.ai.data.repository.TaskRepository
import com.manthalu.ai.ui.components.EmptyState
import com.manthalu.ai.core.ReminderHelper
import android.widget.Toast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val context = LocalContext.current
    val repo = remember { TaskRepository(context) }
    val scope = rememberCoroutineScope()
    val tasks by repo.allTasks.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Tasks & Goals")
                    Text(
                        "${tasks.count { it.status != TaskStatus.DONE }} active · saved on device",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                EmptyState("No tasks yet", "Add a task. They are stored on this device.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { repo.toggleDone(task) } }) {
                                Icon(
                                    if (task.status == TaskStatus.DONE) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (task.status == TaskStatus.DONE) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                                color = if (task.status == TaskStatus.DONE) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface)
                            TextButton(onClick = {
                                val id = task.id.hashCode() and 0x7fffffff
                                ReminderHelper.schedule(
                                    context,
                                    id,
                                    task.title,
                                    System.currentTimeMillis() + 60 * 60 * 1000L
                                )
                                Toast.makeText(context, "Reminder set for 1 hour", Toast.LENGTH_SHORT).show()
                            }) { Text("1h") }
                            TextButton(onClick = { scope.launch { repo.delete(task.id) } }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New task") },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("What needs doing?") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        scope.launch { repo.add(title.trim()) }
                        showAdd = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
