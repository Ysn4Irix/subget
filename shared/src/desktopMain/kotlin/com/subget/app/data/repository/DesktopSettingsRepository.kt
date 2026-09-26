package com.subget.app.data.repository

import com.subget.app.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

class DesktopSettingsRepository : SettingsRepository {

    private val prefs = Preferences.userNodeForPackage(DesktopSettingsRepository::class.java)

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    override val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _preferredLanguagesFlow = MutableStateFlow(getPreferredLanguages())
    override val preferredLanguagesFlow: StateFlow<String> = _preferredLanguagesFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    override val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _recentSearchesFlow = MutableStateFlow(getRecentSearches())
    override val recentSearchesFlow: StateFlow<List<String>> = _recentSearchesFlow.asStateFlow()

    override fun getThemeMode(): String {
        return prefs.get(KEY_THEME_MODE, SettingsRepository.THEME_DARK) // Default desktop theme to DARK
    }

    override fun setThemeMode(mode: String) {
        prefs.put(KEY_THEME_MODE, mode)
        _themeModeFlow.value = mode
    }

    override fun getApiKey(): String {
        return prefs.get(KEY_API_KEY, "") ?: ""
    }

    override fun setApiKey(key: String) {
        val cleanKey = key.trim()
        prefs.put(KEY_API_KEY, cleanKey)
        _apiKeyFlow.value = cleanKey
    }

    override fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    override fun getPreferredLanguages(): String {
        return prefs.get(KEY_LANGUAGES, "en") ?: "en"
    }

    override fun setPreferredLanguages(languages: String) {
        val clean = languages.trim()
        prefs.put(KEY_LANGUAGES, clean)
        _preferredLanguagesFlow.value = clean
    }

    override fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    override fun setFirstLaunchComplete() {
        prefs.putBoolean(KEY_FIRST_LAUNCH, false)
    }

    override fun getRecentSearches(): List<String> {
        val raw = prefs.get(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val updated = current.take(8)
        try {
            val jsonStr = Json.encodeToString(updated)
            prefs.put(KEY_RECENT_SEARCHES, jsonStr)
            _recentSearchesFlow.value = updated
        } catch (e: Exception) {
            AppLogger.e("DesktopSettingsRepository", "Failed to save recent search", e)
        }
    }

    override fun removeRecentSearch(query: String) {
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        try {
            val jsonStr = Json.encodeToString(current)
            prefs.put(KEY_RECENT_SEARCHES, jsonStr)
            _recentSearchesFlow.value = current
        } catch (e: Exception) {
            AppLogger.e("DesktopSettingsRepository", "Failed to remove recent search", e)
        }
    }

    override fun clearRecentSearches() {
        prefs.remove(KEY_RECENT_SEARCHES)
        _recentSearchesFlow.value = emptyList()
    }

    companion object {
        private const val KEY_API_KEY = "subdl_api_key"
        private const val KEY_LANGUAGES = "subdl_languages"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_THEME_MODE = "subget_theme_mode"
        private const val KEY_RECENT_SEARCHES = "subget_recent_searches"

        val instance: DesktopSettingsRepository by lazy { DesktopSettingsRepository() }
    }
}
