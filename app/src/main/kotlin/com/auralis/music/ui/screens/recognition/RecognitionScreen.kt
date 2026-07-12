/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auralis.music.ui.screens.recognition

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.auralis.music.LocalDatabase
import com.auralis.music.LocalPlayerConnection
import com.auralis.music.R
import com.auralis.music.db.entities.RecognitionHistory
import com.auralis.music.ui.component.AnimatedGradientBackground
import com.auralis.music.ui.component.IconButton
import com.auralis.music.ui.theme.PlayerColorExtractor
import com.auralis.music.ui.utils.backToMain
import com.auralis.innertube.YouTube
import com.auralis.innertube.models.*
import com.auralis.music.models.toMediaMetadata
import com.auralis.music.playback.queues.YouTubeQueue
import com.auralis.shazamkit.models.RecognitionResult
import com.auralis.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    navController: NavController,
    autoStart: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()

    // Only reset in Ready state: Listening/Processing belong to a running widget-service
    // recognition that must not be cancelled; Success/NoMatch/Error are results pending
    // display and history saving.
    LaunchedEffect(Unit) {
        if (com.auralis.music.recognition.MusicRecognitionService.recognitionStatus.value
                is RecognitionStatus.Ready
        ) {
            com.auralis.music.recognition.MusicRecognitionService
                .reset()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.auralis.music.recognition.MusicRecognitionService
                .reset()
        }
    }

    // Observe recognition status from service for real-time updates (Listening -> Processing -> Result)
    val recognitionStatus by com.auralis.music.recognition.MusicRecognitionService.recognitionStatus
        .collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                coroutineScope.launch {
                    com.auralis.music.recognition.MusicRecognitionService
                        .recognize(context)
                }
            }
        }

    fun startRecognition() {
        if (hasPermission) {
            coroutineScope.launch {
                com.auralis.music.recognition.MusicRecognitionService
                    .recognize(context)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        if (autoStart &&
            com.auralis.music.recognition.MusicRecognitionService.recognitionStatus.value
                is RecognitionStatus.Ready
        ) {
            startRecognition()
        }
    }

    fun resetToReady() {
        com.auralis.music.recognition.MusicRecognitionService
            .reset()
    }

    fun saveToHistory(result: RecognitionResult) {
        // Skip if the widget service already persisted this result to avoid a duplicate entry
        if (com.auralis.music.recognition.MusicRecognitionService.resultSavedExternally) return
        coroutineScope.launch(Dispatchers.IO) {
            database.query {
                insert(
                    RecognitionHistory(
                        trackId = result.trackId,
                        title = result.title,
                        artist = result.artist,
                        album = result.album,
                        coverArtUrl = result.coverArtUrl,
                        coverArtHqUrl = result.coverArtHqUrl,
                        genre = result.genre,
                        releaseDate = result.releaseDate,
                        label = result.label,
                        shazamUrl = result.shazamUrl,
                        appleMusicUrl = result.appleMusicUrl,
                        spotifyUrl = result.spotifyUrl,
                        isrc = result.isrc,
                        youtubeVideoId = result.youtubeVideoId,
                        recognizedAt = LocalDateTime.now(),
                    ),
                )
            }
        }
    }

    // Check if we should show full-screen success view
    val isSuccessState = recognitionStatus is RecognitionStatus.Success
    
    if (isSuccessState) {
        // Full-screen success view with animated gradient background
        SuccessState(
            result = (recognitionStatus as RecognitionStatus.Success).result,
            navController = navController,
            onSearch = { result ->
                val searchQuery = "${result.title} ${result.artist}"
                navController.navigate("search/${java.net.URLEncoder.encode(searchQuery, "UTF-8")}")
            },
            onPlaySong = { result ->
                if (playerConnection != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val query = "${result.title} ${result.artist}"
                            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                                .onSuccess { searchResult ->
                                    val items = searchResult.items.filterIsInstance<SongItem>()
                                    val firstSong = items.firstOrNull()
                                    if (firstSong != null) {
                                        withContext(Dispatchers.Main) {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(firstSong.toMediaMetadata())
                                            )
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            onTryAgain = {
                startRecognition()
            },
            onClose = ::resetToReady,
            onSaveToHistory = ::saveToHistory,
        )
    } else {
        // Regular scaffold for other states
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.recognize_music)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() },
                            onLongClick = { navController.backToMain() },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        com.auralis.music.ui.component.IconButton(
                            onClick = { navController.navigate("recognition_history") },
                            onLongClick = {},
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.history),
                                contentDescription = stringResource(R.string.recognition_history),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = recognitionStatus,
                    transitionSpec = {
                        (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                    },
                    label = "recognition_content",
                ) { status ->
                    when (status) {
                        is RecognitionStatus.Ready -> {
                            ReadyState(onStartRecognition = ::startRecognition)
                        }

                        is RecognitionStatus.Listening -> {
                            ListeningState(
                                onCancel = {
                                    com.auralis.music.recognition.MusicRecognitionService
                                        .reset()
                                },
                            )
                        }

                        is RecognitionStatus.Processing -> {
                            ProcessingState()
                        }

                        is RecognitionStatus.Success -> {
                            // This won't be reached as we handle it outside
                            ReadyState(onStartRecognition = ::startRecognition)
                        }

                        is RecognitionStatus.NoMatch -> {
                            NoMatchState(
                                message = status.message,
                                onTryAgain = {
                                    startRecognition()
                                },
                            )
                        }

                        is RecognitionStatus.Error -> {
                            ErrorState(
                                message = status.message,
                                onTryAgain = {
                                    startRecognition()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyState(onStartRecognition: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    Color.Transparent,
                                ),
                        ),
                    ).clickable { onStartRecognition() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = stringResource(R.string.tap_to_recognize),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ListeningState(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Container large enough for scaled animation (200dp * 1.2 = 240dp)
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Outer pulsing ring
            Box(
                modifier =
                    Modifier
                        .size(200.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            )

            // Inner pulsing ring
            Box(
                modifier =
                    Modifier
                        .size(180.dp)
                        .scale(scale * 0.9f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            )

            // Main button
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = stringResource(R.string.listening),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        OutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ProcessingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotate")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                ),
            label = "rotation",
        )

        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush =
                                Brush.sweepGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.primary,
                                        ),
                                ),
                            shape = CircleShape,
                        ),
            )

            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = stringResource(R.string.processing),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SuccessState(
    result: RecognitionResult,
    navController: NavController,
    onSearch: (RecognitionResult) -> Unit,
    onPlaySong: (RecognitionResult) -> Unit,
    onTryAgain: () -> Unit,
    onClose: () -> Unit,
    onSaveToHistory: (RecognitionResult) -> Unit,
) {
    val context = LocalContext.current
    val fallbackColor = MaterialTheme.colorScheme.primary.toArgb()
    
    var paletteColorList by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    // Save to history and extract gradient colors when success is shown
    LaunchedEffect(result) {
        onSaveToHistory(result)
        
        // Extract gradient colors from album artwork
        withContext(Dispatchers.IO) {
            val imageUrl = result.coverArtHqUrl ?: result.coverArtUrl
            if (!imageUrl.isNullOrEmpty()) {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("recognition_gradient_${result.title}")
                    .build()
                
                val imgResult = runCatching { context.imageLoader.execute(request) }.getOrNull()
                if (imgResult != null) {
                    val bitmap = imgResult.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                        val extractedColors = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                        paletteColorList = extractedColors
                    }
                }
            }
        }
    }

    // Full screen with animated background
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedGradientBackground(
            colors = paletteColorList,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 28.dp,  // Status bar height (24dp) + small gap
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Transparent header bar with proper spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onClose() },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = stringResource(R.string.recognize_music),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                com.auralis.music.ui.component.IconButton(
                    onClick = { navController.navigate("recognition_history") },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = stringResource(R.string.recognition_history),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content centered vertically
            Spacer(modifier = Modifier.weight(1f))
            
            // Larger Album Art (280dp like Echo Music)
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(result.coverArtHqUrl ?: result.coverArtUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title & Artist with better hierarchy
            Text(
                text = result.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = result.artist,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Display: Genre, Release Date, Label
            val metadataItems = mutableListOf<String>()
            result.genre?.let { if (it.isNotEmpty()) metadataItems.add(it) }
            result.releaseDate?.let { if (it.isNotEmpty()) metadataItems.add(it) }
            
            if (metadataItems.isNotEmpty()) {
                Text(
                    text = metadataItems.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            result.label?.let { label ->
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Horizontal Buttons Row: Search and Play (Echo Music style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Button - Navigate to search
                OutlinedButton(
                    onClick = {
                        onSearch(result)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search")
                }
                
                // Play Button - Play music directly
                Button(
                    onClick = {
                        onPlaySong(result)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF1E1E1E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
            }
        }
    }
}

@Composable
private fun NoMatchState(
    message: String,
    onTryAgain: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Text(
            text = stringResource(R.string.no_match_found),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Button(onClick = onTryAgain) {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.try_again))
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onTryAgain: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.error),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Text(
            text = stringResource(R.string.recognition_error),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Button(onClick = onTryAgain) {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.try_again))
        }
    }
}
