package com.auralis.music.canvas

/**
 * A single entry from the Echo Music Canvas database.
 * Maps a song+artist pair to a looping video URL.
 */
data class CanvasItem(
    val song: String,
    val artist: String,
    val url: String,
)
