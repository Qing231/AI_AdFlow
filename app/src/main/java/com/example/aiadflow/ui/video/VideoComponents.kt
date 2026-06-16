package com.example.aiadflow.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.ui.media.AdVideoPlayerCard
import com.example.aiadflow.ui.media.RetryImageLoader

@Composable
internal fun AdVideoSurface(
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
    AdVideoPlayerCard(
        ad = ad,
        coverLoader = coverLoader,
        videoUrl = videoUrl,
        isPlayerVisible = isPlayerVisible,
        isPlaying = isPlaying,
        isMuted = isMuted,
        playbackPositionMs = playbackPositionMs,
        modifier = modifier,
        badgeLabel = badgeLabel,
        showVideoPill = showVideoPill,
        onPlayerVisibleChange = onPlayerVisibleChange,
        onPlayingChange = onPlayingChange,
        onMutedChange = onMutedChange,
        onPositionChange = onPositionChange
    )
}
