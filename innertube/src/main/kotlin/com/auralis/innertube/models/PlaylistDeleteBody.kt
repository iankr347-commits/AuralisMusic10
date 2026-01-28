// Private Test Build  Not for Redistribution
package com.auralis.innertube.models.body

import com.auralis.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
