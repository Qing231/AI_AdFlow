package com.example.aiadflow.ui.channel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.common.PrimaryGradientBrush
import com.example.aiadflow.ui.common.channelLabelFor
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

@Composable
internal fun CategoryTabs(
    channels: List<Channel>,
    selectedChannel: Channel?,
    onChannelSelected: (Channel?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            ChannelTabChip(
                label = "全部",
                selected = selectedChannel == null,
                onClick = { onChannelSelected(null) }
            )
            channels.forEach { channel ->
                ChannelTabChip(
                    label = channelLabelFor(channel),
                    selected = selectedChannel == channel,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
private fun ChannelTabChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else Color(0xFFE4EAF3),
        label = "channelTabBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF66758B),
        label = "channelTabText"
    )
    val tabHeight by animateDpAsState(
        targetValue = if (selected) AppSpacing.TabSelectedHeight else AppSpacing.TabHeight,
        label = "channelTabHeight"
    )
    val textWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = modifier
            .width(AppSpacing.TabWidth)
            .height(tabHeight)
            .clip(AppRadius.Full)
            .background(
                if (selected) {
                    PrimaryGradientBrush
                } else {
                    Brush.linearGradient(listOf(Color.White, Color.White))
                }
            )
            .border(
                width = AppSpacing.TabBorderWidth,
                color = borderColor,
                shape = AppRadius.Full
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = textWeight)
        )
    }
}
