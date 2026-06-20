package com.example.aiadflow.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.card.AdCard
import com.example.aiadflow.ui.channel.CategoryTabs
import com.example.aiadflow.ui.common.HomeBackgroundBrush
import com.example.aiadflow.ui.common.RefreshSnackbarVisibleMillis
import com.example.aiadflow.ui.feed.AdFeedUiState
import com.example.aiadflow.ui.load.AdFeedRefreshContainer
import com.example.aiadflow.ui.load.EmptyFeed
import com.example.aiadflow.ui.load.LoadMoreFooter
import com.example.aiadflow.ui.search.SearchBar
import com.example.aiadflow.ui.search.SmartSearchStatus
import com.example.aiadflow.ui.tag.ActiveFiltersBar
import com.example.aiadflow.ui.tag.hasActiveFilters
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
    uiState: AdFeedUiState,
    onChannelSelected: (Channel?) -> Unit,
    onSearchChange: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Boolean,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onCollectClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onAdClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // UI only asks for the next page near the bottom; ViewModel still owns duplicate-load guards.
    val shouldLoadMore by remember(
        uiState.hasMoreAds,
        uiState.isLoadingMore,
        uiState.ads.size,
        uiState.loadMoreErrorMessage
    ) {
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

    val refreshWithFeedback: () -> Unit = {
        coroutineScope.launch {
            val isSuccess = onRefresh()
            val snackbarJob = launch {
                snackbarHostState.showSnackbar(
                    message = if (isSuccess) "刷新成功" else "刷新失败，请稍后重试",
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(RefreshSnackbarVisibleMillis)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob.join()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeBackgroundBrush)
                .padding(innerPadding)
        ) {
            AdFeedRefreshContainer(
                isRefreshing = uiState.isLoading,
                onRefresh = refreshWithFeedback
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.PageHorizontal),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item(key = "status-bars-spacer") {
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    }
                    item(key = "header") {
                        HomeHeader()
                    }
                    item(key = "channel-tabs") {
                        CategoryTabs(
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
                    item(key = "smart-search-status") {
                        SmartSearchStatus(
                            uiState = uiState,
                            onTagSelected = onTagSelected
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
                            EmptyFeed(showCollectedOnly = uiState.showCollectedOnly)
                        }
                    } else {
                        items(
                            items = uiState.ads,
                            key = { it.id }
                        ) { ad ->
                            AdCard(
                                ad = ad,
                                liked = uiState.likedOverridesByAdId[ad.id] ?: ad.liked,
                                collected = uiState.collectedOverridesByAdId[ad.id] ?: ad.collected,
                                selectedTag = uiState.selectedTag,
                                onLikeClick = { onLikeClick(ad.id) },
                                onCollectClick = { onCollectClick(ad.id) },
                                onShareClick = { onShareClick(ad.id) },
                                onViewClick = { onAdClick(ad.id) },
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
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AIAdFlow",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "AI 广告信息流",
                color = Color(0xFF6B7A90),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
