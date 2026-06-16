package com.example.aiadflow.ui.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.aiadflow.R
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing
import kotlinx.coroutines.delay

enum class ImageLoadState {
    Loading,
    Success,
    Error
}

enum class VideoLoadState {
    Idle,
    Buffering,
    Ready,
    Ended,
    Error
}

@Stable
class RetryImageLoader internal constructor(
    val data: Any?,
    val cacheKey: String,
    private val maxRetries: Int = 3
) {
    var retryToken by mutableIntStateOf(0)
        private set
    var failureCount by mutableIntStateOf(0)
        private set
    var loadState by mutableStateOf(ImageLoadState.Loading)
        private set

    val canRetry: Boolean
        get() = failureCount <= maxRetries

    fun markLoading() {
        loadState = ImageLoadState.Loading
    }

    fun markSuccess() {
        loadState = ImageLoadState.Success
        failureCount = 0
    }

    fun markError() {
        loadState = ImageLoadState.Error
        failureCount += 1
    }

    fun retry() {
        if (!canRetry) {
            return
        }

        retryToken += 1
        loadState = ImageLoadState.Loading
    }
}

object AdImageLoader {
    @Volatile
    private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return loader ?: synchronized(this) {
            loader ?: ImageLoader.Builder(context.applicationContext)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("ad_media_cache"))
                        .maxSizeBytes(128L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()
                .also { loader = it }
        }
    }

    fun memoryStats(context: Context): String {
        val memoryCache = get(context).memoryCache ?: return "memory cache unavailable"
        return "${memoryCache.size} / ${memoryCache.maxSize}"
    }
}

@OptIn(UnstableApi::class)
object AdVideoCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun dataSourceFactory(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext)
        return CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun get(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: SimpleCache(
                context.applicationContext.cacheDir.resolve("ad_video_cache"),
                LeastRecentlyUsedCacheEvictor(256L * 1024 * 1024),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { cache = it }
        }
    }
}

@Composable
fun rememberRetryImageLoader(
    data: Any?,
    cacheKey: String,
    maxRetries: Int = 3
): RetryImageLoader {
    return remember(cacheKey, data) {
        RetryImageLoader(
            data = data,
            cacheKey = cacheKey,
            maxRetries = maxRetries
        )
    }
}

