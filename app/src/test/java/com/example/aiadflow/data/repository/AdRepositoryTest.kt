package com.example.aiadflow.data.repository

import com.example.aiadflow.data.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRepositoryTest {
    @Test
    fun getAdsReusesCachedChannelAdsWhenQueryIsBlank() {
        val repository = AdRepository()

        val firstResult = repository.getAds(Channel.Ecommerce)
        val secondResult = repository.getAds(Channel.Ecommerce)

        assertSame(firstResult, secondResult)
    }

    @Test
    fun getAdsFiltersFromChannelCache() {
        val repository = AdRepository()

        val ecommerceAds = repository.getAds(Channel.Ecommerce)
        val localAds = repository.getAds(Channel.Local)

        assertTrue(ecommerceAds.isNotEmpty())
        assertTrue(localAds.isNotEmpty())
        assertTrue(ecommerceAds.all { it.channel == Channel.Ecommerce })
        assertTrue(localAds.all { it.channel == Channel.Local })
    }

    @Test
    fun getAdsMatchesAllKeywordsAcrossSearchableFields() {
        val repository = AdRepository()

        val ads = repository.getAds(Channel.Ecommerce, "backpack commute")

        assertEquals(listOf(6L), ads.map { it.id })
    }

    @Test
    fun getAdsSplitsKeywordsByCommonSeparators() {
        val repository = AdRepository()

        val commaSeparatedAds = repository.getAds(Channel.Ecommerce, "backpack, commute")
        val chineseSeparatedAds = repository.getAds(Channel.Ecommerce, "backpack、commute")

        assertEquals(listOf(6L), commaSeparatedAds.map { it.id })
        assertEquals(listOf(6L), chineseSeparatedAds.map { it.id })
    }

    @Test
    fun getAdsReturnsEmptyWhenAnyKeywordDoesNotMatch() {
        val repository = AdRepository()

        val ads = repository.getAds(Channel.Ecommerce, "backpack bakery")

        assertTrue(ads.isEmpty())
    }

    @Test
    fun getAdsDoesNotMatchBrandName() {
        val repository = AdRepository()

        val ads = repository.getAds(Channel.Ecommerce, "Orbit")

        assertTrue(ads.isEmpty())
    }

    @Test
    fun getAdsFiltersBySelectedTag() {
        val repository = AdRepository()

        val ads = repository.getAds(selectedTag = "Local")

        assertTrue(ads.isNotEmpty())
        assertTrue(ads.all { ad ->
            ad.tags.any { it.equals("Local", ignoreCase = true) }
        })
    }

    @Test
    fun getAdsCombinesChannelKeywordAndSelectedTag() {
        val repository = AdRepository()

        val ads = repository.getAds(
            channel = Channel.Ecommerce,
            query = "backpack",
            selectedTag = "Commute"
        )

        assertEquals(listOf(6L), ads.map { it.id })
    }

    @Test
    fun getAdByIdReturnsMatchingAd() {
        val repository = AdRepository()

        val ad = repository.getAdById(6L)

        assertEquals(6L, ad?.id)
        assertEquals(Channel.Ecommerce, ad?.channel)
    }

    @Test
    fun getAdByIdReturnsNullWhenMissing() {
        val repository = AdRepository()

        val ad = repository.getAdById(-1L)

        assertEquals(null, ad)
    }

    @Test
    fun adAiSummaryCachePersistsAcrossRepositoryInstances() {
        val firstRepository = AdRepository()
        val secondRepository = AdRepository()

        firstRepository.saveAdAiSummary(6L, "Generated summary for ad 6")

        assertEquals("Generated summary for ad 6", secondRepository.getAdAiSummary(6L))
        assertEquals(
            mapOf(6L to "Generated summary for ad 6"),
            secondRepository.getAdAiSummaries(listOf(6L, -1L))
        )
    }
}
