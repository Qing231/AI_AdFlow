package com.example.aiadflow.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiadflow.data.local.AdLocalInteractionState
import com.example.aiadflow.data.local.AdLocalStateStore
import com.example.aiadflow.data.local.InMemoryAdLocalStateStore
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent
import com.example.aiadflow.data.repository.AdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdFeedUiState(
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel? = null,
    val searchText: String = "",
    val selectedTag: String? = null,
    val ads: List<AdItem> = emptyList(),
    val totalExposureCount: Int = 0,
    val exposureCountsByAdId: Map<Long, Int> = emptyMap(),
    val clickCount: Int = 0,
    val clickCountsByAdId: Map<Long, Int> = emptyMap(),
    val likedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val collectedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val showCollectedOnly: Boolean = false,
    val collectedCount: Int = 0,
    val adAiSummariesByAdId: Map<Long, String> = emptyMap(),
    val generatingAdSummaryIds: Set<Long> = emptySet(),
    val aiSummary: String = "",
    val isAiSummaryLoading: Boolean = false,
    val aiSummaryAdCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreAds: Boolean = true,
    val loadMoreErrorMessage: String? = null,
    val currentPage: Int = 1
)

class AdFeedViewModel(
    private val repository: AdRepository = AdRepository(),
    private val localStateStore: AdLocalStateStore = InMemoryAdLocalStateStore
) : ViewModel() {
    private companion object {
        const val PageSize = 6
        const val LoadMoreDelayMillis = 350L
        const val AiSummaryStartDelayMillis = 150L
    }

    private val summaryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(
        run {
            val initialAds = repository.getAds()
            val localState = localStateStore.load()
            AdFeedUiState(
                channels = repository.getChannels(),
                ads = initialAds.take(PageSize),
                likedOverridesByAdId = localState.likedOverridesByAdId,
                collectedOverridesByAdId = localState.collectedOverridesByAdId,
                collectedCount = initialAds.count { localState.collectedOverridesByAdId[it.id] ?: it.collected },
                adAiSummariesByAdId = repository.getAdAiSummaries(initialAds.take(PageSize).map(AdItem::id)),
                hasMoreAds = initialAds.size > PageSize,
                currentPage = 1
            )
        }
    )
    val uiState: StateFlow<AdFeedUiState> = _uiState.asStateFlow()

    init {
        requestAiSummary(_uiState.value.ads)
        ensureAdSummaries(_uiState.value.ads)
    }

    fun switchChannel(channel: Channel?) {
        _uiState.update { current ->
            val nextChannel = channel?.takeIf { it in current.channels }
            if (nextChannel == current.selectedChannel) {
                return@update current
            }

            val allAds = current.filteredAds(
                channel = nextChannel,
                query = current.searchText,
                selectedTag = current.selectedTag
            )
            current.pageReset(
                ads = allAds.take(PageSize),
                hasMoreAds = allAds.size > PageSize,
                selectedChannel = nextChannel
            )
        }
        requestAiSummary(_uiState.value.ads)
        ensureAdSummaries(_uiState.value.ads)
    }

    fun selectChannel(channel: Channel?) {
        switchChannel(channel)
    }

    fun updateSearchText(text: String) {
        _uiState.update { current ->
            val allAds = current.filteredAds(query = text)
            current.pageReset(
                ads = allAds.take(PageSize),
                hasMoreAds = allAds.size > PageSize,
                searchText = text
            )
        }
        requestAiSummary(_uiState.value.ads)
        ensureAdSummaries(_uiState.value.ads)
    }

    fun selectTag(tag: String?) {
        _uiState.update { current ->
            val nextTag = tag
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { selectedTag ->
                    if (selectedTag.equals(current.selectedTag, ignoreCase = true)) null else selectedTag
                }

            val allAds = current.filteredAds(selectedTag = nextTag)
            current.pageReset(
                ads = allAds.take(PageSize),
                hasMoreAds = allAds.size > PageSize,
                selectedTag = nextTag
            )
        }
        requestAiSummary(_uiState.value.ads)
        ensureAdSummaries(_uiState.value.ads)
    }

    fun clearFilters() {
        _uiState.update { current ->
            val allAds = repository.getAds()
            current.pageReset(
                ads = allAds.take(PageSize),
                hasMoreAds = allAds.size > PageSize,
                selectedChannel = null,
                searchText = "",
                selectedTag = null,
                showCollectedOnly = false
            )
        }
        requestAiSummary(_uiState.value.ads)
        ensureAdSummaries(_uiState.value.ads)
    }

    fun toggleCollectedOnly() {
        _uiState.update { current ->
            val nextShowCollectedOnly = !current.showCollectedOnly
            val allAds = current.filteredAds(showCollectedOnly = nextShowCollectedOnly)
            current.pageReset(
                ads = allAds.take(PageSize),
                hasMoreAds = allAds.size > PageSize,
                showCollectedOnly = nextShowCollectedOnly
            )
        }
        requestAiSummary(_uiState.value.ads)
        ensureAdSummaries(_uiState.value.ads)
    }

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
                        .count { ad -> it.collectedOverridesByAdId[ad.id] ?: ad.collected },
                    hasMoreAds = refreshedAds.size > PageSize,
                    currentPage = 1,
                    isLoadingMore = false,
                    loadMoreErrorMessage = null
                )
            }
            requestAiSummary(_uiState.value.ads)
            ensureAdSummaries(_uiState.value.ads)
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
            ensureAdSummaries(_uiState.value.ads)
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
            saveLocalState(nextState)
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
                    .count { repositoryAd -> nextOverrides[repositoryAd.id] ?: repositoryAd.collected },
                ads = nextAds.take(current.currentPage * PageSize),
                hasMoreAds = nextAds.size > current.currentPage * PageSize,
                loadMoreErrorMessage = null
            )
            saveLocalState(nextState)
            nextState
        }
        ensureAdSummaries(_uiState.value.ads)
    }

    fun shareAd(adId: Long): String? {
        val ad = repository.getAdById(adId) ?: return null
        track(ad, "share")
        return buildString {
            append(ad.brandName)
            append(" - ")
            append(ad.title)
            append('\n')
            append(ad.summary)
            if (ad.tags.isNotEmpty()) {
                append('\n')
                append(ad.tags.joinToString(separator = " ") { "#$it" })
            }
        }
    }

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

    override fun onCleared() {
        summaryScope.cancel()
        super.onCleared()
    }

    private fun requestAiSummary(ads: List<AdItem>) {
        _uiState.update {
            it.copy(
                aiSummary = "AI 摘要生成中...",
                isAiSummaryLoading = true,
                aiSummaryAdCount = ads.size
            )
        }
        summaryScope.launch {
            delay(AiSummaryStartDelayMillis)
            val summary = try {
                repository.generateAiSummary(ads)
            } catch (_: Exception) {
                "AI 摘要：生成失败，请稍后重试。"
            }
            _uiState.update {
                if (it.ads.map(AdItem::id) != ads.map(AdItem::id)) {
                    it
                } else {
                    it.copy(
                        aiSummary = summary,
                        isAiSummaryLoading = false,
                        aiSummaryAdCount = ads.size
                    )
                }
            }
        }
    }

    private fun ensureAdSummaries(ads: List<AdItem>) {
        if (ads.isEmpty()) {
            return
        }

        val adIds = ads.map(AdItem::id)
        val cachedSummaries = repository.getAdAiSummaries(adIds)
        val missingAds = ads.filter { ad ->
            cachedSummaries[ad.id].isNullOrBlank() &&
                _uiState.value.adAiSummariesByAdId[ad.id].isNullOrBlank() &&
                ad.id !in _uiState.value.generatingAdSummaryIds
        }

        _uiState.update { current ->
            current.copy(
                adAiSummariesByAdId = current.adAiSummariesByAdId + cachedSummaries,
                generatingAdSummaryIds = current.generatingAdSummaryIds + missingAds.map(AdItem::id)
            )
        }

        missingAds.forEach { ad ->
            summaryScope.launch {
                val summary = try {
                    repository.generateAdAiSummary(ad)
                } catch (_: Exception) {
                    "AI 摘要：生成失败，请稍后重试。"
                }
                repository.saveAdAiSummary(ad.id, summary)
                _uiState.update { current ->
                    current.copy(
                        adAiSummariesByAdId = current.adAiSummariesByAdId + (ad.id to summary),
                        generatingAdSummaryIds = current.generatingAdSummaryIds - ad.id
                    )
                }
            }
        }
    }

    private fun saveLocalState(state: AdFeedUiState) {
        localStateStore.save(
            AdLocalInteractionState(
                likedOverridesByAdId = state.likedOverridesByAdId,
                collectedOverridesByAdId = state.collectedOverridesByAdId
            )
        )
    }

    private fun track(ad: AdItem, eventName: String) {
        repository.track(
            TrackEvent(
                adId = ad.id,
                channel = ad.channel,
                eventName = eventName
            )
        )
    }

    private fun AdFeedUiState.pageReset(
        ads: List<AdItem>,
        hasMoreAds: Boolean,
        selectedChannel: Channel? = this.selectedChannel,
        searchText: String = this.searchText,
        selectedTag: String? = this.selectedTag,
        showCollectedOnly: Boolean = this.showCollectedOnly
    ): AdFeedUiState {
        return copy(
            selectedChannel = selectedChannel,
            searchText = searchText,
            selectedTag = selectedTag,
            showCollectedOnly = showCollectedOnly,
            ads = ads,
            hasMoreAds = hasMoreAds,
            currentPage = 1,
            isLoadingMore = false,
            loadMoreErrorMessage = null
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
}