@Composable
fun AsyncAdImage(
    loader: RetryImageLoader,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accentColor: Color = AppColors.Primary,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (LocalInspectionMode.current) {
        PreviewMediaSurface(
            label = contentDescription ?: "Media",
            modifier = modifier,
            accentColor = accentColor
        )
        return
    }

    val context = LocalContext.current
    val currentLoader by rememberUpdatedState(loader)
    val request = remember(loader.data, loader.cacheKey, loader.retryToken) {
        ImageRequest.Builder(context)
            .data(loader.data)
            .memoryCacheKey(loader.cacheKey)
            .diskCacheKey(loader.cacheKey)
            .placeholderMemoryCacheKey(loader.cacheKey)
            .crossfade(220)
            .allowHardware(true)
            .precision(Precision.INEXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    LaunchedEffect(loader.loadState, loader.retryToken) {
        if (loader.loadState == ImageLoadState.Error && loader.canRetry) {
            delay(2_000L)
            currentLoader.retry()
        }
    }

    SubcomposeAsyncImage(
        model = request,
        imageLoader = AdImageLoader.get(context),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> {
                LaunchedEffect(loader.cacheKey, loader.retryToken) {
                    loader.markSuccess()
                }
                SubcomposeAsyncImageContent()
            }
            is AsyncImagePainter.State.Error -> {
                LaunchedEffect(loader.cacheKey, loader.retryToken) {
                    loader.markError()
                }
                ImageErrorState(
                    canRetry = loader.canRetry,
                    onRetryClick = loader::retry
                )
            }
            else -> {
                LaunchedEffect(loader.cacheKey, loader.retryToken) {
                    loader.markLoading()
                }
                SkeletonPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    accentColor = accentColor
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AdVideoPlayerCard(
    ad: AdItem,
    coverLoader: RetryImageLoader,
    videoUrl: String?,
    isPlayerVisible: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    playbackPositionMs: Long,
    modifier: Modifier = Modifier,
    badgeLabel: String = "Video",
    showVideoPill: Boolean = false,
    onPlayerVisibleChange: (Boolean) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onPositionChange: (Long) -> Unit
) {
    if (LocalInspectionMode.current) {
        PreviewVideoPlayerCard(
            ad = ad,
            isPlaying = isPlaying,
            isMuted = isMuted,
            modifier = modifier,
            onPlayClick = {
                onPlayerVisibleChange(true)
                onPlayingChange(!isPlaying)
            },
            onMuteClick = { onMutedChange(!isMuted) }
        )
        return
    }

    val context = LocalContext.current
    var loadState by remember(videoUrl) {
        mutableStateOf(if (videoUrl.isNullOrBlank()) VideoLoadState.Idle else VideoLoadState.Buffering)
    }
    var durationMs by remember(videoUrl) { mutableStateOf(C.TIME_UNSET) }
    var playerError by remember(videoUrl) { mutableStateOf<String?>(null) }
    var retryCount by remember(videoUrl) { mutableIntStateOf(0) }
    var volumeLevel by remember(videoUrl) { mutableFloatStateOf(1f) }
    val player = remember(videoUrl) {
        if (videoUrl.isNullOrBlank()) {
            null
        } else {
            val mediaSourceFactory = DefaultMediaSourceFactory(AdVideoCache.dataSourceFactory(context))
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setAudioAttributes(VideoAudioAttributes, true)
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    seekTo(playbackPositionMs.coerceAtLeast(0L))
                    playWhenReady = false
                    volume = if (isMuted) 0f else volumeLevel.coerceIn(0f, 1f)
                    prepare()
                }
        }
    }

    DisposableEffect(player) {
        val currentPlayer = player
        if (currentPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    loadState = when (playbackState) {
                        Player.STATE_BUFFERING -> VideoLoadState.Buffering
                        Player.STATE_READY -> VideoLoadState.Ready
                        Player.STATE_ENDED -> VideoLoadState.Ended
                        else -> loadState
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        onPlayingChange(false)
                    }
                    val safeDuration = currentPlayer.duration
                    if (safeDuration > 0 && safeDuration != C.TIME_UNSET) {
                        durationMs = safeDuration
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    loadState = VideoLoadState.Error
                    playerError = error.errorCodeName
                }
            }
            currentPlayer.addListener(listener)
            onDispose {
                onPositionChange(currentPlayer.currentPosition.coerceAtLeast(0L))
                currentPlayer.removeListener(listener)
                currentPlayer.release()
            }
        }
    }

    LaunchedEffect(player, isPlaying) {
        player?.let { currentPlayer ->
            currentPlayer.playWhenReady = isPlaying
            if (isPlaying) {
                onPlayerVisibleChange(true)
                currentPlayer.play()
            } else {
                currentPlayer.pause()
            }
        }
    }

    LaunchedEffect(player, isMuted, volumeLevel) {
        player?.volume = if (isMuted) 0f else volumeLevel.coerceIn(0f, 1f)
    }

    LaunchedEffect(player, isPlayerVisible) {
        val currentPlayer = player ?: return@LaunchedEffect
        while (isPlayerVisible) {
            val position = currentPlayer.currentPosition.coerceAtLeast(0L)
            val safeDuration = currentPlayer.duration
            if (safeDuration > 0 && safeDuration != C.TIME_UNSET) {
                durationMs = safeDuration
            }
            onPositionChange(position)
            delay(500L)
        }
    }

    LaunchedEffect(loadState, retryCount) {
        val currentPlayer = player
        if (loadState == VideoLoadState.Error && currentPlayer != null && retryCount < 3) {
            delay(2_000L)
            retryCount += 1
            playerError = null
            loadState = VideoLoadState.Buffering
            currentPlayer.prepare()
            if (isPlaying) {
                currentPlayer.play()
            }
        }
    }

    fun seekToFraction(fraction: Float) {
        val safeDuration = durationMs
        if (player == null || safeDuration <= 0L || safeDuration == C.TIME_UNSET) {
            return
        }

        val targetPosition = (safeDuration.toDouble() * fraction.coerceIn(0f, 1f))
            .toLong()
            .coerceIn(0L, safeDuration)
        player.seekTo(targetPosition)
        onPlayerVisibleChange(true)
        onPositionChange(targetPosition)
        if (loadState == VideoLoadState.Ended && targetPosition < safeDuration) {
            loadState = VideoLoadState.Ready
        }
    }

    fun changeVolume(fraction: Float) {
        val nextVolume = fraction.coerceIn(0f, 1f)
        volumeLevel = nextVolume
        onMutedChange(nextVolume <= 0.01f)
    }

    fun toggleMuted() {
        if (isMuted || volumeLevel <= 0.01f) {
            if (volumeLevel <= 0.01f) {
                volumeLevel = 1f
            }
            onMutedChange(false)
        } else {
            onMutedChange(true)
        }
    }

    val effectiveVolume = if (isMuted) 0f else volumeLevel.coerceIn(0f, 1f)
    val canSeek = durationMs > 0L && durationMs != C.TIME_UNSET && player != null

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEEF4FF),
                        Color(0xFFDDE7FF),
                        Color(0xFFE9DEFF)
                    )
                )
            )
    ) {
        if (player == null) {
            AsyncAdImage(
                loader = coverLoader,
                contentDescription = ad.mediaLabel,
                modifier = Modifier.fillMaxSize(),
                accentColor = Color(0xFF111827)
            )
        } else {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        this.player = player
                    }
                },
                update = { playerView ->
                    playerView.player = player
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (loadState == VideoLoadState.Buffering && isPlaying) {
            SkeletonPlaceholder(
                modifier = Modifier.fillMaxSize(),
                accentColor = Color(0xFF111827)
            )
        }

        if (loadState == VideoLoadState.Error) {
            VideoErrorState(
                message = playerError ?: "stream error",
                canRetry = retryCount < 3,
                onRetryClick = {
                    retryCount += 1
                    playerError = null
                    loadState = VideoLoadState.Buffering
                    player?.prepare()
                    if (isPlaying) {
                        player?.play()
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x332563EB),
                            Color.Transparent,
                            Color(0xAA111827)
                        )
                    )
                )
        )
        VideoBadge(
            label = if (isPlaying) "Playing" else badgeLabel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AppSpacing.Medium)
        )
        if (showVideoPill) {
            VideoBadge(
                label = "video",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppSpacing.Medium)
            )
        }
        VideoPlayButton(
            isPlaying = isPlaying && loadState != VideoLoadState.Ended,
            onClick = {
                if (videoUrl.isNullOrBlank()) {
                    return@VideoPlayButton
                }
                if (loadState == VideoLoadState.Ended) {
                    player?.seekTo(0L)
                    onPositionChange(0L)
                    onPlayingChange(true)
                } else {
                    onPlayingChange(!isPlaying)
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .size(AppSpacing.PlayButton)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.Medium,
                    end = AppSpacing.VideoMuteButton + AppSpacing.Medium + AppSpacing.Small,
                    bottom = AppSpacing.Medium
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            val progress = if (durationMs > 0 && durationMs != C.TIME_UNSET) {
                playbackPositionMs.toFloat() / durationMs.toFloat()
            } else {
                0f
            }
            VideoProgressBar(
                progress = progress,
                enabled = canSeek,
                onProgressChange = { seekToFraction(it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
            ) {
                Text(
                    text = "${formatVideoTime(playbackPositionMs)} / ${formatVideoTime(durationMs)}",
                    modifier = Modifier.weight(1f),
                    color = AppColors.OnPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                VideoVolumeControl(
                    volume = effectiveVolume,
                    enabled = player != null,
                    onVolumeChange = { changeVolume(it) },
                    modifier = Modifier.weight(0.82f)
                )
            }
        }
        VideoMuteButton(
            isMuted = isMuted || volumeLevel <= 0.01f,
            onClick = { toggleMuted() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.Medium)
                .size(AppSpacing.VideoMuteButton)
        )
    }
}

@Composable
fun VideoCoverCard(
    ad: AdItem,
    loader: RetryImageLoader,
    isPlaying: Boolean,
    isMuted: Boolean,
    durationText: String,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onMuteClick: () -> Unit
) {
    var previewVolume by remember(isMuted) { mutableFloatStateOf(if (isMuted) 0f else 1f) }
    Box(
        modifier = modifier
            .clip(AppRadius.Medium)
            .background(Color(0xFF111827))
    ) {
        AsyncAdImage(
            loader = loader,
            contentDescription = ad.mediaLabel,
            modifier = Modifier.fillMaxSize(),
            accentColor = Color(0xFF111827)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )
        VideoBadge(
            label = "瑙嗛",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AppSpacing.Medium)
        )
        VideoPlayButton(
            isPlaying = isPlaying,
            onClick = onPlayClick,
            modifier = Modifier
                .align(Alignment.Center)
                .size(AppSpacing.PlayButton)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.Medium,
                    end = AppSpacing.VideoMuteButton + AppSpacing.Medium + AppSpacing.Small,
                    bottom = AppSpacing.Medium
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            VideoProgressBar(progress = if (isPlaying) 0.42f else 0.08f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
            ) {
                Text(
                    text = if (isPlaying) "00:12 / $durationText" else "00:00 / $durationText",
                    modifier = Modifier.weight(1f),
                    color = AppColors.OnPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                VideoVolumeControl(
                    volume = previewVolume,
                    enabled = true,
                    onVolumeChange = {
                        previewVolume = it.coerceIn(0f, 1f)
                    },
                    modifier = Modifier.weight(0.82f)
                )
            }
        }
        VideoMuteButton(
            isMuted = isMuted || previewVolume <= 0.01f,
            onClick = {
                if (previewVolume <= 0.01f) {
                    previewVolume = 1f
                }
                onMuteClick()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.Medium)
                .size(AppSpacing.VideoMuteButton)
        )
    }
}

@Composable
fun SkeletonPlaceholder(
    modifier: Modifier = Modifier,
    accentColor: Color = AppColors.MediaPlaceholder
) {
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonShimmerOffset"
    )
    val base = accentColor.copy(alpha = 0.22f)
    val highlight = Color.White.copy(alpha = 0.62f)

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(x = shimmerOffset * 420f, y = 0f),
                end = Offset(x = (shimmerOffset + 1f) * 420f, y = 420f)
            )
        )
    )
}

