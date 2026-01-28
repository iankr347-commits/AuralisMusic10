// Private Test Build  Not for Redistribution

package com.auralis.music.workers

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.auralis.music.constants.AutoBackupEnabledKey
import com.auralis.music.constants.LastAutoBackupKey
import com.auralis.music.utils.dataStore
import com.auralis.music.db.MusicDatabase
import com.auralis.music.extensions.div
import com.auralis.music.extensions.zipOutputStream
import com.auralis.music.viewmodels.BackupRestoreViewModel.Companion.SETTINGS_FILENAME
import com.auralis.music.db.InternalDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: MusicDatabase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.tag("AutoBackup").i("Starting automatic backup")

            // Check if auto-backup is still enabled
            val preferences = applicationContext.dataStore.data.first()
            val isEnabled = preferences[AutoBackupEnabledKey] ?: false
            
            if (!isEnabled) {
                Timber.tag("AutoBackup").i("Auto-backup is disabled, skipping")
                return@withContext Result.success()
            }

            // Create backup directory
            val backupDir = File(applicationContext.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "auralis_backup_$timestamp.zip")

            // Perform backup
            backupFile.outputStream().buffered().zipOutputStream().use { outputStream ->
                // Backup settings
                (applicationContext.filesDir / "datastore" / SETTINGS_FILENAME).inputStream().buffered()
                    .use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                        inputStream.copyTo(outputStream)
                    }

                // Backup database
                runBlocking(Dispatchers.IO) {
                    database.checkpoint()
                }
                FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                    outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                    inputStream.copyTo(outputStream)
                }
            }

            // Update last backup timestamp
            applicationContext.dataStore.edit { settings ->
                settings[LastAutoBackupKey] = System.currentTimeMillis()
            }

            // Clean up old backups (keep last 7)
            cleanupOldBackups(backupDir, keepCount = 7)

            Timber.tag("AutoBackup").i("Automatic backup completed successfully: ${backupFile.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Timber.tag("AutoBackup").e(e, "Automatic backup failed")
            Result.failure()
        }
    }

    private fun cleanupOldBackups(backupDir: File, keepCount: Int) {
        try {
            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith("auralis_backup_") && file.name.endsWith(".zip")
            }?.sortedByDescending { it.lastModified() } ?: return

            if (backupFiles.size > keepCount) {
                backupFiles.drop(keepCount).forEach { file ->
                    if (file.delete()) {
                        Timber.tag("AutoBackup").i("Deleted old backup: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("AutoBackup").e(e, "Failed to cleanup old backups")
        }
    }
}
