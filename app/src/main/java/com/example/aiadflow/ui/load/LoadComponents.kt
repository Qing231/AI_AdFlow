package com.example.aiadflow.ui.load

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.aiadflow.ui.interaction.ActionChip
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdFeedRefreshContainer(
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
internal fun LoadMoreFooter(
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
        when {
            !hasMoreAds -> Text(
                text = "没有更多广告了",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(AppSpacing.Small))
                ActionChip(
                    text = "重试",
                    selected = true,
                    onClick = onRetryClick
                )
            }
            isLoadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppSpacing.LoadMoreIndicatorSize),
                    strokeWidth = AppSpacing.LoadMoreIndicatorStroke,
                    color = AppColors.Primary
                )
                Spacer(modifier = Modifier.width(AppSpacing.Small))
                Text(
                    text = "正在加载更多",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> Text(
                text = "上拉加载更多",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun EmptyFeed(showCollectedOnly: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.EmptyHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (showCollectedOnly) "暂无收藏广告" else "没有匹配的广告",
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
