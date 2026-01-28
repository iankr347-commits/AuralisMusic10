// Private Test Build  Not for Redistribution

package com.auralis.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_history")
data class LibraryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String?,
    val action: String,
    val timestamp: Long
)