@Composable
private fun ImageErrorState(
    canRetry: Boolean,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FB))
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            drawCircle(
                color = Color(0xFFE5ECF6),
                radius = size.minDimension / 2f
            )
            drawLine(
                color = AppColors.TextSecondary,
                start = Offset(size.width * 0.28f, size.height * 0.32f),
                end = Offset(size.width * 0.72f, size.height * 0.72f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = AppColors.TextSecondary,
                start = Offset(size.width * 0.72f, size.height * 0.32f),
                end = Offset(size.width * 0.28f, size.height * 0.72f),
                strokeWidth = 3.dp.toPx()
            )
        }
        Text(
            text = "Load failed. Tap to retry.",
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = AppSpacing.Small)
        )
        AnimatedVisibility(visible = canRetry) {
            Box(
                modifier = Modifier
                    .padding(top = AppSpacing.Small)
                    .clip(AppRadius.Full)
                    .background(AppColors.Surface)
                    .border(
                        width = AppSpacing.TagBorderWidth,
                        color = AppColors.MediaPlaceholder,
                        shape = AppRadius.Full
                    )
                    .clickable(onClick = onRetryClick)
                    .padding(horizontal = AppSpacing.Medium, vertical = AppSpacing.TagVertical)
            ) {
                Text(
                    text = "閲嶈瘯",
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        AnimatedVisibility(visible = !canRetry) {
            Text(
                text = "Try again later.",
                color = AppColors.TextMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = AppSpacing.Small)
            )
        }
    }
}

