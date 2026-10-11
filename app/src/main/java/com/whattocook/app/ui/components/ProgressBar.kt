package com.whattocook.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Motion

/** Rounded progress bar that animates from empty to [progress] and between values. */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.cardBorder.copy(alpha = 0.6f),
    height: Dp = 8.dp,
    durationMillis: Int = 900,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.animateTo(target, tween(durationMillis, easing = Motion.Emphasized))
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val width = size.width * animated.value
        if (width > 0f) {
            drawRoundRect(color = color, size = Size(width.coerceAtLeast(size.height), size.height), cornerRadius = radius)
        }
    }
}
