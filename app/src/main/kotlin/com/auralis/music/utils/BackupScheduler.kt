// Private Test Build  Not for Redistribution

package com.auralis.music.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.auralis.music.workers.AutoBackupWorker
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val WORK_NAME = "auto_backup_work"

    /**
     * Schedule daily backup at the specified time
     * @param context Application context
     * @param backupTime Time in "HH:mm" format (24-hour)
     */
    fun scheduleBackup(context: Context, backupTime: String) {
        try {
            val (hour, minute) = backupTime.split(":").map { it.toInt() }
            val now = LocalDateTime.now()
            val scheduledTime = LocalTime.of(hour, minute)
            
            // Calculate delay until next backup time
            var nextBackup = now.with(scheduledTime)
            if (nextBackup.isBefore(now) || nextBackup.isEqual(now)) {
                // If the time has passed today, schedule for tomorrow
                nextBackup = nextBackup.plusDays(1)
            }
            
            val initialDelayMinutes = Duration.between(now, nextBackup).toMinutes()
            
            // Ensure minimum delay of 15 minutes for WorkManager
            val actualDelay = maxOf(initialDelayMinutes, 15)
            
            Timber.tag("BackupScheduler").i("Scheduling backup at $backupTime")
            Timber.tag("BackupScheduler").i("Current time: $now")
            Timber.tag("BackupScheduler").i("Next backup: $nextBackup")
            Timber.tag("BackupScheduler").i("Delay: $actualDelay minutes")

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)  // Allow backup even when battery is low
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)  // Don't require network
                .setRequiresDeviceIdle(false)  // Allow backup while device is in use
                .setRequiresCharging(false)  // Don't require charging
                .build()

            // Cancel any existing work first
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

            val backupWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(actualDelay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupWorkRequest
            )
            
            Timber.tag("BackupScheduler").i("Backup scheduled successfully with $actualDelay min delay")
        } catch (e: Exception) {
            Timber.tag("BackupScheduler").e(e, "Failed to schedule backup")
        }
    }

    /**
     * Cancel scheduled backup
     */
    fun cancelBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Timber.tag("BackupScheduler").i("Backup schedule cancelled")
    }

    /**
     * Check if backup is scheduled
     */
    fun isBackupScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .get()
        return workInfos.isNotEmpty() && workInfos.any { !it.state.isFinished }
    }

    /**
     * Get backup schedule status for debugging
     */
    fun getBackupStatus(context: Context): String {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            
            if (workInfos.isEmpty()) {
                "No backup scheduled"
            } else {
                val workInfo = workInfos.first()
                "Backup status: ${workInfo.state}, Next run attempt: ${workInfo.nextScheduleTimeMillis}"
            }
        } catch (e: Exception) {
            "Error getting backup status: ${e.message}"
        }
    }
}
