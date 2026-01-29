package com.auralis.music.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    private const val KEY_MAINTENANCE_MODE = "maintenance_mode"
    private const val KEY_MAINTENANCE_TEXT = "maintenance_text"
    
    // Default values
    private const val DEFAULT_LATEST_VERSION_CODE = 0L
    private const val DEFAULT_FORCE_UPDATE = false
    private const val DEFAULT_UPDATE_MESSAGE = "A new version is available. Please update to continue."
    private const val DEFAULT_UPDATE_URL = ""
    private const val DEFAULT_MAINTENANCE_MODE = false
    private const val DEFAULT_MAINTENANCE_TEXT = "Soon"

    // Exposed state for Compose/UI to observe maintenance mode
    private val _maintenanceMode = MutableStateFlow(DEFAULT_MAINTENANCE_MODE)
    val maintenanceModeFlow: StateFlow<Boolean> get() = _maintenanceMode

    private val _maintenanceText = MutableStateFlow(DEFAULT_MAINTENANCE_TEXT)
    val maintenanceTextFlow: StateFlow<String> get() = _maintenanceText
    
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
            val activated = remoteConfig.fetchAndActivate().await()
            // Update maintenance state after fetch
            try {
                _maintenanceMode.value = remoteConfig.getBoolean(KEY_MAINTENANCE_MODE)
                val text = remoteConfig.getString(KEY_MAINTENANCE_TEXT)
                _maintenanceText.value = if (text.isEmpty()) DEFAULT_MAINTENANCE_TEXT else text
            } catch (e: Exception) {
                e.printStackTrace()
            }
            activated
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
     * Check if maintenance mode is enabled
     */
    fun isMaintenanceMode(): Boolean {
        return try {
            remoteConfig.getBoolean(KEY_MAINTENANCE_MODE)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get maintenance message text
     */
    fun getMaintenanceText(): String {
        val message = try {
            remoteConfig.getString(KEY_MAINTENANCE_TEXT)
        } catch (e: Exception) {
            ""
        }
        return message.ifEmpty { DEFAULT_MAINTENANCE_TEXT }
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
