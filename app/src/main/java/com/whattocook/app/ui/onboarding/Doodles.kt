package com.whattocook.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Tiny vector ingredient drawings for the onboarding chips (no emoji, no image files). */
enum class Doodle { EGG, TOMATO, ONION, RICE }

@Composable
fun IngredientDoodle(
    doodle: Doodle,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w / 11f, cap = StrokeCap.Round)
        when (doodle) {
            Doodle.EGG -> {
                drawOval(color, Offset(w * 0.2f, h * 0.08f), Size(w * 0.6f, h * 0.84f), style = stroke)
                drawCircle(color.copy(alpha = 0.35f), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.58f))
            }
            Doodle.TOMATO -> {
                drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.58f), style = stroke)
                val leaf = Path().apply {
                    moveTo(w * 0.3f, h * 0.2f)
                    lineTo(w * 0.5f, h * 0.3f)
                    lineTo(w * 0.7f, h * 0.2f)
                }
                drawPath(leaf, color, style = stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.3f), Offset(w * 0.5f, h * 0.06f), stroke.width, StrokeCap.Round)
            }
            Doodle.ONION -> {
                val bulb = Path().apply {
                    moveTo(w * 0.5f, h * 0.06f)
                    cubicTo(w * 0.56f, h * 0.3f, w * 0.92f, h * 0.42f, w * 0.86f, h * 0.68f)
                    cubicTo(w * 0.8f, h * 0.92f, w * 0.2f, h * 0.92f, w * 0.14f, h * 0.68f)
                    cubicTo(w * 0.08f, h * 0.42f, w * 0.44f, h * 0.3f, w * 0.5f, h * 0.06f)
                    close()
                }
                drawPath(bulb, color, style = stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.42f), Offset(w * 0.5f, h * 0.84f), stroke.width * 0.7f, StrokeCap.Round)
            }
            Doodle.RICE -> {
                val bowl = Path().apply {
                    moveTo(w * 0.08f, h * 0.48f)
                    lineTo(w * 0.92f, h * 0.48f)
                    cubicTo(w * 0.88f, h * 0.82f, w * 0.12f, h * 0.82f, w * 0.08f, h * 0.48f)
                    close()
                }
                drawPath(bowl, color, style = stroke)
                listOf(0.3f to 0.34f, 0.5f to 0.26f, 0.7f to 0.34f, 0.42f to 0.16f, 0.6f to 0.16f).forEach { (x, y) ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(w * x - w * 0.05f, h * y - h * 0.035f),
                        size = Size(w * 0.1f, h * 0.07f),
                        cornerRadius = CornerRadius(w * 0.05f),
                    )
                }
            }
        }
    }
}

/** A small sun with rays; [glow] (0..1) adds a soft halo. */
@Composable
fun SunDoodle(
    color: Color,
    glow: Float,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val center = Offset(w / 2f, w / 2f)
        drawCircle(color.copy(alpha = 0.18f * glow), radius = w * 0.5f, center = center)
        drawCircle(color, radius = w * 0.2f, center = center)
        val stroke = w / 14f
        repeat(8) { i ->
            val angle = Math.toRadians(i * 45.0)
            val inner = w * 0.3f
            val outer = w * (0.4f + 0.06f * glow)
            drawLine(
                color = color,
                start = Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                end = Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
