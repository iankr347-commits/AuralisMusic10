// Private Test Build  Not for Redistribution

package com.auralis.music.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.innertube.YouTube
import com.auralis.innertube.models.filterExplicit
import com.auralis.innertube.models.filterVideoSongs
import com.auralis.innertube.pages.ArtistPage
import com.auralis.music.db.MusicDatabase
import com.auralis.music.firebase.FirebaseArtistVerifier
import com.auralis.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.auralis.music.constants.HideExplicitKey
import com.auralis.music.constants.HideVideoSongsKey
import com.auralis.music.extensions.filterExplicit
import com.auralis.music.extensions.filterExplicitAlbums
import com.auralis.music.utils.dataStore
import com.auralis.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val artistVerifier: FirebaseArtistVerifier,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    var isArtistVerified by mutableStateOf<Boolean?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
        
        // Also check verification for local artists
        viewModelScope.launch {
            libraryArtist.collect { artist ->
                if (artist != null && artistPage == null) {
                    checkArtistVerification(artist.artist.name)
                }
            }
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    val filteredSections = page.sections
                        .map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                        }
                        .filter { section -> section.items.isNotEmpty() }

                    artistPage = page.copy(sections = filteredSections)
                    
                    // Check artist verification after page is loaded
                    page.artist?.title?.let { artistName ->
                        checkArtistVerification(artistName)
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }

    fun checkArtistVerification(artistName: String) {
        viewModelScope.launch {
            try {
                artistVerifier.isArtistVerified(artistName).collect { verified ->
                    isArtistVerified = verified
                }
            } catch (e: Exception) {
                // If verification fails, treat as not verified and don't crash
                isArtistVerified = false
                reportException(e)
            }
        }
    }
}
