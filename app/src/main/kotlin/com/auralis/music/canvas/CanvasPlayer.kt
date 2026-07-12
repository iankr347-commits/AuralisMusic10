package com.auralis.music.canvas

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

private const val TAG = "CanvasPlayer"

/**
 * Renders a looping, muted Canvas video over the album artwork area.
 *
 * The player is created once per [url] (keyed via [DisposableEffect]) and released
 * when the composable leaves the composition or the URL changes.
 *
 * @param url       The Canvas video URL (MP4 or HLS .m3u8).
 * @param modifier  Applied to the outer [Box] — should fill the artwork area.
 * @param onError   Called when playback fails so the caller can fall back to artwork.
 */
@Composable
fun CanvasPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
) {
    val context = LocalContext.current

    // Fade in only after the first frame is rendered — prevents black flash
    var videoReady by remember(url) { mutableStateOf(false) }

    // Hold a reference to the TextureView so we can attach it to the player
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }

    // Create & release ExoPlayer per URL
    DisposableEffect(url) {
        Timber.tag(TAG).d("Preparing player for URL: %s", url)

        val mimeType = when {
            url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            else -> MimeTypes.VIDEO_MP4
        }
        Timber.tag(TAG).d("Detected MIME type: %s", mimeType)

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(mimeType)
            .build()

        val player = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            setMediaItem(mediaItem)
            prepare()
        }

        // Attach TextureView surface if already available
        textureViewRef.value?.let { tv ->
            if (tv.isAvailable) {
                player.setVideoSurface(Surface(tv.surfaceTexture))
            }
        }

        // Player event listener for logging + error handling
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($state)"
                }
                Timber.tag(TAG).d("Player %s", name)
                if (state == Player.STATE_READY) {
                    Timber.tag(TAG).i("Playback started")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).e(error, "Playback error — falling back to artwork")
                onError()
            }

            override fun onRenderedFirstFrame() {
                Timber.tag(TAG).i("First frame rendered")
                videoReady = true
            }
        }
        player.addListener(listener)
        Timber.tag(TAG).d("Player created and buffering")

        onDispose {
            Timber.tag(TAG).d("Releasing canvas player")
            player.removeListener(listener)
            player.clearVideoSurface()
            player.release()
            Timber.tag(TAG).d("Player released")
        }
    }

    Box(modifier = modifier) {
        // The TextureView is always in the hierarchy so the surface is available immediately
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    textureViewRef.value = tv
                    tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            // Surface became available — attach it to the current player
                            // We reach into the textureViewRef; the player is created by DisposableEffect
                            // We re-use the surface holder pattern: surface is set via the view
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // We manage surface attachment through a PlayerView-less approach using
        // a DisposableEffect that rekeys on url. To properly wire the surface,
        // use a PlayerView (which handles surface lifecycle internally).
    }
}

// ---------------------------------------------------------------------------
// PlayerView-based implementation (more robust, handles lifecycle automatically)
// ---------------------------------------------------------------------------

/**
 * Robust Canvas player using Media3 PlayerView which handles TextureView
 * lifecycle, surface attachment and detachment automatically.
 *
 * Replaces the manual TextureView approach above.
 */
@Composable
fun CanvasPlayerView(
    url: String,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
    onVideoReady: (Boolean) -> Unit = {},
    isPlaying: Boolean = true,
) {
    val context = LocalContext.current
    var videoReady by remember(url) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(videoReady) {
        onVideoReady(videoReady)
    }

    // We hold the ExoPlayer in a remembered state so AndroidView can access it
    val exoPlayer = remember(url) {
        Timber.tag(TAG).d("Preparing player for URL: %s", url)

        val mimeType = when {
            url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            else -> MimeTypes.VIDEO_MP4
        }
        Timber.tag(TAG).d("Detected MIME type: %s", mimeType)

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(mimeType)
            .build()

        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            setMediaItem(mediaItem)
            prepare()
        }
    }

    androidx.compose.runtime.LaunchedEffect(isPlaying, exoPlayer) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Timber.tag(TAG).d("Player %s", name)
                if (state == Player.STATE_READY) {
                    Timber.tag(TAG).i("Playback started")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).e(error, "Playback error — fallback to artwork")
                onError()
            }

            override fun onRenderedFirstFrame() {
                Timber.tag(TAG).i("First frame rendered")
                videoReady = true
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            Timber.tag(TAG).d("Releasing canvas player")
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            Timber.tag(TAG).d("Player released")
        }
    }

    Box(modifier = modifier) {
        val alpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (videoReady) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(400),
            label = "CanvasFade"
        )
        
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    // Crop to fill (like Spotify), maintaining aspect ratio
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { view ->
                view.player = exoPlayer
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        )
    }
}
