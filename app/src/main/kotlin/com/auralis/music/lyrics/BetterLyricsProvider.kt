// Private Test Build  Not for Redistribution

package com.auralis.music.lyrics

import android.content.Context
import com.auralis.music.betterlyrics.BetterLyrics
import com.auralis.music.constants.EnableBetterLyricsKey
import com.auralis.music.utils.dataStore
import com.auralis.music.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(title, artist, duration, callback)
    }
}
