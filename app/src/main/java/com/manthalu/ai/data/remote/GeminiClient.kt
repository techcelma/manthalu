package com.manthalu.ai.data.remote

import com.manthalu.ai.data.local.SecureConfigStore
import com.manthalu.ai.data.model.AiMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls Gemini directly from the device using the API key stored in SecureConfigStore.
 * Intended for personal use only.
 */
class GeminiClient(
    private val config: SecureConfigStore,
    private val http: OkHttpClient = defaultClient()
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun generate(
        userMessage: String,
        mode: AiMode = AiMode.GENERAL,
        conversationHistory: List<Pair<String, String>> = emptyList(), // role to text
        relevantMemories: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = config.geminiApiKey
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API key. Open Settings → Personal API Keys and save your key.")
            )
        }

        val model = config.geminiModelChat.ifBlank { "gemini-2.0-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val systemInstruction = systemPromptFor(mode, relevantMemories)

        val contents = JSONArray()
        // Simple history (last few turns)
        conversationHistory.takeLast(8).forEach { (role, text) ->
            contents.put(
                JSONObject()
                    .put("role", if (role == "user") "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        // Current user message
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        )

        val body = JSONObject()
            .put("system_instruction", JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            )
            .put("contents", contents)
            .put("generationConfig", JSONObject()
                .put("maxOutputTokens", 4096)
                .put("temperature", 0.7)
            )

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(raw).optJSONObject("error")?.optString("message")
                            ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $raw"
                    }
                    return@withContext Result.failure(RuntimeException(errMsg))
                }

                val json = JSONObject(raw)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(RuntimeException("Empty response from Gemini"))
                }
                val text = candidates
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun systemPromptFor(mode: AiMode, memories: List<String> = emptyList()): String {
        val base = """
            You are Manthalu, a private personal AI assistant for one user.
            Be precise, helpful, and honest. Never invent personal facts about the user.
            If you do not know something, say so clearly.
            Do not claim to have saved a memory or created a reminder unless the system has actually done so.
        """.trimIndent()

        val modeExtra = when (mode) {
            AiMode.STUDY -> "You are in Study mode. Teach from first principles. Prefer clear explanations, examples, and active-recall style questions. Do not fabricate citations."
            AiMode.DEVELOPER -> "You are in Developer mode. Focus on correct, secure, maintainable code. Prefer Kotlin, TypeScript, and modern Android practices when relevant."
            AiMode.REFLECTION -> "You are in Personal Reflection mode. Use empathetic, non-judgmental language. Do not diagnose mental health conditions. Encourage professional help when appropriate."
            AiMode.PRODUCTIVITY -> "You are in Productivity mode. Help with tasks, prioritization, and planning. Never claim a reminder was created unless the system actually did so."
            AiMode.RESEARCH -> "You are in Research mode. Distinguish clearly between evidence and interpretation. Cite sources when you have them."
            AiMode.GENERAL -> ""
        }

        val memoryBlock = if (memories.isNotEmpty()) {
            buildString {
                appendLine()
                appendLine("Approved memories about the user (use only when relevant, do not force them into every answer):")
                memories.take(12).forEach { appendLine("- $it") }
            }
        } else ""

        return listOf(base, modeExtra, memoryBlock).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
