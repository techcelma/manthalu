package com.manthalu.ai.features.knowledge

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manthalu.ai.data.local.DocumentStore
import com.manthalu.ai.data.local.LocalDocument
import com.manthalu.ai.ui.components.AccentIcon
import com.manthalu.ai.ui.components.EmptyState
import com.manthalu.ai.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen() {
    val context = LocalContext.current
    val store = remember { DocumentStore(context) }
    var docs by remember { mutableStateOf(store.list()) }
    var selected by remember { mutableStateOf<LocalDocument?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { /* some providers don't support persistable */ }
        val result = store.importFromUri(uri)
        result.fold(
            onSuccess = {
                docs = store.list()
                Toast.makeText(context, "Imported: ${it.name}", Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Knowledge Library")
                    Text("${docs.size} documents on this device",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    picker.launch(arrayOf(
                        "text/*",
                        "application/pdf",
                        "application/json",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "*/*"
                    ))
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import document")
            }
        }
    ) { padding ->
        if (docs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                EmptyState(
                    "No documents yet",
                    "Tap + to import TXT, Markdown, code, or other text files. PDF is accepted but full text extraction is limited without a PDF library — paste key sections into Chat when needed."
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SectionTitle("Your documents") }
                items(docs, key = { it.id }) { doc ->
                    Card(
                        onClick = { selected = doc },
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AccentIcon(Icons.Default.Description, size = 36)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(doc.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${doc.sizeBytes / 1024} KB · ${doc.textPreview.take(60)}…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                store.delete(doc.id)
                                docs = store.list()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { doc ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(doc.name) },
            text = {
                Text(
                    doc.fullText.take(4000) + if (doc.fullText.length > 4000) "\n\n…(truncated)" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Copy hint: user can paste into Chat
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("doc", doc.fullText.take(8000))
                    )
                    Toast.makeText(context, "Text copied — paste into Chat", Toast.LENGTH_SHORT).show()
                    selected = null
                }) { Text("Copy text") }
            }
        )
    }
}
