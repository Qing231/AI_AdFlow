package com.example.aiadflow.data.mock

import com.example.aiadflow.data.model.AdType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAdProviderTest {
    @Test
    fun adsHaveUniqueIds() {
        val ads = MockAdProvider.ads()

        assertEquals(ads.size, ads.map { it.id }.toSet().size)
    }

    @Test
    fun videoAdsHaveVideoAndCoverUrls() {
        val videoAds = MockAdProvider.ads().filter { it.type == AdType.Video }

        assertTrue(videoAds.isNotEmpty())
        assertTrue(videoAds.all { !it.videoUrl.isNullOrBlank() })
        assertTrue(videoAds.all { !it.coverUrl.isNullOrBlank() })
    }
}
