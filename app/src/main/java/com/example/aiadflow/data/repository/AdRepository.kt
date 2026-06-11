package com.example.aiadflow.data.repository

import com.example.aiadflow.data.mock.MockAdProvider
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent
import com.example.aiadflow.data.summary.AdSummaryDatabase
import com.example.aiadflow.data.summary.AdTagDatabase
import com.example.aiadflow.data.summary.AiSummaryClient
import com.example.aiadflow.data.summary.MockAdSummaryDatabase
import com.example.aiadflow.data.summary.MockAdTagDatabase

class AdRepository(
    private val adProvider: MockAdProvider = MockAdProvider,
    private val aiSummaryClient: AiSummaryClient = AiSummaryClient(),
    private val adSummaryDatabase: AdSummaryDatabase = MockAdSummaryDatabase,
    private val adTagDatabase: AdTagDatabase = MockAdTagDatabase
) {
    private companion object {
        val adSummaryCache = mutableMapOf<Long, String>()
        val adTagCache = mutableMapOf<Long, List<String>>()
    }

    private val keywordSeparator = Regex("[\\s,，、]+")
    private val channelCache = mutableMapOf<Channel?, List<AdItem>>()
    private val trackedEvents = mutableListOf<TrackEvent>()

    fun getChannels(): List<Channel> = adProvider.channels()

    fun getAds(
        channel: Channel? = null,
        query: String = "",
        selectedTag: String? = null,
        aiTagsByAdId: Map<Long, List<String>> = emptyMap()
    ): List<AdItem> {
        val keywords = normalizeKeywords(query)
        val normalizedTag = selectedTag?.trim().orEmpty()
        val tagAds = getCachedAds(channel).filterByTag(normalizedTag, aiTagsByAdId)

        if (keywords.isEmpty()) {
            return tagAds
        }

        return tagAds.filter { ad ->
            ad.matchesKeywords(keywords, aiTagsByAdId[ad.id].orEmpty())
        }
    }

    fun getAdById(adId: Long): AdItem? {
        return getCachedAds(null).firstOrNull { it.id == adId }
    }

    fun getAdAiSummaries(adIds: Collection<Long>): Map<Long, String> {
        val cachedSummaries = synchronized(adSummaryCache) {
            adIds.mapNotNull { adId -> adSummaryCache[adId]?.let { adId to it } }.toMap()
        }
        val missingIds = adIds.filterNot { it in cachedSummaries }
        if (missingIds.isEmpty()) {
            return cachedSummaries
        }

        val persistedSummaries = adSummaryDatabase.getSummaries(missingIds)
        if (persistedSummaries.isNotEmpty()) {
            synchronized(adSummaryCache) {
                adSummaryCache.putAll(persistedSummaries)
            }
        }
        return cachedSummaries + persistedSummaries
    }

    fun saveAdAiSummary(adId: Long, summary: String) {
        synchronized(adSummaryCache) {
            adSummaryCache[adId] = summary
        }
    }

    fun getAdAiTags(adIds: Collection<Long>): Map<Long, List<String>> {
        val cachedTags = synchronized(adTagCache) {
            adIds.mapNotNull { adId -> adTagCache[adId]?.let { adId to it } }.toMap()
        }
        val missingIds = adIds.filterNot { it in cachedTags }
        if (missingIds.isEmpty()) {
            return cachedTags
        }

        val persistedTags = adTagDatabase.getTags(missingIds)
        if (persistedTags.isNotEmpty()) {
            synchronized(adTagCache) {
                adTagCache.putAll(persistedTags)
            }
        }
        return cachedTags + persistedTags
    }

    fun saveAdAiTags(adId: Long, tags: List<String>) {
        synchronized(adTagCache) {
            adTagCache[adId] = tags
        }
    }

    fun syncAdAiSummaryCacheToDatabase(adIds: Collection<Long>? = null) {
        val summariesToPersist = synchronized(adSummaryCache) {
            if (adIds == null) {
                adSummaryCache.toMap()
            } else {
                adIds.mapNotNull { adId -> adSummaryCache[adId]?.let { adId to it } }.toMap()
            }
        }
        adSummaryDatabase.upsertSummaries(summariesToPersist)
    }

    fun syncAdAiTagCacheToDatabase(adIds: Collection<Long>? = null) {
        val tagsToPersist = synchronized(adTagCache) {
            if (adIds == null) {
                adTagCache.toMap()
            } else {
                adIds.mapNotNull { adId -> adTagCache[adId]?.let { adId to it } }.toMap()
            }
        }
        adTagDatabase.upsertTags(tagsToPersist)
    }

    suspend fun generateAdAiSummary(ad: AdItem): String {
        return aiSummaryClient.summarize(listOf(ad))
    }

    suspend fun generateAdAiTags(ad: AdItem): List<String> {
        return aiSummaryClient.generateTags(listOf(ad))
    }

    fun track(event: TrackEvent) {
        trackedEvents += event
    }

    fun getTrackedEvents(): List<TrackEvent> = trackedEvents.toList()

    private fun normalizeKeywords(query: String): List<String> {
        return query
            .trim()
            .split(keywordSeparator)
            .filter { it.isNotBlank() }
    }

    private fun AdItem.matchesKeywords(
        keywords: List<String>,
        aiTags: List<String>
    ): Boolean {
        return keywords.all { keyword ->
            title.contains(keyword, ignoreCase = true) ||
                summary.contains(keyword, ignoreCase = true) ||
                aiTags.any { it.contains(keyword, ignoreCase = true) }
        }
    }

    private fun List<AdItem>.filterByTag(
        selectedTag: String,
        aiTagsByAdId: Map<Long, List<String>>
    ): List<AdItem> {
        if (selectedTag.isBlank()) {
            return this
        }

        return filter { ad ->
            aiTagsByAdId[ad.id].orEmpty().any { it.equals(selectedTag, ignoreCase = true) }
        }
    }

    private fun getCachedAds(channel: Channel?): List<AdItem> {
        return channelCache.getOrPut(channel) {
            val allAds = channelCache.getOrPut(null) {
                adProvider.ads()
            }

            if (channel == null) {
                allAds
            } else {
                allAds.filter { it.channel == channel }
            }
        }
    }
}
