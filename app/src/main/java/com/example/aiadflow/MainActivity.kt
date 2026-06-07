package com.example.aiadflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

                val ad = selectedAd
                if (ad != null) {
                    AdDetailScreen(
                        ad = ad,
                        onBackClick = { selectedAd = null }
                    )
                } else {
                    HomeScreen(
                        uiState = uiState,
                        onChannelSelected = viewModel::selectChannel,
                        onSearchChange = viewModel::updateSearchText,
                        onAdClick = { clickedAd ->
                            viewModel.trackAdClick(clickedAd)
                            selectedAd = clickedAd
                        }
                    )
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
    onAdClick: (AdItem) -> Unit
) {
    val likedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val collectedOverrides = remember { mutableStateMapOf<Long, Boolean>() }

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
                HeaderBar()
            }
            item {
                ChannelTabs(
                    channels = uiState.channels,
                    selectedChannel = uiState.selectedChannel,
                    onChannelSelected = onChannelSelected
                )
            }
            item {
                SearchBar(
                    query = uiState.searchText,
                    onQueryChange = onSearchChange
                )
            }
            if (uiState.ads.isEmpty()) {
                item {
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
                        onLikeClick = {
                            likedOverrides[ad.id] = !(likedOverrides[ad.id] ?: ad.liked)
                        },
                        onCollectClick = {
                            collectedOverrides[ad.id] = !(collectedOverrides[ad.id] ?: ad.collected)
                        },
                        onViewClick = {
                            onAdClick(ad)
                        }
                    )
                }
            }
        }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacing.AdMediaHeight)
                        .clip(AppRadius.Large)
                        .background(mediaColorFor(ad.type))
                        .padding(AppSpacing.Medium)
                ) {
                    Text(
                        text = ad.mediaLabel,
                        color = AppColors.OnPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = channelLabelFor(ad.channel),
                        modifier = Modifier.align(Alignment.BottomStart),
                        color = AppColors.OnPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            item {
                DetailField(
                    label = "\u54c1\u724c\u540d",
                    value = ad.brandName
                )
            }
            item {
                DetailField(
                    label = "\u6807\u9898",
                    value = ad.title
                )
            }
            item {
                DetailField(
                    label = "\u0041\u0049 \u6458\u8981",
                    value = ad.summary
                )
            }
            item {
                DetailTags(tags = ad.tags)
            }
        }
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
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun DetailTags(tags: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Text(
            text = "\u5e7f\u544a\u6807\u7b7e",
            color = AppColors.Primary,
            style = MaterialTheme.typography.labelLarge
        )
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppRadius.Full)
                    .background(AppColors.PageBackground)
                    .padding(
                        horizontal = AppSpacing.Medium,
                        vertical = AppSpacing.TagVertical
                    )
            ) {
                Text(
                    text = "#$tag",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        ChannelTabChip(
            label = "\u5168\u90e8",
            selected = selectedChannel == null,
            modifier = Modifier.weight(1f),
            onClick = { onChannelSelected(null) }
        )
        channels.forEach { channel ->
            ChannelTabChip(
                label = channelLabelFor(channel),
                selected = selectedChannel == channel,
                modifier = Modifier.weight(1f),
                onClick = { onChannelSelected(channel) }
            )
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
    Box(
        modifier = modifier
            .height(AppSpacing.TabHeight)
            .clip(AppRadius.Full)
            .background(if (selected) AppColors.Primary else AppColors.Surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.SearchHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(horizontal = AppSpacing.Medium),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun AdCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        when (ad.type) {
            AdType.SmallImage -> SmallImageAdContent(ad = ad)
            AdType.LargeImage -> LargeImageAdContent(ad = ad)
            else -> StandardAdContent(ad = ad)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
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
            Spacer(modifier = Modifier.weight(1f))
            ActionChip(
                text = "\u67e5\u770b",
                selected = true,
                onClick = onViewClick
            )
        }
    }
}

@Composable
private fun LargeImageAdContent(ad: AdItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.LargeImageMediaHeight)
                .clip(AppRadius.Medium)
                .background(mediaColorFor(ad.type))
                .padding(AppSpacing.Medium)
        ) {
            Text(
                text = ad.mediaLabel,
                color = AppColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
            ) {
                Text(
                    text = channelLabelFor(ad.channel),
                    color = AppColors.OnPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = ad.brandName,
                    color = AppColors.OnPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text(
            text = ad.title,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = ad.summary,
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        TagRow(tags = ad.tags)
    }
}

@Composable
private fun StandardAdContent(ad: AdItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (ad.type == AdType.LargeImage || ad.type == AdType.Video) AppSpacing.AdMediaHeight else AppSpacing.CompactMediaHeight)
                .clip(AppRadius.Medium)
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
        }
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
            text = ad.summary,
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        TagRow(tags = ad.tags)
    }
}

@Composable
private fun SmallImageAdContent(ad: AdItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Medium),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(AppSpacing.SmallImageMediaWidth)
                .height(AppSpacing.CompactMediaHeight)
                .clip(AppRadius.Medium)
                .background(mediaColorFor(ad.type))
                .padding(AppSpacing.Small)
        ) {
            Text(
                text = ad.mediaLabel,
                color = AppColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ad.brandName,
                    modifier = Modifier.weight(1f),
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = channelLabelFor(ad.channel),
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(
                text = ad.title,
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = ad.summary,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            TagRow(tags = ad.tags)
        }
    }
}

@Composable
private fun TagRow(tags: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        tags.take(3).forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(AppRadius.Full)
                    .background(AppColors.PageBackground)
                    .padding(
                        horizontal = AppSpacing.Small,
                        vertical = AppSpacing.TagVertical
                    )
            ) {
                Text(
                    text = "#$tag",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
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

private fun mediaColorFor(adType: AdType): Color = when (adType) {
    AdType.SmallImage -> Color(0xFF0F766E)
    AdType.ImageText -> Color(0xFF7C3AED)
    AdType.Video -> Color(0xFFDC2626)
    AdType.LargeImage -> Color(0xFF2563EB)
}

private fun channelLabelFor(channel: Channel): String = when (channel) {
    Channel.Featured -> "\u63a8\u8350"
    Channel.Ecommerce -> "\u7535\u5546"
    Channel.Local -> "\u672c\u5730"
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AIAdFlowTheme {
        HomeScreen(
            uiState = AdFeedUiState(
                channels = Channel.entries,
                ads = listOf(PreviewAd)
            ),
            onChannelSelected = {},
            onSearchChange = {},
            onAdClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdDetailScreenPreview() {
    AIAdFlowTheme {
        AdDetailScreen(
            ad = PreviewAd,
            onBackClick = {}
        )
    }
}

private val PreviewAd = AdItem(
    id = 100,
    channel = Channel.Featured,
    type = AdType.LargeImage,
    brandName = "\u9752\u96c0\u652f\u4ed8",
    title = "\u8fd4\u73b0\u6743\u76ca\u5f00\u901a\u6545\u4e8b",
    summary = "\u0041\u0049 \u6458\u8981\uff1a\u4e09\u6bb5\u5f0f\u5361\u7247\u5148\u89e3\u91ca\u65e5\u5e38\u7701\u94b1\u573a\u666f\uff0c\u518d\u5f15\u5bfc\u7528\u6237\u6bd4\u8f83\u4e0d\u540c\u8fd4\u73b0\u7b49\u7ea7\u3002",
    mediaLabel = "\u54c1\u724c\u89c6\u89c9",
    tags = listOf("\u91d1\u878d", "\u8fd4\u73b0", "\u5f00\u901a", "\u6743\u76ca")
)
