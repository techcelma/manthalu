package com.manthalu.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for personal API keys and connection settings.
 * This is intended for single-user, non-distributed personal use only.
 *
 * Keys never leave the device unless you explicitly send them to a
 * backend or third-party API that you have configured.
 */
class SecureConfigStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "manthalu_secure_config",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Gemini
    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value.trim()).apply()

    var geminiModelChat: String
        get() = prefs.getString(KEY_GEMINI_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash"
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value.trim()).apply()

    // Supabase
    var supabaseUrl: String
        get() = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_URL, value.trim()).apply()

    var supabaseAnonKey: String
        get() = prefs.getString(KEY_SUPABASE_ANON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_ANON, value.trim()).apply()

    // Optional self-hosted backend (if you still want one)
    var backendBaseUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value.trim().removeSuffix("/")).apply()

    // Behaviour
    var useDirectGemini: Boolean
        get() = prefs.getBoolean(KEY_USE_DIRECT, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_DIRECT, value).apply()

    var memoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEMORY_ENABLED, value).apply()

    var autoApproveLowRisk: Boolean
        get() = prefs.getBoolean(KEY_AUTO_APPROVE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_APPROVE, value).apply()

    fun hasGeminiKey(): Boolean = geminiApiKey.isNotBlank()
    fun hasSupabase(): Boolean = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    fun clearAllSecrets() {
        prefs.edit()
            .remove(KEY_GEMINI)
            .remove(KEY_SUPABASE_ANON)
            .apply()
    }

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model_chat"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON = "supabase_anon_key"
        private const val KEY_BACKEND_URL = "backend_base_url"
        private const val KEY_USE_DIRECT = "use_direct_gemini"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_AUTO_APPROVE = "auto_approve_low_risk"
    }
}
