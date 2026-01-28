// Private Test Build  Not for Redistribution

package com.auralis.music.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.auralis.music.MainActivity
import com.auralis.music.R
import com.auralis.music.db.InternalDatabase
import com.auralis.music.db.MusicDatabase
import com.auralis.music.db.entities.ArtistEntity
import com.auralis.music.db.entities.Song
import com.auralis.music.db.entities.SongEntity
import com.auralis.music.extensions.div
import com.auralis.music.extensions.tryOrNull
import com.auralis.music.extensions.zipInputStream
import com.auralis.music.extensions.zipOutputStream
import com.auralis.music.playback.MusicService
import com.auralis.music.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.auralis.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess
import timber.log.Timber

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir / "datastore" / SETTINGS_FILENAME).inputStream().buffered()
                        .use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                            inputStream.copyTo(outputStream)
                        }
                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }
                    FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure {
            reportException(it)
            Toast.makeText(context, R.string.backup_create_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun restore(context: Context, uri: Uri) {
        runCatching {
            Timber.tag("RESTORE").i("Starting restore from URI: $uri")
            context.applicationContext.contentResolver.openInputStream(uri)?.use { raw ->
                raw.zipInputStream().use { inputStream ->
                    var entry = tryOrNull { inputStream.nextEntry } // prevent ZipException
                    var foundAny = false
                    var dbRestored = false
                    
                    while (entry != null) {
                        Timber.tag("RESTORE").i("Found zip entry: ${entry.name}")
                        when (entry.name) {
                            SETTINGS_FILENAME -> {
                                Timber.tag("RESTORE").i("Restoring settings to datastore")
                                foundAny = true
                                try {
                                    (context.filesDir / "datastore" / SETTINGS_FILENAME).outputStream()
                                        .use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                } catch (e: Exception) {
                                    Timber.tag("RESTORE").e(e, "Error restoring settings")
                                    reportException(e)
                                }
                            }
                            InternalDatabase.DB_NAME -> {
                                Timber.tag("RESTORE").i("Restoring DB (entry = ${entry.name})")
                                foundAny = true
                                try {
                                    // capture path before closing DB to avoid reopening race
                                    val dbPath = database.openHelper.writableDatabase.path
                                    runBlocking(Dispatchers.IO) { database.checkpoint() }
                                    database.close()
                                    Timber.tag("RESTORE").i("Overwriting DB at path: $dbPath")
                                    FileOutputStream(dbPath).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                    Timber.tag("RESTORE").i("DB overwrite complete")
                                    dbRestored = true
                                } catch (e: Exception) {
                                    Timber.tag("RESTORE").e(e, "Error restoring database")
                                    reportException(e)
                                    throw Exception("Failed to restore database: ${e.message}", e)
                                }
                            }
                            else -> {
                                Timber.tag("RESTORE").i("Skipping unexpected entry: ${entry.name}")
                            }
                        }
                        entry = tryOrNull { inputStream.nextEntry } // prevent ZipException
                    }
                    
                    if (!foundAny) {
                        Timber.tag("RESTORE").w("No expected entries found in archive")
                        throw Exception("Invalid backup file: no data found")
                    }
                    
                    // Validate database if restored
                    if (dbRestored) {
                        try {
                            Timber.tag("RESTORE").i("Validating restored database")
                            // Try to open and query the database to ensure it's valid
                            val testDb = InternalDatabase.newInstance(context)
                            testDb.query { 
                                // Simple validation query
                                eventsCount()
                            }
                            testDb.close()
                            Timber.tag("RESTORE").i("Database validation successful")
                        } catch (e: Exception) {
                            Timber.tag("RESTORE").e(e, "Database validation failed")
                            reportException(e)
                            throw Exception("Restored database is corrupted or incompatible: ${e.message}", e)
                        }
                    }
                }
            } ?: run {
                Timber.tag("RESTORE").e("Could not open input stream for uri: $uri")
                throw Exception("Could not read backup file")
            }

            context.stopService(Intent(context, MusicService::class.java))
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            context.startActivity(Intent(context, MainActivity::class.java))
            exitProcess(0)
        }.onFailure {
            reportException(it)
            Timber.tag("RESTORE").e(it, "Restore failed")
            val errorMessage = when {
                it.message?.contains("corrupted") == true -> context.getString(R.string.restore_failed) + ": Database corrupted"
                it.message?.contains("Invalid backup") == true -> context.getString(R.string.restore_failed) + ": Invalid backup file"
                else -> context.getString(R.string.restore_failed)
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                lines.forEachIndexed { _, line ->
                    val parts = line.split(",").map { it.trim() }
                    val title = parts[0]
                    val artistStr = parts[1]

                    val artists = artistStr.split(";").map { it.trim() }.map {
                   ArtistEntity(
                            id = "",
                            name = it,
                        )
                    }
                    val mockSong = Song(
                        song = SongEntity(
                            id = "",
                            title = title,
                        ),
                        artists = artists,
                    )
                    songs.add(mockSong)
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    fun loadM3UOnline(
        context: Context,
        uri: Uri,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.first().startsWith("#EXTM3U")) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            // maybe later write this to be more efficient
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)

                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
    }
}
