package com.example.aiadflow.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.ui.common.AdMediaSpec
import com.example.aiadflow.ui.common.channelLabelFor
import com.example.aiadflow.ui.common.mediaSpecFor
import com.example.aiadflow.ui.interaction.AdActionRow
import com.example.aiadflow.ui.media.AsyncAdImage
import com.example.aiadflow.ui.media.mediaCacheKeyFor
import com.example.aiadflow.ui.media.mediaUrlFor
import com.example.aiadflow.ui.media.rememberRetryImageLoader
import com.example.aiadflow.ui.media.videoStreamUrlFor
import com.example.aiadflow.ui.summary.AiSummaryText
import com.example.aiadflow.ui.tag.TagRow
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing
import com.example.aiadflow.ui.video.AdVideoSurface

@Composable
internal fun AdCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    selectedTag: String?,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewClick: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val mediaSpec = mediaSpecFor(ad.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onViewClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (ad.type) {
            AdType.SmallImage -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Medium),
                    verticalAlignment = Alignment.Top
                ) {
                    AdMediaBlock(
                        ad = ad,
                        mediaSpec = mediaSpec,
                        modifier = Modifier
                            .width(AppSpacing.SmallImageMediaWidth)
                            .height(AppSpacing.CompactMediaHeight)
                    )
                    AdSummaryContent(
                        ad = ad,
                        modifier = Modifier.weight(1f),
                        showChannelInline = true,
                        selectedTag = selectedTag,
                        onTagClick = onTagClick
                    )
                }
            }
            AdType.ImageText -> {
                AdMediaBlock(
                    ad = ad,
                    mediaSpec = mediaSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaSpec.height)
                )
                AdSummaryContent(
                    ad = ad,
                    selectedTag = selectedTag,
                    onTagClick = onTagClick
                )
            }
            AdType.Video -> {
                AdSummaryHeader(ad = ad)
                AdMediaBlock(
                    ad = ad,
                    mediaSpec = mediaSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaSpec.height)
                )
                AiSummaryText(summary = ad.summary)
                TagRow(
                    tags = ad.tags,
                    selectedTag = selectedTag,
                    onTagClick = onTagClick
                )
            }
            AdType.LargeImage -> {
                AdMediaBlock(
                    ad = ad,
                    mediaSpec = mediaSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaSpec.height)
                )
                AdSummaryContent(
                    ad = ad,
                    titleFirst = true,
                    selectedTag = selectedTag,
                    onTagClick = onTagClick
                )
            }
        }
        AdActionRow(
            liked = liked,
            collected = collected,
            onLikeClick = onLikeClick,
            onCollectClick = onCollectClick,
            onShareClick = onShareClick,
            onViewClick = onViewClick
        )
    }
}

@Composable
internal fun AdMediaBlock(
    ad: AdItem,
    mediaSpec: AdMediaSpec,
    modifier: Modifier = Modifier
) {
    var isVideoPlaying by remember(ad.id) { mutableStateOf(false) }
    var isVideoMuted by remember(ad.id) { mutableStateOf(false) }
    var isPlayerVisible by remember(ad.id) { mutableStateOf(false) }
    var playbackPositionMs by remember(ad.id) { mutableStateOf(0L) }
    val imageLoader = rememberRetryImageLoader(
        data = mediaUrlFor(ad),
        cacheKey = mediaCacheKeyFor(ad)
    )

    if (ad.type == AdType.Video) {
        AdVideoSurface(
            ad = ad,
            coverLoader = imageLoader,
            videoUrl = videoStreamUrlFor(ad),
            isPlayerVisible = isPlayerVisible,
            isPlaying = isVideoPlaying,
            isMuted = isVideoMuted,
            playbackPositionMs = playbackPositionMs,
            modifier = modifier,
            onPlayerVisibleChange = { isPlayerVisible = it },
            onPlayingChange = { isVideoPlaying = it },
            onMutedChange = { isVideoMuted = it },
            onPositionChange = { playbackPositionMs = it }
        )
    } else {
        Box(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
            AsyncAdImage(
                loader = imageLoader,
                contentDescription = ad.mediaLabel,
                modifier = Modifier.fillMaxSize(),
                accentColor = mediaSpec.color,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
            Text(
                text = channelLabelFor(ad.channel),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacing.Medium),
                color = AppColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun AdSummaryContent(
    ad: AdItem,
    modifier: Modifier = Modifier,
    titleFirst: Boolean = false,
    showChannelInline: Boolean = false,
    selectedTag: String?,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        if (titleFirst) {
            Text(
                text = ad.title,
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            AdSummaryHeader(ad = ad, showTitle = false, showChannelInline = true)
        } else {
            AdSummaryHeader(ad = ad, showChannelInline = showChannelInline)
        }
        AiSummaryText(summary = ad.summary)
        TagRow(
            tags = ad.tags,
            selectedTag = selectedTag,
            onTagClick = onTagClick
        )
    }
}

@Composable
private fun AdSummaryHeader(
    ad: AdItem,
    showTitle: Boolean = true,
    showChannelInline: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ad.brandName,
                color = Color(0xFF71839A),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showTitle) {
                Text(
                    text = ad.title,
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showChannelInline) {
            Text(
                text = channelLabelFor(ad.channel),
                modifier = Modifier
                    .clip(AppRadius.Full)
                    .background(Color(0xFFEFF5FF))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
