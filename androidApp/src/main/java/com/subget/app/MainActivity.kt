package com.subget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.subget.app.data.repository.SettingsRepository
import com.subget.app.ui.SubgetApp
import com.subget.app.ui.theme.SubgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = SubgetApplication.instance
            val themeMode by app.settingsRepository.themeModeFlow.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDark = when (themeMode) {
                SettingsRepository.THEME_DARK -> true
                SettingsRepository.THEME_SYSTEM -> systemDark
                else -> false
            }

            SubgetTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SubgetApp(dependencies = app.appDependencies)
                }
            }
        }
    }
}
