package com.example.aiadflow.ui.feed

import com.example.aiadflow.data.local.AdLocalInteractionState
import com.example.aiadflow.data.local.AdLocalStateStore
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.repository.AdRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFeedViewModelTest {
    @Test
    fun switchChannelFiltersAdsWithCurrentSearchText() {
        val viewModel = AdFeedViewModel()

        viewModel.updateSearchText("backpack")
        viewModel.switchChannel(Channel.Ecommerce)

        val state = viewModel.uiState.value
        assertEquals(Channel.Ecommerce, state.selectedChannel)
        assertEquals("backpack", state.searchText)
        assertTrue(state.ads.isNotEmpty())
        assertTrue(state.ads.all { it.channel == Channel.Ecommerce })
        assertTrue(state.ads.all { ad ->
            ad.title.contains("backpack", ignoreCase = true) ||
                ad.summary.contains("backpack", ignoreCase = true) ||
                ad.tags.any { it.contains("backpack", ignoreCase = true) }
        })
    }

    @Test
    fun switchChannelToAllKeepsSearchTextAndClearsSelectedChannel() {
        val viewModel = AdFeedViewModel()

        viewModel.updateSearchText("backpack")
        viewModel.switchChannel(Channel.Ecommerce)
        viewModel.switchChannel(null)

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedChannel)
        assertEquals("backpack", state.searchText)
        assertTrue(state.ads.isNotEmpty())
        assertTrue(state.ads.any { it.channel == Channel.Featured })
        assertTrue(state.ads.any { it.channel == Channel.Ecommerce })
    }

    @Test
    fun switchChannelIgnoresRepeatedSelection() {
        val viewModel = AdFeedViewModel()

        viewModel.switchChannel(Channel.Local)
        val stateAfterFirstSwitch = viewModel.uiState.value
        viewModel.switchChannel(Channel.Local)

        assertSame(stateAfterFirstSwitch, viewModel.uiState.value)
    }

    @Test
    fun updateSearchTextFiltersCurrentChannelAds() {
        val viewModel = AdFeedViewModel()

        viewModel.switchChannel(Channel.Ecommerce)
        viewModel.updateSearchText("backpack")

        val state = viewModel.uiState.value
        assertEquals("backpack", state.searchText)
        assertTrue(state.ads.isNotEmpty())
        assertTrue(state.ads.all { it.channel == Channel.Ecommerce })
        assertTrue(state.ads.all { ad ->
            ad.title.contains("backpack", ignoreCase = true) ||
                ad.summary.contains("backpack", ignoreCase = true) ||
            ad.tags.any { it.contains("backpack", ignoreCase = true) }
        })
    }

    @Test
    fun selectTagFiltersCurrentAds() {
        val viewModel = AdFeedViewModel()

        viewModel.selectTag("Local")

        val state = viewModel.uiState.value
        assertEquals("Local", state.selectedTag)
        assertTrue(state.ads.isNotEmpty())
        assertTrue(state.ads.all { ad ->
            ad.tags.any { it.equals("Local", ignoreCase = true) }
        })
    }

    @Test
    fun selectTagCombinesWithChannelAndSearchText() {
        val viewModel = AdFeedViewModel()

        viewModel.switchChannel(Channel.Ecommerce)
        viewModel.updateSearchText("backpack")
        viewModel.selectTag("Commute")

        val state = viewModel.uiState.value
        assertEquals(Channel.Ecommerce, state.selectedChannel)
        assertEquals("backpack", state.searchText)
        assertEquals("Commute", state.selectedTag)
        assertEquals(listOf(6L), state.ads.map { it.id })
    }

    @Test
    fun selectTagAgainClearsSelectedTag() {
        val viewModel = AdFeedViewModel()
        val initialAdCount = viewModel.uiState.value.ads.size

        viewModel.selectTag("Local")
        viewModel.selectTag("local")

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedTag)
        assertEquals(initialAdCount, state.ads.size)
    }

    @Test
    fun clearFiltersResetsChannelSearchTextSelectedTagAndAds() {
        val viewModel = AdFeedViewModel()
        val initialAdIds = viewModel.uiState.value.ads.map { it.id }

        viewModel.switchChannel(Channel.Ecommerce)
        viewModel.updateSearchText("backpack")
        viewModel.selectTag("Commute")
        viewModel.toggleCollectedOnly()
        viewModel.clearFilters()

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedChannel)
        assertEquals("", state.searchText)
        assertEquals(null, state.selectedTag)
        assertFalse(state.showCollectedOnly)
        assertEquals(initialAdIds, state.ads.map { it.id })
    }

    @Test
    fun toggleCollectedOnlyShowsCollectedAdsAcrossPages() {
        val viewModel = AdFeedViewModel()

        viewModel.toggleCollectedOnly()

        val state = viewModel.uiState.value
        assertTrue(state.showCollectedOnly)
        assertEquals(2, state.collectedCount)
        assertEquals(listOf(1L, 7L), state.ads.map { it.id })
        assertTrue(state.ads.all { ad ->
            state.collectedOverridesByAdId[ad.id] ?: ad.collected
        })
    }

    @Test
    fun toggleCollectedOnlyCombinesWithCurrentFilters() {
        val viewModel = AdFeedViewModel()

        viewModel.switchChannel(Channel.Ecommerce)
        viewModel.toggleCollectedOnly()

        val state = viewModel.uiState.value
        assertEquals(Channel.Ecommerce, state.selectedChannel)
        assertTrue(state.showCollectedOnly)
        assertEquals(listOf(7L), state.ads.map { it.id })
    }

    @Test
    fun initializesInteractionStateFromLocalStore() {
        val localStateStore = RecordingAdLocalStateStore(
            initialState = AdLocalInteractionState(
                likedOverridesByAdId = mapOf(4L to false),
                collectedOverridesByAdId = mapOf(
                    1L to false,
                    7L to false,
                    2L to true
                )
            )
        )

        val viewModel = AdFeedViewModel(localStateStore = localStateStore)

        val state = viewModel.uiState.value
        assertEquals(false, state.likedOverridesByAdId[4L])
        assertEquals(true, state.collectedOverridesByAdId[2L])
        assertEquals(1, state.collectedCount)
    }

    @Test
    fun getAdDetailReturnsAdByIdOutsideCurrentFilters() {
        val viewModel = AdFeedViewModel()

        viewModel.switchChannel(Channel.Local)

        val ad = viewModel.getAdDetail(6L)

        assertEquals(6L, ad?.id)
        assertEquals(Channel.Ecommerce, ad?.channel)
    }

    @Test
    fun trackAdImpressionUpdatesExposureStats() {
        val viewModel = AdFeedViewModel()
        val ad = viewModel.uiState.value.ads.first()

        viewModel.trackAdImpression(ad)
        viewModel.trackAdImpression(ad)

        val state = viewModel.uiState.value
        assertEquals(2, state.totalExposureCount)
        assertEquals(2, state.exposureCountsByAdId[ad.id])
    }

    @Test
    fun trackAdClickUpdatesClickStats() {
        val viewModel = AdFeedViewModel()
        val ad = viewModel.uiState.value.ads.first()

        viewModel.trackAdClick(ad)
        viewModel.trackAdClick(ad)

        val state = viewModel.uiState.value
        assertEquals(2, state.clickCount)
        assertEquals(2, state.clickCountsByAdId[ad.id])
    }

    @Test
    fun shareAdReturnsShareTextAndTracksShareEvent() {
        val repository = AdRepository()
        val viewModel = AdFeedViewModel(repository = repository)
        val ad = viewModel.uiState.value.ads.first()

        val shareText = viewModel.shareAd(ad.id)

        assertTrue(shareText?.contains(ad.brandName) == true)
        assertTrue(shareText?.contains(ad.title) == true)
        assertTrue(shareText?.contains("#${ad.tags.first()}") == true)
        val event = repository.getTrackedEvents().last()
        assertEquals(ad.id, event.adId)
        assertEquals("share", event.eventName)
    }

    @Test
    fun shareAdReturnsNullAndDoesNotTrackMissingAdId() {
        val repository = AdRepository()
        val viewModel = AdFeedViewModel(repository = repository)

        val shareText = viewModel.shareAd(-1L)

        assertEquals(null, shareText)
        assertTrue(repository.getTrackedEvents().isEmpty())
    }

    @Test
    fun toggleLikeLikesUnlikedAd() {
        val viewModel = AdFeedViewModel()
        val ad = viewModel.uiState.value.ads.first { !it.liked }

        viewModel.toggleLike(ad.id)

        val state = viewModel.uiState.value
        assertEquals(true, state.likedOverridesByAdId[ad.id])
    }

    @Test
    fun toggleLikeUnlikesInitiallyLikedAd() {
        val viewModel = AdFeedViewModel()
        val ad = viewModel.uiState.value.ads.first { it.liked }

        viewModel.toggleLike(ad.id)

        val state = viewModel.uiState.value
        assertEquals(false, state.likedOverridesByAdId[ad.id])
    }

    @Test
    fun toggleLikeSavesLocalState() {
        val localStateStore = RecordingAdLocalStateStore()
        val viewModel = AdFeedViewModel(localStateStore = localStateStore)
        val ad = viewModel.uiState.value.ads.first { !it.liked }

        viewModel.toggleLike(ad.id)

        assertEquals(true, localStateStore.savedState?.likedOverridesByAdId?.get(ad.id))
    }

    @Test
    fun toggleLikeIgnoresMissingAdId() {
        val viewModel = AdFeedViewModel()
        val initialLikedOverrides = viewModel.uiState.value.likedOverridesByAdId

        viewModel.toggleLike(-1L)

        assertSame(initialLikedOverrides, viewModel.uiState.value.likedOverridesByAdId)
    }

    @Test
    fun toggleCollectCollectsUncollectedAd() {
        val viewModel = AdFeedViewModel()
        val ad = viewModel.uiState.value.ads.first { !it.collected }

        viewModel.toggleCollect(ad.id)

        val state = viewModel.uiState.value
        assertEquals(true, state.collectedOverridesByAdId[ad.id])
    }

    @Test
    fun toggleCollectSavesLocalState() {
        val localStateStore = RecordingAdLocalStateStore()
        val viewModel = AdFeedViewModel(localStateStore = localStateStore)
        val ad = viewModel.uiState.value.ads.first { !it.collected }

        viewModel.toggleCollect(ad.id)

        assertEquals(true, localStateStore.savedState?.collectedOverridesByAdId?.get(ad.id))
    }

    @Test
    fun toggleCollectUncollectsInitiallyCollectedAd() {
        val viewModel = AdFeedViewModel()
        val ad = viewModel.uiState.value.ads.first { it.collected }

        viewModel.toggleCollect(ad.id)

        val state = viewModel.uiState.value
        assertEquals(false, state.collectedOverridesByAdId[ad.id])
        assertEquals(1, state.collectedCount)
    }

    @Test
    fun toggleCollectRemovesUncollectedAdFromCollectedOnlyList() {
        val viewModel = AdFeedViewModel()

        viewModel.toggleCollectedOnly()
        viewModel.toggleCollect(1L)

        val state = viewModel.uiState.value
        assertTrue(state.showCollectedOnly)
        assertEquals(1, state.collectedCount)
        assertEquals(listOf(7L), state.ads.map { it.id })
    }

    @Test
    fun toggleCollectIgnoresMissingAdId() {
        val viewModel = AdFeedViewModel()
        val initialCollectedOverrides = viewModel.uiState.value.collectedOverridesByAdId

        viewModel.toggleCollect(-1L)

        assertSame(initialCollectedOverrides, viewModel.uiState.value.collectedOverridesByAdId)
    }
}

private class RecordingAdLocalStateStore(
    private val initialState: AdLocalInteractionState = AdLocalInteractionState()
) : AdLocalStateStore {
    var savedState: AdLocalInteractionState? = null
        private set

    override fun load(): AdLocalInteractionState = initialState

    override fun save(state: AdLocalInteractionState) {
        savedState = state
    }
}
