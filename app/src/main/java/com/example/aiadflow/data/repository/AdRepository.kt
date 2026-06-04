package com.example.aiadflow.data.repository

import com.example.aiadflow.data.mock.MockAdProvider
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent

class AdRepository(
    private val adProvider: MockAdProvider = MockAdProvider
) {
    private val trackedEvents = mutableListOf<TrackEvent>()

    fun getChannels(): List<Channel> = adProvider.channels()

    fun getAds(
        channel: Channel,
        query: String = ""
    ): List<AdItem> {
        val normalizedQuery = query.trim()

        return adProvider.ads()
            .filter { it.channel == channel }
            .filter { ad ->
                normalizedQuery.isEmpty() ||
                    ad.brandName.contains(normalizedQuery, ignoreCase = true) ||
                    ad.title.contains(normalizedQuery, ignoreCase = true) ||
                    ad.summary.contains(normalizedQuery, ignoreCase = true) ||
                    ad.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
    }

    fun track(event: TrackEvent) {
        trackedEvents += event
    }

    fun getTrackedEvents(): List<TrackEvent> = trackedEvents.toList()
}
