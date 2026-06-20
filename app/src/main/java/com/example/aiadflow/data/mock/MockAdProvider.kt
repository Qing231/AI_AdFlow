package com.example.aiadflow.data.mock

import com.example.aiadflow.R
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

    fun getAiTags(adIds: Collection<Long>): Map<Long, List<String>> = synchronized(aiTagOverridesByAdId) {
        adIds.mapNotNull { adId -> aiTagOverridesByAdId[adId]?.let { adId to it } }.toMap()
    }

    fun upsertAiSummaries(summariesByAdId: Map<Long, String>) {
        synchronized(aiSummaryOverridesByAdId) {
            aiSummaryOverridesByAdId.putAll(summariesByAdId)
        }
    }

    fun clearAiSummaries() {
        synchronized(aiSummaryOverridesByAdId) {
            aiSummaryOverridesByAdId.clear()
        }
    }

    fun upsertAiTags(tagsByAdId: Map<Long, List<String>>) {
        synchronized(aiTagOverridesByAdId) {
            aiTagOverridesByAdId.putAll(tagsByAdId)
        }
    }

    fun clearAiTags() {
        synchronized(aiTagOverridesByAdId) {
            aiTagOverridesByAdId.clear()
        }
    }

    private val aiSummaryOverridesByAdId = mutableMapOf<Long, String>()
    private val aiTagOverridesByAdId = mutableMapOf<Long, List<String>>()
    private val audibleVideoResourceIds = intArrayOf(
        R.raw.adv2,
        R.raw.adv3,
        R.raw.adv4,
        R.raw.adv5
    )

    private fun audibleVideoUri(offset: Int): String {
        val resourceId = audibleVideoResourceIds[offset % audibleVideoResourceIds.size]
        return "android.resource://com.example.aiadflow/$resourceId"
    }

    private val baseAds: List<AdItem> = requireUniqueIds(
        listOf(
            AdItem(
                id = 23,
                channel = Channel.Featured,
                type = AdType.LargeImage,
                brandName = "PixelNest",
                title = "新款轻薄智能手机发布",
                summary = "主打轻薄机身、高清影像和全天续航。",
                coverUrl = "https://images.pexels.com/photos/788946/pexels-photo-788946.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "新品手机",
                tags = listOf("科技", "新品", "手机")
            ),
            AdItem(
                id = 24,
                channel = Channel.Ecommerce,
                type = AdType.SmallImage,
                brandName = "UrbanWear",
                title = "夏季通勤穿搭上新",
                summary = "适合校园、实习和日常通勤的轻便穿搭。",
                coverUrl = "https://images.pexels.com/photos/2983464/pexels-photo-2983464.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "服饰",
                tags = listOf("电商", "穿搭")
            ),
            AdItem(
                id = 25,
                channel = Channel.Local,
                type = AdType.LargeImage,
                brandName = "BeanLab",
                title = "附近新开咖啡店限时优惠",
                summary = "工作日到店享受第二杯半价。",
                coverUrl = "https://images.pexels.com/photos/302899/pexels-photo-302899.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "咖啡",
                tags = listOf("本地", "咖啡")
            ),
            AdItem(
                id = 26,
                channel = Channel.Featured,
                type = AdType.SmallImage,
                brandName = "FitGo",
                title = "智能运动手环",
                summary = "记录跑步、睡眠和心率数据。",
                coverUrl = "https://images.pexels.com/photos/437037/pexels-photo-437037.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "运动手环",
                tags = listOf("运动", "智能设备")
            ),
            AdItem(
                id = 27,
                channel = Channel.Ecommerce,
                type = AdType.LargeImage,
                brandName = "HomeEase",
                title = "桌面办公好物组合",
                summary = "键盘、台灯、收纳架组合优惠。",
                coverUrl = "https://images.pexels.com/photos/4050315/pexels-photo-4050315.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "办公",
                tags = listOf("电商", "办公")
            ),
            AdItem(
                id = 28,
                channel = Channel.Local,
                type = AdType.SmallImage,
                brandName = "FreshBite",
                title = "周末轻食套餐",
                summary = "低脂沙拉、三明治和鲜榨果汁。",
                coverUrl = "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "轻食",
                tags = listOf("本地生活", "美食")
            ),
            AdItem(
                id = 29,
                channel = Channel.Featured,
                type = AdType.LargeImage,
                brandName = "NovaBook",
                title = "高性能轻薄笔记本",
                summary = "适合编程、学习和移动办公。",
                coverUrl = "https://images.pexels.com/photos/18105/pexels-photo.jpg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "笔记本",
                tags = listOf("科技", "电脑")
            ),
            AdItem(
                id = 30,
                channel = Channel.Ecommerce,
                type = AdType.SmallImage,
                brandName = "SoundMax",
                title = "降噪蓝牙耳机",
                summary = "通勤、运动、学习都能沉浸使用。",
                coverUrl = "https://images.pexels.com/photos/3394651/pexels-photo-3394651.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "耳机",
                tags = listOf("数码", "耳机")
            ),
            AdItem(
                id = 31,
                channel = Channel.Local,
                type = AdType.LargeImage,
                brandName = "CityTrip",
                title = "城市周边一日游",
                summary = "精选自然风景、露营和轻徒步路线。",
                coverUrl = "https://images.pexels.com/photos/417173/pexels-photo-417173.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "旅行",
                tags = listOf("旅行", "周末")
            ),
            AdItem(
                id = 32,
                channel = Channel.Featured,
                type = AdType.SmallImage,
                brandName = "AutoX",
                title = "新能源试驾活动",
                summary = "预约试驾可领取充电权益礼包。",
                coverUrl = "https://images.pexels.com/photos/3802510/pexels-photo-3802510.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "汽车",
                tags = listOf("汽车", "新能源")
            ),
            AdItem(
                id = 33,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "MorningFit",
                title = "晨间运动计划",
                summary = "15 分钟轻运动，开启高效一天。",
                coverUrl = "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(0),
                mediaLabel = "健身视频",
                tags = listOf("视频", "健身")
            ),
            AdItem(
                id = 34,
                channel = Channel.Ecommerce,
                type = AdType.Video,
                brandName = "StyleBox",
                title = "新品穿搭短片",
                summary = "夏季清爽穿搭灵感。",
                coverUrl = "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(1),
                mediaLabel = "穿搭视频",
                tags = listOf("视频", "电商")
            ),
            AdItem(
                id = 35,
                channel = Channel.Local,
                type = AdType.Video,
                brandName = "CoffeeTime",
                title = "手冲咖啡体验课",
                summary = "学习基础萃取和拉花技巧。",
                coverUrl = "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(2),
                mediaLabel = "咖啡视频",
                tags = listOf("视频", "咖啡")
            ),
            AdItem(
                id = 36,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "TravelNow",
                title = "海边度假目的地推荐",
                summary = "适合周末短途旅行的海岸路线。",
                coverUrl = "https://images.pexels.com/photos/457882/pexels-photo-457882.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(3),
                mediaLabel = "旅行视频",
                tags = listOf("视频", "旅行")
            ),
            AdItem(
                id = 37,
                channel = Channel.Ecommerce,
                type = AdType.Video,
                brandName = "CookMate",
                title = "厨房好物展示",
                summary = "提升做饭效率的小工具合集。",
                coverUrl = "https://images.pexels.com/photos/699953/pexels-photo-699953.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(0),
                mediaLabel = "美食视频",
                tags = listOf("视频", "厨房")
            ),
            AdItem(
                id = 38,
                channel = Channel.Local,
                type = AdType.Video,
                brandName = "CityLife",
                title = "城市夜生活推荐",
                summary = "餐厅、街区和夜景路线合集。",
                coverUrl = "https://images.pexels.com/photos/313782/pexels-photo-313782.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(1),
                mediaLabel = "城市视频",
                tags = listOf("视频", "本地")
            ),
            AdItem(
                id = 39,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "WorkSpace",
                title = "高效办公空间改造",
                summary = "用简单桌搭提升学习和工作专注度。",
                coverUrl = "https://images.pexels.com/photos/3184465/pexels-photo-3184465.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(2),
                mediaLabel = "办公视频",
                tags = listOf("视频", "办公")
            ),
            AdItem(
                id = 40,
                channel = Channel.Ecommerce,
                type = AdType.Video,
                brandName = "TechPro",
                title = "数码产品开箱",
                summary = "耳机、平板和键盘新品体验。",
                coverUrl = "https://images.pexels.com/photos/5082579/pexels-photo-5082579.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(3),
                mediaLabel = "数码视频",
                tags = listOf("视频", "数码")
            ),
            AdItem(
                id = 41,
                channel = Channel.Local,
                type = AdType.Video,
                brandName = "FreshMarket",
                title = "本地生鲜配送",
                summary = "蔬果、肉蛋奶最快当日送达。",
                coverUrl = "https://images.pexels.com/photos/264636/pexels-photo-264636.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(0),
                mediaLabel = "生鲜视频",
                tags = listOf("视频", "生鲜")
            ),
            AdItem(
                id = 42,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "RoadPlus",
                title = "周末自驾计划",
                summary = "精选路线、车辆保养和出行装备。",
                coverUrl = "https://images.pexels.com/photos/358070/pexels-photo-358070.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(1),
                mediaLabel = "汽车视频",
                tags = listOf("视频", "汽车")
            ),
            AdItem(
                id = 43,
                channel = Channel.Education,
                type = AdType.SmallImage,
                brandName = "LangLoop",
                title = "AI 英语口语每日训练",
                summary = "每天 10 分钟情景对话练习，帮助提升真实交流中的表达自信。",
                coverUrl = "https://images.pexels.com/photos/4145153/pexels-photo-4145153.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "课程图片",
                tags = listOf("教育", "英语", "AI学习")
            ),
            AdItem(
                id = 44,
                channel = Channel.Education,
                type = AdType.LargeImage,
                brandName = "CodeCamp",
                title = "Java 后端项目实战课",
                summary = "从 Spring Boot 到 Redis、MySQL、接口设计，适合求职前系统补强。",
                coverUrl = "https://images.pexels.com/photos/574071/pexels-photo-574071.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "编程课程",
                tags = listOf("教育", "Java", "后端")
            ),
            AdItem(
                id = 45,
                channel = Channel.Ecommerce,
                type = AdType.SmallImage,
                brandName = "简居生活",
                title = "宿舍桌面收纳好物",
                summary = "小空间也能保持整洁，适合学生宿舍和租房桌面改造。",
                coverUrl = "https://images.pexels.com/photos/4050315/pexels-photo-4050315.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "收纳图片",
                tags = listOf("电商", "收纳", "宿舍")
            ),
            AdItem(
                id = 46,
                channel = Channel.Local,
                type = AdType.SmallImage,
                brandName = "城市书咖",
                title = "安静自习空间限时体验",
                summary = "高速网络、独立座位和免费咖啡，适合备考与远程办公。",
                coverUrl = "https://images.pexels.com/photos/3184465/pexels-photo-3184465.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "自习室图片",
                tags = listOf("本地", "学习", "咖啡")
            ),
            AdItem(
                id = 47,
                channel = Channel.Featured,
                type = AdType.LargeImage,
                brandName = "AuroraPad",
                title = "学习娱乐两用平板新品",
                summary = "高清大屏、长续航和分屏笔记，满足网课、阅读和观影需求。",
                coverUrl = "https://images.pexels.com/photos/5082579/pexels-photo-5082579.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "平板图片",
                tags = listOf("科技", "平板", "学习")
            ),
            AdItem(
                id = 48,
                channel = Channel.Local,
                type = AdType.LargeImage,
                brandName = "FreshTea",
                title = "夏日鲜果茶第二杯半价",
                summary = "精选新鲜水果制作，清爽解腻，适合午后学习休息。",
                coverUrl = "https://images.pexels.com/photos/1793035/pexels-photo-1793035.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "饮品图片",
                tags = listOf("本地", "饮品", "优惠")
            ),
            AdItem(
                id = 49,
                channel = Channel.Education,
                type = AdType.SmallImage,
                brandName = "Offer训练营",
                title = "大厂笔试面试冲刺计划",
                summary = "覆盖算法、数据库、Redis、消息队列和项目表达训练。",
                coverUrl = "https://images.pexels.com/photos/5905709/pexels-photo-5905709.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "面试课程",
                tags = listOf("求职", "面试", "计算机")
            ),
            AdItem(
                id = 50,
                channel = Channel.Featured,
                type = AdType.SmallImage,
                brandName = "SoundMax",
                title = "主动降噪耳机学生优惠",
                summary = "通勤、学习和运动都能稳定佩戴，支持长续航和高清通话。",
                coverUrl = "https://images.pexels.com/photos/3394651/pexels-photo-3394651.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "耳机图片",
                tags = listOf("数码", "耳机")
            ),
            AdItem(
                id = 51,
                channel = Channel.Ecommerce,
                type = AdType.LargeImage,
                brandName = "LightDesk",
                title = "高效学习桌搭套装",
                summary = "护眼台灯、键盘、支架组合，让学习桌面更清爽。",
                coverUrl = "https://images.pexels.com/photos/18105/pexels-photo.jpg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "桌搭图片",
                tags = listOf("电商", "办公", "桌搭")
            ),
            AdItem(
                id = 52,
                channel = Channel.Local,
                type = AdType.SmallImage,
                brandName = "健身星球",
                title = "暑期健身卡买一赠一",
                summary = "器械区、团课区全面开放，新会员可预约免费体测。",
                coverUrl = "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = null,
                mediaLabel = "健身图片",
                tags = listOf("本地", "运动", "健身")
            ),

            AdItem(
                id = 53,
                channel = Channel.Education,
                type = AdType.Video,
                brandName = "CodeCamp",
                title = "零基础编程体验课",
                summary = "通过小项目学习变量、函数、页面开发和接口调用。",
                coverUrl = "https://images.pexels.com/photos/574071/pexels-photo-574071.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(2),
                mediaLabel = "编程视频",
                tags = listOf("视频", "教育", "编程")
            ),
            AdItem(
                id = 54,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "MorningFit",
                title = "15 分钟晨间训练",
                summary = "低门槛居家运动课程，适合学生党和久坐办公人群。",
                coverUrl = "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(3),
                mediaLabel = "健身视频",
                tags = listOf("视频", "健身")
            ),
            AdItem(
                id = 55,
                channel = Channel.Local,
                type = AdType.Video,
                brandName = "CoffeeTime",
                title = "手冲咖啡体验课",
                summary = "从研磨、水温到萃取时间，学习一杯好咖啡的基础方法。",
                coverUrl = "https://images.pexels.com/photos/373639/pexels-photo-373639.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(0),
                mediaLabel = "咖啡视频",
                tags = listOf("视频", "本地", "咖啡")
            ),
            AdItem(
                id = 56,
                channel = Channel.Ecommerce,
                type = AdType.Video,
                brandName = "StyleBox",
                title = "夏季通勤穿搭短片",
                summary = "基础款也能穿出清爽感，适合上课、实习和日常出门。",
                coverUrl = "https://images.pexels.com/photos/994523/pexels-photo-994523.jpeg?auto=compress&cs=tinysrgb&w=900",
                videoUrl = audibleVideoUri(1),
                mediaLabel = "穿搭视频",
                tags = listOf("视频", "电商", "穿搭")
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
