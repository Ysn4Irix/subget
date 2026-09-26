package com.subget.app

import android.app.Application
import com.subget.app.data.api.SubdlApiService
import com.subget.app.data.repository.AndroidSettingsRepository
import com.subget.app.data.repository.PosterRepository
import com.subget.app.data.repository.SettingsRepository
import com.subget.app.data.repository.SubtitleRepository
import com.subget.app.data.storage.AndroidSubtitleStorageManager
import com.subget.app.data.storage.SubtitleStorageManager
import com.subget.app.platform.AndroidPlatformActions
import com.subget.app.platform.PlatformActions
import com.subget.app.ui.AppDependencies

class SubgetApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var subtitleRepository: SubtitleRepository
        private set

    lateinit var storageManager: SubtitleStorageManager
        private set

    lateinit var posterRepository: PosterRepository
        private set

    lateinit var platformActions: PlatformActions
        private set

    lateinit var appDependencies: AppDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = AndroidSettingsRepository(this)
        val apiService = SubdlApiService()
        subtitleRepository = SubtitleRepository(apiService, settingsRepository)
        val androidStorage = AndroidSubtitleStorageManager(this)
        storageManager = androidStorage
        platformActions = AndroidPlatformActions(this)
        posterRepository = PosterRepository()

        appDependencies = AppDependencies(
            subtitleRepository = subtitleRepository,
            settingsRepository = settingsRepository,
            storageManager = storageManager,
            posterRepository = posterRepository,
            platformActions = platformActions
        )
    }

    companion object {
        lateinit var instance: SubgetApplication
            private set
    }
}
