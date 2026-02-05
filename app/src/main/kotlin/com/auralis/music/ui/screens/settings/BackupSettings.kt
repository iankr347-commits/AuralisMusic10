// Private Test Build  Not for Redistribution

package com.auralis.music.ui.screens.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auralis.music.LocalPlayerAwareWindowInsets
import com.auralis.music.R
import com.auralis.music.constants.AutoBackupEnabledKey
import com.auralis.music.constants.AutoBackupTimeKey
import com.auralis.music.constants.LastAutoBackupKey
import com.auralis.music.ui.component.IconButton
import com.auralis.music.ui.component.Material3SettingsGroup
import com.auralis.music.ui.component.Material3SettingsItem
import com.auralis.music.ui.utils.backToMain
import com.auralis.music.utils.BackupScheduler
import com.auralis.music.utils.rememberPreference
import com.auralis.music.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (autoBackupEnabled, onAutoBackupEnabledChange) = rememberPreference(
        key = AutoBackupEnabledKey,
        defaultValue = false
    )
    val (autoBackupTime, onAutoBackupTimeChange) = rememberPreference(
        key = AutoBackupTimeKey,
        defaultValue = "12:00"
    )
    val (lastAutoBackup, _) = rememberPreference(
        key = LastAutoBackupKey,
        defaultValue = 0L
    )

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.backup(context, uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restore(context, uri)
        }
    }

    // Update backup schedule when settings change
    LaunchedEffect(autoBackupEnabled, autoBackupTime) {
        try {
            if (autoBackupEnabled) {
                BackupScheduler.scheduleBackup(context, autoBackupTime)
                Timber.tag("BackupSettings").i("Backup scheduled for $autoBackupTime")
            } else {
                BackupScheduler.cancelBackup(context)
                Timber.tag("BackupSettings").i("Backup schedule cancelled")
            }
        } catch (e: Exception) {
            Timber.tag("BackupSettings").e(e, "Failed to update backup schedule")
        }
    }

    val timePickerDialog = remember {
        val (hour, minute) = autoBackupTime.split(":").map { it.toInt() }
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onAutoBackupTimeChange(newTime)
            },
            hour,
            minute,
            true // 24-hour format
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Auto Backup Section
            Material3SettingsGroup(
                title = stringResource(R.string.auto_backup),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.schedule),
                        title = { Text(stringResource(R.string.enable_auto_backup)) },
                        description = { Text(stringResource(R.string.auto_backup_description)) },
                        trailingContent = {
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = onAutoBackupEnabledChange
                            )
                        },
                        onClick = { onAutoBackupEnabledChange(!autoBackupEnabled) }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.schedule),
                        title = { Text(stringResource(R.string.backup_time)) },
                        description = { 
                            Text(
                                if (autoBackupEnabled) {
                                    stringResource(R.string.daily_at, autoBackupTime)
                                } else {
                                    stringResource(R.string.auto_backup_disabled)
                                }
                            )
                        },
                        onClick = {
                            if (autoBackupEnabled) {
                                timePickerDialog.show()
                            }
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.history),
                        title = { Text(stringResource(R.string.last_backup)) },
                        description = {
                            Text(
                                if (lastAutoBackup > 0) {
                                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                        .format(Date(lastAutoBackup))
                                } else {
                                    stringResource(R.string.never)
                                }
                            )
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text("Schedule Status") },
                        description = {
                            Text(
                                if (autoBackupEnabled) {
                                    BackupScheduler.getBackupStatus(context)
                                } else {
                                    "Auto backup disabled"
                                }
                            )
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Backup & Restore
            Material3SettingsGroup(
                title = stringResource(R.string.manual_backup_restore),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.backup),
                        title = { Text(stringResource(R.string.create_backup)) },
                        description = { Text(stringResource(R.string.create_backup_description)) },
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(Date())
                            backupLauncher.launch("auralis_backup_$timestamp.zip")
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.restore),
                        title = { Text(stringResource(R.string.restore_backup)) },
                        description = { Text(stringResource(R.string.restore_backup_description)) },
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.folder),
                        title = { Text(stringResource(R.string.view_backup_files)) },
                        description = { 
                            Text(stringResource(R.string.backup_location_description))
                        },
                        onClick = {
                            try {
                                val backupDir = File(context.getExternalFilesDir(null), "backups")
                                if (!backupDir.exists()) {
                                    backupDir.mkdirs()
                                }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.FileProvider",
                                    backupDir
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "resource/folder")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.cannot_open_folder),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
