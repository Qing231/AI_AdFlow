package com.example.aiadflow.data.tag

import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.summary.AiSummaryClient

class SmartTagService(
    private val aiSummaryClient: AiSummaryClient
) {
    suspend fun generateTags(ad: AdItem): List<String> {
        // 单条广告生成失败由调用方兜底，避免标签失败影响信息流展示。
        return aiSummaryClient.generateTags(listOf(ad))
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .take(MaxSmartTags)
    }

    private companion object {
        const val MaxSmartTags = 6
    }
}
