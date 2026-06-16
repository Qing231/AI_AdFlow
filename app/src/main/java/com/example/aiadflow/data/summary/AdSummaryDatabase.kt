package com.example.aiadflow.data.summary

import com.example.aiadflow.data.mock.MockAdProvider

interface AdSummaryDatabase {
    fun getSummaries(adIds: Collection<Long>): Map<Long, String>

    fun upsertSummaries(summariesByAdId: Map<Long, String>)
}

object MockAdSummaryDatabase : AdSummaryDatabase {
    override fun getSummaries(adIds: Collection<Long>): Map<Long, String> {
        return MockAdProvider.getAiSummaries(adIds)
    }

    override fun upsertSummaries(summariesByAdId: Map<Long, String>) {
        MockAdProvider.upsertAiSummaries(summariesByAdId)
    }

    fun clear() {
        MockAdProvider.clearAiSummaries()
    }
}

interface AdTagDatabase {
    fun getTags(adIds: Collection<Long>): Map<Long, List<String>>

    fun upsertTags(tagsByAdId: Map<Long, List<String>>)
}

object MockAdTagDatabase : AdTagDatabase {
    override fun getTags(adIds: Collection<Long>): Map<Long, List<String>> {
        return MockAdProvider.getAiTags(adIds)
    }

    override fun upsertTags(tagsByAdId: Map<Long, List<String>>) {
        MockAdProvider.upsertAiTags(tagsByAdId)
    }

    fun clear() {
        MockAdProvider.clearAiTags()
    }
}
