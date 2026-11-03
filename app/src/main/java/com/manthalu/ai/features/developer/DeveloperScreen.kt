package com.manthalu.ai.features.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manthalu.ai.ui.components.AccentIcon
import com.manthalu.ai.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(onStartChat: () -> Unit = {}) {
    var prompt by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Developer Workspace") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Code, architecture, debugging and reviews", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = prompt, onValueChange = { prompt = it },
                            label = { Text("What do you need?") },
                            placeholder = { Text("e.g. Review this Kotlin function for null safety…") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
                        )
                        Button(onClick = onStartChat, modifier = Modifier.fillMaxWidth(), enabled = prompt.isNotBlank()) {
                            Text("Open Developer chat")
                        }
                    }
                }
            }
            item { SectionTitle("Quick actions") }
            item { DevCard(Icons.Default.Code, "Explain code", "Paste code in Chat (Developer mode) for a clear walkthrough") { onStartChat() } }
            item { DevCard(Icons.Default.BugReport, "Debug help", "Describe the bug and stack trace for systematic debugging") { onStartChat() } }
            item { DevCard(Icons.Default.Architecture, "Architecture", "Discuss system design, modules and trade-offs") { onStartChat() } }
            item { DevCard(Icons.Default.Security, "Security review", "Ask for threat modeling or secure coding checks") { onStartChat() } }
            item {
                Text("Store project decisions as memories (e.g. “Using Room + Compose for Manthalu”) so future chats stay consistent.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DevCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(icon, size = 40)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
