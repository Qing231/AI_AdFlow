package com.example.aiadflow.ui.tag

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aiadflow.ui.common.TagBackgroundColors
import com.example.aiadflow.ui.common.channelLabelFor
import com.example.aiadflow.ui.feed.AdFeedUiState
import com.example.aiadflow.ui.interaction.ActionChip
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagRow(
    tags: List<String>,
    selectedTag: String?,
    onTagClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = 2
    ) {
        tags.forEachIndexed { index, tag ->
            val selected = tag.equals(selectedTag, ignoreCase = true)
            val backgroundColor by animateColorAsState(
                targetValue = if (selected) AppColors.Primary else TagBackgroundColors[index % TagBackgroundColors.size],
                label = "tagBackground"
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) AppColors.Primary else Color.Transparent,
                label = "tagBorder"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) AppColors.OnPrimary else Color(0xFF63758C),
                label = "tagText"
            )
            TagChip(
                text = "#$tag",
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                textColor = textColor,
                onClick = { onTagClick(tag) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReadOnlyTagRow(tags: List<String>) {
    if (tags.isEmpty()) {
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEachIndexed { index, tag ->
            TagChip(
                text = "#$tag",
                backgroundColor = TagBackgroundColors[index % TagBackgroundColors.size],
                borderColor = Color.Transparent,
                textColor = Color(0xFF63758C),
                onClick = null
            )
        }
    }
}

@Composable
internal fun ActiveFiltersBar(
    uiState: AdFeedUiState,
    onClearFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Text(
            text = activeFilterLabel(uiState),
            modifier = Modifier.weight(1f),
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ActionChip(
            text = "清除",
            selected = false,
            onClick = onClearFilters
        )
    }
}

internal fun AdFeedUiState.hasActiveFilters(): Boolean {
    return selectedChannel != null ||
        searchText.isNotBlank() ||
        !selectedTag.isNullOrBlank() ||
        showCollectedOnly
}

private fun activeFilterLabel(uiState: AdFeedUiState): String {
    val filters = buildList {
        if (uiState.showCollectedOnly) add("收藏")
        uiState.selectedChannel?.let { add(channelLabelFor(it)) }
        uiState.searchText.takeIf { it.isNotBlank() }?.let { add("\"${it.trim()}\"") }
        uiState.selectedTag?.takeIf { it.isNotBlank() }?.let { add("#${it.trim()}") }
    }

    return filters.joinToString(separator = "  ")
}

@Composable
private fun TagChip(
    text: String,
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: (() -> Unit)?
) {
    val clickableModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(AppRadius.Full)
            .background(backgroundColor)
            .border(
                width = AppSpacing.TagBorderWidth,
                color = borderColor,
                shape = AppRadius.Full
            )
            .then(clickableModifier)
            .widthIn(max = AppSpacing.TagMaxWidth)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}
