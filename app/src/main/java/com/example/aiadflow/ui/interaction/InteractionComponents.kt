package com.example.aiadflow.ui.interaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiadflow.ui.common.PrimaryGradientBrush
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AdActionRow(
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LikeButtonWithEffect(
                selected = liked,
                onClick = onLikeClick
            )
            CollectButtonWithEffect(
                selected = collected,
                onClick = onCollectClick
            )
            ActionChip(
                text = "分享",
                selected = false,
                onClick = onShareClick
            )
        }
        PrimaryActionChip(
            text = "查看",
            onClick = onViewClick
        )
    }
}

@Composable
internal fun DetailActionRow(
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LikeButtonWithEffect(
            selected = liked,
            onClick = onLikeClick
        )
        CollectButtonWithEffect(
            selected = collected,
            onClick = onCollectClick
        )
        ActionChip(
            text = "分享",
            selected = false,
            onClick = onShareClick
        )
    }
}

@Composable
internal fun LikeButtonWithEffect(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InteractionToggleButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = if (selected) "已点赞" else "点赞",
        iconKind = InteractionIconKind.Heart,
        selectedBackground = Color(0xFFFFE4EF),
        selectedAccent = Color(0xFFEC4899),
        selectedTextColor = Color(0xFFDB2777),
        unselectedTextColor = Color(0xFF66758B),
        effectLabel = "❤ +1",
        effectTint = Color(0xFFF472B6),
        effectKind = InteractionEffectKind.Like
    )
}

@Composable
internal fun CollectButtonWithEffect(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InteractionToggleButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = if (selected) "已收藏" else "收藏",
        iconKind = InteractionIconKind.Star,
        selectedBackground = Color(0xFFFFF7ED),
        selectedAccent = Color(0xFFF59E0B),
        selectedTextColor = Color(0xFFD97706),
        unselectedTextColor = Color(0xFF66758B),
        effectLabel = "★ 已收藏",
        effectTint = Color(0xFFF59E0B),
        effectKind = InteractionEffectKind.Collect
    )
}

@Composable
private fun InteractionToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    iconKind: InteractionIconKind,
    selectedBackground: Color,
    selectedAccent: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    effectLabel: String,
    effectTint: Color,
    effectKind: InteractionEffectKind
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val pressScale = remember { Animatable(1f) }
    val pressRotation = remember { Animatable(0f) }
    val effectProgress = remember { Animatable(0f) }
    var effectVisible by remember { mutableStateOf(false) }
    var effectToken by remember { mutableLongStateOf(0L) }
    var effectArmed by remember { mutableStateOf(false) }

    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedBackground else Color(0xFFF3F6FA),
        label = "interactionBackground"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedAccent.copy(alpha = 0.18f) else Color(0xFFE4EAF3),
        label = "interactionBorder"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedTextColor else unselectedTextColor,
        label = "interactionContent"
    )
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "interactionGlowAlpha"
    )

    suspend fun runPressBounce() {
        pressScale.snapTo(1f)
        when (effectKind) {
            InteractionEffectKind.Like -> {
                pressScale.animateTo(1.2f, tween(90, easing = FastOutSlowInEasing))
                pressScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
            }
            InteractionEffectKind.Collect -> {
                pressScale.animateTo(1.08f, tween(100, easing = FastOutSlowInEasing))
                pressRotation.animateTo(7f, tween(100, easing = FastOutSlowInEasing))
                pressScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                pressRotation.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            }
        }
    }

    fun triggerSuccessEffect() {
        if (effectArmed) {
            return
        }
        effectArmed = true
        effectToken += 1
    }

    LaunchedEffect(selected) {
        if (!selected) {
            effectArmed = false
        }
    }

    LaunchedEffect(effectToken) {
        if (effectToken == 0L) {
            return@LaunchedEffect
        }
        effectVisible = true
        effectProgress.snapTo(0f)
        effectProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
        )
        delay(140L)
        effectVisible = false
    }

    Box(
        modifier = modifier
            .height(38.dp)
    ) {
        AnimatedVisibility(
            visible = effectVisible,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 22.dp),
            enter = fadeIn(tween(90)) + scaleIn(initialScale = 0.88f, animationSpec = tween(90)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f, animationSpec = tween(160))
        ) {
            when (effectKind) {
                InteractionEffectKind.Like -> FloatingLikeEffect(
                    progress = effectProgress.value,
                    label = effectLabel,
                    tint = effectTint
                )
                InteractionEffectKind.Collect -> FloatingCollectEffect(
                    progress = effectProgress.value,
                    label = effectLabel,
                    tint = effectTint
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(
                    scaleX = pressScale.value,
                    scaleY = pressScale.value,
                    rotationZ = pressRotation.value
                )
                .height(38.dp)
                .clip(AppRadius.Full)
                .background(backgroundColor)
                .semantics {
                    contentDescription = label
                }
                .clickable {
                    val shouldCelebrate = !selected && !effectArmed
                    currentOnClick()
                    scope.launch { runPressBounce() }
                    if (shouldCelebrate) {
                        triggerSuccessEffect()
                    }
                }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(AppRadius.Full)
                    .background(
                        color = selectedAccent.copy(alpha = 0.07f * glowAlpha)
                    )
            )
            InteractionShapeIcon(
                kind = iconKind,
                selected = selected,
                color = contentColor,
                selectedColor = selectedAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun InteractionShapeIcon(
    kind: InteractionIconKind,
    selected: Boolean,
    color: Color,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val path = when (kind) {
            InteractionIconKind.Heart -> heartPath(size.width, size.height)
            InteractionIconKind.Star -> starPath(size.width, size.height)
        }
        val stroke = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        if (selected) {
            drawPath(path = path, color = selectedColor)
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.34f),
                style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
            )
        } else {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.86f),
                style = stroke
            )
        }
    }
}

