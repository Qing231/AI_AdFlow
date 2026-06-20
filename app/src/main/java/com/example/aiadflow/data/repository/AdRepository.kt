package com.example.aiadflow.data.repository

import com.example.aiadflow.data.mock.MockAdProvider
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent
import com.example.aiadflow.data.search.SemanticSearchResult
import com.example.aiadflow.data.search.SmartSearchService
import com.example.aiadflow.data.summary.AdSummaryDatabase
import com.example.aiadflow.data.summary.AdTagDatabase
import com.example.aiadflow.data.summary.AiSummaryClient
import com.example.aiadflow.data.summary.MockAdSummaryDatabase
import com.example.aiadflow.data.summary.MockAdTagDatabase
import com.example.aiadflow.data.tag.SmartTagService

class AdRepository(
    private val adProvider: MockAdProvider = MockAdProvider,
    private val aiSummaryClient: AiSummaryClient = AiSummaryClient(),
    private val adSummaryDatabase: AdSummaryDatabase = MockAdSummaryDatabase,
    private val adTagDatabase: AdTagDatabase = MockAdTagDatabase
) {
    private val smartSearchService = SmartSearchService(aiSummaryClient)
    private val smartTagService = SmartTagService(aiSummaryClient)

    private companion object {
        val adSummaryCache = mutableMapOf<Long, String>()
        val adTagCache = mutableMapOf<Long, List<String>>()

        val semanticGroups = listOf(
            SemanticGroup(
                key = "ai",
                defaultTag = "AI工具",
                terms = listOf("ai", "人工智能", "智能助手", "agent", "大模型", "chat", "chatgpt", "assistant")
            ),
            SemanticGroup(
                key = "student",
                defaultTag = "学生党",
                terms = listOf("学生", "学习", "课程", "教育", "study", "class", "course", "trial lesson", "speaking practice")
            ),
            SemanticGroup(
                key = "productivity",
                defaultTag = "效率办公",
                terms = listOf("效率", "办公", "工作", "专注", "桌面", "工位", "程序员", "office", "work", "desk", "productivity", "laptop", "commuter", "commute")
            ),
            SemanticGroup(
                key = "sports",
                defaultTag = "运动健身",
                terms = listOf("运动", "健身", "跑步", "减脂", "训练", "健康", "姿势", "fitness", "run", "gym", "posture", "training", "walking", "hiking", "stretch", "health")
            ),
            SemanticGroup(
                key = "digital",
                defaultTag = "数码好物",
                terms = listOf("数码", "手机", "耳机", "相机", "电脑", "拍照", "降噪", "digital", "phone", "earbuds", "camera", "audio", "battery", "clips")
            ),
            SemanticGroup(
                key = "video",
                defaultTag = "视频广告",
                terms = listOf("视频", "短视频", "播放", "素材", "video", "creative", "creator", "clips", "route preview")
            ),
            SemanticGroup(
                key = "coffee_food",
                defaultTag = "餐饮优惠",
                terms = listOf("咖啡", "饮品", "饮料", "外卖", "餐饮", "午餐", "下午茶", "美食", "coffee", "cafe", "bakery", "lunch", "food", "desserts", "pickup")
            ),
            SemanticGroup(
                key = "deal",
                defaultTag = "低价好物",
                terms = listOf("优惠", "低价", "便宜", "折扣", "省钱", "返现", "套餐", "限时", "deal", "discount", "cashback", "savings", "bundle", "offer", "pricing")
            ),
            SemanticGroup(
                key = "finance",
                defaultTag = "省钱理财",
                terms = listOf("金融", "预算", "账本", "理财", "支付", "返现", "finance", "budget", "ledger", "pay", "cashback", "spend")
            ),
            SemanticGroup(
                key = "travel",
                defaultTag = "周末出行",
                terms = listOf("旅行", "出行", "周末", "徒步", "城市", "路线", "travel", "trip", "hiking", "route", "city break", "booking", "transit")
            ),
            SemanticGroup(
                key = "local",
                defaultTag = "附近生活",
                terms = listOf("附近", "本地", "同城", "门店", "到店", "local", "nearby", "pickup", "within three kilometers", "same-day")
            ),
            SemanticGroup(
                key = "bag",
                defaultTag = "通勤背包",
                terms = listOf("背包", "双肩包", "电脑包", "backpack", "bag", "commute", "commuter", "waterproof")
            )
        )

        val channelTerms = mapOf(
            Channel.Featured to listOf("推荐", "精选", "featured", "trending"),
            Channel.Ecommerce to listOf("电商", "商品", "购物", "好物", "ecommerce", "product", "bundle"),
            Channel.Local to listOf("本地", "附近", "门店", "到店", "local", "nearby", "pickup"),
            Channel.NewArrival to listOf("新品", "首发", "发布", "new", "launch", "first drop"),
            Channel.Finance to listOf("金融", "支付", "理财", "返现", "finance", "cashback", "budget"),
            Channel.Health to listOf("健康", "运动", "健身", "营养", "health", "fitness", "wellness"),
            Channel.Travel to listOf("旅行", "出行", "路线", "travel", "trip", "hiking"),
            Channel.Education to listOf("教育", "学习", "课程", "学生", "education", "course", "study")
        )

        val typeTerms = mapOf(
            AdType.SmallImage to listOf("小图", "图片", "image"),
            AdType.ImageText to listOf("图文", "图片", "文案", "story", "card"),
            AdType.Video to listOf("视频", "短视频", "播放", "video", "creative"),
            AdType.LargeImage to listOf("大图", "海报", "hero", "visual")
        )

        val querySeparator = Regex("[\\s,，、。！？!?.;；:：\"'（）()\\[\\]【】]+")
        val punctuation = Regex("[\\s_\\-,，、。！？!?.;；:：\"'（）()\\[\\]【】]+")
    }

    private val channelCache = mutableMapOf<Channel?, List<AdItem>>()
    private val trackedEvents = mutableListOf<TrackEvent>()

    fun getChannels(): List<Channel> = adProvider.channels()

    fun getAds(
        channel: Channel? = null,
        query: String = "",
        selectedTag: String? = null,
        aiTagsByAdId: Map<Long, List<String>> = emptyMap(),
        aiSummariesByAdId: Map<Long, String> = emptyMap()
    ): List<AdItem> = searchAds(
        channel = channel,
        query = query,
        selectedTag = selectedTag,
        aiTagsByAdId = aiTagsByAdId,
        aiSummariesByAdId = aiSummariesByAdId
    ).ads

    fun searchAds(
        channel: Channel? = null,
        query: String = "",
        selectedTag: String? = null,
        aiTagsByAdId: Map<Long, List<String>> = emptyMap(),
        aiSummariesByAdId: Map<Long, String> = emptyMap()
    ): SemanticSearchResult {
        val normalizedTag = selectedTag?.trim().orEmpty()
        val baseAds = getCachedAds(channel).filterByTag(normalizedTag, aiTagsByAdId)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return SemanticSearchResult(ads = baseAds)
        }

        val intent = buildIntent(trimmedQuery)
        val scoredAds = baseAds
            .mapIndexedNotNull { index, ad ->
                scoreAd(ad, index, intent, aiTagsByAdId[ad.id].orEmpty(), aiSummariesByAdId[ad.id])
            }
            .sortedWith(
                compareByDescending<ScoredAd> { it.score }
                    .thenByDescending { it.matchedTermCount }
                    .thenBy { it.index }
            )

        val sortedAds = scoredAds.map(ScoredAd::ad)
        val suggestedTags = (intent.suggestedTags + sortedAds.flatMap { it.tags } + sortedAds.flatMap { aiTagsByAdId[it.id].orEmpty() })
            .distinctBy { it.normalizeForCompare() }
            .take(3)

        return SemanticSearchResult(
            ads = sortedAds,
            interpretation = intent.description,
            suggestedTags = suggestedTags,
            expandedTerms = intent.expandedTerms
        )
    }

    suspend fun searchAdsWithAiUnderstanding(
        channel: Channel? = null,
        query: String = "",
        selectedTag: String? = null,
        aiTagsByAdId: Map<Long, List<String>> = emptyMap(),
        aiSummariesByAdId: Map<Long, String> = emptyMap()
    ): SemanticSearchResult {
        val normalizedTag = selectedTag?.trim().orEmpty()
        val baseAds = getCachedAds(channel).filterByTag(normalizedTag, aiTagsByAdId)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return SemanticSearchResult(ads = baseAds)
        }

        return smartSearchService.search(
            query = trimmedQuery,
            baseAds = baseAds,
            aiTagsByAdId = aiTagsByAdId,
            aiSummariesByAdId = aiSummariesByAdId,
            fallback = {
                searchAds(
                    channel = channel,
                    query = query,
                    selectedTag = selectedTag,
                    aiTagsByAdId = aiTagsByAdId,
                    aiSummariesByAdId = aiSummariesByAdId
                )
            }
        )
    }

    fun getAdById(adId: Long): AdItem? = getCachedAds(null).firstOrNull { it.id == adId }

    fun getAdAiSummary(adId: Long): String? = getAdAiSummaries(listOf(adId))[adId]

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

    fun clearAdAiSummaryMemoryCache() {
        synchronized(adSummaryCache) {
            adSummaryCache.clear()
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

    suspend fun generateAdAiSummary(ad: AdItem): String = aiSummaryClient.summarize(listOf(ad))

    suspend fun generateAdAiTags(ad: AdItem): List<String> = smartTagService.generateTags(ad)

    fun track(event: TrackEvent) {
        trackedEvents += event
    }

    fun getTrackedEvents(): List<TrackEvent> = trackedEvents.toList()

    private fun buildIntent(query: String): SearchIntent {
        val normalizedQuery = query.normalizeForCompare()
        val inputTerms = query
            .split(querySeparator)
            .map(String::trim)
            .filter { it.isNotBlank() }
            .distinctBy { it.normalizeForCompare() }
        val matchedGroups = semanticGroups.filter { group ->
            group.terms.any { term ->
                val normalizedTerm = term.normalizeForCompare()
                normalizedQuery.contains(normalizedTerm) ||
                    inputTerms.any { inputTerm -> inputTerm.semanticMatches(term) }
            }
        }
        val expandedTerms = (inputTerms + matchedGroups.flatMap(SemanticGroup::terms))
            .map { it.normalizeForCompare() }
            .filter { it.isNotBlank() }
            .distinct()

        return SearchIntent(
            description = describeIntent(query, matchedGroups),
            inputTerms = inputTerms.map { it.normalizeForCompare() },
            expandedTerms = expandedTerms,
            suggestedTags = suggestedTagsFor(matchedGroups)
        )
    }

    private fun scoreAd(
        ad: AdItem,
        index: Int,
        intent: SearchIntent,
        aiTags: List<String>,
        aiSummary: String?
    ): ScoredAd? {
        val tagText = (ad.tags + aiTags).joinToString(separator = " ")
        val summaryText = aiSummary?.takeIf { it.isNotBlank() } ?: ad.summary
        val categoryText = buildCategoryText(ad)
        val exactText = listOf(ad.title, tagText, summaryText, categoryText).joinToString(separator = " ")
        val semanticText = listOf(ad.brandName, ad.title, tagText, summaryText, categoryText).joinToString(separator = " ")

        val exactMatches = intent.inputTerms.filter { exactText.matchesTerm(it) }
        if (intent.inputTerms.isNotEmpty() && exactMatches.size != intent.inputTerms.size) {
            return null
        }

        var score = exactMatches.size * 10
        if (ad.title.matchesAny(intent.inputTerms)) score += 5
        if (tagText.matchesAny(intent.inputTerms)) score += 4
        if (summaryText.matchesAny(intent.inputTerms)) score += 3
        if (categoryText.matchesAny(intent.inputTerms)) score += 2

        val semanticMatches = intent.expandedTerms.filter { semanticText.matchesTerm(it) }
        score += semanticMatches.size
        if (score <= 0) {
            return null
        }

        return ScoredAd(
            ad = ad,
            score = score,
            matchedTermCount = (exactMatches + semanticMatches).distinct().size,
            index = index
        )
    }

    private fun buildCategoryText(ad: AdItem): String {
        return listOf(
            ad.channel.id,
            ad.channel.title,
            channelTerms[ad.channel].orEmpty().joinToString(separator = " "),
            ad.type.name,
            typeTerms[ad.type].orEmpty().joinToString(separator = " ")
        ).joinToString(separator = " ")
    }

    private fun suggestedTagsFor(groups: List<SemanticGroup>): List<String> {
        return groups
            .map(SemanticGroup::defaultTag)
            .distinctBy { it.normalizeForCompare() }
            .take(3)
    }

    private fun describeIntent(query: String, groups: List<SemanticGroup>): String {
        val keys = groups.map(SemanticGroup::key).toSet()
        return when {
            "student" in keys && "ai" in keys -> "适合学生用户的 AI 效率工具"
            "ai" in keys && "productivity" in keys -> "AI 智能助手与效率办公内容"
            "digital" in keys && "video" in keys -> "数码类视频广告创意"
            "sports" in keys -> "运动健身、跑步训练或健康内容"
            "coffee_food" in keys && "deal" in keys -> "餐饮或咖啡优惠内容"
            "productivity" in keys && query.contains("程序员", ignoreCase = true) -> "适合程序员的办公效率与桌面装备"
            "productivity" in keys -> "办公效率、专注工作或通勤装备"
            "finance" in keys -> "省钱、预算管理或金融权益内容"
            "travel" in keys -> "周末出行、旅行路线或本地体验"
            "local" in keys -> "附近生活服务与本地优惠"
            groups.isNotEmpty() -> groups.joinToString(separator = "、") { it.defaultTag }
            else -> query.take(28)
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
            (ad.tags + aiTagsByAdId[ad.id].orEmpty()).any { it.equals(selectedTag, ignoreCase = true) }
        }
    }

    private fun getCachedAds(channel: Channel?): List<AdItem> {
        return channelCache.getOrPut(channel) {
            val allAds = channelCache.getOrPut(null) {
                adProvider.ads().map { ad ->
                    if (ad.tags.isEmpty()) ad.copy(tags = defaultTagsFor(ad)) else ad
                }
            }

            if (channel == null) {
                allAds
            } else {
                allAds.filter { it.channel == channel }
            }
        }
    }

    private fun defaultTagsFor(ad: AdItem): List<String> {
        val text = "${ad.channel.id} ${ad.channel.title} ${ad.title} ${ad.summary}".lowercase()
        return buildList {
            add(ad.channel.title)
            when {
                "backpack" in text || "commuter" in text || "commute" in text -> addAll(listOf("Backpack", "Commute"))
                "cashback" in text || "budget" in text || "finance" in text -> addAll(listOf("Finance", "Savings"))
                "video" in text || ad.type == AdType.Video -> add("Video")
                "fitness" in text || "posture" in text || "health" in text -> add("Health")
                "coffee" in text || "bakery" in text || "lunch" in text || "food" in text -> add("Local")
                "course" in text || "study" in text || "practice" in text -> add("Education")
                "travel" in text || "hiking" in text || "city break" in text -> add("Travel")
            }
        }.distinct()
    }

    private fun String.matchesAny(terms: List<String>): Boolean = terms.any { term -> matchesTerm(term) }

    private fun String.matchesTerm(term: String): Boolean {
        val normalizedTerm = term.normalizeForCompare()
        if (normalizedTerm.isBlank()) {
            return false
        }

        val normalizedText = normalizeForCompare()
        if (normalizedText.contains(normalizedTerm)) {
            if (normalizedTerm.length <= 2 && normalizedTerm.all { it in 'a'..'z' }) {
                return lowercase().split(Regex("[^a-z0-9]+")).any { it == normalizedTerm }
            }
            return true
        }

        return lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 && normalizedTerm.length >= 4 }
            .any { word -> word.startsWith(normalizedTerm.take(4)) || normalizedTerm.startsWith(word.take(4)) }
    }

    private fun String.semanticMatches(term: String): Boolean {
        val normalizedInput = normalizeForCompare()
        val normalizedTerm = term.normalizeForCompare()
        return normalizedInput == normalizedTerm ||
            normalizedInput.contains(normalizedTerm) ||
            normalizedTerm.contains(normalizedInput)
    }

    private fun String.normalizeForCompare(): String {
        return lowercase().replace(punctuation, "")
    }

    private data class SemanticGroup(
        val key: String,
        val defaultTag: String,
        val terms: List<String>
    )

    private data class SearchIntent(
        val description: String,
        val inputTerms: List<String>,
        val expandedTerms: List<String>,
        val suggestedTags: List<String>
    )

    private data class ScoredAd(
        val ad: AdItem,
        val score: Int,
        val matchedTermCount: Int,
        val index: Int
    )
}