@Composable
private fun VideoErrorState(
    message: String,
    canRetry: Boolean,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(enabled = canRetry, onClick = onRetryClick)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(38.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = size.minDimension / 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.28f, size.height * 0.30f),
                end = Offset(size.width * 0.72f, size.height * 0.72f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.72f, size.height * 0.30f),
                end = Offset(size.width * 0.28f, size.height * 0.72f),
                strokeWidth = 3.dp.toPx()
            )
        }
        Text(
            text = "Video failed to load. Tap to retry.",
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = AppSpacing.Small)
        )
        Text(
            text = if (canRetry) message else "Retry limit reached.",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PreviewMediaSurface(
    label: String,
    modifier: Modifier,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.86f),
                        Color(0xFF111827)
                    )
                )
            )
            .padding(AppSpacing.Medium)
    ) {
        Text(
            text = label,
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreviewVideoPlayerCard(
    ad: AdItem,
    isPlaying: Boolean,
    isMuted: Boolean,
    modifier: Modifier,
    onPlayClick: () -> Unit,
    onMuteClick: () -> Unit
) {
    var previewVolume by remember(isMuted) { mutableFloatStateOf(if (isMuted) 0f else 1f) }
    Box(
        modifier = modifier
            .clip(AppRadius.Medium)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1D4ED8),
                        Color(0xFF111827)
                    )
                )
            )
    ) {
        Text(
            text = ad.mediaLabel,
            modifier = Modifier.padding(AppSpacing.Medium),
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        VideoBadge(
            label = if (isPlaying) "Playing" else "Video",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppSpacing.Medium)
        )
        VideoPlayButton(
            isPlaying = isPlaying,
            onClick = onPlayClick,
            modifier = Modifier
                .align(Alignment.Center)
                .size(AppSpacing.PlayButton)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.Medium,
                    end = AppSpacing.VideoMuteButton + AppSpacing.Medium + AppSpacing.Small,
                    bottom = AppSpacing.Medium
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            VideoProgressBar(progress = if (isPlaying) 0.38f else 0.08f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
            ) {
                Text(
                    text = if (isPlaying) "00:12 / 00:30" else "00:00 / 00:30",
                    modifier = Modifier.weight(1f),
                    color = AppColors.OnPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                VideoVolumeControl(
                    volume = previewVolume,
                    enabled = true,
                    onVolumeChange = {
                        previewVolume = it.coerceIn(0f, 1f)
                    },
                    modifier = Modifier.weight(0.82f)
                )
            }
        }
        VideoMuteButton(
            isMuted = isMuted || previewVolume <= 0.01f,
            onClick = {
                if (previewVolume <= 0.01f) {
                    previewVolume = 1f
                }
                onMuteClick()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.Medium)
                .size(AppSpacing.VideoMuteButton)
        )
    }
}

