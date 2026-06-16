package com.example.aiadflow.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aiadflow.ui.feed.AdFeedUiState
import com.example.aiadflow.ui.interaction.ActionChip
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

@Composable
internal fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.SearchHeight)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x120F2D5C),
                spotColor = Color(0x120F2D5C)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                width = AppSpacing.SearchBorderWidth,
                color = if (query.isBlank()) Color(0xFFE5ECF6) else Color(0xFF7C3AED),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "⌕",
            color = Color(0xFF8EA0B8),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        HorizontalDivider(
            modifier = Modifier
                .height(AppSpacing.SearchDividerHeight)
                .size(width = AppSpacing.SearchDividerWidth, height = AppSpacing.SearchDividerHeight),
            color = Color(0xFFE5ECF6)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextPrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "搜索广告、品牌、标签",
                        color = Color(0xFF9AA8BB),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.SearchClearButton)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5FB))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SmartSearchStatus(
    uiState: AdFeedUiState,
    onTagSelected: (String?) -> Unit
) {
    val query = uiState.activeAiSearchQuery ?: uiState.searchText.takeIf { it.isNotBlank() }
    if (query.isNullOrBlank() && !uiState.isAiSearchUnderstanding) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(Color.White.copy(alpha = 0.86f))
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            if (uiState.isAiSearchUnderstanding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.Primary
                )
            }
            Text(
                text = if (uiState.isAiSearchUnderstanding) {
                    "大模型正在理解：$query"
                } else {
                    val intent = uiState.aiSearchUnderstanding.takeIf { it.isNotBlank() } ?: query.orEmpty()
                    "AI 已理解：$intent · ${uiState.aiSearchResultCount} 条"
                },
                modifier = Modifier.weight(1f),
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!uiState.isAiSearchUnderstanding && uiState.aiSearchSuggestedTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.aiSearchSuggestedTags.forEach { tag ->
                    ActionChip(
                        text = "#$tag",
                        selected = tag.equals(uiState.selectedTag, ignoreCase = true),
                        onClick = { onTagSelected(tag) }
                    )
                }
            }
        }
    }
}
