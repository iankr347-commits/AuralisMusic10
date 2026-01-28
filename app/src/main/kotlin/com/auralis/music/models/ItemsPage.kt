// Private Test Build  Not for Redistribution

package com.auralis.music.models

import com.auralis.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
