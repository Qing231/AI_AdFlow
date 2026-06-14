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
import com.example.aiadflow.data.search.SemanticSearchResult
import kotlinx.coroutines.Job
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
    val isAiSearchUnderstanding: Boolean = false,
    val activeAiSearchQuery: String? = null,
    val aiSearchUnderstanding: String = "",
    val aiSearchSuggestedTags: List<String> = emptyList(),
    val aiSearchResultCount: Int = 0,
    val selectedTag: String? = null,
    val ads: List<AdItem> = emptyList(),
    val totalExposureCount: Int = 0,
    val exposureCountsByAdId: Map<Long, Int> = emptyMap(),
    val clickCount: Int = 0,
    val clickCountsByAdId: Map<Long, Int> = emptyMap(),
    val likedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val collectedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val likeCountsByAdId: Map<Long, Int> = emptyMap(),
    val collectCountsByAdId: Map<Long, Int> = emptyMap(),
    val showCollectedOnly: Boolean = false,
    val collectedCount: Int = 0,
    val adAiSummariesByAdId: Map<Long, String> = emptyMap(),
    val generatingAdSummaryIds: Set<Long> = emptySet(),
    val adAiTagsByAdId: Map<Long, List<String>> = emptyMap(),
    val generatingAdTagIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreAds: Boolean = true,
    val loadMoreErrorMessage: String? = null,
    val currentPage: Int = 1,
    val conversationDraft: String = "",
    val conversationMessages: List<ConversationSearchMessage> = emptyList()
)

data class ConversationSearchMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

