package com.manthalu.ai.features.reflection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manthalu.ai.ui.components.AccentIcon
import com.manthalu.ai.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionScreen(onStartChat: () -> Unit = {}) {
    var note by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Personal Reflection") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("A private space for thought and clarity. Journal content is sensitive — only store what you choose in Memory.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = note, onValueChange = { note = it },
                            label = { Text("What's on your mind?") },
                            modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6
                        )
                        Button(onClick = onStartChat, modifier = Modifier.fillMaxWidth()) {
                            Text("Reflect with Manthalu")
                        }
                    }
                }
            }
            item { SectionTitle("Guided prompts") }
            item { ReflectCard(Icons.Default.EditNote, "Free journal", "Write freely, then ask Manthalu to help organise thoughts") { onStartChat() } }
            item { ReflectCard(Icons.Default.Favorite, "Gratitude", "List three things you're grateful for today") { onStartChat() } }
            item { ReflectCard(Icons.Default.Psychology, "Decision framework", "Clarify options, values and next steps") { onStartChat() } }
            item { ReflectCard(Icons.Default.Spa, "Stress check-in", "Name the feeling and explore gentle next actions") { onStartChat() } }
            item {
                Text("Manthalu does not diagnose mental health conditions. For crisis support, contact local emergency or professional services.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReflectCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
