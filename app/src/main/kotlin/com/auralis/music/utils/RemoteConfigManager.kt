package com.auralis.music.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Singleton manager for Firebase Remote Config
 * Handles fetching and accessing remote configuration values for app updates
 */
object RemoteConfigManager {
    private lateinit var remoteConfig: FirebaseRemoteConfig
    
    // Remote Config keys
    private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    private const val KEY_FORCE_UPDATE = "force_update"
    private const val KEY_UPDATE_MESSAGE = "update_message"
    private const val KEY_UPDATE_URL = "update_url"
    
    // Default values
    private const val DEFAULT_LATEST_VERSION_CODE = 0L
    private const val DEFAULT_FORCE_UPDATE = false
    private const val DEFAULT_UPDATE_MESSAGE = "A new version is available. Please update to continue."
    private const val DEFAULT_UPDATE_URL = ""
    
    /**
     * Initialize Remote Config with settings
     * @param fetchIntervalSeconds Minimum fetch interval (use 0 for testing, 3600 for production)
     */
    fun initialize(fetchIntervalSeconds: Long = 3600) {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values as fallback
        val defaults = mapOf(
            KEY_LATEST_VERSION_CODE to DEFAULT_LATEST_VERSION_CODE,
            KEY_FORCE_UPDATE to DEFAULT_FORCE_UPDATE,
            KEY_UPDATE_MESSAGE to DEFAULT_UPDATE_MESSAGE,
            KEY_UPDATE_URL to DEFAULT_UPDATE_URL
        )
        remoteConfig.setDefaultsAsync(defaults)
    }
    
    /**
     * Fetch and activate remote config values
     * @return true if fetch and activate successful, false otherwise
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get the latest version code from Remote Config
     */
    fun getLatestVersionCode(): Int {
        return remoteConfig.getLong(KEY_LATEST_VERSION_CODE).toInt()
    }
    
    /**
     * Check if force update is enabled
     */
    fun isForceUpdate(): Boolean {
        return remoteConfig.getBoolean(KEY_FORCE_UPDATE)
    }
    
    /**
     * Get the update message to display to users
     */
    fun getUpdateMessage(): String {
        val message = remoteConfig.getString(KEY_UPDATE_MESSAGE)
        return message.ifEmpty { DEFAULT_UPDATE_MESSAGE }
    }
    
    /**
     * Get the update URL (APK or Play Store link)
     */
    fun getUpdateUrl(): String {
        return remoteConfig.getString(KEY_UPDATE_URL)
    }
}
