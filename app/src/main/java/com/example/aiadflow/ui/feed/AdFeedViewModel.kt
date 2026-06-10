package com.example.aiadflow.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiadflow.data.local.AdLocalInteractionState
import com.example.aiadflow.data.local.AdLocalStateStore
import com.example.aiadflow.data.local.NoOpAdLocalStateStore
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent
import com.example.aiadflow.data.repository.AdRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 广告信息流页面的 UI 状态。
 *
 * ViewModel 通过 StateFlow 暴露该状态，Compose 页面可以订阅并渲染。
 */
data class AdFeedUiState(
    /** 当前可展示的频道列表。 */
    val channels: List<Channel> = emptyList(),
    /** 当前选中的频道。 */
    val selectedChannel: Channel? = null,
    /** 搜索框文本。 */
    val searchText: String = "",
    val selectedTag: String? = null,
    /** 根据频道和搜索词过滤后的广告列表。 */
    val ads: List<AdItem> = emptyList(),
    /** 当前会话内累计记录的广告曝光次数。 */
    val totalExposureCount: Int = 0,
    /** 当前会话内按广告 id 聚合的曝光次数。 */
    val exposureCountsByAdId: Map<Long, Int> = emptyMap(),
    val clickCount: Int = 0,
    val clickCountsByAdId: Map<Long, Int> = emptyMap(),
    val likedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val collectedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val showCollectedOnly: Boolean = false,
    val collectedCount: Int = 0,
    /** 是否正在加载数据，预留给后续真实接口接入。 */
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreAds: Boolean = true,
    val loadMoreErrorMessage: String? = null,
    val currentPage: Int = 1
)

/**
 * 广告信息流 ViewModel。
 *
 * 负责维护频道选择、搜索文本、广告列表刷新以及广告行为埋点。
 */
