// Private Test Build  Not for Redistribution

package com.auralis.music.db.entities

sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
