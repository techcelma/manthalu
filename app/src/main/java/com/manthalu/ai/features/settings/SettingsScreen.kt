package com.manthalu.ai.features.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.manthalu.ai.data.local.SecureConfigStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { SecureConfigStore(context) }

    // Load current values
    var geminiKey by remember { mutableStateOf(store.geminiApiKey) }
    var geminiModel by remember { mutableStateOf(store.geminiModelChat) }
    var supabaseUrl by remember { mutableStateOf(store.supabaseUrl) }
    var supabaseAnon by remember { mutableStateOf(store.supabaseAnonKey) }
    var backendUrl by remember { mutableStateOf(store.backendBaseUrl) }
    var useDirectGemini by remember { mutableStateOf(store.useDirectGemini) }
    var memoryEnabled by remember { mutableStateOf(store.memoryEnabled) }
    var autoApprove by remember { mutableStateOf(store.autoApproveLowRisk) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showSupabaseKey by remember { mutableStateOf(false) }
    var keysSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings & Privacy") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Personal Keys (the part you asked for) ──────────────────────
            item {
                Text(
                    "Personal API Keys",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Because this app is for your private use only, you can paste your own keys here. " +
                    "They are stored encrypted on this device and never leave it unless you make a request that needs them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Gemini", style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { geminiKey = it },
                            label = { Text("Gemini API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showGeminiKey) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        if (showGeminiKey) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        OutlinedTextField(
                            value = geminiModel,
                            onValueChange = { geminiModel = it },
                            label = { Text("Preferred model (e.g. gemini-2.0-flash)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Call Gemini directly from the app", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Recommended for personal use. No backend required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useDirectGemini,
                                onCheckedChange = { useDirectGemini = it }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Supabase (optional but recommended)", style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = supabaseUrl,
                            onValueChange = { supabaseUrl = it },
                            label = { Text("Supabase Project URL") },
                            placeholder = { Text("https://xxxx.supabase.co") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = supabaseAnon,
                            onValueChange = { supabaseAnon = it },
                            label = { Text("Supabase Anon / Public Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showSupabaseKey) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSupabaseKey = !showSupabaseKey }) {
                                    Icon(
                                        if (showSupabaseKey) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Optional Backend", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Only needed if you still want a Node server. Leave blank to use direct Gemini + Supabase from the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = backendUrl,
                            onValueChange = { backendUrl = it },
                            label = { Text("Backend base URL (optional)") },
                            placeholder = { Text("https://your-server.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        store.geminiApiKey = geminiKey
                        store.geminiModelChat = geminiModel
                        store.supabaseUrl = supabaseUrl
                        store.supabaseAnonKey = supabaseAnon
                        store.backendBaseUrl = backendUrl
                        store.useDirectGemini = useDirectGemini
                        keysSaved = true
                        Toast.makeText(context, "Keys saved securely on this device", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save keys")
                }
                if (keysSaved) {
                    Text(
                        "Saved. Keys are encrypted with Android EncryptedSharedPreferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Memory behaviour ────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text("Memory behaviour", style = MaterialTheme.typography.titleMedium)
            }

            item {
                SettingsToggle(
                    title = "Enable memory",
                    subtitle = "Allow Manthalu to store facts you approve",
                    checked = memoryEnabled,
                    onCheckedChange = {
                        memoryEnabled = it
                        store.memoryEnabled = it
                    }
                )
            }

            item {
                SettingsToggle(
                    title = "Auto-approve low-risk memories",
                    subtitle = "Sensitive items still need your explicit OK",
                    checked = autoApprove,
                    onCheckedChange = {
                        autoApprove = it
                        store.autoApproveLowRisk = it
                    }
                )
            }

            // ── Data & privacy ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text("Data & privacy", style = MaterialTheme.typography.titleMedium)
            }

            item {
                SettingsNavRow("Clear stored API keys", "Removes Gemini and Supabase keys from this device") {
                    store.clearAllSecrets()
                    geminiKey = ""
                    supabaseAnon = ""
                    keysSaved = false
                    Toast.makeText(context, "Secrets cleared", Toast.LENGTH_SHORT).show()
                }
            }

            item {
                SettingsNavRow("Export my data", "Conversations, memories and tasks (coming next)")
            }

            item {
                SettingsNavRow("Privacy notes", "What is stored and which external services receive data")
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Personal use only · Keys stay on your device · Never distribute an APK that contains your real keys",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
