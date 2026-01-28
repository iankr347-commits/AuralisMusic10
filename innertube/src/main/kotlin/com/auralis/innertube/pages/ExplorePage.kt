// Private Test Build  Not for Redistribution
package com.auralis.innertube.pages

import com.auralis.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)
