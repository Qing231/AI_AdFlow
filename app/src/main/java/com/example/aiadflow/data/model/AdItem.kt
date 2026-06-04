package com.example.aiadflow.data.model

data class AdItem(
    val id: Long,
    val channel: Channel,
    val type: AdType,
    val brandName: String,
    val title: String,
    val summary: String,
    val mediaLabel: String,
    val tags: List<String>,
    val liked: Boolean = false,
    val collected: Boolean = false
)
