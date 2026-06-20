package com.example.aiadflow.data.summary

import com.example.aiadflow.BuildConfig
import com.example.aiadflow.data.model.AdItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AiSearchUnderstanding(
    val interpretation: String,
    val matchedAdIds: List<Long>,
    val suggestedTags: List<String>,
    val expandedTerms: List<String>
)

class AiSummaryClient(
    private val apiKey: String = BuildConfig.AI_SUMMARY_API_KEY,
    private val endpoint: String = BuildConfig.AI_SUMMARY_ENDPOINT,
    private val model: String = BuildConfig.AI_SUMMARY_MODEL
) {
    suspend fun summarize(ads: List<AdItem>): String = withContext(Dispatchers.IO) {
        if (ads.isEmpty()) {
            return@withContext "\u0041\u0049 \u6458\u8981\uff1a\u5f53\u524d\u7b5b\u9009\u6ca1\u6709\u5339\u914d\u5e7f\u544a\uff0c\u6682\u65e0\u8db3\u591f\u7d20\u6750\u53ef\u4f9b\u6458\u8981\u3002"
        }
        if (apiKey.isBlank()) {
            return@withContext "\u0041\u0049 \u6458\u8981\uff1a\u672a\u914d\u7f6e API Key\uff0c\u8bf7\u5728 local.properties \u4e2d\u8bbe\u7f6e AI_SUMMARY_API_KEY \u6216 DASHSCOPE_API_KEY\u3002"
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = ConnectTimeoutMillis
            readTimeout = ReadTimeoutMillis
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            connection.outputStream.use { output ->
                output.write(buildRequestBody(ads).toString().toByteArray(Charsets.UTF_8))
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                throw IOException("AI summary request failed: HTTP ${connection.responseCode} ${errorText.orEmpty()}")
            }

            parseSummary(responseText)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun generateTags(ads: List<AdItem>): List<String> = withContext(Dispatchers.IO) {
        if (ads.isEmpty() || apiKey.isBlank()) {
            return@withContext emptyList()
        }

        val responseText = executeRequest(buildTagRequestBody(ads))
        parseTags(responseText)
    }

    suspend fun understandSearch(
        query: String,
        ads: List<AdItem>
    ): AiSearchUnderstanding = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext AiSearchUnderstanding(
                interpretation = "",
                matchedAdIds = ads.map(AdItem::id),
                suggestedTags = emptyList(),
                expandedTerms = emptyList()
            )
        }
        if (ads.isEmpty()) {
            return@withContext AiSearchUnderstanding(
                interpretation = trimmedQuery,
                matchedAdIds = emptyList(),
                suggestedTags = emptyList(),
                expandedTerms = emptyList()
            )
        }
        if (apiKey.isBlank()) {
            throw IOException("AI search understanding requires AI_SUMMARY_API_KEY or DASHSCOPE_API_KEY.")
        }

        val candidates = ads.take(MaxSearchCandidateAds)
        val responseText = executeRequest(buildSearchRequestBody(trimmedQuery, candidates))
        parseSearchUnderstanding(
            responseText = responseText,
            fallbackInterpretation = trimmedQuery,
            validAdIds = candidates.map(AdItem::id).toSet()
        )
    }

    private fun buildRequestBody(ads: List<AdItem>): JSONObject {
        val adPayload = JSONArray().apply {
            ads.forEach { ad ->
                put(
                    JSONObject()
                        .put("brandName", ad.brandName)
                        .put("title", ad.title)
                        .put("summary", ad.summary)
                        .put("channel", ad.channel.name)
                        .put("type", ad.type.name)
                        .put("tags", JSONArray(ad.tags))
                )
            }
        }
        val systemPrompt =
            "\u4f60\u662f\u5e7f\u544a\u6295\u653e\u5206\u6790\u52a9\u624b\u3002\u8bf7\u7528\u4e2d\u6587\u8f93\u51fa 2-3 \u53e5\u6458\u8981\uff0c\u805a\u7126\u5f53\u524d\u5e7f\u544a\u7d20\u6750\u7684\u6295\u653e\u4e3b\u9898\u3001\u7528\u6237\u610f\u56fe\u548c\u4e0b\u4e00\u6b65\u4f18\u5316\u5efa\u8bae\u3002\u4e0d\u8981\u7f16\u9020\u6570\u636e\u3002"
        val userPrompt =
            "\u8bf7\u603b\u7ed3\u8fd9\u4e9b\u5e7f\u544a\u7d20\u6750\uff1a\n$adPayload"

        return JSONObject()
            .put("model", model)
            .put("messages", JSONArray().apply {
                put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt)
                )
                put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userPrompt)
                )
            })
            .put("temperature", 0.3)
            .put("max_tokens", 220)
    }

    private fun buildTagRequestBody(ads: List<AdItem>): JSONObject {
        val adPayload = JSONArray().apply {
            ads.forEach { ad ->
                put(
                    JSONObject()
                        .put("brandName", ad.brandName)
                        .put("title", ad.title)
                        .put("summary", ad.summary)
                        .put("channel", ad.channel.name)
                        .put("type", ad.type.name)
                )
            }
        }
        val systemPrompt =
            "\u4f60\u662f\u5e7f\u544a\u6295\u653e\u6807\u7b7e\u751f\u6210\u52a9\u624b\u3002\u6839\u636e\u5e7f\u544a\u7d20\u6750\u751f\u6210 2-3 \u4e2a\u4e2d\u6587\u77ed\u6807\u7b7e\u3002\u6807\u7b7e\u5e94\u63cf\u8ff0\u54c1\u7c7b\u3001\u573a\u666f\u6216\u7528\u6237\u610f\u56fe\uff0c\u4e0d\u8981\u7f16\u9020\u7d20\u6750\u4e2d\u6ca1\u6709\u7684\u4fe1\u606f\u3002\u53ea\u8fd4\u56de JSON \u5b57\u7b26\u4e32\u6570\u7ec4\uff0c\u4e0d\u8981\u8fd4\u56de\u89e3\u91ca\u3002"
        val userPrompt =
            "\u8bf7\u4e3a\u8fd9\u4e9b\u5e7f\u544a\u7d20\u6750\u751f\u6210\u6807\u7b7e\uff1a\n$adPayload"

        return JSONObject()
            .put("model", model)
            .put("messages", JSONArray().apply {
                put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt)
                )
                put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userPrompt)
                )
            })
            .put("temperature", 0.2)
            .put("max_tokens", 80)
    }

    private fun buildSearchRequestBody(
        query: String,
        ads: List<AdItem>
    ): JSONObject {
        val adPayload = JSONArray().apply {
            ads.forEach { ad ->
                put(
                    JSONObject()
                        .put("id", ad.id)
                        .put("brandName", ad.brandName)
                        .put("title", ad.title)
                        .put("summary", ad.summary)
                        .put("channel", ad.channel.name)
                        .put("type", ad.type.name)
                        .put("tags", JSONArray(ad.tags))
                )
            }
        }
        val systemPrompt = """
            你是广告检索意图理解器。根据用户自然语言和候选广告，选出最相关的广告。
            只返回 JSON 对象，不要返回 Markdown 或解释文字。
            JSON schema:
            {
              "interpretation": "用中文概括用户真实搜索意图，20字以内",
              "matchedAdIds": [候选广告 id，按相关性从高到低排序],
              "expandedTerms": ["用于检索的中文或英文扩展词，最多12个"],
              "suggestedTags": ["建议用户继续筛选的短标签，最多3个"]
            }
            规则：
            - matchedAdIds 只能使用候选广告里的 id。
            - 如果没有相关广告，matchedAdIds 返回空数组。
            - 不要编造候选广告中不存在的品牌、渠道或素材类型。
            - 理解同义表达、场景、人群、预算、地域、素材类型和否定条件。
        """.trimIndent()
        val userPrompt = "用户搜索：$query\n候选广告：\n$adPayload"

        return JSONObject()
            .put("model", model)
            .put("messages", JSONArray().apply {
                put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt)
                )
                put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userPrompt)
                )
            })
            .put("temperature", 0.1)
            .put("max_tokens", 520)
    }

    private fun executeRequest(body: JSONObject): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = ConnectTimeoutMillis
            readTimeout = ReadTimeoutMillis
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            return if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                throw IOException("AI request failed: HTTP ${connection.responseCode} ${errorText.orEmpty()}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSummary(responseText: String): String {
        val response = JSONObject(responseText)
        return response
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    private fun parseTags(responseText: String): List<String> {
        val content = extractJsonArrayText(parseSummary(responseText))
        val tags = JSONArray(content)
        return List(tags.length()) { index -> tags.optString(index).trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(MaxTags)
    }

    private fun parseSearchUnderstanding(
        responseText: String,
        fallbackInterpretation: String,
        validAdIds: Set<Long>
    ): AiSearchUnderstanding {
        val content = extractJsonObjectText(parseSummary(responseText))
        val json = JSONObject(content)
        return AiSearchUnderstanding(
            interpretation = json.optString("interpretation")
                .trim()
                .ifBlank { fallbackInterpretation },
            matchedAdIds = json.optJSONArray("matchedAdIds")
                ?.toLongList(validAdIds)
                .orEmpty(),
            suggestedTags = json.optJSONArray("suggestedTags")
                ?.toStringList(MaxTags)
                .orEmpty(),
            expandedTerms = json.optJSONArray("expandedTerms")
                ?.toStringList(MaxSearchTerms)
                .orEmpty()
        )
    }

    private fun extractJsonArrayText(content: String): String {
        val normalized = content
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val arrayStart = normalized.indexOf('[')
        val arrayEnd = normalized.lastIndexOf(']')
        return if (arrayStart >= 0 && arrayEnd >= arrayStart) {
            normalized.substring(arrayStart, arrayEnd + 1)
        } else {
            normalized
        }
    }

    private fun extractJsonObjectText(content: String): String {
        val normalized = content
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val objectStart = normalized.indexOf('{')
        val objectEnd = normalized.lastIndexOf('}')
        return if (objectStart >= 0 && objectEnd >= objectStart) {
            normalized.substring(objectStart, objectEnd + 1)
        } else {
            normalized
        }
    }

    private fun JSONArray.toStringList(maxSize: Int): List<String> {
        return List(length()) { index -> optString(index).trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(maxSize)
    }

    private fun JSONArray.toLongList(validAdIds: Set<Long>): List<Long> {
        return List(length()) { index ->
            when (val value = opt(index)) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
        }
            .filterNotNull()
            .filter { it in validAdIds }
            .distinct()
    }

    private companion object {
        const val ConnectTimeoutMillis = 10_000
        const val ReadTimeoutMillis = 20_000
        const val MaxTags = 3
        const val MaxSearchTerms = 12
        const val MaxSearchCandidateAds = 80
    }
}
