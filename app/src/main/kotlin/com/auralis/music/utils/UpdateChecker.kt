package com.auralis.music.utils

import com.auralis.music.BuildConfig

/**
 * Data class holding update information
 */
data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val forceUpdate: Boolean,
    val message: String,
    val updateUrl: String
)

/**
 * Utility class for checking app updates using Remote Config
 */
object UpdateChecker {
    
    /**
     * Check if an update is available by comparing current version with remote config
     * @return UpdateInfo containing update status and details
     */
    fun checkForUpdate(): UpdateInfo {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val latestVersionCode = RemoteConfigManager.getLatestVersionCode()
        val forceUpdate = RemoteConfigManager.isForceUpdate()
        val message = RemoteConfigManager.getUpdateMessage()
        val updateUrl = RemoteConfigManager.getUpdateUrl()
        
        val isUpdateAvailable = latestVersionCode > currentVersionCode
        
        return UpdateInfo(
            isUpdateAvailable = isUpdateAvailable,
            forceUpdate = forceUpdate,
            message = message,
            updateUrl = updateUrl
        )
    }
}
