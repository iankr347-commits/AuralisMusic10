// Private Test Build  Not for Redistribution
package com.auralis.innertube.pages

import com.auralis.innertube.models.YTItem

data class GeoRecommendationsPage(
    val countryCode: String,
    val countryName: String,
    val trending: List<YTItem>,
    val topSongs: List<YTItem>,
    val newReleases: List<YTItem>,
    val popularArtists: List<YTItem>,
)
