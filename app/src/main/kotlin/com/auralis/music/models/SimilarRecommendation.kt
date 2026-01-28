// Private Test Build  Not for Redistribution

package com.auralis.music.models

import com.auralis.innertube.models.YTItem
import com.auralis.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
