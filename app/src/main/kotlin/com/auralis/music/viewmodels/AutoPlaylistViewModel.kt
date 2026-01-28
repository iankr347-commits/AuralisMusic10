// Private Test Build  Not for Redistribution

package com.auralis.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.music.constants.HideExplicitKey
import com.auralis.music.constants.SongSortDescendingKey
import com.auralis.music.constants.SongSortType
import com.auralis.music.constants.SongSortTypeKey
import com.auralis.music.db.MusicDatabase
import com.auralis.music.db.entities.EventWithSong
import com.auralis.music.db.entities.Song
import com.auralis.music.extensions.filterExplicit
import com.auralis.music.extensions.toEnum
import com.auralis.music.utils.SyncUtils
import com.auralis.music.utils.dataStore
import com.auralis.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        context.dataStore.data
            .map {
                Pair(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit) ->
                val (sortType, descending) = sortDesc
                when (playlist) {
                    "liked" -> database.likedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit) }

                    "downloaded" -> database.downloadedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit) }

                    "uploaded" -> database.uploadedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit) }

                    "history" -> try {
                        database.events()
                            .catch { e: Throwable ->
                                // If events table doesn't exist or has issues, return empty flow
                                flowOf<List<EventWithSong>>(emptyList())
                            }
                            .map { events: List<EventWithSong> -> 
                                if (events.isEmpty()) {
                                    emptyList<Song>()
                                } else {
                                    events.mapNotNull { event: EventWithSong ->
                                        try {
                                            event.song
                                        } catch (e: Exception) {
                                            // Handle cases where song might be null or corrupted
                                            null
                                        }
                                    }
                                    .filterExplicit(hideExplicit)
                                    .let { songs: List<Song> ->
                                        try {
                                            when (sortType) {
                                                SongSortType.CREATE_DATE -> 
                                                    if (descending) songs.sortedByDescending { it.id }
                                                    else songs.sortedBy { it.id }
                                                SongSortType.NAME -> 
                                                    if (descending) songs.sortedByDescending { it.title }
                                                    else songs.sortedBy { it.title }
                                                SongSortType.ARTIST -> 
                                                    if (descending) songs.sortedByDescending { it.artists.joinToString() }
                                                    else songs.sortedBy { it.artists.joinToString() }
                                                SongSortType.PLAY_TIME -> 
                                                    if (descending) songs.sortedByDescending { it.song.totalPlayTime }
                                                    else songs.sortedBy { it.song.totalPlayTime }
                                            }
                                        } catch (e: Exception) {
                                            // Fallback to unsorted list if sorting fails
                                            songs
                                        }
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        // If anything fails, return empty flow
                        flowOf<List<Song>>(emptyList())
                    }

                    else -> kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }
}
