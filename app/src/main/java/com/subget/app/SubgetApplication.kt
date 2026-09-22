package com.subget.app

import android.app.Application
import com.subget.app.data.api.SubdlApiService
import com.subget.app.data.repository.SettingsRepository
import com.subget.app.data.repository.SubtitleRepository
import com.subget.app.data.storage.SubtitleStorageManager

class SubgetApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var subtitleRepository: SubtitleRepository
        private set

    lateinit var storageManager: SubtitleStorageManager
        private set

    lateinit var posterRepository: com.subget.app.data.repository.PosterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = SettingsRepository.getInstance(this)
        val apiService = SubdlApiService()
        subtitleRepository = SubtitleRepository(apiService, settingsRepository)
        storageManager = SubtitleStorageManager(this)
        posterRepository = com.subget.app.data.repository.PosterRepository()
    }

    companion object {
        lateinit var instance: SubgetApplication
            private set
    }
}
