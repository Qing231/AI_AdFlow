package com.example.aiadflow.data.local

import android.content.Context

data class AdLocalInteractionState(
    val likedOverridesByAdId: Map<Long, Boolean> = emptyMap(),
    val collectedOverridesByAdId: Map<Long, Boolean> = emptyMap()
)

interface AdLocalStateStore {
    fun load(): AdLocalInteractionState

    fun save(state: AdLocalInteractionState)
}

object NoOpAdLocalStateStore : AdLocalStateStore {
    override fun load(): AdLocalInteractionState = AdLocalInteractionState()

    override fun save(state: AdLocalInteractionState) = Unit
}

class SharedPreferencesAdLocalStateStore(context: Context) : AdLocalStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    override fun load(): AdLocalInteractionState {
        return AdLocalInteractionState(
            likedOverridesByAdId = preferences.getStringSet(LikedOverridesKey, emptySet())
                .orEmpty()
                .toBooleanMap(),
            collectedOverridesByAdId = preferences.getStringSet(CollectedOverridesKey, emptySet())
                .orEmpty()
                .toBooleanMap()
        )
    }

    override fun save(state: AdLocalInteractionState) {
        preferences.edit()
            .putStringSet(LikedOverridesKey, state.likedOverridesByAdId.toStringSet())
            .putStringSet(CollectedOverridesKey, state.collectedOverridesByAdId.toStringSet())
            .apply()
    }

    private fun Set<String>.toBooleanMap(): Map<Long, Boolean> {
        return mapNotNull { entry ->
            val parts = entry.split(EntrySeparator, limit = 2)
            val id = parts.getOrNull(0)?.toLongOrNull()
            val value = parts.getOrNull(1)?.toBooleanStrictOrNull()

            if (id == null || value == null) {
                null
            } else {
                id to value
            }
        }.toMap()
    }

    private fun Map<Long, Boolean>.toStringSet(): Set<String> {
        return map { (id, value) -> "$id$EntrySeparator$value" }.toSet()
    }

    private companion object {
        const val PreferencesName = "ad_local_state"
        const val LikedOverridesKey = "liked_overrides_by_ad_id"
        const val CollectedOverridesKey = "collected_overrides_by_ad_id"
        const val EntrySeparator = ":"
    }
}
