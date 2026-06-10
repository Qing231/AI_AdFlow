package com.example.aiadflow.data.repository

import com.example.aiadflow.data.mock.MockAdProvider
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent
import com.example.aiadflow.data.summary.AiSummaryClient

/**
 * 广告数据仓库。
 *
 * 负责从数据源读取频道和广告，并提供搜索过滤与埋点记录能力。
 */
class AdRepository(
    /** 广告数据来源，当前默认使用本地 mock provider。 */
    private val adProvider: MockAdProvider = MockAdProvider,
    private val aiSummaryClient: AiSummaryClient = AiSummaryClient()
) {
    private companion object {
        val adSummaryCache = mutableMapOf<Long, String>()
    }

    private val keywordSeparator = Regex("[\\s,，、]+")
    private val channelCache = mutableMapOf<Channel?, List<AdItem>>()

    /** 内存中的埋点事件列表，便于开发阶段验证点击和曝光行为。 */
    private val trackedEvents = mutableListOf<TrackEvent>()

    /** 获取信息流支持的频道列表。 */
    fun getChannels(): List<Channel> = adProvider.channels()

    /**
     * 按频道和搜索词获取广告。
     *
     * 搜索词会匹配品牌名、标题、摘要和标签。
     */
    fun getAds(
        channel: Channel? = null,
        query: String = "",
        selectedTag: String? = null
    ): List<AdItem> {
        val keywords = normalizeKeywords(query)
        val normalizedTag = selectedTag?.trim().orEmpty()
        val tagAds = getCachedAds(channel).filterByTag(normalizedTag)

        if (keywords.isEmpty()) {
            return tagAds
        }

        return tagAds.filter { ad -> ad.matchesKeywords(keywords) }
    }

    fun getAdById(adId: Long): AdItem? {
        return getCachedAds(null).firstOrNull { it.id == adId }
    }

    suspend fun generateAiSummary(ads: List<AdItem>): String = aiSummaryClient.summarize(ads)

    fun getAdAiSummary(adId: Long): String? = synchronized(adSummaryCache) {
        adSummaryCache[adId]
    }

    fun getAdAiSummaries(adIds: Collection<Long>): Map<Long, String> = synchronized(adSummaryCache) {
        adIds.mapNotNull { adId -> adSummaryCache[adId]?.let { adId to it } }.toMap()
    }

    fun saveAdAiSummary(adId: Long, summary: String) {
        synchronized(adSummaryCache) {
            adSummaryCache[adId] = summary
        }
    }

    suspend fun generateAdAiSummary(ad: AdItem): String {
        return aiSummaryClient.summarize(listOf(ad))
    }

    private fun normalizeKeywords(query: String): List<String> {
        return query
            .trim()
            .split(keywordSeparator)
            .filter { it.isNotBlank() }
    }

    private fun AdItem.matchesKeywords(keywords: List<String>): Boolean {
        return keywords.all { keyword ->
            title.contains(keyword, ignoreCase = true) ||
                summary.contains(keyword, ignoreCase = true) ||
                tags.any { it.contains(keyword, ignoreCase = true) }
        }
    }

    private fun List<AdItem>.filterByTag(selectedTag: String): List<AdItem> {
        if (selectedTag.isBlank()) {
            return this
        }

        return filter { ad ->
            ad.tags.any { it.equals(selectedTag, ignoreCase = true) }
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

    /** 记录一条广告行为事件。 */
    fun track(event: TrackEvent) {
        trackedEvents += event
    }

    /** 返回已记录埋点事件的只读副本，避免外部直接修改内部列表。 */
    fun getTrackedEvents(): List<TrackEvent> = trackedEvents.toList()
}
