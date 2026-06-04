package com.example.aiadflow.data.model

data class TrackEvent(
    val adId: Long,
    val channel: Channel,
    val eventName: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
