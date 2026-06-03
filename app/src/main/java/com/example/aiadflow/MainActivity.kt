package com.example.aiadflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.example.aiadflow.ui.theme.AIAdFlowTheme
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAdFlowTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.PageBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            HeaderBar()
            ChannelTabs()
            SearchBar()
            AdCard()
        }
    }
}

@Composable
private fun HeaderBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AIAdFlow",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "AI advertising feed",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Box(
            modifier = Modifier
                .size(AppSpacing.IconButton)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ChannelTabs() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppSpacing.TabHeight)
                    .clip(AppRadius.Full)
                    .background(if (index == 0) AppColors.Primary else AppColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = listOf("Featured", "Ecommerce", "Local")[index],
                    color = if (index == 0) AppColors.OnPrimary else AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SearchBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.SearchHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(horizontal = AppSpacing.Medium),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Search ads, brands, tags",
            color = AppColors.TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AdCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.AdMediaHeight)
                .clip(AppRadius.Medium)
                .background(AppColors.MediaPlaceholder)
        )
        Text(
            text = "Ad card placeholder",
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Content and interaction states will be added in the next development part.",
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AIAdFlowTheme {
        HomeScreen()
    }
}
