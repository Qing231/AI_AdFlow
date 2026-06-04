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

data class AdFeedUiState(
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel = Channel.Featured,
    val searchText: String = "",
    val ads: List<AdItem> = emptyList(),
    val isLoading: Boolean = false
)

class AdFeedViewModel(
    private val repository: AdRepository = AdRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdFeedUiState(
            channels = repository.getChannels(),
            ads = repository.getAds(Channel.Featured)
        )
    )
    val uiState: StateFlow<AdFeedUiState> = _uiState.asStateFlow()

    fun selectChannel(channel: Channel) {
        _uiState.update { current ->
            current.copy(
                selectedChannel = channel,
                ads = repository.getAds(channel, current.searchText)
            )
        }
    }

    fun updateSearchText(text: String) {
        _uiState.update { current ->
            current.copy(searchText = text)
        }
    }

    fun refreshAds() {
        _uiState.update { current ->
            current.copy(
                ads = repository.getAds(current.selectedChannel, current.searchText)
            )
        }
    }

    fun trackAdImpression(ad: AdItem) {
        track(ad, "impression")
    }

    fun trackAdClick(ad: AdItem) {
        track(ad, "click")
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
}
