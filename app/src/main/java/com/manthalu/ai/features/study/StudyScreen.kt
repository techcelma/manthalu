package com.manthalu.ai.features.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manthalu.ai.ui.components.AccentIcon
import com.manthalu.ai.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(onStartChat: () -> Unit = {}) {
    var topic by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Study Center") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Learn from first principles", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = topic, onValueChange = { topic = it },
                            label = { Text("Topic or question") },
                            placeholder = { Text("e.g. Explain recursion from first principles") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Button(
                            onClick = onStartChat,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = topic.isNotBlank()
                        ) { Text("Ask Manthalu in Study mode") }
                        Text("Tip: switch to Study mode in Chat for teaching-style answers, quizzes and active recall.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionTitle("Study tools") }
            item {
                StudyToolCard(Icons.Default.School, "Explain a concept", "First-principles breakdown with examples") { onStartChat() }
            }
            item {
                StudyToolCard(Icons.Default.Quiz, "Practice quiz", "Ask for multiple-choice or short-answer questions on a topic") { onStartChat() }
            }
            item {
                StudyToolCard(Icons.Default.Style, "Flashcards", "Request a set of Q&A flashcards for active recall") { onStartChat() }
            }
            item {
                StudyToolCard(Icons.Default.Timer, "Revision plan", "Get a spaced-repetition style schedule for an exam") { onStartChat() }
            }
            item {
                Text("Upload lecture notes in Knowledge Library, then ask grounded questions in Chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StudyToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
