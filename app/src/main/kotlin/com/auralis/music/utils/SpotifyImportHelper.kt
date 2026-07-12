package com.auralis.music.utils

import android.util.Log
import com.auralis.innertube.YouTube
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.json.JSONObject

object SpotifyImportHelper {
    private const val TAG = "SpotifyImportHelper"
    private val client = OkHttpClient.Builder().build()

    data class ImportProgress(
        val playlistName: String,
        val totalTracks: Int,
        val processedTracks: Int,
        val foundSongIds: List<String>,
        val failedTracks: List<String>,
    )

    suspend fun getPlaylistSongs(url: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = "Spotify Import"

        try {
            val playlistId = extractPlaylistId(url)
            if (playlistId == null) {
                Log.e(TAG, "Could not extract playlist ID from URL: $url")
                return@withContext playlistName to emptyList()
            }

            val accessToken = getSpotifyAccessToken()
            if (accessToken != null) {
                try {
                    val (name, apiSongs) = fetchTracksViaApi(playlistId, accessToken)
                    if (name.isNotEmpty()) playlistName = name
                    if (apiSongs.isNotEmpty()) {
                        return@withContext playlistName to apiSongs
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "API method failed, falling back to embed", e)
                }
            }

            try {
                val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
                val doc = Jsoup.connect(embedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()

                val nextDataScript = doc.select("script#__NEXT_DATA__").first()
                if (nextDataScript != null) {
                    val json = nextDataScript.html()
                    val jsonObject = JSONObject(json)
                    val entity = jsonObject.optJSONObject("props")
                        ?.optJSONObject("pageProps")
                        ?.optJSONObject("state")
                        ?.optJSONObject("data")
                        ?.optJSONObject("entity")

                    if (entity != null) {
                        playlistName = entity.optString("name", entity.optString("title", playlistName))

                        val trackList = entity.optJSONArray("trackList")
                        if (trackList != null) {
                            for (i in 0 until trackList.length()) {
                                val trackObj = trackList.optJSONObject(i) ?: continue
                                val title = trackObj.optString("title")
                                val subtitle = trackObj.optString("subtitle", "")
                                if (title.isNotBlank()) songs.add(title to subtitle)
                            }
                        }
                    }
                }
                if (songs.isNotEmpty()) {
                    return@withContext playlistName to songs.toList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Embed method failed, falling back to HTML", e)
            }

            try {
                val doc = Jsoup.connect("https://open.spotify.com/playlist/$playlistId")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()

                val titleEl = doc.selectFirst("meta[property=og:title]")
                if (titleEl != null) {
                    playlistName = titleEl.attr("content")
                }

                val trackElements = doc.select("meta[name=music:song]")
                for (el in trackElements) {
                    val trackUrl = el.attr("content")
                    val trackDoc = try {
                        Jsoup.connect(trackUrl).userAgent("Mozilla/5.0").get()
                    } catch (_: Exception) { null }
                    val trackTitle = trackDoc?.selectFirst("meta[property=og:title]")?.attr("content")
                    val trackArtist = trackDoc?.selectFirst("meta[property=og:description]")?.attr("content")
                    if (trackTitle != null) {
                        songs.add(trackTitle to (trackArtist ?: ""))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "All methods failed", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting playlist: ${e.message}", e)
        }

        playlistName to songs.toList()
    }

    suspend fun searchYouTubeForSong(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$title $artist"
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            val items = result.getOrNull()?.items ?: return@withContext null
            val songItem = items.filterIsInstance<com.auralis.innertube.models.SongItem>().firstOrNull()
            songItem?.id
        } catch (e: Exception) {
            Log.e(TAG, "YouTube search failed for '$title - $artist': ${e.message}")
            null
        }
    }

    private fun extractPlaylistId(url: String): String? {
        val patterns = listOf(
            Regex("playlist/([a-zA-Z0-9]+)"),
            Regex("playlist%2F([a-zA-Z0-9]+)"),
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun getSpotifyAccessToken(): String? {
        return try {
            val request = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            json.optString("accessToken").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Spotify access token: ${e.message}")
            null
        }
    }

    private fun fetchTracksViaApi(
        playlistId: String,
        accessToken: String,
    ): Pair<String, List<Pair<String, String>>> {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = ""
        var nextUrl: String? =
            "https://api.spotify.com/v1/playlists/$playlistId/tracks?offset=0&limit=100&fields=items(track(name,artists(name))),total,next"

        while (nextUrl != null) {
            val json = fetchSpotifyApiPage(nextUrl, accessToken) ?: break

            if (playlistName.isEmpty()) {
                try {
                    val nameRequest = Request.Builder()
                        .url("https://api.spotify.com/v1/playlists/$playlistId?fields=name")
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val nameResponse = client.newCall(nameRequest).execute()
                    val nameBody = nameResponse.body?.string()
                    if (nameBody != null) {
                        playlistName = JSONObject(nameBody).optString("name", "")
                    }
                } catch (_: Exception) { }
            }

            val items = json.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val track = items.optJSONObject(i)?.optJSONObject("track") ?: continue
                val name = track.optString("name", "")
                if (name.isBlank()) continue
                
                val artistsArr = track.optJSONArray("artists")
                val artists = mutableListOf<String>()
                if (artistsArr != null) {
                    for (j in 0 until artistsArr.length()) {
                        val authName = artistsArr.optJSONObject(j)?.optString("name") ?: ""
                        if (authName.isNotBlank()) artists.add(authName)
                    }
                }
                songs.add(name to artists.joinToString(", "))
            }

            nextUrl = json.optString("next", null).takeIf { it != "null" && it.isNotBlank() }
        }

        return playlistName to songs
    }

    private fun fetchSpotifyApiPage(
        url: String,
        accessToken: String,
    ): JSONObject? {
        var attempts = 0
        while (attempts < 4) {
            attempts++
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()

            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 1L
                Log.w(TAG, "Spotify rate-limited page fetch, retrying in ${retryAfterSeconds}s (attempt $attempts)")
                TimeUnit.SECONDS.sleep(retryAfterSeconds.coerceAtLeast(1L))
                continue
            }

            if (!response.isSuccessful) {
                Log.w(TAG, "Spotify API page fetch failed: HTTP ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            return try { JSONObject(body) } catch (_: Exception) { null }
        }

        return null
    }
}
