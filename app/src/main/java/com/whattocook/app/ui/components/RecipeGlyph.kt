package com.whattocook.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Simple hand-drawn style line illustrations for recipe cards, drawn in Compose. */
enum class RecipeGlyphKind { BOWL, POT, PAN, NOODLES, SALAD, EGG, PLATE }

fun glyphFor(title: String): RecipeGlyphKind {
    val t = title.lowercase()
    return when {
        listOf("noodle", "ramen", "pasta", "spaghetti", "chow mein").any { it in t } -> RecipeGlyphKind.NOODLES
        listOf("salad", "slaw", "bowl of greens").any { it in t } -> RecipeGlyphKind.SALAD
        listOf("omelette", "egg", "anday", "shakshuka").any { it in t } -> RecipeGlyphKind.EGG
        listOf("fried rice", "stir", "fry", "karahi", "pan").any { it in t } -> RecipeGlyphKind.PAN
        listOf("dal", "soup", "stew", "curry", "masala", "keema", "chili").any { it in t } -> RecipeGlyphKind.POT
        listOf("rice", "biryani", "pulao", "oats", "porridge", "bowl").any { it in t } -> RecipeGlyphKind.BOWL
        else -> RecipeGlyphKind.PLATE
    }
}

@Composable
fun RecipeGlyph(
    kind: RecipeGlyphKind,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() / 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            RecipeGlyphKind.BOWL -> drawBowl(color, stroke, steam = true)
            RecipeGlyphKind.POT -> drawPot(color, stroke)
            RecipeGlyphKind.PAN -> drawPan(color, stroke)
            RecipeGlyphKind.NOODLES -> drawNoodles(color, stroke)
            RecipeGlyphKind.SALAD -> drawSalad(color, stroke)
            RecipeGlyphKind.EGG -> drawEgg(color, stroke)
            RecipeGlyphKind.PLATE -> drawPlate(color, stroke)
        }
    }
}

private fun DrawScope.drawBowl(color: Color, stroke: Stroke, steam: Boolean) {
    val w = size.width
    val h = size.height
    val bowl = Path().apply {
        moveTo(w * 0.12f, h * 0.5f)
        lineTo(w * 0.88f, h * 0.5f)
        cubicTo(w * 0.86f, h * 0.78f, w * 0.68f, h * 0.88f, w * 0.5f, h * 0.88f)
        cubicTo(w * 0.32f, h * 0.88f, w * 0.14f, h * 0.78f, w * 0.12f, h * 0.5f)
        close()
    }
    drawPath(bowl, color, style = stroke)
    drawLine(color, Offset(w * 0.38f, h * 0.94f), Offset(w * 0.62f, h * 0.94f), stroke.width, StrokeCap.Round)
    if (steam) drawSteam(color, stroke, h * 0.12f, h * 0.38f)
}

private fun DrawScope.drawSteam(color: Color, stroke: Stroke, top: Float, bottom: Float) {
    val w = size.width
    listOf(0.36f, 0.5f, 0.64f).forEach { x ->
        val path = Path().apply {
            moveTo(w * x, bottom)
            cubicTo(w * (x - 0.06f), bottom - (bottom - top) * 0.35f, w * (x + 0.06f), bottom - (bottom - top) * 0.65f, w * x, top)
        }
        drawPath(path, color.copy(alpha = 0.7f), style = Stroke(stroke.width * 0.8f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawPot(color: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(color, Offset(w * 0.18f, h * 0.42f), Size(w * 0.64f, h * 0.44f), androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = stroke)
    drawLine(color, Offset(w * 0.12f, h * 0.42f), Offset(w * 0.88f, h * 0.42f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.06f, h * 0.52f), Offset(w * 0.18f, h * 0.52f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.82f, h * 0.52f), Offset(w * 0.94f, h * 0.52f), stroke.width, StrokeCap.Round)
    drawSteam(color, stroke, h * 0.08f, h * 0.32f)
}

private fun DrawScope.drawPan(color: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawOval(color, Offset(w * 0.06f, h * 0.38f), Size(w * 0.6f, h * 0.38f), style = stroke)
    drawLine(color, Offset(w * 0.64f, h * 0.55f), Offset(w * 0.96f, h * 0.44f), stroke.width * 1.3f, StrokeCap.Round)
    drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.3f, h * 0.56f), style = stroke)
    drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.45f, h * 0.52f), style = stroke)
    drawSteam(color, stroke, h * 0.06f, h * 0.3f)
}

private fun DrawScope.drawNoodles(color: Color, stroke: Stroke) {
    drawBowl(color, stroke, steam = false)
    val w = size.width
    val h = size.height
    drawLine(color, Offset(w * 0.55f, h * 0.48f), Offset(w * 0.86f, h * 0.06f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.63f, h * 0.48f), Offset(w * 0.96f, h * 0.12f), stroke.width, StrokeCap.Round)
    listOf(0.3f, 0.4f).forEach { x ->
        val path = Path().apply {
            moveTo(w * x, h * 0.48f)
            cubicTo(w * (x - 0.05f), h * 0.36f, w * (x + 0.05f), h * 0.28f, w * x, h * 0.18f)
        }
        drawPath(path, color, style = stroke)
    }
}

private fun DrawScope.drawSalad(color: Color, stroke: Stroke) {
    drawBowl(color, stroke, steam = false)
    val w = size.width
    val h = size.height
    listOf(Offset(0.32f, 0.36f) to -30f, Offset(0.5f, 0.3f) to 0f, Offset(0.68f, 0.36f) to 30f).forEach { (c, _) ->
        drawOval(color, Offset(w * (c.x - 0.09f), h * (c.y - 0.12f)), Size(w * 0.18f, h * 0.22f), style = stroke)
    }
}

private fun DrawScope.drawEgg(color: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    val white = Path().apply {
        moveTo(w * 0.2f, h * 0.5f)
        cubicTo(w * 0.1f, h * 0.2f, w * 0.5f, h * 0.08f, w * 0.68f, h * 0.22f)
        cubicTo(w * 0.95f, h * 0.3f, w * 0.95f, h * 0.7f, w * 0.7f, h * 0.82f)
        cubicTo(w * 0.45f, h * 0.95f, w * 0.28f, h * 0.8f, w * 0.2f, h * 0.5f)
        close()
    }
    drawPath(white, color, style = stroke)
    drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.52f, h * 0.52f), style = stroke)
}

private fun DrawScope.drawPlate(color: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.52f), style = stroke)
    drawCircle(color, radius = w * 0.2f, center = Offset(w * 0.5f, h * 0.52f), style = Stroke(stroke.width * 0.7f))
    drawLine(color, Offset(w * 0.08f, h * 0.24f), Offset(w * 0.08f, h * 0.82f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.92f, h * 0.24f), Offset(w * 0.92f, h * 0.82f), stroke.width, StrokeCap.Round)
}
