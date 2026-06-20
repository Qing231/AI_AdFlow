package com.example.aiadflow.data.search

import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.summary.AiSummaryClient

data class SemanticSearchResult(
    val ads: List<AdItem>,
    val interpretation: String = "",
    val suggestedTags: List<String> = emptyList(),
    val expandedTerms: List<String> = emptyList(),
    val usedAiUnderstanding: Boolean = false,
    val fallbackReason: String? = null
)

class SmartSearchService(
    private val aiSummaryClient: AiSummaryClient
) {
    suspend fun search(
        query: String,
        baseAds: List<AdItem>,
        aiTagsByAdId: Map<Long, List<String>>,
        aiSummariesByAdId: Map<Long, String>,
        fallback: () -> SemanticSearchResult
    ): SemanticSearchResult {
        if (query.isBlank()) {
            return SemanticSearchResult(ads = baseAds)
        }

        return try {
            val enrichedAds = baseAds.map { ad ->
                val aiTags = aiTagsByAdId[ad.id].orEmpty()
                ad.copy(
                    summary = aiSummariesByAdId[ad.id]?.takeIf { it.isNotBlank() } ?: ad.summary,
                    tags = (ad.tags + aiTags).distinctBy { it.normalizeForSmartSearch() }
                )
            }
            val understanding = aiSummaryClient.understandSearch(query, enrichedAds)
            val adById = baseAds.associateBy(AdItem::id)
            val sortedAds = understanding.matchedAdIds.mapNotNull(adById::get)
            val suggestedTags = (
                understanding.suggestedTags +
                    sortedAds.flatMap { it.tags } +
                    sortedAds.flatMap { aiTagsByAdId[it.id].orEmpty() }
                )
                .distinctBy { it.normalizeForSmartSearch() }
                .take(3)

            SemanticSearchResult(
                ads = sortedAds,
                interpretation = understanding.interpretation.takeIf { it.isNotBlank() } ?: query,
                suggestedTags = suggestedTags,
                expandedTerms = understanding.expandedTerms,
                usedAiUnderstanding = true
            )
        } catch (error: Exception) {
            // 大模型不可用时保留搜索可用性，明确标记为本地语义兜底结果。
            fallback().copy(
                fallbackReason = error.message?.take(160) ?: "AI search understanding failed."
            )
        }
    }
}

private val SmartSearchPunctuation = Regex("[\\s_\\-,，、。！？?.;:：\"'（）()\\[\\]【】]+")

private fun String.normalizeForSmartSearch(): String {
    return lowercase().replace(SmartSearchPunctuation, "")
}
