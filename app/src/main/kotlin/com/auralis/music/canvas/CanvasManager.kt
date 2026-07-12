package com.auralis.music.canvas

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.auralis.music.constants.CanvasEnabledKey
import com.auralis.music.utils.dataStore
import com.auralis.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Canvas state for the currently-playing track.
 *
 * Acts as the single source of truth for [CanvasState]. Callers (UI) collect [state]
 * and call [onSongChanged] whenever the active track changes.
 */
@Singleton
class CanvasManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CanvasRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<CanvasState>(CanvasState.Loading)
    val state: StateFlow<CanvasState> = _state.asStateFlow()

    private var currentTitle: String? = null
    private var currentArtist: String? = null

    init {
        scope.launch {
            context.dataStore.data
                .map { it[CanvasEnabledKey] ?: true }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (!enabled) {
                        Timber.tag(TAG).d("Canvas disabled via settings flow")
                        _state.value = CanvasState.Disabled
                    } else {
                        Timber.tag(TAG).d("Canvas enabled via settings flow")
                        val title = currentTitle
                        val artist = currentArtist
                        if (title != null && artist != null) {
                            performLookup(title, artist)
                        } else {
                            _state.value = CanvasState.Unavailable
                        }
                    }
                }
        }
    }

    /**
     * Trigger a Canvas lookup for the given song + artist.
     * Must be called on every media item transition.
     */
    fun onSongChanged(title: String, artist: String) {
        currentTitle = title
        currentArtist = artist
        scope.launch {
            val enabled = context.dataStore[CanvasEnabledKey] ?: true
            if (!enabled) {
                Timber.tag(TAG).d("Canvas disabled by user — skipping lookup")
                _state.value = CanvasState.Disabled
                return@launch
            }
            performLookup(title, artist)
        }
    }

    private suspend fun performLookup(title: String, artist: String) {
        Timber.tag(TAG).d("Searching canvas... Song: %s  Artist: %s", title, artist)
        _state.value = CanvasState.Loading

        val url = runCatching {
            repository.findCanvas(title, artist)
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Canvas lookup threw exception")
        }.getOrNull()

        if (url != null) {
            Timber.tag(TAG).i("Canvas found — URL: %s", url)
            _state.value = CanvasState.Playing(url)
        } else {
            Timber.tag(TAG).d("No canvas for this track — falling back to artwork")
            _state.value = CanvasState.Unavailable
        }
    }

    /**
     * Immediately move to [CanvasState.Unavailable].
     * Called by the player composable when a playback error occurs.
     */
    fun onPlaybackError() {
        Timber.tag(TAG).w("Playback error reported — fallback to artwork")
        _state.value = CanvasState.Unavailable
    }

    companion object {
        private const val TAG = "CanvasManager"
    }
}
