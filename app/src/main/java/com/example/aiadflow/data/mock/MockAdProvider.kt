package com.example.aiadflow.data.mock

import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel

object MockAdProvider {
    fun channels(): List<Channel> = Channel.entries

    fun ads(): List<AdItem> = baseAds

    fun getAiSummary(adId: Long): String? = synchronized(aiSummaryOverridesByAdId) {
        aiSummaryOverridesByAdId[adId] ?: baseAds.firstOrNull { it.id == adId }?.summary
    }

    fun getAiSummaries(adIds: Collection<Long>): Map<Long, String> = synchronized(aiSummaryOverridesByAdId) {
        adIds.mapNotNull { adId -> getAiSummary(adId)?.let { adId to it } }.toMap()
    }

    fun upsertAiSummary(adId: Long, summary: String) {
        synchronized(aiSummaryOverridesByAdId) {
            aiSummaryOverridesByAdId[adId] = summary
        }
    }

    fun upsertAiSummaries(summariesByAdId: Map<Long, String>) {
        synchronized(aiSummaryOverridesByAdId) {
            aiSummaryOverridesByAdId.putAll(summariesByAdId)
        }
    }

    fun clearAiSummaryOverrides() {
        synchronized(aiSummaryOverridesByAdId) {
            aiSummaryOverridesByAdId.clear()
        }
    }

    private val aiSummaryOverridesByAdId = mutableMapOf<Long, String>()

