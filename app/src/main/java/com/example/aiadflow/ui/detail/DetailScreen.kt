package com.example.aiadflow.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.card.AdMediaBlock
import com.example.aiadflow.ui.common.HomeBackgroundBrush
import com.example.aiadflow.ui.common.channelLabelFor
import com.example.aiadflow.ui.common.mediaSpecFor
import com.example.aiadflow.ui.interaction.DetailActionRow
import com.example.aiadflow.ui.media.mediaCacheKeyFor
import com.example.aiadflow.ui.media.mediaUrlFor
import com.example.aiadflow.ui.media.rememberRetryImageLoader
import com.example.aiadflow.ui.media.videoStreamUrlFor
import com.example.aiadflow.ui.summary.DetailSummarySection
import com.example.aiadflow.ui.tag.ReadOnlyTagRow
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing
import com.example.aiadflow.ui.video.AdVideoSurface

@Composable
internal fun AdDetailScreen(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onBackClick) {
                var totalDragX = 0f
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        val isRightSwipe = totalDragX > SwipeBackDistancePx
                        val isMostlyHorizontal = totalDragX > kotlin.math.abs(totalDragY) * SwipeBackHorizontalRatio
                        if (isRightSwipe && isMostlyHorizontal) {
                            onBackClick()
                        }
                    }
                )
            }
            .background(HomeBackgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
            item {
                DetailTopBar(onBackClick = onBackClick)
            }
            item {
                DetailMediaCard(ad = ad)
            }
            item {
                DetailInfoCard(
                    ad = ad,
                    liked = liked,
                    collected = collected,
                    onLikeClick = onLikeClick,
                    onCollectClick = onCollectClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}

private const val SwipeBackDistancePx = 120f
private const val SwipeBackHorizontalRatio = 1.6f

@Composable
private fun DetailTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = AppRadius.Full,
                    ambientColor = Color(0x120F2D5C),
                    spotColor = Color(0x120F2D5C)
                )
                .clip(AppRadius.Full)
                .background(Color.White)
                .clickable(onClick = onBackClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "返回",
                color = Color(0xFF60738D),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = "广告详情",
            color = Color(0xFF102033),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun DetailMediaCard(ad: AdItem) {
    val mediaSpec = mediaSpecFor(ad.type)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (ad.type == AdType.Video) 220.dp else mediaSpec.height)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x180F2D5C),
                spotColor = Color(0x180F2D5C)
            )
            .clip(RoundedCornerShape(26.dp))
    ) {
        if (ad.type == AdType.Video) {
            var isVideoPlaying by remember(ad.id) { mutableStateOf(true) }
            var isVideoMuted by remember(ad.id) { mutableStateOf(false) }
            var isPlayerVisible by remember(ad.id) { mutableStateOf(true) }
            var playbackPositionMs by remember(ad.id) { mutableStateOf(0L) }
            val imageLoader = rememberRetryImageLoader(
                data = mediaUrlFor(ad),
                cacheKey = mediaCacheKeyFor(ad)
            )
            AdVideoSurface(
                ad = ad,
                coverLoader = imageLoader,
                videoUrl = videoStreamUrlFor(ad),
                isPlayerVisible = isPlayerVisible,
                isPlaying = isVideoPlaying,
                isMuted = isVideoMuted,
                playbackPositionMs = playbackPositionMs,
                modifier = Modifier.fillMaxSize(),
                badgeLabel = "视频素材",
                showVideoPill = true,
                onPlayerVisibleChange = { isPlayerVisible = it },
                onPlayingChange = { isVideoPlaying = it },
                onMutedChange = { isVideoMuted = it },
                onPositionChange = { playbackPositionMs = it }
            )
        } else {
            AdMediaBlock(
                ad = ad,
                mediaSpec = mediaSpec,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DetailInfoCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ad.brandName,
                    color = Color(0xFF71839A),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = ad.title,
                    color = Color(0xFF102033),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DetailChannelPill(channel = ad.channel)
        }
        DetailMediaInfoBlock(ad = ad)
        DetailSummarySection(summary = ad.summary)
        ReadOnlyTagRow(tags = ad.tags)
        DetailActionRow(
            liked = liked,
            collected = collected,
            onLikeClick = onLikeClick,
            onCollectClick = onCollectClick,
            onShareClick = onShareClick
        )
    }
}

@Composable
private fun DetailChannelPill(channel: Channel) {
    Text(
        text = channelLabelFor(channel),
        modifier = Modifier
            .clip(AppRadius.Full)
            .background(Color(0xFFEFF5FF))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = AppColors.Primary,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun DetailMediaInfoBlock(ad: AdItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF3F7FC))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = ad.mediaLabel,
            color = AppColors.Primary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = if (ad.type == AdType.Video) {
                ad.videoUrl ?: "本地视频素材"
            } else {
                ad.coverUrl ?: "本地图片素材"
            },
            color = Color(0xFF60738D),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