@Composable
private fun VideoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    trackHeight: Dp = AppSpacing.VideoProgressHeight,
    thumbSize: Dp = 10.dp,
    activeColor: Color = Color.White.copy(alpha = 0.92f),
    inactiveColor: Color = Color.White.copy(alpha = 0.32f),
    thumbColor: Color = Color.White,
    onProgressChange: (Float) -> Unit = {}
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val touchHeight = if (enabled) 20.dp else trackHeight

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(touchHeight)
            .videoDragInput(
                enabled = enabled,
                onProgressChange = onProgressChange
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(inactiveColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(safeProgress)
                .height(trackHeight)
                .clip(CircleShape)
                .background(activeColor)
        )
        if (enabled) {
            val travel = if (maxWidth > thumbSize) maxWidth - thumbSize else 0.dp
            Box(
                modifier = Modifier
                    .offset(x = travel * safeProgress)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            )
        }
    }
}

@Composable
private fun VideoVolumeControl(
    volume: Float,
    enabled: Boolean,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VideoVolumeLevelIcon(
            volume = volume,
            modifier = Modifier.size(15.dp)
        )
        VideoProgressBar(
            progress = volume,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            trackHeight = 3.dp,
            thumbSize = 8.dp,
            activeColor = Color.White.copy(alpha = 0.86f),
            inactiveColor = Color.White.copy(alpha = 0.22f),
            thumbColor = Color.White.copy(alpha = 0.96f),
            onProgressChange = onVolumeChange
        )
    }
}

