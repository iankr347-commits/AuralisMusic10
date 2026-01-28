// Private Test Build  Not for Redistribution

package com.auralis.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.innertube.YouTube
import com.auralis.innertube.models.PlaylistItem
import com.auralis.innertube.models.WatchEndpoint
import com.auralis.innertube.models.YTItem
import com.auralis.innertube.models.filterExplicit
import com.auralis.innertube.models.filterVideoSongs
import com.auralis.innertube.pages.ExplorePage
import com.auralis.innertube.pages.GeoRecommendationsPage
import com.auralis.innertube.pages.HomePage
import com.auralis.innertube.utils.completed
import com.auralis.music.constants.ContentCountryKey
import com.auralis.music.constants.GeoRecommendationsKey
import com.auralis.music.constants.GeoSelectedCountryKey
import com.auralis.music.constants.HideExplicitKey
import com.auralis.music.constants.HideVideoSongsKey
import com.auralis.music.constants.InnerTubeCookieKey
import com.auralis.music.constants.QuickPicks
import com.auralis.music.constants.QuickPicksKey
import com.auralis.music.constants.SYSTEM_DEFAULT
import com.auralis.music.constants.YtmSyncKey
import com.auralis.music.db.MusicDatabase
import com.auralis.music.db.entities.Album
import com.auralis.music.db.entities.LocalItem
import com.auralis.music.db.entities.Song
import com.auralis.music.extensions.toEnum
import com.auralis.music.models.SimilarRecommendation
import com.auralis.music.utils.dataStore
import com.auralis.music.utils.get
import com.auralis.music.utils.reportException
import com.auralis.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.auralis.music.constants.ShowWrappedCardKey
import com.auralis.music.constants.WrappedSeenKey
import com.auralis.music.ui.screens.wrapped.WrappedAudioService
import com.auralis.music.ui.screens.wrapped.WrappedManager
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val geoRecommendations = MutableStateFlow<GeoRecommendationsPage?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

	val showWrappedCard: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        val showWrappedPref = prefs[ShowWrappedCardKey] ?: false
        val seen = prefs[WrappedSeenKey] ?: false
        val isBeforeDate = LocalDate.now().isBefore(LocalDate.of(2026, 1, 10))

        isBeforeDate && (!seen || showWrappedPref)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wrappedSeen: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WrappedSeenKey] ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun markWrappedAsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit {
                it[WrappedSeenKey] = true
            }
        }
    }
    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    // Track if we're currently processing account data
    private var isProcessingAccountData = false

    private suspend fun getQuickPicks() {
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().shuffled().take(20)
                }
            }
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

        getQuickPicks()
        forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        if (YouTube.cookie != null) {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
        }

        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).onSuccess { page ->
                    items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                    items += page.sections.lastOrNull()?.items.orEmpty()
                }
                SimilarRecommendation(
                    title = it,
                    items = items
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
            .filter { it.album != null }
            .shuffled().take(2)
            .mapNotNull { song ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (page.songs.shuffled().take(8) +
                            page.albums.shuffled().take(4) +
                            page.artists.shuffled().take(4) +
                            page.playlists.shuffled().take(4))
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }
        similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        YouTube.home().onSuccess { page ->
            homePage.value = page.copy(
                sections = page.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
        }.onFailure {
            reportException(it)
        }

        YouTube.explore().onSuccess { page ->
            explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }

        // Load geo-based recommendations if enabled
        val geoEnabled = context.dataStore.get(GeoRecommendationsKey, true)
        if (geoEnabled) {
            loadGeoRecommendations(hideExplicit, hideVideoSongs)
        } else {
            geoRecommendations.value = null
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                geoRecommendations.value?.let { it.trending + it.topSongs + it.newReleases }.orEmpty()

        isLoading.value = false
    }

    private suspend fun loadGeoRecommendations(hideExplicit: Boolean, hideVideoSongs: Boolean) {
        // Get country code from geo settings first, then fallback to content country, then device locale
        val geoSelectedCountry = context.dataStore.get(GeoSelectedCountryKey, SYSTEM_DEFAULT)
        val countryCode = when {
            geoSelectedCountry != SYSTEM_DEFAULT -> geoSelectedCountry
            else -> {
                val contentCountry = context.dataStore.get(ContentCountryKey, SYSTEM_DEFAULT)
                contentCountry?.takeIf { it != SYSTEM_DEFAULT }
                    ?: Locale.getDefault().country
            }
        }

        YouTube.getGeoRecommendations(countryCode).onSuccess { page ->
            geoRecommendations.value = page.copy(
                trending = page.trending.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs),
                topSongs = page.topSongs.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs),
                newReleases = page.newReleases.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs),
                popularArtists = page.popularArtists.filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val nextSections = YouTube.home(params = chip?.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
            selectedChip.value = chip
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()
            
            load()

            val isSyncEnabled = context.dataStore.get(YtmSyncKey, true)
            if (isSyncEnabled) {
                syncUtils.runAllSyncs()
            }

            try {
                if (showWrappedCard.first()) {
                    android.util.Log.d("HomeViewModel", "Preparing Wrapped data")
                    wrappedManager.prepare()
                    val state = wrappedManager.state.first() // Correctly get the state object
                    val trackMap = state.trackMap
                    if (trackMap.isNotEmpty()) {
                        val firstTrackId = trackMap.entries.first().value
                        wrappedAudioService.prepareTrack(firstTrackId)
                    }
                    android.util.Log.d("HomeViewModel", "Wrapped data prepared")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error preparing Wrapped data", e)
            }
        }

        // Listen for cookie changes and reload account data
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    // Avoid processing if already processing
                    if (isProcessingAccountData) return@collect
                    
                    // Always process cookie changes, even if same value (for logout/login scenarios)
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    
                    try {
                        if (cookie != null && cookie.isNotEmpty()) {
                            
                            // Update YouTube.cookie manually to ensure it's set
                            YouTube.cookie = cookie
                            
                            // Fetch new account data
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                            }.onFailure {
                                reportException(it)
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }
    }
}
