// Private Test Build  Not for Redistribution
package com.auralis.innertube.pages

import com.auralis.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