@Composable
private fun VideoVolumeLevelIcon(
    volume: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val speaker = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.38f)
            lineTo(size.width * 0.28f, size.height * 0.38f)
            lineTo(size.width * 0.50f, size.height * 0.20f)
            lineTo(size.width * 0.50f, size.height * 0.80f)
            lineTo(size.width * 0.28f, size.height * 0.62f)
            lineTo(size.width * 0.08f, size.height * 0.62f)
            close()
        }
        val iconColor = Color.White.copy(alpha = 0.92f)
        val strokeWidth = size.width * 0.08f
        drawPath(path = speaker, color = iconColor)

        if (volume <= 0.01f) {
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.66f, size.height * 0.34f),
                end = Offset(size.width * 0.92f, size.height * 0.66f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.92f, size.height * 0.34f),
                end = Offset(size.width * 0.66f, size.height * 0.66f),
                strokeWidth = strokeWidth
            )
            return@Canvas
        }

        drawLine(
            color = iconColor,
            start = Offset(size.width * 0.64f, size.height * 0.40f),
            end = Offset(size.width * 0.78f, size.height * 0.50f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = iconColor,
            start = Offset(size.width * 0.78f, size.height * 0.50f),
            end = Offset(size.width * 0.64f, size.height * 0.60f),
            strokeWidth = strokeWidth
        )
        if (volume > 0.55f) {
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.78f, size.height * 0.28f),
                end = Offset(size.width * 0.94f, size.height * 0.50f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.94f, size.height * 0.50f),
                end = Offset(size.width * 0.78f, size.height * 0.72f),
                strokeWidth = strokeWidth
            )
        }
    }
}

private fun Modifier.videoDragInput(
    enabled: Boolean,
    onProgressChange: (Float) -> Unit
): Modifier {
    if (!enabled) {
        return this
    }

    return pointerInput(onProgressChange) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val trackWidth = size.width.toFloat().coerceAtLeast(1f)

            fun updateProgress(x: Float) {
                onProgressChange((x / trackWidth).coerceIn(0f, 1f))
            }

            updateProgress(down.position.x)
            down.consume()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    break
                }
                updateProgress(change.position.x)
                change.consume()
            }
        }
    }
}

