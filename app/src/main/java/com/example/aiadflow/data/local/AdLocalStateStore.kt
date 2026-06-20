package com.example.aiadflow.data.local

import android.content.Context

data class AdLocalInteractionState(
    val likedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val collectedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val likeCountsByAdId: Map<Long, Int> = emptyMap(),
    val collectCountsByAdId: Map<Long, Int> = emptyMap()
)

interface AdLocalStateStore {
    fun load(): AdLocalInteractionState

    fun save(state: AdLocalInteractionState)
}

object InMemoryAdLocalStateStore : AdLocalStateStore {
    private var state = AdLocalInteractionState()

    override fun load(): AdLocalInteractionState = state

    override fun save(state: AdLocalInteractionState) {
        this.state = state
    }
}

class SharedPreferencesAdLocalStateStore(context: Context) : AdLocalStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    override fun load(): AdLocalInteractionState {
        return AdLocalInteractionState(
            likedOverridesByAdId = preferences.getString(KeyLiked, null).toBooleanMap(),
            collectedOverridesByAdId = preferences.getString(KeyCollected, null).toBooleanMap(),
            likeCountsByAdId = preferences.getString(KeyLikeCounts, null).toIntMap(),
            collectCountsByAdId = preferences.getString(KeyCollectCounts, null).toIntMap()
        )
    }

    override fun save(state: AdLocalInteractionState) {
        preferences.edit()
            .putString(KeyLiked, state.likedOverridesByAdId.encodeMap())
            .putString(KeyCollected, state.collectedOverridesByAdId.encodeMap())
            .putString(KeyLikeCounts, state.likeCountsByAdId.encodeMap())
            .putString(KeyCollectCounts, state.collectCountsByAdId.encodeMap())
            .apply()
    }

    private fun <T : Any> Map<Long, T>.encodeMap(): String {
        return entries.joinToString(separator = EntrySeparator) { (key, value) -> "$key$ValueSeparator$value" }
    }

    private fun String?.toBooleanMap(): Map<Long, Boolean> {
        return toTypedMap { it.toBooleanStrictOrNull() }
    }

    private fun String?.toIntMap(): Map<Long, Int> {
        return toTypedMap { it.toIntOrNull() }
    }

    private fun <T : Any> String?.toTypedMap(valueParser: (String) -> T?): Map<Long, T> {
        if (isNullOrBlank()) {
            return emptyMap()
        }

        return split(EntrySeparator)
            .mapNotNull { entry ->
                val parts = entry.split(ValueSeparator, limit = 2)
                val key = parts.getOrNull(0)?.toLongOrNull()
                val value = parts.getOrNull(1)?.let(valueParser)
                if (key == null || value == null) null else key to value
            }
            .toMap()
    }

    private companion object {
        const val PreferencesName = "ad_local_state"
        const val KeyLiked = "liked_overrides"
        const val KeyCollected = "collected_overrides"
        const val KeyLikeCounts = "like_counts"
        const val KeyCollectCounts = "collect_counts"
        const val EntrySeparator = ";"
        const val ValueSeparator = ":"
    }
}
