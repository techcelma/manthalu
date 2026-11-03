package com.manthalu.ai.features.chat

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.speech.RecognizerIntent
import android.content.Intent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.manthalu.ai.core.VoiceHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manthalu.ai.data.local.SecureConfigStore
import com.manthalu.ai.data.model.AiMode
import com.manthalu.ai.data.model.Message
import com.manthalu.ai.data.model.MessageRole
import com.manthalu.ai.data.repository.ChatRepository
import com.manthalu.ai.ui.components.ChatBubble
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationIdArg: String? = null,
    onOpenConversation: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember { ChatRepository(context) }
    val config = remember { SecureConfigStore(context) }

    val messages = remember { mutableStateListOf<Message>() }
    var conversationId by remember { mutableStateOf<String?>(conversationIdArg) }
    var input by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(AiMode.GENERAL) }
    var showModeMenu by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input = if (input.isBlank()) spoken else "$input $spoken"
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                speechLauncher.launch(VoiceHelper.createSpeechIntent())
            } catch (e: Exception) {
                Toast.makeText(context, "Speech unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
        }
    }

    // Load or create conversation
    LaunchedEffect(conversationIdArg) {
        if (conversationIdArg != null) {
            conversationId = conversationIdArg
            messages.clear()
            repo.messagesFor(conversationIdArg).collectLatest { list ->
                messages.clear()
                messages.addAll(list)
            }
        } else if (conversationId == null) {
            conversationId = repo.createConversation(selectedMode)
        }
        if (!config.hasGeminiKey()) {
            statusMessage = "No Gemini key yet. Open Settings → Personal API Keys and save your key."
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun startNewChat() {
        scope.launch {
            messages.clear()
            input = ""
            isGenerating = false
            conversationId = repo.createConversation(selectedMode)
            statusMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manthalu", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = selectedMode.displayName + if (config.hasGeminiKey()) " · Live" else " · Key needed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showModeMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mode")
                        }
                        DropdownMenu(
                            expanded = showModeMenu,
                            onDismissRequest = { showModeMenu = false }
                        ) {
                            AiMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    onClick = {
                                        selectedMode = mode
                                        showModeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { startNewChat() }) {
                        Icon(Icons.Default.Add, contentDescription = "New chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(AiMode.GENERAL, AiMode.STUDY, AiMode.DEVELOPER, AiMode.REFLECTION).forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            if (statusMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start a conversation", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (config.hasGeminiKey())
                                "Your Gemini key is configured. Messages and memories are saved on this device."
                            else
                                "Save your Gemini API key in Settings to get real answers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            content = msg.content,
                            isUser = msg.role == MessageRole.USER,
                            isStreaming = msg.isStreaming,
                            error = msg.error
                        )
                    }
                }
            }

            Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            val permission = Manifest.permission.RECORD_AUDIO
                            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(permission)
                            } else {
                                try {
                                    speechLauncher.launch(VoiceHelper.createSpeechIntent())
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Speech unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isGenerating
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice input")
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Manthalu…") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        maxLines = 5,
                        enabled = !isGenerating
                    )

                    Spacer(Modifier.width(8.dp))

                    if (isGenerating) {
                        IconButton(onClick = { isGenerating = false }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (input.isBlank() || conversationId == null) return@IconButton
                                if (!config.hasGeminiKey()) {
                                    statusMessage = "No Gemini key. Open Settings → Personal API Keys."
                                    Toast.makeText(context, "Add your Gemini key in Settings first", Toast.LENGTH_LONG).show()
                                    return@IconButton
                                }

                                val userText = input.trim()
                                input = ""
                                statusMessage = null

                                val userMsg = Message(
                                    conversationId = conversationId!!,
                                    role = MessageRole.USER,
                                    content = userText
                                )
                                messages.add(userMsg)

                                val assistantId = java.util.UUID.randomUUID().toString()
                                messages.add(
                                    Message(
                                        id = assistantId,
                                        conversationId = conversationId!!,
                                        role = MessageRole.ASSISTANT,
                                        content = "",
                                        isStreaming = true
                                    )
                                )
                                isGenerating = true

                                scope.launch {
                                    val result = repo.sendMessage(conversationId!!, userText, selectedMode)
                                    val idx = messages.indexOfFirst { it.id == assistantId }
                                    if (idx >= 0) {
                                        result.fold(
                                            onSuccess = { reply ->
                                                messages[idx] = messages[idx].copy(
                                                    content = reply,
                                                    isStreaming = false
                                                )
                                            },
                                            onFailure = { err ->
                                                messages[idx] = messages[idx].copy(
                                                    content = "",
                                                    isStreaming = false,
                                                    error = err.message ?: "Request failed"
                                                )
                                                statusMessage = err.message
                                            }
                                        )
                                    }
                                    isGenerating = false
                                }
                            },
                            enabled = input.isNotBlank() && !isGenerating
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
