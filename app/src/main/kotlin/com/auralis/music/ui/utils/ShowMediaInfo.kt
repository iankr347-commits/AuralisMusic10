// Private Test Build  Not for Redistribution

package com.auralis.music.ui.utils

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.auralis.innertube.YouTube
import com.auralis.innertube.models.MediaInfo
import com.auralis.music.LocalDatabase
import com.auralis.music.LocalPlayerConnection
import com.auralis.music.R
import com.auralis.music.constants.DarkModeKey
import com.auralis.music.constants.PureBlackKey
import com.auralis.music.constants.ThumbnailCornerRadius
import com.auralis.music.db.entities.FormatEntity
import com.auralis.music.db.entities.Song
import com.auralis.music.ui.component.LocalMenuState
import com.auralis.music.ui.component.MenuState
import com.auralis.music.ui.component.shimmer.ShimmerHost
import com.auralis.music.ui.component.shimmer.TextPlaceholder
import com.auralis.music.ui.screens.settings.DarkMode
import com.auralis.music.utils.rememberEnumPreference
import com.auralis.music.utils.rememberPreference
import android.content.ClipData
import android.content.ClipboardManager

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }

    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }
    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect {
            song = it
        }
    }
    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(windowInsets.asPaddingValues())
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Hero Artwork Section with Title
        item(contentType = "Artwork") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Artwork Card with Shadow
                Card(
                    modifier = Modifier.size(180.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = song?.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Subtle gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Song Title
                Text(
                    text = song?.title ?: info?.title ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Artist Name with Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.artist),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = song?.artists?.joinToString { it.name } ?: info?.author ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
        
        // Spacing after header section
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (song != null) {
            // Song Details Card
            item(contentType = "SongDetails") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Section header with accent
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.music_note),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Song Information",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        val baseList = listOf(
                            Triple(R.drawable.music_note, stringResource(R.string.song_title), song?.title),
                            Triple(R.drawable.artist, stringResource(R.string.song_artists), song?.artists?.joinToString { it.name }),
                            Triple(R.drawable.token, stringResource(R.string.media_id), song?.id)
                        )
                        
                        baseList.forEachIndexed { index, (icon, label, text) ->
                            DetailRow(
                                icon = icon,
                                label = label,
                                value = text ?: stringResource(R.string.unknown),
                                context = context
                            )
                            if (index < baseList.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
            
            // Spacing after Song Details
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Audio Quality Card
            if (currentFormat != null) {
                item(contentType = "AudioQuality") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Section header with accent
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.graphic_eq),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Audio Quality",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))

                            val formatList = listOf(
                                Triple(R.drawable.token, "Itag", currentFormat?.itag?.toString()),
                                Triple(R.drawable.description, stringResource(R.string.mime_type), currentFormat?.mimeType),
                                Triple(R.drawable.input, stringResource(R.string.codecs), currentFormat?.codecs),
                                Triple(R.drawable.speed, stringResource(R.string.bitrate), currentFormat?.bitrate?.let { "${it / 1000} Kbps" }),
                                Triple(R.drawable.graphic_eq, stringResource(R.string.sample_rate), currentFormat?.sampleRate?.let { "$it Hz" }),
                                Triple(R.drawable.volume_up, stringResource(R.string.loudness), currentFormat?.loudnessDb?.let { "$it dB" }),
                                Triple(R.drawable.volume_up, stringResource(R.string.volume), if (playerConnection != null) "${(playerConnection.player.volume * 100).toInt()}%" else null),
                                Triple(R.drawable.storage, stringResource(R.string.file_size), currentFormat?.contentLength?.let { Formatter.formatShortFileSize(context, it) })
                            )
                            
                            formatList.forEachIndexed { index, (icon, label, text) ->
                                DetailRow(
                                    icon = icon,
                                    label = label,
                                    value = text ?: stringResource(R.string.unknown),
                                    context = context,
                                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (index < formatList.size - 1) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            // Spacing after Audio Quality
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Media Information Card
        if (info != null) {
            if (song == null) {
                item(contentType = "MediaTitle") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = info?.title ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Spacing after MediaTitle
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item(contentType = "MediaInfo") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Section header with accent
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.info),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.information),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        // Author
                        Text(
                            text = stringResource(R.string.artists),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info?.author ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Description
                        if (!info?.description.isNullOrEmpty()) {
                            Text(
                                text = stringResource(R.string.description),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                            )
                        }
                    }
                }
            }
            
            // Spacing after MediaInfo
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Statistics Card
            item(contentType = "Statistics") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Section header with accent
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.stats),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.numbers),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatItem(
                                icon = R.drawable.person,
                                label = stringResource(R.string.subscribers),
                                value = info?.subscribers ?: "0",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            StatItem(
                                icon = R.drawable.info,
                                label = stringResource(R.string.views),
                                value = info?.viewCount?.toInt()?.let { numberFormatter(it) } ?: "0",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatItem(
                                icon = R.drawable.favorite,
                                label = stringResource(R.string.likes),
                                value = info?.like?.toInt()?.let { numberFormatter(it) } ?: "0",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            StatItem(
                                icon = R.drawable.favorite_border,
                                label = stringResource(R.string.dislikes),
                                value = info?.dislike?.toInt()?.let { numberFormatter(it) } ?: "0",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Spacing after Statistics
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            item(contentType = "Loading") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    ShimmerHost {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextPlaceholder()
                        }
                    }
                }
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: Int,
    label: String,
    value: String,
    context: android.content.Context,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = textColor.copy(alpha = 0.08f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("text", value))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    }
                )
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: Int,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(100.dp)
    ) {
        Surface(
            color = color.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