private fun heartPath(width: Float, height: Float): Path = Path().apply {
    moveTo(width * 0.50f, height * 0.88f)
    cubicTo(width * 0.18f, height * 0.70f, width * 0.05f, height * 0.49f, width * 0.09f, height * 0.29f)
    cubicTo(width * 0.13f, height * 0.10f, width * 0.34f, height * 0.03f, width * 0.50f, height * 0.22f)
    cubicTo(width * 0.66f, height * 0.03f, width * 0.87f, height * 0.10f, width * 0.91f, height * 0.29f)
    cubicTo(width * 0.95f, height * 0.49f, width * 0.82f, height * 0.70f, width * 0.50f, height * 0.88f)
    close()
}

private fun starPath(width: Float, height: Float): Path {
    val outerRadius = minOf(width, height) * 0.48f
    val innerRadius = outerRadius * 0.46f
    val centerX = width / 2f
    val centerY = height / 2f

    return Path().apply {
        for (index in 0 until 10) {
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            val angle = -PI / 2.0 + index * PI / 5.0
            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius
            if (index == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }
}

@Composable
private fun FloatingLikeEffect(
    progress: Float,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    FloatingBurstEffect(
        progress = progress,
        label = label,
        tint = tint,
        modifier = modifier,
        particles = listOf(
            FloatingParticleSpec("❤", Color(0xFFF472B6), -18.dp, 22.dp, -28.dp, 28.dp, 0.00f),
            FloatingParticleSpec("❤", Color(0xFFFB7185), 12.dp, 18.dp, 20.dp, 36.dp, 0.12f),
            FloatingParticleSpec("❤", Color(0xFF8B5CF6), -34.dp, 26.dp, -18.dp, 46.dp, 0.24f)
        )
    )
}

@Composable
private fun FloatingCollectEffect(
    progress: Float,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    FloatingBurstEffect(
        progress = progress,
        label = label,
        tint = tint,
        modifier = modifier,
        particles = listOf(
            FloatingParticleSpec("★", Color(0xFFF59E0B), -18.dp, 18.dp, -30.dp, 26.dp, 0.00f),
            FloatingParticleSpec("★", Color(0xFFFDE68A), 14.dp, 22.dp, 22.dp, 38.dp, 0.10f),
            FloatingParticleSpec("★", Color(0xFFA855F7), -34.dp, 24.dp, -14.dp, 44.dp, 0.20f)
        )
    )
}

@Composable
private fun FloatingBurstEffect(
    progress: Float,
    label: String,
    tint: Color,
    particles: List<FloatingParticleSpec>,
    modifier: Modifier = Modifier
) {
    val easedProgress = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    val labelAlpha = (1f - easedProgress * 0.55f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier
                .offset(y = (-24).dp),
            color = tint.copy(alpha = labelAlpha),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )

        particles.forEach { particle ->
            FloatingParticle(
                spec = particle,
                progress = easedProgress
            )
        }
    }
}

@Composable
private fun FloatingParticle(
    spec: FloatingParticleSpec,
    progress: Float
) {
    val offsetProgress = FastOutSlowInEasing.transform(
        ((progress - spec.delayFraction) / (1f - spec.delayFraction))
            .coerceIn(0f, 1f)
    )
    val x = spec.startX + (spec.endX - spec.startX) * offsetProgress
    val y = spec.startY + (spec.endY - spec.startY) * offsetProgress
    val alpha = when {
        offsetProgress < 0.12f -> offsetProgress / 0.12f
        offsetProgress > 0.78f -> (1f - offsetProgress) / 0.22f
        else -> 1f
    }.coerceIn(0f, 1f)
    val scale = 0.96f + offsetProgress * 0.24f

    Text(
        text = spec.symbol,
        modifier = Modifier
            .offset(x = x, y = y)
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            ),
        color = spec.color,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = spec.fontSize,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
internal fun ActionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) Color(0xFFEFF5FF) else Color(0xFFF3F6FA),
        label = "actionChipBackground"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) AppColors.Primary else Color(0xFF66758B),
        label = "actionChipContent"
    )

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(AppRadius.Full)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun PrimaryActionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(AppRadius.Full)
            .background(PrimaryGradientBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

private enum class InteractionEffectKind {
    Like,
    Collect
}

private enum class InteractionIconKind {
    Heart,
    Star
}

private data class FloatingParticleSpec(
    val symbol: String,
    val color: Color,
    val startX: Dp,
    val endX: Dp,
    val startY: Dp,
    val endY: Dp,
    val delayFraction: Float,
    val fontSize: androidx.compose.ui.unit.TextUnit = 18.sp
)