    private val baseAds: List<AdItem> = requireUniqueIds(
        listOf(
            AdItem(
                id = 1,
                channel = Channel.Featured,
                type = AdType.LargeImage,
                brandName = "Northline Gear",
                title = "Lightweight commuter backpack",
                summary = "AI predicts strong purchase intent for waterproof laptop bags among weekday commuters.",
                mediaLabel = "Product hero",
                tags = listOf("Backpack", "Commute", "Waterproof"),
                collected = true
            ),
            AdItem(
                id = 2,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "RunLab",
                title = "Seven-day creator fitness challenge",
                summary = "Short video creative highlights daily progress, shareable milestones, and a low-friction trial path.",
                mediaLabel = "Video creative",
                videoUrl = "https://cdn.example.com/ads/runlab-creator-challenge.mp4",
                coverUrl = "https://cdn.example.com/ads/runlab-creator-challenge-cover.jpg",
                tags = listOf("Fitness", "Creator", "Trial")
            ),
            AdItem(
                id = 3,
                channel = Channel.Featured,
                type = AdType.ImageText,
                brandName = "Bluebird Pay",
                title = "Cashback benefits onboarding story",
                summary = "A three-card sequence explains everyday savings before guiding users to compare cashback tiers.",
                mediaLabel = "Brand visual",
                tags = listOf("Finance", "Cashback", "Onboarding")
            ),
            AdItem(
                id = 4,
                channel = Channel.Featured,
                type = AdType.SmallImage,
                brandName = "Nova Audio",
                title = "Noise cancelling earbuds for study",
                summary = "Balanced sound, long battery life, and a compact case for daily use.",
                mediaLabel = "Small image",
                tags = listOf("Digital", "Student", "Budget"),
                liked = true
            ),
            AdItem(
                id = 5,
                channel = Channel.Ecommerce,
                type = AdType.LargeImage,
                brandName = "Orbit Phone",
                title = "Slim phone with strong night photos",
                summary = "A daily phone focused on battery, portrait mode, and low-light image details.",
                mediaLabel = "Large image",
                tags = listOf("Digital", "Deal", "Trend")
            ),
            AdItem(
                id = 6,
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
                id = 7,
                channel = Channel.Ecommerce,
                type = AdType.SmallImage,
                brandName = "Warm Home",
                title = "Smart aroma diffuser bundle",
                summary = "Top-performing copy pairs nighttime relaxation with a limited-time bundle discount.",
                mediaLabel = "Small image",
                tags = listOf("Home", "Wellness", "Bundle"),
                collected = true
            ),
            AdItem(
                id = 8,
                channel = Channel.Ecommerce,
                type = AdType.Video,
                brandName = "DeskLite",
                title = "Adjustable desk lamp for focused work",
                summary = "Video creative shows brightness modes, cable management, and compact desk setups.",
                mediaLabel = "Video",
                videoUrl = "https://cdn.example.com/ads/desklite-focused-work.mp4",
                coverUrl = "https://cdn.example.com/ads/desklite-focused-work-cover.jpg",
                tags = listOf("Office", "Study", "Lighting")
            ),
            AdItem(
                id = 9,
                channel = Channel.Local,
                type = AdType.ImageText,
                brandName = "Corner Bakery",
                title = "Evening bakery discount",
                summary = "Fresh bread and desserts with a local pickup offer after work.",
                mediaLabel = "Local offer",
                tags = listOf("Food", "Local", "Deal")
            ),
            AdItem(
                id = 10,
                channel = Channel.Local,
                type = AdType.Video,
                brandName = "Blue Bridge Gym",
                title = "First visit posture assessment",
                summary = "A short AI-assisted movement check with beginner training suggestions.",
                mediaLabel = "Video",
                videoUrl = "https://cdn.example.com/ads/blue-bridge-posture-assessment.mp4",
                coverUrl = "https://cdn.example.com/ads/blue-bridge-posture-assessment-cover.jpg",
                tags = listOf("Sports", "Local", "Health")
            ),
            AdItem(
                id = 11,
                channel = Channel.Local,
                type = AdType.LargeImage,
                brandName = "Street Cafe",
                title = "Weekday lunch set near the office",
                summary = "Promote a decision-time lunch bundle to users within three kilometers before noon.",
                mediaLabel = "Nearby deal",
                tags = listOf("Dining", "Nearby", "Lunch"),
                liked = true
            ),
            AdItem(
                id = 12,
                channel = Channel.Local,
                type = AdType.SmallImage,
                brandName = "City Cleaners",
                title = "Same-day shirt cleaning pickup",
                summary = "Local service ads emphasize evening pickup, transparent pricing, and first-order savings.",
                mediaLabel = "Service image",
                tags = listOf("Service", "Local", "Pickup")
            ),
            AdItem(
                id = 13,
                channel = Channel.NewArrival,
                type = AdType.LargeImage,
                brandName = "PixelNest",
                title = "New compact creator camera launch",
                summary = "Launch creative should emphasize pocket size, stabilized clips, and quick social publishing.",
                mediaLabel = "Launch hero",
                tags = listOf("New", "Creator", "Digital")
            ),
            AdItem(
                id = 14,
                channel = Channel.NewArrival,
                type = AdType.SmallImage,
                brandName = "FreshStep",
                title = "First drop breathable walking shoes",
                summary = "Early campaign copy connects daily walking comfort with limited first-week colorways.",
                mediaLabel = "New item",
                tags = listOf("New", "Sports", "Commute")
            ),
            AdItem(
                id = 15,
                channel = Channel.Finance,
                type = AdType.ImageText,
                brandName = "Bluebird Pay",
                title = "Weekend cashback boost",
                summary = "AI suggests highlighting groceries, transport, and dining as everyday cashback scenes.",
                mediaLabel = "Finance card",
                tags = listOf("Finance", "Cashback", "Dining")
            ),
            AdItem(
                id = 16,
                channel = Channel.Finance,
                type = AdType.Video,
                brandName = "Mint Ledger",
                title = "Budget planner onboarding",
                summary = "Short video explains monthly spend categories and a low-friction setup path.",
                mediaLabel = "Video",
                videoUrl = "https://cdn.example.com/ads/mint-ledger-onboarding.mp4",
                coverUrl = "https://cdn.example.com/ads/mint-ledger-onboarding-cover.jpg",
                tags = listOf("Finance", "Budget", "Onboarding")
            ),
            AdItem(
                id = 17,
                channel = Channel.Health,
                type = AdType.LargeImage,
                brandName = "Daily Greens",
                title = "Morning nutrition subscription",
                summary = "Best-performing copy connects breakfast routines with simple energy and wellness habits.",
                mediaLabel = "Health visual",
                tags = listOf("Health", "Wellness", "Subscription")
            ),
            AdItem(
                id = 18,
                channel = Channel.Health,
                type = AdType.SmallImage,
                brandName = "CalmDesk",
                title = "Five-minute office stretch guide",
                summary = "Promote short breaks to desk workers searching for posture and neck-relief tips.",
                mediaLabel = "Health image",
                tags = listOf("Health", "Office", "Wellness")
            ),
            AdItem(
                id = 19,
                channel = Channel.Travel,
                type = AdType.ImageText,
                brandName = "CloudTrip",
                title = "Three-day city break package",
                summary = "AI recommends pairing flexible booking with local food and transit convenience.",
                mediaLabel = "Travel story",
                tags = listOf("Travel", "Dining", "Deal")
            ),
            AdItem(
                id = 20,
                channel = Channel.Travel,
                type = AdType.Video,
                brandName = "TrailGo",
                title = "Beginner weekend hiking route",
                summary = "Video creative uses route preview, gear checklist, and weather planning to reduce hesitation.",
                mediaLabel = "Video",
                videoUrl = "https://cdn.example.com/ads/trailgo-weekend-route.mp4",
                coverUrl = "https://cdn.example.com/ads/trailgo-weekend-route-cover.jpg",
                tags = listOf("Travel", "Outdoor", "Weekend")
            ),
            AdItem(
                id = 21,
                channel = Channel.Education,
                type = AdType.LargeImage,
                brandName = "SkillForge",
                title = "AI design course trial lesson",
                summary = "Campaign should highlight portfolio outcomes, guided practice, and a short trial format.",
                mediaLabel = "Course hero",
                tags = listOf("Education", "AI", "Creator")
            ),
            AdItem(
                id = 22,
                channel = Channel.Education,
                type = AdType.SmallImage,
                brandName = "LangLoop",
                title = "Daily speaking practice plan",
                summary = "AI suggests focusing on ten-minute habit formation and confidence in real conversations.",
                mediaLabel = "Course image",
                tags = listOf("Education", "Language", "Student")
            )
        )
    )

    private fun requireUniqueIds(ads: List<AdItem>): List<AdItem> {
        val duplicateIds = ads
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Ad ids must be unique. Duplicate ids: ${duplicateIds.joinToString()}"
        }

        return ads
    }
}
