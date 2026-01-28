// Private Test Build  Not for Redistribution
package com.auralis.innertube.pages

import com.auralis.innertube.models.Album
import com.auralis.innertube.models.AlbumItem
import com.auralis.innertube.models.Artist
import com.auralis.innertube.models.ArtistItem
import com.auralis.innertube.models.MusicResponsiveListItemRenderer
import com.auralis.innertube.models.MusicTwoRowItemRenderer
import com.auralis.innertube.models.PlaylistItem
import com.auralis.innertube.models.SongItem
import com.auralis.innertube.models.YTItem
import com.auralis.innertube.models.oddElements
import com.auralis.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
