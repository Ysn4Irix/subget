package com.subget.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _preferredLanguagesFlow = MutableStateFlow(getPreferredLanguages())
    val preferredLanguagesFlow: StateFlow<String> = _preferredLanguagesFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _recentSearchesFlow = MutableStateFlow(getRecentSearches())
    val recentSearchesFlow: StateFlow<List<String>> = _recentSearchesFlow.asStateFlow()

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeModeFlow.value = mode
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(key: String) {
        val cleanKey = key.trim()
        prefs.edit().putString(KEY_API_KEY, cleanKey).apply()
        _apiKeyFlow.value = cleanKey
    }

    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    fun getPreferredLanguages(): String {
        return prefs.getString(KEY_LANGUAGES, "en") ?: "en"
    }

    fun setPreferredLanguages(languages: String) {
        val clean = languages.trim()
        prefs.edit().putString(KEY_LANGUAGES, clean).apply()
        _preferredLanguagesFlow.value = clean
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val updated = current.take(8)
        try {
            val jsonStr = Json.encodeToString(updated)
            prefs.edit().putString(KEY_RECENT_SEARCHES, jsonStr).apply()
            _recentSearchesFlow.value = updated
        } catch (e: Exception) {
            Log.e("SettingsRepository", "Failed to save recent search", e)
        }
    }

    fun removeRecentSearch(query: String) {
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        try {
            val jsonStr = Json.encodeToString(current)
            prefs.edit().putString(KEY_RECENT_SEARCHES, jsonStr).apply()
            _recentSearchesFlow.value = current
        } catch (e: Exception) {
            Log.e("SettingsRepository", "Failed to remove recent search", e)
        }
    }

    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
        _recentSearchesFlow.value = emptyList()
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w("SettingsRepository", "EncryptedSharedPreferences failed, falling back to standard prefs", e)
            context.getSharedPreferences(PREFS_FALLBACK_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_FILE_NAME = "subget_secure_prefs"
        private const val PREFS_FALLBACK_FILE_NAME = "subget_standard_prefs"
        private const val KEY_API_KEY = "subdl_api_key"
        private const val KEY_LANGUAGES = "subdl_languages"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_THEME_MODE = "subget_theme_mode"
        private const val KEY_RECENT_SEARCHES = "subget_recent_searches"

        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
