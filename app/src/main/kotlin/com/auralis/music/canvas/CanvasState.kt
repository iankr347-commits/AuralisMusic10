package com.auralis.music.canvas

/**
 * Represents the current state of the Canvas feature for the playing track.
 *
 * State machine:
 *   Disabled    - user has turned Canvas off in settings; never fetch, never play
 *   Loading     - lookup in progress (JSON being fetched or match being searched)
 *   Unavailable - no canvas found for this song, or a network/playback error occurred
 *   Playing     - a matching canvas URL was found; play the video
 */
sealed class CanvasState {
    /** Canvas feature is disabled in settings. */
    object Disabled : CanvasState()

    /** Currently searching for a canvas (JSON download or match lookup). */
    object Loading : CanvasState()

    /** No canvas available for the current song (or an error occurred). */
    object Unavailable : CanvasState()

    /** A canvas video URL was found. */
    data class Playing(val url: String) : CanvasState()
}
