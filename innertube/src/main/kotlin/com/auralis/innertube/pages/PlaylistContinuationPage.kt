// Private Test Build  Not for Redistribution
package com.auralis.innertube.pages

import com.auralis.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
