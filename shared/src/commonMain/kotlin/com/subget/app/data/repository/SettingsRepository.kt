package com.subget.app.data.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val apiKeyFlow: StateFlow<String>
    val preferredLanguagesFlow: StateFlow<String>
    val themeModeFlow: StateFlow<String>
    val recentSearchesFlow: StateFlow<List<String>>

    fun getThemeMode(): String
    fun setThemeMode(mode: String)
    fun getApiKey(): String
    fun setApiKey(key: String)
    fun hasApiKey(): Boolean
    fun getPreferredLanguages(): String
    fun setPreferredLanguages(languages: String)
    fun isFirstLaunch(): Boolean
    fun setFirstLaunchComplete()
    fun getRecentSearches(): List<String>
    fun addRecentSearch(query: String)
    fun removeRecentSearch(query: String)
    fun clearRecentSearches()

    companion object {
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"
    }
}
