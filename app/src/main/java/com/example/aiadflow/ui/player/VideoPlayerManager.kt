package com.example.aiadflow.ui.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoPlaybackState(
    val adId: Long,
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = VideoPlayerManager.DefaultDurationMillis
) {
    val progressFraction: Float
        get() {
            val safeDurationMillis = durationMillis.coerceAtLeast(1L)
            val safePositionMillis = positionMillis.coerceIn(0L, safeDurationMillis)
            return (safePositionMillis.toFloat() / safeDurationMillis.toFloat()).coerceIn(0f, 1f)
        }
}

data class VideoPlayerState(
    val activeAdId: Long? = null,
    val isPlaying: Boolean = false,
    val mutedAdIds: Set<Long> = emptySet(),
    val positionsByAdId: Map<Long, Long> = emptyMap(),
    val durationsByAdId: Map<Long, Long> = emptyMap()
) {
    fun playbackFor(adId: Long): VideoPlaybackState {
        val durationMillis = (durationsByAdId[adId] ?: VideoPlayerManager.DefaultDurationMillis)
            .coerceAtLeast(1L)
        val positionMillis = (positionsByAdId[adId] ?: 0L).coerceIn(0L, durationMillis)
        val isActive = activeAdId == adId

        return VideoPlaybackState(
            adId = adId,
            isActive = isActive,
            isPlaying = isActive && isPlaying,
            isMuted = adId in mutedAdIds,
            positionMillis = positionMillis,
            durationMillis = durationMillis
        )
    }
}

class VideoPlayerManager {
    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state.asStateFlow()

    fun togglePlayback(
        adId: Long,
        durationMillis: Long = DefaultDurationMillis
    ) {
        val safeDurationMillis = durationMillis.coerceAtLeast(1L)
        _state.update { current ->
            val isSameVideo = current.activeAdId == adId
            val currentPositionMillis = (current.positionsByAdId[adId] ?: 0L)
                .coerceIn(0L, safeDurationMillis)
            val nextIsPlaying = if (isSameVideo) !current.isPlaying else true
            val nextPositionMillis = if (nextIsPlaying && currentPositionMillis >= safeDurationMillis) {
                0L
            } else {
                currentPositionMillis
            }

            current.copy(
                activeAdId = adId,
                isPlaying = nextIsPlaying,
                positionsByAdId = current.positionsByAdId + (adId to nextPositionMillis),
                durationsByAdId = current.durationsByAdId + (adId to safeDurationMillis)
            )
        }
    }

    fun pause(adId: Long) {
        _state.update { current ->
            if (current.activeAdId == adId) {
                current.copy(isPlaying = false)
            } else {
                current
            }
        }
    }

    fun pauseAll() {
        _state.update { current -> current.copy(isPlaying = false) }
    }

    fun toggleMute(adId: Long) {
        _state.update { current ->
            current.copy(
                mutedAdIds = if (adId in current.mutedAdIds) {
                    current.mutedAdIds - adId
                } else {
                    current.mutedAdIds + adId
                }
            )
        }
    }

    fun setMuted(adId: Long, isMuted: Boolean) {
        _state.update { current ->
            current.copy(
                mutedAdIds = if (isMuted) {
                    current.mutedAdIds + adId
                } else {
                    current.mutedAdIds - adId
                }
            )
        }
    }

    fun updateProgress(
        adId: Long,
        positionMillis: Long,
        durationMillis: Long? = null
    ) {
        _state.update { current ->
            val safeDurationMillis = (durationMillis ?: current.durationsByAdId[adId] ?: DefaultDurationMillis)
                .coerceAtLeast(1L)
            val safePositionMillis = positionMillis.coerceIn(0L, safeDurationMillis)
            val didFinish = safePositionMillis >= safeDurationMillis

            current.copy(
                isPlaying = if (current.activeAdId == adId && didFinish) false else current.isPlaying,
                positionsByAdId = current.positionsByAdId + (adId to safePositionMillis),
                durationsByAdId = current.durationsByAdId + (adId to safeDurationMillis)
            )
        }
    }

    fun advancePlaying(elapsedMillis: Long) {
        if (elapsedMillis <= 0L) {
            return
        }

        _state.update { current ->
            val activeAdId = current.activeAdId
            if (!current.isPlaying || activeAdId == null) {
                return@update current
            }

            val durationMillis = (current.durationsByAdId[activeAdId] ?: DefaultDurationMillis)
                .coerceAtLeast(1L)
            val currentPositionMillis = (current.positionsByAdId[activeAdId] ?: 0L)
                .coerceIn(0L, durationMillis)
            val nextPositionMillis = (currentPositionMillis + elapsedMillis).coerceAtMost(durationMillis)

            current.copy(
                isPlaying = nextPositionMillis < durationMillis,
                positionsByAdId = current.positionsByAdId + (activeAdId to nextPositionMillis),
                durationsByAdId = current.durationsByAdId + (activeAdId to durationMillis)
            )
        }
    }

    fun reset(adId: Long) {
        _state.update { current ->
            val isActive = current.activeAdId == adId

            current.copy(
                activeAdId = if (isActive) null else current.activeAdId,
                isPlaying = if (isActive) false else current.isPlaying,
                mutedAdIds = current.mutedAdIds - adId,
                positionsByAdId = current.positionsByAdId - adId,
                durationsByAdId = current.durationsByAdId - adId
            )
        }
    }

    fun resetAll() {
        _state.value = VideoPlayerState()
    }

    companion object {
        const val DefaultDurationMillis = 30_000L
    }
}
