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
            ad.brandName.contains("backpack", ignoreCase = true) ||
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
            ad.brandName.contains("backpack", ignoreCase = true) ||
                ad.title.contains("backpack", ignoreCase = true) ||
                ad.summary.contains("backpack", ignoreCase = true) ||
                ad.tags.any { it.contains("backpack", ignoreCase = true) }
        })
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
}
