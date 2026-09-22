package com.subget.app.ui.screens.downloads

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subget.app.data.storage.DownloadedSubtitle
import com.subget.app.data.storage.SubtitleStorageManager
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
    private val storageManager: SubtitleStorageManager
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

    fun openInPlayer(item: DownloadedSubtitle, context: Context) {
        try {
            val intent = storageManager.createViewIntent(item.uri, item.fileName)
            val chooser = Intent.createChooser(intent, "Open Subtitle With")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDownload(item: DownloadedSubtitle, context: Context) {
        try {
            val intent = storageManager.createShareIntent(item.uri, item.fileName)
            val chooser = Intent.createChooser(intent, "Share Subtitle")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share file", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    class Factory(
        private val storageManager: SubtitleStorageManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DownloadsViewModel(storageManager) as T
        }
    }
}