class AdFeedViewModel(
    /** 广告仓库，默认使用本地 mock 数据实现。 */
    private val repository: AdRepository = AdRepository(),
    private val localStateStore: AdLocalStateStore = NoOpAdLocalStateStore
) : ViewModel() {
    private companion object {
        const val PageSize = 6
        const val LoadMoreDelayMillis = 350L
    }

    /** 可变内部状态，只允许 ViewModel 自己更新。 */
    private val _uiState = MutableStateFlow(
        run {
            val initialAds = repository.getAds()
            val localState = localStateStore.load()
            AdFeedUiState(
                channels = repository.getChannels(),
                ads = initialAds.take(PageSize),
                likedOverridesByAdId = localState.likedOverridesByAdId,
                collectedOverridesByAdId = localState.collectedOverridesByAdId,
                collectedCount = initialAds.count { ad ->
                    localState.collectedOverridesByAdId[ad.id] ?: ad.collected
                },
                hasMoreAds = initialAds.size > PageSize,
                currentPage = 1
            )
        }
    )
    /** 暴露给 UI 的只读状态流。 */
    val uiState: StateFlow<AdFeedUiState> = _uiState.asStateFlow()

    /** 切换频道，并按当前搜索词刷新广告列表。 */
    fun switchChannel(channel: Channel?) {
        _uiState.update { current ->
            val nextChannel = channel?.takeIf { it in current.channels }
            if (nextChannel == current.selectedChannel) {
                return@update current
            }

            current.copy(
                selectedChannel = nextChannel,
                ads = current.filteredAds(
                    channel = nextChannel,
                    query = current.searchText,
                    selectedTag = current.selectedTag
                ).take(PageSize),
                hasMoreAds = current.filteredAds(
                    channel = nextChannel,
                    query = current.searchText,
                    selectedTag = current.selectedTag
                ).size > PageSize,
                currentPage = 1,
                isLoadingMore = false,
                loadMoreErrorMessage = null
            )
        }
    }

    /** Backward-compatible alias for existing callers. */
    fun selectChannel(channel: Channel?) {
        switchChannel(channel)
    }

    /** 更新搜索框文本。 */
    fun updateSearchText(text: String) {
        _uiState.update { current ->
            current.copy(
                searchText = text,
                ads = current.filteredAds(query = text).take(PageSize),
                hasMoreAds = current.filteredAds(query = text).size > PageSize,
                currentPage = 1,
                isLoadingMore = false,
                loadMoreErrorMessage = null
            )
        }
    }

    fun selectTag(tag: String?) {
        _uiState.update { current ->
            val nextTag = tag
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { selectedTag ->
                    if (selectedTag.equals(current.selectedTag, ignoreCase = true)) {
                        null
                    } else {
                        selectedTag
                    }
                }

            current.copy(
                selectedTag = nextTag,
                ads = current.filteredAds(selectedTag = nextTag).take(PageSize),
                hasMoreAds = current.filteredAds(selectedTag = nextTag).size > PageSize,
                currentPage = 1,
                isLoadingMore = false,
                loadMoreErrorMessage = null
            )
        }
    }

    fun clearFilters() {
        _uiState.update { current ->
            current.copy(
                selectedChannel = null,
                searchText = "",
                selectedTag = null,
                showCollectedOnly = false,
                ads = repository.getAds().take(PageSize),
                hasMoreAds = repository.getAds().size > PageSize,
                currentPage = 1,
                isLoadingMore = false,
                loadMoreErrorMessage = null
            )
        }
    }

    fun toggleCollectedOnly() {
        _uiState.update { current ->
            val nextShowCollectedOnly = !current.showCollectedOnly
            val nextAds = current.filteredAds(showCollectedOnly = nextShowCollectedOnly)

            current.copy(
                showCollectedOnly = nextShowCollectedOnly,
                ads = nextAds.take(PageSize),
                hasMoreAds = nextAds.size > PageSize,
                currentPage = 1,
                isLoadingMore = false,
                loadMoreErrorMessage = null
            )
        }
    }

    /** 使用当前频道和搜索词重新拉取广告列表。 */
    fun refreshAds(): Boolean {
        val current = _uiState.value
        return try {
            val refreshedAds = repository.getAds(
                current.selectedChannel,
                current.searchText,
                current.selectedTag
            ).filterCollected(current.showCollectedOnly, current.collectedOverridesByAdId)
            _uiState.update {
                it.copy(
                    ads = refreshedAds.take(PageSize),
                    collectedCount = repository.getAds()
                        .filter { ad -> it.collectedOverridesByAdId[ad.id] ?: ad.collected }
                        .size,
                    hasMoreAds = refreshedAds.size > PageSize,
                    currentPage = 1,
                    isLoadingMore = false,
                    loadMoreErrorMessage = null
                )
            }
            true
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    loadMoreErrorMessage = null
                )
            }
            false
        }
    }

    fun loadMoreAds() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMoreAds || current.ads.isEmpty() || current.loadMoreErrorMessage != null) {
            return
        }

        _uiState.update {
            it.copy(
                isLoadingMore = true,
                loadMoreErrorMessage = null
            )
        }

        viewModelScope.launch {
            delay(LoadMoreDelayMillis)
            _uiState.update { latest ->
                if (!latest.isLoadingMore) {
                    return@update latest
                }

                try {
                    val nextPage = latest.currentPage + 1
                    val allAds = repository.getAds(
                        latest.selectedChannel,
                        latest.searchText,
                        latest.selectedTag
                    ).filterCollected(latest.showCollectedOnly, latest.collectedOverridesByAdId)
                    val nextAds = allAds.take(nextPage * PageSize)

                    latest.copy(
                        ads = nextAds,
                        currentPage = nextPage,
                        hasMoreAds = nextAds.size < allAds.size,
                        isLoadingMore = false,
                        loadMoreErrorMessage = null
                    )
                } catch (_: Exception) {
                    latest.copy(
                        isLoadingMore = false,
                        loadMoreErrorMessage = "加载失败，请重试"
                    )
                }
            }
        }
    }

    fun retryLoadMoreAds() {
        _uiState.update { it.copy(loadMoreErrorMessage = null) }
        loadMoreAds()
    }

    fun getAdDetail(adId: Long): AdItem? {
        return repository.getAdById(adId)
    }

    fun toggleLike(adId: Long) {
        val ad = repository.getAdById(adId) ?: return
        _uiState.update { current ->
            val currentLiked = current.likedOverridesByAdId[adId] ?: ad.liked
            val nextState = current.copy(
                likedOverridesByAdId = current.likedOverridesByAdId + (adId to !currentLiked)
            )
            localStateStore.save(nextState.toLocalInteractionState())
            nextState
        }
    }

    fun toggleCollect(adId: Long) {
        val ad = repository.getAdById(adId) ?: return
        _uiState.update { current ->
            val currentCollected = current.collectedOverridesByAdId[adId] ?: ad.collected
            val nextOverrides = current.collectedOverridesByAdId + (adId to !currentCollected)
            val nextAds = current.copy(collectedOverridesByAdId = nextOverrides)
                .filteredAds(collectedOverridesByAdId = nextOverrides)
            val nextState = current.copy(
                collectedOverridesByAdId = nextOverrides,
                collectedCount = repository.getAds()
                    .filter { repositoryAd -> nextOverrides[repositoryAd.id] ?: repositoryAd.collected }
                    .size,
                ads = nextAds.take(current.currentPage * PageSize),
                hasMoreAds = nextAds.size > current.currentPage * PageSize,
                loadMoreErrorMessage = null
            )
            localStateStore.save(nextState.toLocalInteractionState())
            nextState
        }
    }

    /** 生成分享文案并记录分享事件。 */
    fun shareAd(adId: Long): String? {
        val ad = repository.getAdById(adId) ?: return null

        track(ad, "share")
        return buildString {
            append(ad.brandName)
            append(" - ")
            append(ad.title)
            appendLine()
            append(ad.summary)
            if (ad.tags.isNotEmpty()) {
                appendLine()
                append(ad.tags.joinToString(separator = " ") { "#$it" })
            }
            appendLine()
            append("来自 AIAdFlow")
        }
    }

    /** 记录广告曝光事件。 */
    fun trackAdImpression(ad: AdItem) {
        track(ad, "impression")
        _uiState.update { current ->
            current.copy(
                totalExposureCount = current.totalExposureCount + 1,
                exposureCountsByAdId = current.exposureCountsByAdId + (
                    ad.id to ((current.exposureCountsByAdId[ad.id] ?: 0) + 1)
                )
            )
        }
    }

    /** 记录广告点击事件。 */
    fun trackAdClick(ad: AdItem) {
        track(ad, "click")
        _uiState.update { current ->
            current.copy(
                clickCount = current.clickCount + 1,
                clickCountsByAdId = current.clickCountsByAdId + (
                    ad.id to ((current.clickCountsByAdId[ad.id] ?: 0) + 1)
                )
            )
        }
    }

    /** 统一封装埋点事件创建逻辑，避免曝光和点击重复组装 TrackEvent。 */
    private fun track(ad: AdItem, eventName: String) {
        repository.track(
            TrackEvent(
                adId = ad.id,
                channel = ad.channel,
                eventName = eventName
            )
        )
    }

    private fun AdFeedUiState.filteredAds(
        channel: Channel? = selectedChannel,
        query: String = searchText,
        selectedTag: String? = this.selectedTag,
        showCollectedOnly: Boolean = this.showCollectedOnly,
        collectedOverridesByAdId: Map<Long, Boolean> = this.collectedOverridesByAdId
    ): List<AdItem> {
        return repository.getAds(channel, query, selectedTag)
            .filterCollected(showCollectedOnly, collectedOverridesByAdId)
    }

    private fun List<AdItem>.filterCollected(
        showCollectedOnly: Boolean,
        collectedOverridesByAdId: Map<Long, Boolean>
    ): List<AdItem> {
        if (!showCollectedOnly) {
            return this
        }

        return filter { ad -> collectedOverridesByAdId[ad.id] ?: ad.collected }
    }

    private fun AdFeedUiState.toLocalInteractionState(): AdLocalInteractionState {
        return AdLocalInteractionState(
            likedOverridesByAdId = likedOverridesByAdId,
            collectedOverridesByAdId = collectedOverridesByAdId
        )
    }
}
