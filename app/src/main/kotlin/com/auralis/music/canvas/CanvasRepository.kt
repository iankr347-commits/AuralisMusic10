package com.auralis.music.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and caches the Echo Music Canvas JSON database.
 *
 * The database is fetched once on first request and refreshed every [CACHE_TTL_MS].
 * All network I/O is done on [Dispatchers.IO].
 */
@Singleton
class CanvasRepository @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** In-memory cache */
    private var cachedItems: List<CanvasItem> = emptyList()
    private var lastFetchTime: Long = 0L
    private val mutex = Mutex()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Look up a canvas URL for the given [title] and [artist].
     *
     * Returns the URL string on a match, or `null` if unavailable.
     */
    suspend fun findCanvas(title: String, artist: String): String? {
        ensureDatabaseLoaded()
        val normalizedTitle = normalize(title)
        val cleanedTitle = normalize(cleanTitle(title))
        val queryArtist = normalizeArtist(artist)
        
        Timber.tag(TAG).d("Lookup normalizedTitle: '%s', cleanedTitle: '%s', queryArtist: '%s'", normalizedTitle, cleanedTitle, queryArtist)

        // Step 1: Look for exact normalized song title match
        var match = cachedItems.firstOrNull { item ->
            val dbTitle = normalize(item.song)
            dbTitle == normalizedTitle && matchArtist(item.artist, queryArtist)
        }
        
        // Step 2: Look for cleaned song title match (ignoring parentheses/brackets)
        if (match == null) {
            match = cachedItems.firstOrNull { item ->
                val dbTitleCleaned = normalize(cleanTitle(item.song))
                dbTitleCleaned.isNotEmpty() && cleanedTitle.isNotEmpty() && dbTitleCleaned == cleanedTitle && matchArtist(item.artist, queryArtist)
            }
        }
        
        // Step 3: Look for contains title match if no match yet
        if (match == null) {
            match = cachedItems.firstOrNull { item ->
                val dbTitle = normalize(item.song)
                val dbTitleCleaned = normalize(cleanTitle(item.song))
                (dbTitleCleaned.isNotEmpty() && cleanedTitle.isNotEmpty() && (dbTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(dbTitleCleaned))) && matchArtist(item.artist, queryArtist)
            }
        }

        return match?.url
    }

    private fun matchArtist(dbArtistRaw: String, queryArtist: String): Boolean {
        val dbArtist = normalizeArtist(dbArtistRaw)
        return dbArtist == queryArtist || dbArtist.contains(queryArtist) || queryArtist.contains(dbArtist)
    }

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun cleanTitle(s: String): String {
        return s.replace(Regex("\\s*[\\[\\(].*?[\\]\\)]"), "").trim()
    }

    private fun normalizeArtist(s: String): String {
        return s.lowercase()
            .replace("&", " ")
            .replace(",", " ")
            .replace("and", " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private suspend fun ensureDatabaseLoaded() = mutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedItems.isNotEmpty() && now - lastFetchTime < CACHE_TTL_MS) {
            Timber.tag(TAG).d("Canvas cache loaded (%d items)", cachedItems.size)
            return@withLock
        }
        fetchDatabase()
    }

    private suspend fun fetchDatabase() = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("Downloading Canvas database from %s", DATABASE_URL)
        try {
            val request = Request.Builder().url(DATABASE_URL).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag(TAG).w("Canvas database fetch failed: HTTP %d", response.code)
                return@withContext
            }
            val body = response.body?.string() ?: run {
                Timber.tag(TAG).w("Canvas database response body was empty")
                return@withContext
            }
            val items = parseJson(body)
            cachedItems = items
            lastFetchTime = System.currentTimeMillis()
            Timber.tag(TAG).i("Canvas database downloaded — %d items cached", items.size)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to download Canvas database")
        }
    }

    private fun parseJson(json: String): List<CanvasItem> {
        return try {
            val root = JSONObject(json)
            val array = root.getJSONArray("items")
            val list = mutableListOf<CanvasItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list += CanvasItem(
                    song = obj.optString("song", ""),
                    artist = obj.optString("artist", ""),
                    url = obj.optString("url", ""),
                )
            }
            list.filter { it.url.isNotBlank() }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse Canvas JSON")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "CanvasRepository"
        private const val DATABASE_URL = "https://canvas.echomusic.fun/canvas.json"

        /** 30 minutes */
        private const val CACHE_TTL_MS = 30L * 60 * 1000
    }
}
