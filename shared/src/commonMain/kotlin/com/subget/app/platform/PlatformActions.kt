package com.subget.app.platform

interface PlatformActions {
    fun openFile(filePathOrUri: String, fileName: String)
    fun openFolder(filePathOrUri: String)
    fun shareFile(filePathOrUri: String, fileName: String)
    fun openUrl(url: String)
}
