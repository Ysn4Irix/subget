package com.subget.app.platform

import com.subget.app.util.AppLogger
import java.awt.Desktop
import java.io.File
import java.net.URI

class DesktopPlatformActions : PlatformActions {

    override fun openFile(filePathOrUri: String, fileName: String) {
        try {
            val file = File(filePathOrUri)
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            }
        } catch (e: Exception) {
            AppLogger.e("DesktopPlatformActions", "Failed to open file $filePathOrUri", e)
        }
    }

    override fun openFolder(filePathOrUri: String) {
        try {
            val file = File(filePathOrUri)
            val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
            if (isWindows && file.exists()) {
                ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
            } else if (file.parentFile != null && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.parentFile)
            }
        } catch (e: Exception) {
            AppLogger.e("DesktopPlatformActions", "Failed to reveal file/folder in Explorer", e)
        }
    }

    override fun shareFile(filePathOrUri: String, fileName: String) {
        // On desktop, reveal the file in the system file manager
        openFolder(filePathOrUri)
    }

    override fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            AppLogger.e("DesktopPlatformActions", "Failed to open URL $url", e)
        }
    }
}
