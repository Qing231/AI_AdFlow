package com.example.aiadflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    background = AppColors.PageBackground,
    surface = AppColors.Surface,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary
)

object AppRadius {
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(20.dp)
    val Full = RoundedCornerShape(999.dp)
}

object AppSpacing {
    val Small = 8.dp
    val Medium = 16.dp
    val Section = 18.dp
    val PageHorizontal = 20.dp
    val HeaderHeight = 72.dp
    val IconButton = 44.dp
    val TabHeight = 44.dp
    val SearchHeight = 56.dp
    val AdMediaHeight = 168.dp
}

@Composable
fun AIAdFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
