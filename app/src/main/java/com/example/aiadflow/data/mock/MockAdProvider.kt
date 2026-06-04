package com.example.aiadflow.data.mock

import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel

object MockAdProvider {
    fun channels(): List<Channel> = Channel.entries

    fun ads(): List<AdItem> = listOf(
        AdItem(
            id = 1,
            channel = Channel.Featured,
            type = AdType.SmallImage,
            brandName = "Nova Audio",
            title = "Noise cancelling earbuds for study and commute",
            summary = "Balanced sound, long battery life, and a compact case for daily use.",
            mediaLabel = "Small image",
            tags = listOf("Digital", "Student", "Budget")
        ),
        AdItem(
            id = 2,
            channel = Channel.Featured,
            type = AdType.Video,
            brandName = "RunLab",
            title = "City running starter kit",
            summary = "Light shoes and breathable basics for short morning runs.",
            mediaLabel = "Video",
            tags = listOf("Sports", "Commute", "Lifestyle"),
            collected = true
        ),
        AdItem(
            id = 3,
            channel = Channel.Ecommerce,
            type = AdType.LargeImage,
            brandName = "Orbit Phone",
            title = "Slim phone with strong night photos",
            summary = "A daily phone focused on battery, portrait mode, and low-light details.",
            mediaLabel = "Large image",
            tags = listOf("Digital", "Deal", "Trend")
        ),
        AdItem(
            id = 4,
            channel = Channel.Ecommerce,
            type = AdType.ImageText,
            brandName = "Simple Pack",
            title = "Laptop backpack for work and class",
            summary = "Clear compartments, water resistant fabric, and a protected laptop sleeve.",
            mediaLabel = "Image text",
            tags = listOf("Commute", "Student", "Work"),
            liked = true
        ),
        AdItem(
            id = 5,
            channel = Channel.Local,
            type = AdType.ImageText,
            brandName = "Corner Bakery",
            title = "Evening bakery discount",
            summary = "Fresh bread and desserts with a local pickup offer after work.",
            mediaLabel = "Image text",
            tags = listOf("Food", "Local", "Deal")
        ),
        AdItem(
            id = 6,
            channel = Channel.Local,
            type = AdType.Video,
            brandName = "Blue Bridge Gym",
            title = "First visit posture assessment",
            summary = "A short AI-assisted movement check with beginner training suggestions.",
            mediaLabel = "Video",
            tags = listOf("Sports", "Local", "Health")
        )
    )
}
