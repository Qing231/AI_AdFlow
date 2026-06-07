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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                HomeScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    var selectedChannel by remember { mutableStateOf(ChannelTabs.first().id) }
    var searchQuery by remember { mutableStateOf("") }
    val likedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val collectedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val visibleAds = remember(selectedChannel, searchQuery, likedOverrides.size, collectedOverrides.size) {
        SampleAds.filter { ad ->
            val matchesChannel = selectedChannel == "all" || ad.channel.id == selectedChannel
            val query = searchQuery.trim()
            val matchesSearch = query.isBlank() ||
                ad.brandName.contains(query, ignoreCase = true) ||
                ad.title.contains(query, ignoreCase = true) ||
                ad.summary.contains(query, ignoreCase = true) ||
                ad.tags.any { it.contains(query, ignoreCase = true) }

            matchesChannel && matchesSearch
        }
    }

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
                    selectedChannel = selectedChannel,
                    onChannelSelected = { selectedChannel = it }
                )
            }
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
            if (visibleAds.isEmpty()) {
                item {
                    EmptyFeed()
                }
            } else {
                items(
                    items = visibleAds,
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
                        }
                    )
                }
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
    selectedChannel: String,
    onChannelSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        ChannelTabs.forEach { tab ->
            val selected = tab.id == selectedChannel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppSpacing.TabHeight)
                    .clip(AppRadius.Full)
                    .background(if (selected) AppColors.Primary else AppColors.Surface)
                    .clickable { onChannelSelected(tab.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
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
    onCollectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
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
                onClick = {}
            )
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

private fun channelLabelFor(channel: Channel): String =
    ChannelTabs.firstOrNull { it.id == channel.id }?.label ?: channel.title

private data class ChannelTab(
    val id: String,
    val label: String
)

private val ChannelTabs = listOf(
    ChannelTab("all", "\u5168\u90e8"),
    ChannelTab("featured", "\u63a8\u8350"),
    ChannelTab("ecommerce", "\u7535\u5546"),
    ChannelTab("local", "\u672c\u5730")
)

private val SampleAds = listOf(
    AdItem(
        id = 1,
        channel = Channel.Ecommerce,
        type = AdType.LargeImage,
        brandName = "\u5317\u7ebf\u88c5\u5907",
        title = "\u8f7b\u91cf\u901a\u52e4\u53cc\u80a9\u5305",
        summary = "\u0041\u0049 \u9884\u6d4b\u672c\u5468\u641c\u7d22\u9632\u6c34\u7535\u8111\u5305\u7684\u901a\u52e4\u4eba\u7fa4\u8d2d\u4e70\u610f\u5411\u8f83\u9ad8\uff0c\u9002\u5408\u6295\u653e\u6548\u7387\u578b\u7d20\u6750\u3002",
        mediaLabel = "\u5546\u54c1\u5927\u56fe",
        tags = listOf("\u53cc\u80a9\u5305", "\u901a\u52e4", "\u9632\u6c34"),
        liked = false,
        collected = true
    ),
    AdItem(
        id = 2,
        channel = Channel.Local,
        type = AdType.ImageText,
        brandName = "\u8857\u89d2\u5c0f\u9986",
        title = "\u9644\u8fd1\u56e2\u961f\u5de5\u4f5c\u65e5\u5348\u9910\u5957\u9910",
        summary = "\u5728\u4e0a\u5348\u4e34\u8fd1\u51b3\u7b56\u65f6\u6bb5\uff0c\u5411 \u0033 \u516c\u91cc\u5185\u7528\u6237\u63a8\u5e7f\u5de5\u4f5c\u65e5\u5348\u9910\u7ec4\u5408\u4f18\u60e0\u3002",
        mediaLabel = "\u672c\u5730\u4f18\u60e0",
        tags = listOf("\u9910\u996e", "\u9644\u8fd1", "\u5348\u9910"),
        liked = true,
        collected = false
    ),
    AdItem(
        id = 3,
        channel = Channel.Featured,
        type = AdType.Video,
        brandName = "\u8dc3\u52a8\u5de5\u574a",
        title = "\u4e03\u5929\u521b\u4f5c\u8005\u6311\u6218",
        summary = "\u77ed\u89c6\u9891\u7d20\u6750\u7a81\u51fa\u8bad\u7ec3\u8fdb\u5ea6\u3001\u53ef\u5206\u4eab\u91cc\u7a0b\u7891\uff0c\u4ee5\u53ca\u4f4e\u95e8\u69db\u8bd5\u7528\u62a5\u540d\u8def\u5f84\u3002",
        mediaLabel = "\u89c6\u9891\u7d20\u6750",
        tags = listOf("\u5065\u8eab", "\u521b\u4f5c\u8005", "\u8bd5\u7528"),
        liked = false,
        collected = false
    ),
    AdItem(
        id = 4,
        channel = Channel.Ecommerce,
        type = AdType.SmallImage,
        brandName = "\u6696\u5c45\u751f\u6d3b",
        title = "\u667a\u80fd\u9999\u85b0\u673a\u7ec4\u5408\u88c5",
        summary = "\u8868\u73b0\u6700\u597d\u7684\u6587\u6848\u4f1a\u628a\u591c\u95f4\u653e\u677e\u573a\u666f\u4e0e\u9650\u65f6\u7ec4\u5408\u6298\u6263\u653e\u5728\u4e00\u8d77\u8868\u8fbe\u3002",
        mediaLabel = "\u5c0f\u56fe\u7d20\u6750",
        tags = listOf("\u5bb6\u5c45", "\u7597\u6108", "\u7ec4\u5408"),
        liked = true,
        collected = true
    ),
    AdItem(
        id = 5,
        channel = Channel.Featured,
        type = AdType.LargeImage,
        brandName = "\u9752\u96c0\u652f\u4ed8",
        title = "\u8fd4\u73b0\u6743\u76ca\u5f00\u901a\u6545\u4e8b",
        summary = "\u4e09\u6bb5\u5f0f\u5361\u7247\u5148\u89e3\u91ca\u65e5\u5e38\u7701\u94b1\u573a\u666f\uff0c\u518d\u5f15\u5bfc\u7528\u6237\u6bd4\u8f83\u4e0d\u540c\u8fd4\u73b0\u7b49\u7ea7\u3002",
        mediaLabel = "\u54c1\u724c\u89c6\u89c9",
        tags = listOf("\u91d1\u878d", "\u8fd4\u73b0", "\u5f00\u901a"),
        liked = false,
        collected = false
    )
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AIAdFlowTheme {
        HomeScreen()
    }
}
