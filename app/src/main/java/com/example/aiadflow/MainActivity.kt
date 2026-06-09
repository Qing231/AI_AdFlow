package com.example.aiadflow

import android.os.Bundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.feed.AdFeedUiState
import com.example.aiadflow.ui.feed.AdFeedViewModel
import com.example.aiadflow.ui.theme.AIAdFlowTheme
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAdFlowTheme {
                val viewModel = remember { AdFeedViewModel() }
                val uiState by viewModel.uiState.collectAsState()
                var selectedAd by remember { mutableStateOf<AdItem?>(null) }

                AnimatedContent(
                    targetState = selectedAd,
                    label = "adDetailTransition",
                    transitionSpec = {
                        val goingToDetail = initialState == null && targetState != null
                        if (goingToDetail) {
                            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 4 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 3 } + fadeOut())
                        }
                    }
                ) { detailAd ->
                    if (detailAd == null) {
                        HomeScreen(
                            uiState = uiState,
                            onChannelSelected = viewModel::switchChannel,
                        onSearchChange = viewModel::updateSearchText,
                        onTagSelected = viewModel::selectTag,
                        onClearFilters = viewModel::clearFilters,
                        onRefresh = viewModel::refreshAds,
                        onLoadMore = viewModel::loadMoreAds,
                        onRetryLoadMore = viewModel::retryLoadMoreAds,
                        onAdClick = { ad ->
                            viewModel.trackAdClick(ad)
                            selectedAd = ad
                            }
                        )
                    } else {
                        AdDetailScreen(
                            ad = detailAd,
                            onBackClick = { selectedAd = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: AdFeedUiState,
    onChannelSelected: (Channel?) -> Unit,
    onSearchChange: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onAdClick: (AdItem) -> Unit
) {
    val likedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val collectedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(uiState.hasMoreAds, uiState.isLoadingMore, uiState.ads.size) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val totalItems = layoutInfo.totalItemsCount

            uiState.ads.isNotEmpty() &&
                uiState.hasMoreAds &&
                !uiState.isLoadingMore &&
                uiState.loadMoreErrorMessage == null &&
                totalItems > 0 &&
                lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.PageBackground
    ) {
        AdFeedRefreshContainer(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.PageHorizontal),
                contentPadding = PaddingValues(bottom = AppSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)
            ) {
            item(key = "status-bars-spacer") {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
            item(key = "header") {
                HeaderBar()
            }
            item(key = "channel-tabs") {
                ChannelTabs(
                    channels = uiState.channels,
                    selectedChannel = uiState.selectedChannel,
                    onChannelSelected = onChannelSelected
                )
            }
            item(key = "search-bar") {
                SearchBar(
                    query = uiState.searchText,
                    onQueryChange = onSearchChange
                )
            }
            if (uiState.hasActiveFilters()) {
                item(key = "active-filters") {
                    ActiveFiltersBar(
                        uiState = uiState,
                        onClearFilters = onClearFilters
                    )
                }
            }
            if (uiState.ads.isEmpty()) {
                item(key = "empty-feed") {
                    EmptyFeed()
                }
            } else {
                items(
                    items = uiState.ads,
                    key = { it.id }
                ) { ad ->
                    AdCard(
                        ad = ad,
                        liked = likedOverrides[ad.id] ?: ad.liked,
                        collected = collectedOverrides[ad.id] ?: ad.collected,
                        selectedTag = uiState.selectedTag,
                        onLikeClick = {
                            likedOverrides[ad.id] = !(likedOverrides[ad.id] ?: ad.liked)
                        },
                        onCollectClick = {
                            collectedOverrides[ad.id] = !(collectedOverrides[ad.id] ?: ad.collected)
                        },
                        onViewClick = {
                            onAdClick(ad)
                        },
                        onTagClick = onTagSelected
                    )
                }
                item(key = "load-more-footer") {
                    LoadMoreFooter(
                        isLoadingMore = uiState.isLoadingMore,
                        hasMoreAds = uiState.hasMoreAds,
                        errorMessage = uiState.loadMoreErrorMessage,
                        onRetryClick = onRetryLoadMore
                    )
                }
            }
        }
    }
}
}

@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    hasMoreAds: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!hasMoreAds) {
            Text(
                text = "\u6ca1\u6709\u66f4\u591a\u5e7f\u544a\u4e86",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(AppSpacing.Small))
            ActionChip(
                text = "\u91cd\u8bd5",
                selected = true,
                onClick = onRetryClick
            )
        } else if (isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppSpacing.LoadMoreIndicatorSize),
                strokeWidth = AppSpacing.LoadMoreIndicatorStroke,
                color = AppColors.Primary
            )
            Spacer(modifier = Modifier.width(AppSpacing.Small))
            Text(
                text = "\u6b63\u5728\u52a0\u8f7d\u66f4\u591a",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "\u4e0a\u62c9\u52a0\u8f7d\u66f4\u591a",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HeaderBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AIAdFlow",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "\u0041\u0049 \u5e7f\u544a\u4fe1\u606f\u6d41",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Box(
            modifier = Modifier
                .size(AppSpacing.IconButton)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ChannelTabs(
    channels: List<Channel>,
    selectedChannel: Channel?,
    onChannelSelected: (Channel?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            ChannelTabChip(
                label = "\u5168\u90e8",
                selected = selectedChannel == null,
                onClick = { onChannelSelected(null) }
            )
            channels.forEach { channel ->
                ChannelTabChip(
                    label = channelLabelFor(channel),
                    selected = selectedChannel == channel,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
private fun ChannelTabChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) AppColors.Primary else AppColors.PageBackground,
        label = "channelTabBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) AppColors.Primary else AppColors.MediaPlaceholder,
        label = "channelTabBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
        label = "channelTabText"
    )
    val tabHeight by animateDpAsState(
        targetValue = if (selected) AppSpacing.TabSelectedHeight else AppSpacing.TabHeight,
        label = "channelTabHeight"
    )
    val textWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = modifier
            .width(AppSpacing.TabWidth)
            .height(tabHeight)
            .clip(AppRadius.Full)
            .background(backgroundColor)
            .border(
                width = AppSpacing.TabBorderWidth,
                color = borderColor,
                shape = AppRadius.Full
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = textWeight)
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.SearchHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .border(
                width = AppSpacing.SearchBorderWidth,
                color = if (query.isBlank()) AppColors.MediaPlaceholder else AppColors.Primary,
                shape = AppRadius.Large
            )
            .padding(horizontal = AppSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Text(
            text = "\u641c",
            color = if (query.isBlank()) AppColors.TextMuted else AppColors.Primary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        HorizontalDivider(
            modifier = Modifier
                .height(AppSpacing.SearchDividerHeight)
                .width(AppSpacing.SearchDividerWidth),
            color = AppColors.MediaPlaceholder
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextPrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "\u641c\u7d22\u5e7f\u544a\u3001\u54c1\u724c\u3001\u6807\u7b7e",
                        color = AppColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.SearchClearButton)
                    .clip(CircleShape)
                    .background(AppColors.PageBackground)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u00d7",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ActiveFiltersBar(
    uiState: AdFeedUiState,
    onClearFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Text(
            text = activeFilterLabel(uiState),
            modifier = Modifier.weight(1f),
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ActionChip(
            text = "\u6e05\u9664",
            selected = false,
            onClick = onClearFilters
        )
    }
}

@Composable
private fun AdCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    selectedTag: String?,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onViewClick: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val mediaSpec = mediaSpecFor(ad.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
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
                Text(
                    text = ad.summary,
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
            onViewClick = onViewClick
        )
    }
}

@Composable
private fun AdMediaBlock(
    ad: AdItem,
    mediaSpec: AdMediaSpec,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppRadius.Medium)
            .background(mediaSpec.color)
            .padding(AppSpacing.Medium)
    ) {
        Text(
            text = mediaSpec.labelPrefix + ad.mediaLabel,
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.labelLarge
        )
        if (mediaSpec.showPlayButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(AppSpacing.PlayButton)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u64ad\u653e",
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (mediaSpec.showChannelBadge) {
            Text(
                text = channelLabelFor(ad.channel),
                modifier = Modifier.align(Alignment.BottomStart),
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
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            AdSummaryHeader(ad = ad, showTitle = false, showChannelInline = true)
        } else {
            AdSummaryHeader(ad = ad, showChannelInline = showChannelInline)
        }
        Text(
            text = ad.summary,
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showTitle) {
                Text(
                    text = ad.title,
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showChannelInline) {
            Text(
                text = channelLabelFor(ad.channel),
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun AdActionRow(
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)) {
            ActionChip(
                text = if (liked) "\u5df2\u70b9\u8d5e" else "\u70b9\u8d5e",
                selected = liked,
                onClick = onLikeClick
            )
            ActionChip(
                text = if (collected) "\u5df2\u6536\u85cf" else "\u6536\u85cf",
                selected = collected,
                onClick = onCollectClick
            )
        }
        ActionChip(
            text = "\u67e5\u770b",
            selected = true,
            onClick = onViewClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(
    tags: List<String>,
    selectedTag: String?,
    onTagClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small),
        maxLines = 2
    ) {
        tags.forEach { tag ->
            val selected = tag.equals(selectedTag, ignoreCase = true)
            val backgroundColor by animateColorAsState(
                targetValue = if (selected) AppColors.Primary else AppColors.PageBackground,
                label = "tagBackground"
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) AppColors.Primary else AppColors.MediaPlaceholder,
                label = "tagBorder"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
                label = "tagText"
            )
            Box(
                modifier = Modifier
                    .clip(AppRadius.Full)
                    .background(backgroundColor)
                    .border(
                        width = AppSpacing.TagBorderWidth,
                        color = borderColor,
                        shape = AppRadius.Full
                    )
                    .clickable { onTagClick(tag) }
                    .widthIn(max = AppSpacing.TagMaxWidth)
                    .padding(
                        horizontal = AppSpacing.Small,
                        vertical = AppSpacing.TagVertical
                    )
            ) {
                Text(
                    text = "#$tag",
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(AppSpacing.ActionHeight)
            .clip(AppRadius.Full)
            .background(if (selected) AppColors.Primary else AppColors.PageBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun EmptyFeed() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.EmptyHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u6ca1\u6709\u5339\u914d\u7684\u5e7f\u544a",
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private data class AdMediaSpec(
    val height: Dp,
    val color: Color,
    val labelPrefix: String = "",
    val showPlayButton: Boolean = false,
    val showChannelBadge: Boolean = false
)

private fun mediaSpecFor(adType: AdType): AdMediaSpec = when (adType) {
    AdType.SmallImage -> AdMediaSpec(
        height = AppSpacing.CompactMediaHeight,
        color = Color(0xFF0F766E),
        labelPrefix = "\u5c0f\u56fe / "
    )
    AdType.ImageText -> AdMediaSpec(
        height = AppSpacing.ImageTextMediaHeight,
        color = Color(0xFF7C3AED),
        labelPrefix = "\u56fe\u6587 / "
    )
    AdType.Video -> AdMediaSpec(
        height = AppSpacing.VideoMediaHeight,
        color = Color(0xFFDC2626),
        labelPrefix = "\u89c6\u9891 / ",
        showPlayButton = true
    )
    AdType.LargeImage -> AdMediaSpec(
        height = AppSpacing.LargeImageMediaHeight,
        color = Color(0xFF2563EB),
        labelPrefix = "\u5927\u56fe / ",
        showChannelBadge = true
    )
}

private fun mediaColorFor(adType: AdType): Color = mediaSpecFor(adType).color

private fun channelLabelFor(channel: Channel): String = when (channel) {
    Channel.Featured -> "\u63a8\u8350"
    Channel.Ecommerce -> "\u7535\u5546"
    Channel.Local -> "\u672c\u5730"
    Channel.NewArrival -> "\u65b0\u54c1"
    Channel.Finance -> "\u91d1\u878d"
    Channel.Health -> "\u5065\u5eb7"
    Channel.Travel -> "\u51fa\u884c"
    Channel.Education -> "\u6559\u80b2"
}

private fun AdFeedUiState.hasActiveFilters(): Boolean {
    return selectedChannel != null || searchText.isNotBlank() || !selectedTag.isNullOrBlank()
}

private fun activeFilterLabel(uiState: AdFeedUiState): String {
    val filters = buildList {
        uiState.selectedChannel?.let { add(channelLabelFor(it)) }
        uiState.searchText.takeIf { it.isNotBlank() }?.let { add("\"${it.trim()}\"") }
        uiState.selectedTag?.takeIf { it.isNotBlank() }?.let { add("#${it.trim()}") }
    }

    return filters.joinToString(separator = "  ")
}

@Preview(
    name = "Home feed",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun HomeScreenPreview() {
    AIAdFlowTheme {
        var selectedChannel by remember { mutableStateOf<Channel?>(null) }
        var searchText by remember { mutableStateOf("") }
        var selectedTag by remember { mutableStateOf<String?>(null) }
        var selectedAd by remember { mutableStateOf<AdItem?>(null) }
        val visibleAds = remember(selectedChannel, searchText, selectedTag) {
            PreviewAds
                .filter { selectedChannel == null || it.channel == selectedChannel }
                .filter { ad ->
                    val query = searchText.trim()
                    query.isBlank() ||
                        ad.title.contains(query, ignoreCase = true) ||
                        ad.summary.contains(query, ignoreCase = true) ||
                        ad.tags.any { it.contains(query, ignoreCase = true) }
                }
                .filter { ad ->
                    selectedTag.isNullOrBlank() ||
                        ad.tags.any { it.equals(selectedTag, ignoreCase = true) }
                }
        }

        AnimatedContent(
            targetState = selectedAd,
            label = "previewAdDetailTransition",
            transitionSpec = {
                val goingToDetail = initialState == null && targetState != null
                if (goingToDetail) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            }
        ) { ad ->
            if (ad != null) {
                AdDetailScreen(
                    ad = ad,
                    onBackClick = { selectedAd = null }
                )
            } else {
                HomeScreen(
                    uiState = AdFeedUiState(
                        channels = Channel.entries,
                        selectedChannel = selectedChannel,
                        searchText = searchText,
                        selectedTag = selectedTag,
                        ads = visibleAds,
                        isLoadingMore = false,
                        hasMoreAds = false,
                        loadMoreErrorMessage = null
                    ),
                    onChannelSelected = { selectedChannel = it },
                    onSearchChange = { searchText = it },
                    onTagSelected = { tag ->
                        selectedTag = tag
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { nextTag ->
                                if (nextTag.equals(selectedTag, ignoreCase = true)) null else nextTag
                            }
                    },
                    onClearFilters = {
                        selectedChannel = null
                        searchText = ""
                        selectedTag = null
                    },
                    onRefresh = {},
                    onLoadMore = {},
                    onRetryLoadMore = {},
                    onAdClick = { selectedAd = it }
                )
            }
        }
    }
}

@Preview(
    name = "Ad detail",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun AdDetailScreenPreview() {
    AIAdFlowTheme {
        AdDetailScreen(
            ad = PreviewAds.first(),
            onBackClick = {}
        )
    }
}

@Composable
private fun AdDetailScreen(
    ad: AdItem,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.PageBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.PageHorizontal),
            contentPadding = PaddingValues(bottom = AppSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
            item {
                DetailTopBar(onBackClick = onBackClick)
            }
            item {
                when (ad.type) {
                    AdType.ImageText -> ImageTextDetailContent(ad = ad)
                    AdType.Video -> VideoDetailContent(ad = ad)
                    else -> StandardDetailContent(ad = ad)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdFeedRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
private fun StandardDetailContent(ad: AdItem) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)) {
        DetailMediaBlock(ad = ad, height = AppSpacing.AdMediaHeight)
        DetailField(label = "\u54c1\u724c\u540d", value = ad.brandName)
        DetailField(label = "\u6807\u9898", value = ad.title)
        DetailField(label = "\u0041\u0049 \u6458\u8981", value = ad.summary)
        DetailField(
            label = "\u5e7f\u544a\u6807\u7b7e",
            value = ad.tags.joinToString(separator = "  ") { "#$it" }
        )
    }
}

@Composable
private fun ImageTextDetailContent(ad: AdItem) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)) {
        DetailMediaBlock(ad = ad, height = AppSpacing.ImageTextMediaHeight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.Large)
                .background(AppColors.Surface)
                .padding(AppSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            Text(
                text = ad.brandName,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = ad.title,
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "\u0041\u0049 \u6458\u8981",
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = ad.summary,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = ad.tags.joinToString(separator = "  ") { "#$it" },
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun VideoDetailContent(ad: AdItem) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)) {
        DetailMediaBlock(ad = ad, height = AppSpacing.VideoMediaHeight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.Large)
                .background(AppColors.Surface)
                .padding(AppSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ad.brandName,
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = ad.title,
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = channelLabelFor(ad.channel),
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(
                text = "\u89c6\u9891\u7d20\u6750",
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = ad.videoUrl ?: "\u672c\u5730 mock \u89c6\u9891",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "\u0041\u0049 \u6458\u8981",
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = ad.summary,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = ad.tags.joinToString(separator = "  ") { "#$it" },
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun DetailMediaBlock(
    ad: AdItem,
    height: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(AppRadius.Large)
            .background(mediaColorFor(ad.type))
            .padding(AppSpacing.Medium)
    ) {
        Text(
            text = ad.mediaLabel,
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.labelLarge
        )
        if (ad.type == AdType.Video) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(AppSpacing.PlayButton)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u64ad\u653e",
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Text(
            text = channelLabelFor(ad.channel),
            modifier = Modifier.align(Alignment.BottomStart),
            color = AppColors.OnPrimary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun DetailTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Medium)
    ) {
        ActionChip(
            text = "\u8fd4\u56de",
            selected = false,
            onClick = onBackClick
        )
        Text(
            text = "\u5e7f\u544a\u8be6\u60c5",
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Text(
            text = label,
            color = AppColors.Primary,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = value,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val PreviewAds = listOf(
    AdItem(
        id = 100,
        channel = Channel.Featured,
        type = AdType.Video,
        brandName = "\u8dc3\u52a8\u5de5\u574a",
        title = "\u4e03\u5929\u521b\u4f5c\u8005\u6311\u6218",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u77ed\u89c6\u9891\u7d20\u6750\u7a81\u51fa\u8bad\u7ec3\u8fdb\u5ea6\u3001\u53ef\u5206\u4eab\u91cc\u7a0b\u7891\u548c\u4f4e\u95e8\u69db\u8bd5\u7528\u62a5\u540d\u8def\u5f84\u3002",
        mediaLabel = "\u89c6\u9891\u7d20\u6750",
        videoUrl = "https://cdn.example.com/ads/runlab-creator-challenge.mp4",
        coverUrl = "https://cdn.example.com/ads/runlab-creator-challenge-cover.jpg",
        tags = listOf("\u5065\u8eab", "\u521b\u4f5c\u8005", "\u8bd5\u7528")
    ),
    AdItem(
        id = 101,
        channel = Channel.Ecommerce,
        type = AdType.LargeImage,
        brandName = "\u5317\u7ebf\u88c5\u5907",
        title = "\u8f7b\u91cf\u901a\u52e4\u53cc\u80a9\u5305",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u901a\u52e4\u4eba\u7fa4\u5bf9\u9632\u6c34\u7535\u8111\u5305\u7684\u641c\u7d22\u610f\u5411\u8f83\u9ad8\uff0c\u9002\u5408\u6295\u653e\u6548\u7387\u578b\u7d20\u6750\u3002",
        mediaLabel = "\u5546\u54c1\u5927\u56fe",
        tags = listOf("\u53cc\u80a9\u5305", "\u901a\u52e4", "\u9632\u6c34")
    ),
    AdItem(
        id = 102,
        channel = Channel.Local,
        type = AdType.ImageText,
        brandName = "\u8857\u89d2\u5c0f\u9986",
        title = "\u9644\u8fd1\u56e2\u961f\u5de5\u4f5c\u65e5\u5348\u9910\u5957\u9910",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u5728\u4e34\u8fd1\u51b3\u7b56\u65f6\u6bb5\uff0c\u5411\u9644\u8fd1\u7528\u6237\u63a8\u5e7f\u5de5\u4f5c\u65e5\u5348\u9910\u7ec4\u5408\u4f18\u60e0\u3002",
        mediaLabel = "\u56fe\u6587\u7d20\u6750",
        tags = listOf("\u9910\u996e", "\u9644\u8fd1", "\u5348\u9910")
    ),
    AdItem(
        id = 103,
        channel = Channel.Featured,
        type = AdType.SmallImage,
        brandName = "\u6696\u5c45\u751f\u6d3b",
        title = "\u667a\u80fd\u9999\u85b0\u673a\u7ec4\u5408\u88c5",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u591c\u95f4\u653e\u677e\u573a\u666f\u4e0e\u9650\u65f6\u7ec4\u5408\u6298\u6263\u7684\u7ed3\u5408\u8868\u73b0\u66f4\u7a33\u5b9a\u3002",
        mediaLabel = "\u5c0f\u56fe\u7d20\u6750",
        tags = listOf("\u5bb6\u5c45", "\u7597\u6108", "\u7ec4\u5408")
    ),
    AdItem(
        id = 104,
        channel = Channel.Finance,
        type = AdType.ImageText,
        brandName = "Bluebird Pay",
        title = "Weekend cashback boost",
        summary = "AI suggests highlighting groceries, transport, and dining as everyday cashback scenes.",
        mediaLabel = "Finance card",
        tags = listOf("Finance", "Cashback", "Dining")
    ),
    AdItem(
        id = 105,
        channel = Channel.Health,
        type = AdType.LargeImage,
        brandName = "Daily Greens",
        title = "Morning nutrition subscription",
        summary = "Best-performing copy connects breakfast routines with simple energy and wellness habits.",
        mediaLabel = "Health visual",
        tags = listOf("Health", "Wellness", "Subscription")
    ),
    AdItem(
        id = 106,
        channel = Channel.Education,
        type = AdType.SmallImage,
        brandName = "SkillForge",
        title = "AI design course trial lesson",
        summary = "Campaign should highlight portfolio outcomes, guided practice, and a short trial format.",
        mediaLabel = "Course image",
        tags = listOf("Education", "AI", "Creator")
    )
)