class AdFeedViewModel(
    private val repository: AdRepository = AdRepository(),
    private val localStateStore: AdLocalStateStore = InMemoryAdLocalStateStore
) : ViewModel() {
    private companion object {
        const val PageSize = 6
        const val LoadMoreDelayMillis = 350L
        const val SearchUnderstandingDelayMillis = 280L
    }

    private val summaryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tagScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null
    private var conversationMessageId = 0L

    private val _uiState = MutableStateFlow(
        run {
            val initialAds = repository.getAds()
            val localState = localStateStore.load()
            AdFeedUiState(
                channels = repository.getChannels(),
                ads = initialAds.take(PageSize),
                likedOverridesByAdId = localState.likedOverridesByAdId,
                collectedOverridesByAdId = localState.collectedOverridesByAdId,
                likeCountsByAdId = initialAds.associate { ad ->
                    ad.id to (localState.likeCountsByAdId[ad.id] ?: ad.effectiveInitialLikeCount())
                },
                collectCountsByAdId = initialAds.associate { ad ->
                    ad.id to (localState.collectCountsByAdId[ad.id] ?: ad.effectiveInitialCollectCount())
                },
                collectedCount = initialAds.count { localState.collectedOverridesByAdId[it.id] ?: it.collected },
                adAiSummariesByAdId = repository.getAdAiSummaries(initialAds.take(PageSize).map(AdItem::id)),
                adAiTagsByAdId = repository.getAdAiTags(initialAds.take(PageSize).map(AdItem::id)),
                aiSearchResultCount = initialAds.size,
                hasMoreAds = initialAds.size > PageSize,
                currentPage = 1
            )
        }
    )
    val uiState: StateFlow<AdFeedUiState> = _uiState.asStateFlow()

    init {
        ensureAiFields(_uiState.value.ads)
    }

    fun switchChannel(channel: Channel?) {
        searchJob?.cancel()
        _uiState.update { current ->
            val nextChannel = channel?.takeIf { it in current.channels }
            if (nextChannel == current.selectedChannel) {
                return@update current
            }

            val page = current.buildSearchPage(
                channel = nextChannel,
                query = current.searchText,
                selectedTag = current.selectedTag
            )
            current.pageReset(
                page = page,
                selectedChannel = nextChannel
            )
        }
        val activeQuery = _uiState.value.activeAiSearchQuery
        if (!activeQuery.isNullOrBlank()) {
            startAiSearch(
                query = activeQuery,
                appendConversationReply = false
            )
            return
        }
        ensureAiFields(_uiState.value.ads)
    }

    fun selectChannel(channel: Channel?) {
        switchChannel(channel)
    }

    fun updateSearchText(text: String) {
        searchJob?.cancel()
        if (text.isBlank()) {
            applySearch(text, force = true)
            return
        }

        startAiSearch(
            query = text,
            appendConversationReply = false,
            delayMillis = SearchUnderstandingDelayMillis
        )
    }

    fun submitSearch() {
        searchJob?.cancel()
        val query = _uiState.value.searchText.trim()
        if (query.isBlank()) {
            applySearch("")
        } else {
            startAiSearch(
                query = query,
                appendConversationReply = false
            )
        }
    }

    fun updateConversationDraft(text: String) {
        _uiState.update { it.copy(conversationDraft = text) }
    }

    fun submitConversationalSearch() {
        val query = _uiState.value.conversationDraft.trim()
        if (query.isBlank()) {
            return
        }

        searchJob?.cancel()
        startAiSearch(
            query = query,
            appendConversationReply = true
        )
    }

    fun clearConversation() {
        searchJob?.cancel()
        _uiState.update {
            val keepActiveAiSearch = !it.isAiSearchUnderstanding
            it.copy(
                conversationDraft = "",
                conversationMessages = emptyList(),
                isAiSearchUnderstanding = false,
                activeAiSearchQuery = if (keepActiveAiSearch) it.activeAiSearchQuery else null
            )
        }
    }

    private fun startAiSearch(
        query: String,
        appendConversationReply: Boolean,
        delayMillis: Long = 0L
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            // 空输入直接回退本地搜索，避免保留上一轮 AI 查询状态。
            applySearch("")
            return
        }

        _uiState.update { current ->
            current.copy(
                conversationDraft = if (appendConversationReply) "" else current.conversationDraft,
                searchText = query,
                isAiSearchUnderstanding = true,
                activeAiSearchQuery = trimmedQuery,
                ads = emptyList(),
                aiSearchUnderstanding = "",
                aiSearchSuggestedTags = emptyList(),
                aiSearchResultCount = 0,
                hasMoreAds = false,
                currentPage = 1,
                loadMoreErrorMessage = null,
                conversationMessages = if (appendConversationReply) {
                    current.conversationMessages + ConversationSearchMessage(
                        id = nextConversationMessageId(),
                        text = trimmedQuery,
                        isUser = true
                    )
                } else {
                    current.conversationMessages
                }
            )
        }

        searchJob = viewModelScope.launch {
            if (delayMillis > 0L) {
                delay(delayMillis)
            }

            val page = _uiState.value.buildAiSearchPage(query = trimmedQuery)
            _uiState.update { current ->
                if (current.searchText.trim() != trimmedQuery || current.activeAiSearchQuery != trimmedQuery) {
                    return@update current
                }

                val resetState = current.pageReset(
                    page = page,
                    searchText = trimmedQuery,
                    isAiSearchUnderstanding = false,
                    activeAiSearchQuery = trimmedQuery
                )
                if (appendConversationReply) {
                    resetState.copy(
                        conversationDraft = "",
                        conversationMessages = current.conversationMessages + ConversationSearchMessage(
                            id = nextConversationMessageId(),
                            text = buildConversationSearchReply(trimmedQuery, page),
                            isUser = false
                        )
                    )
                } else {
                    resetState
                }
            }
            ensureAiFields(_uiState.value.ads)
        }
    }

    private fun applySearch(query: String, force: Boolean = false) {
        _uiState.update { current ->
            if (!force && current.searchText != query) {
                return@update current
            }

            val page = current.buildSearchPage(query = query)
            current.pageReset(
                page = page,
                searchText = query,
                isAiSearchUnderstanding = false,
                activeAiSearchQuery = null
            )
        }
        ensureAiFields(_uiState.value.ads)
    }

    fun selectTag(tag: String?) {
        searchJob?.cancel()
        _uiState.update { current ->
            val nextTag = tag
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { selectedTag ->
                    if (selectedTag.equals(current.selectedTag, ignoreCase = true)) null else selectedTag
                }

            val page = current.buildSearchPage(selectedTag = nextTag)
            current.pageReset(
                page = page,
                selectedTag = nextTag
            )
        }
        val activeQuery = _uiState.value.activeAiSearchQuery
        if (!activeQuery.isNullOrBlank()) {
            startAiSearch(
                query = activeQuery,
                appendConversationReply = false
            )
            return
        }
        ensureAiFields(_uiState.value.ads)
    }

    fun clearFilters() {
        searchJob?.cancel()
        _uiState.update { current ->
            val page = current.buildSearchPage(
                channel = null,
                query = "",
                selectedTag = null,
                showCollectedOnly = false
            )
            current.pageReset(
                page = page,
                selectedChannel = null,
                searchText = "",
                selectedTag = null,
                showCollectedOnly = false,
                activeAiSearchQuery = null
            )
        }
        ensureAiFields(_uiState.value.ads)
    }

    fun toggleCollectedOnly() {
        searchJob?.cancel()
        _uiState.update { current ->
            val nextShowCollectedOnly = !current.showCollectedOnly
            val page = current.buildSearchPage(showCollectedOnly = nextShowCollectedOnly)
            current.pageReset(
                page = page,
                showCollectedOnly = nextShowCollectedOnly
            )
        }
        val activeQuery = _uiState.value.activeAiSearchQuery
        if (!activeQuery.isNullOrBlank()) {
            startAiSearch(
                query = activeQuery,
                appendConversationReply = false
            )
            return
        }
        ensureAiFields(_uiState.value.ads)
    }

    fun refreshAds(): Boolean {
        val current = _uiState.value
        if (!current.activeAiSearchQuery.isNullOrBlank()) {
            // 当前正在使用 AI 解释时，刷新要保持同一查询语义，不能切回规则搜索。
            startAiSearch(
                query = current.activeAiSearchQuery,
                appendConversationReply = false
            )
            return true
        }

        return try {
            val page = current.buildSearchPage(page = 1)
            _uiState.update {
                it.copy(
                    ads = page.ads,
                    collectedCount = repository.getAds()
                        .count { ad -> it.collectedOverridesByAdId[ad.id] ?: ad.collected },
                    hasMoreAds = page.hasMoreAds,
                    currentPage = 1,
                    isAiSearchUnderstanding = false,
                    activeAiSearchQuery = null,
                    aiSearchUnderstanding = page.interpretation,
                    aiSearchSuggestedTags = page.suggestedTags,
                    aiSearchResultCount = page.totalCount,
                    isLoadingMore = false,
                    loadMoreErrorMessage = null
                )
            }
            ensureAiFields(_uiState.value.ads)
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
            // 这些边界都表示当前不应该继续分页，避免重复请求或空列表翻页。
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
            val latest = _uiState.value
            if (!latest.isLoadingMore) {
                return@launch
            }

            try {
                val nextPage = latest.currentPage + 1
                val page = if (latest.activeAiSearchQuery.isNullOrBlank()) {
                    latest.buildSearchPage(page = nextPage)
                } else {
                    latest.buildAiSearchPage(
                        query = latest.activeAiSearchQuery,
                        page = nextPage
                    )
                }

                _uiState.update { current ->
                    if (!current.isLoadingMore) {
                        return@update current
                    }
                    current.copy(
                        ads = page.ads,
                        currentPage = nextPage,
                        hasMoreAds = page.hasMoreAds,
                        aiSearchUnderstanding = page.interpretation,
                        aiSearchSuggestedTags = page.suggestedTags,
                        aiSearchResultCount = page.totalCount,
                        isLoadingMore = false,
                        loadMoreErrorMessage = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoadingMore = false,
                        loadMoreErrorMessage = "加载失败，请重试"
                    )
                }
            }
            ensureAiFields(_uiState.value.ads)
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
            val nextLiked = !currentLiked
            val currentCount = current.likeCountsByAdId[adId] ?: ad.effectiveInitialLikeCount()
            val nextState = current.copy(
                likedOverridesByAdId = current.likedOverridesByAdId + (adId to nextLiked),
                likeCountsByAdId = current.likeCountsByAdId + (adId to currentCount.adjustCount(nextLiked))
            )
            saveLocalState(nextState)
            nextState
        }
    }

    fun toggleCollect(adId: Long) {
        val ad = repository.getAdById(adId) ?: return
        _uiState.update { current ->
            val currentCollected = current.collectedOverridesByAdId[adId] ?: ad.collected
            val nextCollected = !currentCollected
            val currentCount = current.collectCountsByAdId[adId] ?: ad.effectiveInitialCollectCount()
            val nextOverrides = current.collectedOverridesByAdId + (adId to nextCollected)
            val page = current.copy(collectedOverridesByAdId = nextOverrides)
                .buildSearchPage(
                    collectedOverridesByAdId = nextOverrides,
                    page = current.currentPage
                )
            val nextState = current.copy(
                collectedOverridesByAdId = nextOverrides,
                collectCountsByAdId = current.collectCountsByAdId + (adId to currentCount.adjustCount(nextCollected)),
                collectedCount = repository.getAds()
                    .count { repositoryAd -> nextOverrides[repositoryAd.id] ?: repositoryAd.collected },
                ads = page.ads,
                hasMoreAds = page.hasMoreAds,
                aiSearchUnderstanding = page.interpretation,
                aiSearchSuggestedTags = page.suggestedTags,
                aiSearchResultCount = page.totalCount,
                loadMoreErrorMessage = null
            )
            saveLocalState(nextState)
            nextState
        }
        ensureAiFields(_uiState.value.ads)
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
            val tags = _uiState.value.adAiTagsByAdId[ad.id].orEmpty()
            if (tags.isNotEmpty()) {
                append('\n')
                append(tags.joinToString(separator = " ") { "#$it" })
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
        searchJob?.cancel()
        summaryScope.cancel()
        tagScope.cancel()
        super.onCleared()
    }

    private fun ensureAdSummaries(ads: List<AdItem>) {
        if (ads.isEmpty()) {
            return
        }

        val adIds = ads.map(AdItem::id)
        val cachedSummaries = repository.getAdAiSummaries(adIds)
        // 只补齐未缓存且未在生成中的摘要，避免滚动时重复开后台任务。
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
                repository.syncAdAiSummaryCacheToDatabase(listOf(ad.id))
                _uiState.update { current ->
                    val nextSummariesByAdId = current.adAiSummariesByAdId + (ad.id to summary)
                    val nextState = current.copy(
                        adAiSummariesByAdId = nextSummariesByAdId,
                        generatingAdSummaryIds = current.generatingAdSummaryIds - ad.id
                    )
                    if (current.searchText.isBlank()) {
                        nextState
                    } else if (!current.activeAiSearchQuery.isNullOrBlank()) {
                        nextState
                    } else {
                        val page = nextState.buildSearchPage(
                            aiSummariesByAdId = nextSummariesByAdId,
                            page = current.currentPage
                        )
                        nextState.copy(
                            ads = page.ads,
                            hasMoreAds = page.hasMoreAds,
                            aiSearchUnderstanding = page.interpretation,
                            aiSearchSuggestedTags = page.suggestedTags,
                            aiSearchResultCount = page.totalCount
                        )
                    }
                }
            }
        }
    }

    private fun ensureAdTags(ads: List<AdItem>) {
        if (ads.isEmpty()) {
            return
        }

        val adIds = ads.map(AdItem::id)
        val cachedTags = repository.getAdAiTags(adIds)
        // 智能标签生成失败时允许保留原标签；成功结果会写入缓存和本地数据库。
        val missingAds = ads.filter { ad ->
            cachedTags[ad.id].isNullOrEmpty() &&
                _uiState.value.adAiTagsByAdId[ad.id].isNullOrEmpty() &&
                ad.id !in _uiState.value.generatingAdTagIds
        }

        _uiState.update { current ->
            current.copy(
                adAiTagsByAdId = current.adAiTagsByAdId + cachedTags,
                generatingAdTagIds = current.generatingAdTagIds + missingAds.map(AdItem::id)
            )
        }

        missingAds.forEach { ad ->
            tagScope.launch {
                val tags = try {
                    repository.generateAdAiTags(ad)
                } catch (_: Exception) {
                    emptyList()
                }
                repository.saveAdAiTags(ad.id, tags)
                repository.syncAdAiTagCacheToDatabase(listOf(ad.id))
                _uiState.update { current ->
                    val nextTagsByAdId = if (tags.isEmpty()) {
                        current.adAiTagsByAdId
                    } else {
                        current.adAiTagsByAdId + (ad.id to tags)
                    }
                    val nextState = current.copy(
                        adAiTagsByAdId = nextTagsByAdId,
                        generatingAdTagIds = current.generatingAdTagIds - ad.id
                    )
                    if (current.searchText.isBlank()) {
                        nextState
                    } else if (!current.activeAiSearchQuery.isNullOrBlank()) {
                        nextState
                    } else {
                        val page = nextState.buildSearchPage(
                            aiTagsByAdId = nextTagsByAdId,
                            page = current.currentPage
                        )
                        nextState.copy(
                            ads = page.ads,
                            hasMoreAds = page.hasMoreAds,
                            aiSearchUnderstanding = page.interpretation,
                            aiSearchSuggestedTags = page.suggestedTags,
                            aiSearchResultCount = page.totalCount
                        )
                    }
                }
            }
        }
    }

    private fun ensureAiFields(ads: List<AdItem>) {
        ensureAdSummaries(ads)
        ensureAdTags(ads)
    }

    private fun saveLocalState(state: AdFeedUiState) {
        localStateStore.save(
            AdLocalInteractionState(
                likedOverridesByAdId = state.likedOverridesByAdId,
                collectedOverridesByAdId = state.collectedOverridesByAdId,
                likeCountsByAdId = state.likeCountsByAdId,
                collectCountsByAdId = state.collectCountsByAdId
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
        page: SearchPage,
        selectedChannel: Channel? = this.selectedChannel,
        searchText: String = this.searchText,
        selectedTag: String? = this.selectedTag,
        showCollectedOnly: Boolean = this.showCollectedOnly,
        isAiSearchUnderstanding: Boolean = false,
        activeAiSearchQuery: String? = this.activeAiSearchQuery
    ): AdFeedUiState {
        return copy(
            selectedChannel = selectedChannel,
            searchText = searchText,
            selectedTag = selectedTag,
            showCollectedOnly = showCollectedOnly,
            ads = page.ads,
            hasMoreAds = page.hasMoreAds,
            isAiSearchUnderstanding = isAiSearchUnderstanding,
            activeAiSearchQuery = activeAiSearchQuery,
            aiSearchUnderstanding = page.interpretation,
            aiSearchSuggestedTags = page.suggestedTags,
            aiSearchResultCount = page.totalCount,
            currentPage = 1,
            isLoadingMore = false,
            loadMoreErrorMessage = null
        )
    }

    private fun AdFeedUiState.buildSearchPage(
        channel: Channel? = selectedChannel,
        query: String = searchText,
        selectedTag: String? = this.selectedTag,
        showCollectedOnly: Boolean = this.showCollectedOnly,
        collectedOverridesByAdId: Map<Long, Boolean> = this.collectedOverridesByAdId,
        aiTagsByAdId: Map<Long, List<String>> = this.adAiTagsByAdId,
        aiSummariesByAdId: Map<Long, String> = this.adAiSummariesByAdId,
        page: Int = 1
    ): SearchPage {
        val result = repository.searchAds(
            channel = channel,
            query = query,
            selectedTag = selectedTag,
            aiTagsByAdId = aiTagsByAdId,
            aiSummariesByAdId = aiSummariesByAdId
        )
        return result.toSearchPage(
            showCollectedOnly = showCollectedOnly,
            collectedOverridesByAdId = collectedOverridesByAdId,
            page = page
        )
    }

    private suspend fun AdFeedUiState.buildAiSearchPage(
        channel: Channel? = selectedChannel,
        query: String = searchText,
        selectedTag: String? = this.selectedTag,
        showCollectedOnly: Boolean = this.showCollectedOnly,
        collectedOverridesByAdId: Map<Long, Boolean> = this.collectedOverridesByAdId,
        aiTagsByAdId: Map<Long, List<String>> = this.adAiTagsByAdId,
        aiSummariesByAdId: Map<Long, String> = this.adAiSummariesByAdId,
        page: Int = 1
    ): SearchPage {
        val result = repository.searchAdsWithAiUnderstanding(
            channel = channel,
            query = query,
            selectedTag = selectedTag,
            aiTagsByAdId = aiTagsByAdId,
            aiSummariesByAdId = aiSummariesByAdId
        )
        return result.toSearchPage(
            showCollectedOnly = showCollectedOnly,
            collectedOverridesByAdId = collectedOverridesByAdId,
            page = page
        )
    }

    private fun SemanticSearchResult.toSearchPage(
        showCollectedOnly: Boolean,
        collectedOverridesByAdId: Map<Long, Boolean>,
        page: Int
    ): SearchPage {
        val filteredAds = ads.filterCollected(showCollectedOnly, collectedOverridesByAdId)
        val limit = page * PageSize
        return SearchPage(
            ads = filteredAds.take(limit),
            totalCount = filteredAds.size,
            hasMoreAds = filteredAds.size > limit,
            interpretation = interpretation,
            suggestedTags = suggestedTags,
            usedAiUnderstanding = usedAiUnderstanding,
            fallbackReason = fallbackReason
        )
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

    private fun AdItem.effectiveInitialLikeCount(): Int {
        return likeCount.coerceAtLeast(if (liked) 1 else 0)
    }

    private fun AdItem.effectiveInitialCollectCount(): Int {
        return collectCount.coerceAtLeast(if (collected) 1 else 0)
    }

    private fun Int.adjustCount(selected: Boolean): Int {
        return (this + if (selected) 1 else -1).coerceAtLeast(0)
    }

    private fun nextConversationMessageId(): Long {
        conversationMessageId += 1
        return conversationMessageId
    }

    private fun buildConversationSearchReply(query: String, page: SearchPage): String {
        val intent = page.interpretation.takeIf { it.isNotBlank() } ?: query
        val fallbackPrefix = if (page.fallbackReason == null) {
            ""
        } else {
            "AI 理解暂不可用，已使用本地语义搜索。"
        }

        if (page.totalCount == 0) {
            return fallbackPrefix + "没有找到和“$intent”匹配的广告。可以换成品牌、场景、品类或标签再试一次。"
        }

        val tags = page.suggestedTags
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " ") { "#$it" }

        return buildString {
            append(fallbackPrefix)
            append(if (page.usedAiUnderstanding) "AI 已理解为“" else "已按“")
            append(intent)
            append("”为你筛出 ")
            append(page.totalCount)
            append(" 条广告。")
            if (tags != null) {
                append(" 推荐关注 ")
                append(tags)
                append("。")
            }
        }
    }

    private data class SearchPage(
        val ads: List<AdItem>,
        val totalCount: Int,
        val hasMoreAds: Boolean,
        val interpretation: String,
        val suggestedTags: List<String>,
        val usedAiUnderstanding: Boolean = false,
        val fallbackReason: String? = null
    )
}
