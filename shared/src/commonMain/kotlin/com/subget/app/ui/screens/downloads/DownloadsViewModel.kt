package com.subget.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subget.app.data.storage.DownloadedSubtitle
import com.subget.app.data.storage.SubtitleStorageManager
import com.subget.app.platform.PlatformActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val isLoading: Boolean = false,
    val downloads: List<DownloadedSubtitle> = emptyList(),
    val snackbarMessage: String? = null
)

class DownloadsViewModel(
    private val storageManager: SubtitleStorageManager,
    private val platformActions: PlatformActions
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val list = storageManager.getDownloadedSubtitles()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                downloads = list
            )
        }
    }

    fun deleteDownload(item: DownloadedSubtitle) {
        viewModelScope.launch {
            val success = storageManager.deleteSubtitle(item)
            if (success) {
                val updated = _uiState.value.downloads.filter { it.fileName != item.fileName }
                _uiState.value = _uiState.value.copy(
                    downloads = updated,
                    snackbarMessage = "Deleted ${item.fileName}"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Could not delete ${item.fileName}"
                )
            }
        }
    }

    fun openInPlayer(item: DownloadedSubtitle) {
        platformActions.openFile(item.id, item.fileName)
    }

    fun openFolder(item: DownloadedSubtitle) {
        platformActions.openFolder(item.id)
    }

    fun shareDownload(item: DownloadedSubtitle) {
        platformActions.shareFile(item.id, item.fileName)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    class Factory(
        private val storageManager: SubtitleStorageManager,
        private val platformActions: PlatformActions
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
            return DownloadsViewModel(storageManager, platformActions) as T
        }
    }
}
