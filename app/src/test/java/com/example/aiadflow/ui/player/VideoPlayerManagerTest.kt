package com.example.aiadflow.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlayerManagerTest {
    @Test
    fun togglePlaybackStartsSelectedVideo() {
        val manager = VideoPlayerManager()

        manager.togglePlayback(adId = 1L)

        val state = manager.state.value
        val playback = state.playbackFor(1L)
        assertEquals(1L, state.activeAdId)
        assertTrue(playback.isActive)
        assertTrue(playback.isPlaying)
    }

    @Test
    fun togglePlaybackPausesSameVideo() {
        val manager = VideoPlayerManager()

        manager.togglePlayback(adId = 1L)
        manager.togglePlayback(adId = 1L)

        val playback = manager.state.value.playbackFor(1L)
        assertTrue(playback.isActive)
        assertFalse(playback.isPlaying)
    }

    @Test
    fun startingAnotherVideoStopsPreviousPlayback() {
        val manager = VideoPlayerManager()

        manager.togglePlayback(adId = 1L)
        manager.togglePlayback(adId = 2L)

        val state = manager.state.value
        assertFalse(state.playbackFor(1L).isPlaying)
        assertTrue(state.playbackFor(2L).isPlaying)
        assertEquals(2L, state.activeAdId)
    }

    @Test
    fun muteStateIsStoredPerVideo() {
        val manager = VideoPlayerManager()

        manager.toggleMute(adId = 1L)

        val state = manager.state.value
        assertTrue(state.playbackFor(1L).isMuted)
        assertFalse(state.playbackFor(2L).isMuted)
    }

    @Test
    fun advancePlayingClampsAtDurationAndStopsPlayback() {
        val manager = VideoPlayerManager()

        manager.togglePlayback(adId = 1L, durationMillis = 2_000L)
        manager.advancePlaying(elapsedMillis = 1_500L)
        manager.advancePlaying(elapsedMillis = 1_000L)

        val playback = manager.state.value.playbackFor(1L)
        assertEquals(2_000L, playback.positionMillis)
        assertFalse(playback.isPlaying)
        assertEquals(1f, playback.progressFraction)
    }

    @Test
    fun replayFinishedVideoStartsFromBeginning() {
        val manager = VideoPlayerManager()

        manager.togglePlayback(adId = 1L, durationMillis = 2_000L)
        manager.advancePlaying(elapsedMillis = 2_000L)
        manager.togglePlayback(adId = 1L, durationMillis = 2_000L)

        val playback = manager.state.value.playbackFor(1L)
        assertEquals(0L, playback.positionMillis)
        assertTrue(playback.isPlaying)
    }
}
