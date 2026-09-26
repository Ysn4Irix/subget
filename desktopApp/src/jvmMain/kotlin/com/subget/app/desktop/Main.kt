package com.subget.app.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.subget.app.data.api.SubdlApiService
import com.subget.app.data.repository.DesktopSettingsRepository
import com.subget.app.data.repository.PosterRepository
import com.subget.app.data.repository.SubtitleRepository
import com.subget.app.data.storage.DesktopSubtitleStorageManager
import com.subget.app.desktop.components.CinemaModernTitleBar
import com.subget.app.platform.DesktopPlatformActions
import com.subget.app.ui.AppDependencies
import com.subget.app.ui.SubgetApp
import com.subget.app.ui.theme.SubgetTheme
import okhttp3.OkHttpClient
import org.jetbrains.compose.resources.painterResource
import subget.shared.generated.resources.Res
import subget.shared.generated.resources.brand_logo
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(
        width = 1100.dp,
        height = 760.dp,
        position = WindowPosition.PlatformDefault
    )

    // Instantiate Desktop Multiplatform Dependencies
    val okHttpClient = remember { OkHttpClient() }
    val apiService = remember { SubdlApiService(okHttpClient) }
    val settingsRepository = remember { DesktopSettingsRepository() }
    val storageManager = remember { DesktopSubtitleStorageManager() }
    val platformActions = remember { DesktopPlatformActions() }
    val subtitleRepository = remember { SubtitleRepository(apiService, settingsRepository) }
    val posterRepository = remember { PosterRepository(okHttpClient) }

    val appDependencies = remember {
        AppDependencies(
            subtitleRepository = subtitleRepository,
            settingsRepository = settingsRepository,
            storageManager = storageManager,
            posterRepository = posterRepository,
            platformActions = platformActions
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Subget - Subtitle Downloader",
        icon = painterResource(Res.drawable.brand_logo),
        undecorated = true,
        transparent = false
    ) {
        window.minimumSize = Dimension(800, 600)

        SubgetTheme(darkTheme = true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0E14))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CinemaModernTitleBar(
                        windowState = windowState,
                        onCloseRequest = ::exitApplication
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        SubgetApp(dependencies = appDependencies)
                    }
                }
            }
        }
    }
}
