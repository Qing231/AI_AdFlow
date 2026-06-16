package com.example.aiadflow.ui.common

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.theme.AppSpacing

internal const val RefreshSnackbarVisibleMillis = 1_200L

internal val HomeBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF4F8FF),
        Color(0xFFEEF6FF),
        Color(0xFFFFFFFF)
    )
)

internal val PrimaryGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2563EB),
        Color(0xFF7C3AED)
    )
)

internal val TagBackgroundColors = listOf(
    Color(0xFFEFF6FF),
    Color(0xFFF3EEFF),
    Color(0xFFF3F6FA)
)

internal data class AdMediaSpec(
    val height: Dp,
    val color: Color
)

internal fun mediaSpecFor(adType: AdType): AdMediaSpec = when (adType) {
    AdType.SmallImage -> AdMediaSpec(
        height = AppSpacing.CompactMediaHeight,
        color = Color(0xFF0F766E)
    )
    AdType.ImageText -> AdMediaSpec(
        height = AppSpacing.ImageTextMediaHeight,
        color = Color(0xFF7C3AED)
    )
    AdType.Video -> AdMediaSpec(
        height = AppSpacing.VideoMediaHeight,
        color = Color(0xFFDC2626)
    )
    AdType.LargeImage -> AdMediaSpec(
        height = AppSpacing.LargeImageMediaHeight,
        color = Color(0xFF2563EB)
    )
}

internal fun channelLabelFor(channel: Channel): String = when (channel) {
    Channel.Featured -> "推荐"
    Channel.Ecommerce -> "电商"
    Channel.Local -> "本地"
    Channel.NewArrival -> "新品"
    Channel.Finance -> "金融"
    Channel.Health -> "健康"
    Channel.Travel -> "出行"
    Channel.Education -> "教育"
}
