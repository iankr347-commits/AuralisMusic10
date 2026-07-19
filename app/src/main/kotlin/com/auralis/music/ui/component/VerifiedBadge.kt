// Private Test Build  Not for Redistribution

package com.auralis.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralis.music.R

/**
 * Auralis Verified Badge Component
 * 
 * Displays a verification badge indicating that the artist is verified by Auralis.
 * This badge should only be shown when the artist's verification status is confirmed
 * via Firebase Realtime Database.
 */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_verified_custom),
            contentDescription = "Verified",
            tint = Color(0xFF1DB954), // Spotify green
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Verified by Auralis Music",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
