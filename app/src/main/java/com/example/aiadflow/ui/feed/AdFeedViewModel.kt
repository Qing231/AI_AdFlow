package com.example.aiadflow.ui.feed

import androidx.lifecycle.ViewModel
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent
import com.example.aiadflow.data.repository.AdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    /** 根据频道和搜索词过滤后的广告列表。 */
    val ads: List<AdItem> = emptyList(),
    /** 当前会话内累计记录的广告曝光次数。 */
    val totalExposureCount: Int = 0,
    /** 当前会话内按广告 id 聚合的曝光次数。 */
    val exposureCountsByAdId: Map<Long, Int> = emptyMap(),
    /** 是否正在加载数据，预留给后续真实接口接入。 */
    val isLoading: Boolean = false
)

/**
 * 广告信息流 ViewModel。
 *
 * 负责维护频道选择、搜索文本、广告列表刷新以及广告行为埋点。
 */
class AdFeedViewModel(
    /** 广告仓库，默认使用本地 mock 数据实现。 */
    private val repository: AdRepository = AdRepository()
) : ViewModel() {
    /** 可变内部状态，只允许 ViewModel 自己更新。 */
    private val _uiState = MutableStateFlow(
        AdFeedUiState(
            channels = repository.getChannels(),
            ads = repository.getAds()
        )
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
                ads = repository.getAds(nextChannel, current.searchText)
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
                ads = repository.getAds(current.selectedChannel, text)
            )
        }
    }

    /** 使用当前频道和搜索词重新拉取广告列表。 */
    fun refreshAds() {
        _uiState.update { current ->
            current.copy(
                ads = repository.getAds(current.selectedChannel, current.searchText)
            )
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
}
