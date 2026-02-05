// Private Test Build  Not for Redistribution

package com.auralis.music.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.auralis.music.constants.AutoBackupEnabledKey
import com.auralis.music.constants.AutoBackupTimeKey
import com.auralis.music.utils.BackupScheduler
import com.auralis.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Broadcast receiver that reschedules auto backup work after device boot
 * This ensures that the backup schedule persists across device restarts
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            
            Timber.tag("BootReceiver").i("Device boot or app update detected, checking backup schedule")
            
            // Use coroutine scope to handle async datastore operations
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val preferences = context.dataStore.data.first()
                    val autoBackupEnabled = preferences[AutoBackupEnabledKey] ?: false
                    val autoBackupTime = preferences[AutoBackupTimeKey] ?: "12:00"
                    
                    if (autoBackupEnabled) {
                        Timber.tag("BootReceiver").i("Rescheduling backup at $autoBackupTime")
                        BackupScheduler.scheduleBackup(context, autoBackupTime)
                        
                        // Log current status for debugging
                        val status = BackupScheduler.getBackupStatus(context)
                        Timber.tag("BootReceiver").i("Backup rescheduled - Status: $status")
                    } else {
                        Timber.tag("BootReceiver").i("Auto backup is disabled, no scheduling needed")
                    }
                } catch (e: Exception) {
                    Timber.tag("BootReceiver").e(e, "Failed to reschedule backup after boot")
                }
            }
        }
    }
}