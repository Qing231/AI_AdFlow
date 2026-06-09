package com.example.aiadflow.ui.feed

import com.example.aiadflow.data.model.Channel
import org.junit.Assert.assertEquals
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
        viewModel.clearFilters()

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedChannel)
        assertEquals("", state.searchText)
        assertEquals(null, state.selectedTag)
        assertEquals(initialAdIds, state.ads.map { it.id })
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
    fun toggleLikeIgnoresMissingAdId() {
        val viewModel = AdFeedViewModel()
        val initialLikedOverrides = viewModel.uiState.value.likedOverridesByAdId

        viewModel.toggleLike(-1L)

        assertSame(initialLikedOverrides, viewModel.uiState.value.likedOverridesByAdId)
    }
}
