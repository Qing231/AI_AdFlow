package com.example.aiadflow.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiadflow.ui.common.PrimaryGradientBrush
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius

@Composable
internal fun AiSummaryText(summary: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("AI 摘要：")
            }
            append(summary.cleanAiSummary())
        },
        color = Color(0xFF60738D),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun DetailSummarySection(summary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(AppRadius.Full)
                    .background(PrimaryGradientBrush)
            )
            Text(
                text = "AI 摘要",
                color = Color(0xFF102033),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = summary.cleanAiSummary(),
            color = Color(0xFF60738D),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun String.cleanAiSummary(): String {
    return removePrefix("AI 摘要：")
        .removePrefix("AI摘要：")
        .trimStart()
        .ifBlank { "暂无摘要" }
}
