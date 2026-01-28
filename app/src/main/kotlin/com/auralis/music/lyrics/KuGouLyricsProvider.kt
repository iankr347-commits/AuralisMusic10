// Private Test Build  Not for Redistribution

package com.auralis.music.lyrics

import android.content.Context
import com.auralis.kugou.KuGou
import com.auralis.music.constants.EnableKugouKey
import com.auralis.music.utils.dataStore
import com.auralis.music.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> =
        KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
