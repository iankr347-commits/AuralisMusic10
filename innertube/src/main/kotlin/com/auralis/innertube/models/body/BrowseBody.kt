// Private Test Build  Not for Redistribution
package com.auralis.innertube.models.body

import com.auralis.innertube.models.Context
import com.auralis.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
