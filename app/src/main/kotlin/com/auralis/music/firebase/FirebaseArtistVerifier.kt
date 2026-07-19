// Private Test Build  Not for Redistribution

package com.auralis.music.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for verifying artist status using Firebase Realtime Database
 */
@Singleton
class FirebaseArtistVerifier @Inject constructor() {

    /**
     * Checks if an artist is verified by name by querying the Firebase Realtime Database
     * 
     * @param artistName The name of the artist to check (case-insensitive, trimmed)
     * @return Flow emitting the verification status (true if verified, false otherwise)
     */
    fun isArtistVerified(artistName: String): Flow<Boolean> = callbackFlow {
        if (artistName.isBlank()) {
            Timber.d("Artist name is blank, returning not verified")
            trySend(false)
            close()
            return@callbackFlow
        }

        val normalizedArtistName = artistName.trim().lowercase()
        Timber.d("Starting verification check for: $normalizedArtistName")

        val database = FirebaseDatabase.getInstance("https://aura-app-e6a57-default-rtdb.firebaseio.com")
        val artistsRef = database.getReference("artists")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isVerified = false
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java)
                    val verified = child.child("verified").getValue(Boolean::class.java) ?: false
                    
                    if (name != null && name.trim().lowercase() == normalizedArtistName && verified) {
                        isVerified = true
                        break
                    }
                }
                Timber.d("Verification status updated for '$normalizedArtistName': $isVerified")
                trySend(isVerified)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Firebase database check cancelled/failed")
                trySend(false)
            }
        }

        artistsRef.addValueEventListener(listener)

        awaitClose {
            Timber.d("Closing verification listener for: $normalizedArtistName")
            artistsRef.removeEventListener(listener)
        }
    }

    /**
     * Add a verified artist
     */
    fun addVerifiedArtist(artistName: String) {
        Timber.d("Request to add verified artist: $artistName")
    }
}

