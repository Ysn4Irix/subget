package com.subget.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.subget.app.data.storage.AndroidSubtitleStorageManager
import com.subget.app.util.AppLogger

class AndroidPlatformActions(private val context: Context) : PlatformActions {

    override fun openFile(filePathOrUri: String, fileName: String) {
        try {
            val uri = Uri.parse(filePathOrUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, AndroidSubtitleStorageManager.getMimeType(fileName))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open Subtitle With").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            AppLogger.e("AndroidPlatformActions", "Failed to open file", e)
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun openFolder(filePathOrUri: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(filePathOrUri), "resource/folder")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open Downloads via file manager
            try {
                val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Saved to Downloads/Subget", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun shareFile(filePathOrUri: String, fileName: String) {
        try {
            val uri = Uri.parse(filePathOrUri)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = AndroidSubtitleStorageManager.getMimeType(fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Subtitle").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            AppLogger.e("AndroidPlatformActions", "Failed to share file", e)
            Toast.makeText(context, "Failed to share file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e("AndroidPlatformActions", "Failed to open URL $url", e)
        }
    }
}
