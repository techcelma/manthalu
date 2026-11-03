package com.manthalu.ai.features.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manthalu.ai.data.model.Memory
import com.manthalu.ai.data.repository.MemoryRepository
import com.manthalu.ai.ui.components.EmptyState
import com.manthalu.ai.ui.components.MemoryCard
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CATEGORIES = listOf(
    "All", "personal", "learning", "academic", "project",
    "technical", "preference", "instruction", "other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen() {
    val context = LocalContext.current
    val repo = remember { MemoryRepository(context) }
    val scope = rememberCoroutineScope()

    val allMemories by repo.allMemories.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var pendingDelete by remember { mutableStateOf<Memory?>(null) }

    val dateFmt = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())
    }

    val filtered = allMemories.filter { mem ->
        val matchesCategory = selectedCategory == "All" ||
                mem.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                mem.content.contains(searchQuery, ignoreCase = true) ||
                mem.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Memory Center")
                        Text(
                            text = "${allMemories.size} memories · saved on this device",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add memory")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search memories…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (allMemories.isEmpty()) "No memories yet" else "No matching memories",
                        subtitle = if (allMemories.isEmpty())
                            "Add facts you want Manthalu to remember, or approve ones it proposes later."
                        else
                            "Try a different category or clear the search."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { mem ->
                        MemoryCard(
                            content = mem.content,
                            category = mem.category,
                            importance = mem.importance,
                            isPinned = mem.isPinned,
                            createdAtLabel = dateFmt.format(mem.createdAt),
                            onEdit = { editingMemory = mem },
                            onDelete = { pendingDelete = mem },
                            onTogglePin = {
                                scope.launch { repo.togglePin(mem) }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Add / Edit dialog
    if (showAddDialog || editingMemory != null) {
        val isEdit = editingMemory != null
        var content by remember(editingMemory) { mutableStateOf(editingMemory?.content ?: "") }
        var category by remember(editingMemory) { mutableStateOf(editingMemory?.category ?: "other") }
        var importance by remember(editingMemory) { mutableStateOf(editingMemory?.importance ?: 3) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingMemory = null
            },
            title = { Text(if (isEdit) "Edit memory" else "Add memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Importance: $importance", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { if (importance > 1) importance-- }) { Text("−") }
                        TextButton(onClick = { if (importance < 5) importance++ }) { Text("+") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (content.isBlank()) return@TextButton
                        scope.launch {
                            if (isEdit) {
                                repo.update(
                                    editingMemory!!.copy(
                                        content = content.trim(),
                                        category = category.trim().ifBlank { "other" },
                                        importance = importance
                                    )
                                )
                            } else {
                                repo.add(
                                    content = content.trim(),
                                    category = category.trim().ifBlank { "other" },
                                    importance = importance
                                )
                            }
                            showAddDialog = false
                            editingMemory = null
                        }
                    }
                ) {
                    Text(if (isEdit) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingMemory = null
                }) { Text("Cancel") }
            }
        )
    }

    pendingDelete?.let { mem ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete memory?") },
            text = {
                Text("This permanently removes the memory from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repo.delete(mem.id)
                            pendingDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
