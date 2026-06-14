package com.example.aiadflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aiadflow.data.local.SharedPreferencesAdLocalStateStore
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.ui.detail.AdDetailScreen
import com.example.aiadflow.ui.feed.AdFeedViewModel
import com.example.aiadflow.ui.home.HomeScreen
import com.example.aiadflow.ui.theme.AIAdFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIAdFlowTheme {
                val viewModel = remember {
                    AdFeedViewModel(
                        localStateStore = SharedPreferencesAdLocalStateStore(this)
                    )
                }
                val uiState by viewModel.uiState.collectAsState()
                var selectedAd by remember { mutableStateOf<AdItem?>(null) }
                val shareAd: (Long) -> Unit = { adId ->
                    viewModel.shareAd(adId)?.let { shareText ->
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        startActivity(Intent.createChooser(sendIntent, "分享广告"))
                    }
                }

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
                            onLikeClick = viewModel::toggleLike,
                            onCollectClick = viewModel::toggleCollect,
                            onShareClick = shareAd,
                            onAdClick = { adId ->
                                viewModel.getAdDetail(adId)?.let { ad ->
                                    viewModel.trackAdClick(ad)
                                    selectedAd = ad
                                }
                            }
                        )
                    } else {
                        AdDetailScreen(
                            ad = detailAd,
                            liked = uiState.likedOverridesByAdId[detailAd.id] ?: detailAd.liked,
                            collected = uiState.collectedOverridesByAdId[detailAd.id] ?: detailAd.collected,
                            onBackClick = { selectedAd = null },
                            onLikeClick = { viewModel.toggleLike(detailAd.id) },
                            onCollectClick = { viewModel.toggleCollect(detailAd.id) },
                            onShareClick = { shareAd(detailAd.id) }
                        )
                    }
                }
            }
        }
    }
}
