package com.manthalu.ai.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manthalu.ai.data.local.SecureConfigStore
import com.manthalu.ai.data.repository.ChatRepository
import com.manthalu.ai.data.repository.MemoryRepository
import com.manthalu.ai.data.repository.TaskRepository
import com.manthalu.ai.ui.components.EmptyState
import com.manthalu.ai.ui.components.SectionTitle
import com.manthalu.ai.ui.theme.AccentEnd
import com.manthalu.ai.ui.theme.AccentStart
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToKnowledge: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val config = remember { SecureConfigStore(context) }
    val chatRepo = remember { ChatRepository(context) }
    val memoryRepo = remember { MemoryRepository(context) }
    val taskRepo = remember { TaskRepository(context) }

    val conversations by chatRepo.conversations.collectAsState(initial = emptyList())
    val memories by memoryRepo.allMemories.collectAsState(initial = emptyList())
    val tasks by taskRepo.allTasks.collectAsState(initial = emptyList())

    var quickInput by remember { mutableStateOf("") }
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val activeTasks = tasks.count { it.status.name != "DONE" }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Manthalu", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    if (config.hasGeminiKey()) {
                        Text("Gemini connected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Add Gemini key in Settings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = quickInput, onValueChange = { quickInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ask Manthalu anything…") },
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { IconButton(onClick = {}) { Icon(Icons.Default.Mic, null) } },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onNavigateToChat, modifier = Modifier.align(Alignment.End)) {
                        Text("Open full chat")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            SectionTitle("Quick actions")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { QA("Chat", Icons.Default.AutoAwesome, onNavigateToChat) }
                item { QA("Study", Icons.Default.Book, onNavigateToChat) }
                item { QA("Code", Icons.Default.Code, onNavigateToChat) }
                item { QA("Reflect", Icons.Default.SelfImprovement, onNavigateToChat) }
                item { QA("Docs", Icons.Default.UploadFile, onNavigateToKnowledge) }
                item { QA("Tasks", Icons.Default.TaskAlt, onNavigateToTasks) }
                item { QA("Memory", Icons.Default.Memory, onNavigateToMemory) }
                item { QA("History", Icons.Default.History, onNavigateToHistory) }
            }
        }

        item {
            SectionTitle("At a glance", action = {})
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Chats", conversations.size.toString(), Modifier.weight(1f), onNavigateToHistory)
                StatCard("Memories", memories.size.toString(), Modifier.weight(1f), onNavigateToMemory)
                StatCard("Tasks", activeTasks.toString(), Modifier.weight(1f), onNavigateToTasks)
            }
        }

        item {
            SectionTitle("Today’s priorities", action = { TextButton(onClick = onNavigateToTasks) { Text("See all") } })
            if (tasks.none { it.status.name != "DONE" }) {
                EmptyState("No open tasks", "Add a task to track priorities.")
            } else {
                tasks.filter { it.status.name != "DONE" }.take(3).forEach { t ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(t.title, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            SectionTitle("Memory", action = { TextButton(onClick = onNavigateToMemory) { Text("Open") } })
            if (memories.isEmpty()) {
                EmptyState("Your memory is under your control", "Manthalu only stores facts you approve.")
            } else {
                memories.take(2).forEach { m ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(m.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(m.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QA(label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(AccentStart, AccentEnd))), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