@Composable
private fun VideoBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppRadius.Full)
            .background(Color(0xCC111827))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun VideoMuteButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x66111827))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(AppSpacing.VideoMuteIcon)) {
            val speaker = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.38f)
                lineTo(size.width * 0.30f, size.height * 0.38f)
                lineTo(size.width * 0.52f, size.height * 0.20f)
                lineTo(size.width * 0.52f, size.height * 0.80f)
                lineTo(size.width * 0.30f, size.height * 0.62f)
                lineTo(size.width * 0.12f, size.height * 0.62f)
                close()
            }
            drawPath(path = speaker, color = Color.White)

            if (isMuted) {
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.66f, size.height * 0.34f),
                    end = Offset(size.width * 0.90f, size.height * 0.66f),
                    strokeWidth = size.width * 0.09f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.90f, size.height * 0.34f),
                    end = Offset(size.width * 0.66f, size.height * 0.66f),
                    strokeWidth = size.width * 0.09f
                )
            } else {
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.66f, size.height * 0.38f),
                    end = Offset(size.width * 0.82f, size.height * 0.50f),
                    strokeWidth = size.width * 0.08f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.82f, size.height * 0.50f),
                    end = Offset(size.width * 0.66f, size.height * 0.62f),
                    strokeWidth = size.width * 0.08f
                )
            }
        }
    }
}

@Composable
private fun VideoPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.86f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(AppSpacing.VideoPlayIcon)) {
            if (isPlaying) {
                val barWidth = size.width * 0.22f
                val gap = size.width * 0.16f
                val top = size.height * 0.22f
                val bottom = size.height * 0.78f
                val leftStart = (size.width - barWidth * 2 - gap) / 2f
                drawRect(
                    color = AppColors.Primary,
                    topLeft = Offset(leftStart, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, bottom - top)
                )
                drawRect(
                    color = AppColors.Primary,
                    topLeft = Offset(leftStart + barWidth + gap, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, bottom - top)
                )
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.36f, size.height * 0.22f)
                    lineTo(size.width * 0.36f, size.height * 0.78f)
                    lineTo(size.width * 0.82f, size.height * 0.5f)
                    close()
                }
                drawPath(path = path, color = AppColors.Primary)
            }
        }
    }
}

fun mediaUrlFor(ad: AdItem): String {
    return ad.coverUrl?.takeIf { it.isUsableMediaUrl() }
        ?: LocalFallbackImageUri
}

fun mediaCacheKeyFor(ad: AdItem): String {
    return "ad-media-${ad.id}-${mediaUrlFor(ad)}"
}

fun videoStreamUrlFor(ad: AdItem): String? {
    if (ad.type != AdType.Video) {
        return null
    }

    return ad.videoUrl?.takeIf { it.isUsableMediaUrl() }
        ?: LocalFallbackVideoUri
}

private fun String.isUsableMediaUrl(): Boolean {
    if (isBlank() || contains("cdn.example.com", ignoreCase = true)) {
        return false
    }

    return startsWith("android.resource://", ignoreCase = true) ||
        startsWith("file:///android_asset/", ignoreCase = true) ||
        startsWith("content://", ignoreCase = true) ||
        startsWith("file://", ignoreCase = true) ||
        startsWith("http://", ignoreCase = true) ||
        startsWith("https://", ignoreCase = true)
}

fun videoDurationFor(ad: AdItem): String {
    val seconds = 18 + ((ad.id * 7) % 28).toInt()
    return "00:${seconds.toString().padStart(2, '0')}"
}

fun preloadAdMedia(
    context: Context,
    ads: List<AdItem>
) {
    val imageLoader = AdImageLoader.get(context)
    ads.forEach { ad ->
        val request = ImageRequest.Builder(context)
            .data(mediaUrlFor(ad))
            .memoryCacheKey(mediaCacheKeyFor(ad))
            .diskCacheKey(mediaCacheKeyFor(ad))
            .precision(Precision.INEXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
        imageLoader.enqueue(request)
    }
}

private fun formatVideoTime(timeMs: Long): String {
    if (timeMs <= 0 || timeMs == C.TIME_UNSET) {
        return "00:00"
    }

    val totalSeconds = timeMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

private const val PackageName = "com.example.aiadflow"
private val VideoAudioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
    .build()
private val LocalFallbackImageUri = "android.resource://$PackageName/${R.mipmap.ic_launcher}"
private val LocalFallbackVideoUri = "android.resource://$PackageName/${R.raw.adv2}"
