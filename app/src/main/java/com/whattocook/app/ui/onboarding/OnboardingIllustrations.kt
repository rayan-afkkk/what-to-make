package com.whattocook.app.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whattocook.app.R
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Pastel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/** Every illustration is drawn into this box and scaled down on small screens. */
internal val IllustrationWidth = 320.dp
internal val IllustrationHeight = 300.dp

private val TwoPi = (2 * PI).toFloat()

/** A slow 0..2π phase for idle floating loops; constant 0 when motion is reduced. */
@Composable
private fun rememberLoopPhase(animate: Boolean, durationMillis: Int): State<Float> {
    if (!animate) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "loop")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = TwoPi,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "phase",
    )
}

// ---------------------------------------------------------------------------
// Page 1: a pantry shelf where ingredient chips drop in and then float gently.
// ---------------------------------------------------------------------------

private data class ShelfChip(
    @StringRes val label: Int,
    val doodle: Doodle,
    val x: Dp,
    val y: Dp,
    val tilt: Float,
)

private val ShelfChips = listOf(
    ShelfChip(R.string.onboarding_chip_egg, Doodle.EGG, 34.dp, 80.dp, -3f),
    ShelfChip(R.string.onboarding_chip_tomato, Doodle.TOMATO, 170.dp, 80.dp, 2.5f),
    ShelfChip(R.string.onboarding_chip_onion, Doodle.ONION, 50.dp, 210.dp, 2f),
    ShelfChip(R.string.onboarding_chip_rice, Doodle.RICE, 182.dp, 210.dp, -2.5f),
)

@Composable
fun PantryShelfIllustration(active: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val pastels = listOf(colors.pastels.amber, colors.pastels.pink, colors.pastels.purple, colors.pastels.green)
    // 1 = still above the shelf, 0 = landed.
    val drops = remember { List(ShelfChips.size) { Animatable(if (animate) 1f else 0f) } }
    val floatAmount = remember { Animatable(0f) }
    val phase = rememberLoopPhase(animate, durationMillis = 3600)

    LaunchedEffect(active, animate) {
        if (!animate) {
            drops.forEach { it.snapTo(0f) }
            return@LaunchedEffect
        }
        if (!active) {
            floatAmount.snapTo(0f)
            drops.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }
        drops.mapIndexed { index, drop ->
            launch {
                delay(150L + index * 180L)
                drop.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
            }
        }.joinAll()
        floatAmount.animateTo(1f, tween(900))
    }

    Box(modifier.size(IllustrationWidth, IllustrationHeight)) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 16.dp.toPx()
            val panelSize = Size(size.width - inset * 2, size.height - inset * 2)
            val radius = CornerRadius(28.dp.toPx())
            drawRoundRect(colors.card, Offset(inset, inset), panelSize, radius)
            drawRoundRect(colors.cardBorder, Offset(inset, inset), panelSize, radius, style = Stroke(1.5.dp.toPx()))
            listOf(130.dp, 260.dp).forEach { y ->
                drawRoundRect(
                    color = colors.cardBorder,
                    topLeft = Offset(inset + 14.dp.toPx(), y.toPx()),
                    size = Size(panelSize.width - 28.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
        ShelfChips.forEachIndexed { index, chip ->
            val pastel = pastels[index]
            DoodleChip(
                label = stringResource(chip.label),
                doodle = chip.doodle,
                pastel = pastel,
                modifier = Modifier
                    .offset(chip.x, chip.y)
                    .graphicsLayer {
                        val drop = drops[index].value
                        val bob = sin(phase.value + index * 1.4f) * 4.dp.toPx() * floatAmount.value
                        translationY = -drop * (chip.y.toPx() + 140.dp.toPx()) + bob
                        alpha = (1f - drop * 1.5f).coerceIn(0f, 1f)
                        rotationZ = chip.tilt
                    },
            )
        }
    }
}

@Composable
private fun DoodleChip(
    label: String,
    doodle: Doodle,
    pastel: Pastel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(pastel.container)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IngredientDoodle(doodle, pastel.content)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = pastel.content,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Page 2: a search field types itself out, chips pop out, the button pulses once.
// ---------------------------------------------------------------------------

@Composable
fun TypingSearchIllustration(active: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val fullText = stringResource(R.string.onboarding_typing_text)
    var typed by remember { mutableIntStateOf(if (animate) 0 else fullText.length) }
    val chipScales = remember { List(3) { Animatable(if (animate) 0f else 1f) } }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(active, animate, fullText) {
        if (!animate) {
            typed = fullText.length
            chipScales.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }
        if (!active) {
            typed = 0
            chipScales.forEach { it.snapTo(0f) }
            pulse.snapTo(1f)
            return@LaunchedEffect
        }
        typed = 0
        delay(350)
        for (count in 1..fullText.length) {
            typed = count
            delay(70)
        }
        delay(200)
        chipScales.mapIndexed { index, scale ->
            launch {
                delay(index * 130L)
                scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            }
        }.joinAll()
        delay(250)
        pulse.animateTo(1.1f, tween(180, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
    }

    val cursorAlpha by if (animate) {
        rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
            label = "cursorAlpha",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val chips = listOf(
        Triple(R.string.onboarding_chip_egg, Doodle.EGG, colors.pastels.amber),
        Triple(R.string.onboarding_chip_rice, Doodle.RICE, colors.pastels.green),
        Triple(R.string.onboarding_chip_onion, Doodle.ONION, colors.pastels.purple),
    )

    Column(
        modifier = modifier.size(IllustrationWidth, IllustrationHeight),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(60.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.5.dp, if (typed in 1 until fullText.length) colors.accent else colors.cardBorder, CircleShape)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textSecondary)
            Spacer(Modifier.width(10.dp))
            Text(
                text = fullText.take(typed),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
            )
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(2.dp, 22.dp)
                    .graphicsLayer { alpha = cursorAlpha }
                    .background(colors.accent),
            )
        }
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            chips.forEachIndexed { index, (label, doodle, pastel) ->
                DoodleChip(
                    label = stringResource(label),
                    doodle = doodle,
                    pastel = pastel,
                    modifier = Modifier.graphicsLayer {
                        val scale = chipScales[index].value
                        scaleX = scale
                        scaleY = scale
                        alpha = scale.coerceIn(0f, 1f)
                        translationY = (1f - scale) * -70.dp.toPx()
                        rotationZ = (index - 1) * 3f
                    },
                )
            }
        }
        Spacer(Modifier.height(34.dp))
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                }
                .clip(CircleShape)
                .background(colors.accent)
                .padding(horizontal = 30.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.pantry_find_recipes),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onAccent,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Page 3: a recipe card slides up and flips in; lines reveal; a swap fades in.
// ---------------------------------------------------------------------------

private val CardLines = listOf(
    R.string.onboarding_card_line_1 to true,
    R.string.onboarding_card_line_2 to true,
    R.string.onboarding_card_line_3 to false,
    R.string.onboarding_card_line_4 to false,
)

@Composable
fun RecipeCardIllustration(active: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val start = if (animate) 0f else 1f
    val enter = remember { Animatable(start) }
    val lines = remember { List(CardLines.size) { Animatable(start) } }
    val tag = remember { Animatable(start) }
    val swap = remember { Animatable(start) }

    LaunchedEffect(active, animate) {
        val all = listOf(enter, tag, swap) + lines
        if (!animate) {
            all.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }
        if (!active) {
            all.forEach { it.snapTo(0f) }
            return@LaunchedEffect
        }
        delay(120)
        enter.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow))
        lines.mapIndexed { index, line ->
            launch {
                delay(index * 170L)
                line.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            }
        }.joinAll()
        tag.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
        delay(150)
        swap.animateTo(1f, tween(500))
    }

    Box(modifier.size(IllustrationWidth, IllustrationHeight), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .offset(x = (-14).dp, y = (-8).dp)
                .graphicsLayer {
                    val progress = enter.value
                    translationY = (1f - progress) * 260.dp.toPx()
                    rotationX = (1f - progress) * 80f
                    alpha = progress.coerceIn(0f, 1f)
                    cameraDistance = 16f * density
                }
                .width(250.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.card)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_card_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.onboarding_card_meta),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(14.dp))
            CardLines.forEachIndexed { index, (label, have) ->
                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .graphicsLayer {
                            val v = lines[index].value
                            alpha = v
                            translationX = (1f - v) * -24.dp.toPx()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (have) colors.accent else colors.card)
                            .border(1.5.dp, if (have) colors.accent else colors.textMuted, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (have) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (have) colors.textPrimary else colors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val v = tag.value
                        scaleX = v
                        scaleY = v
                        alpha = v.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(colors.pastels.pink.container)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_card_missing),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.pastels.pink.content,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-14).dp)
                .graphicsLayer {
                    alpha = swap.value
                    translationX = (1f - swap.value) * 24.dp.toPx()
                    rotationZ = 3f
                }
                .clip(RoundedCornerShape(16.dp))
                .background(colors.pastels.green.container)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = colors.pastels.green.content,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.onboarding_card_swap),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.pastels.green.content,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Page 4: a phone frame with big step text, a sweeping timer and a glowing sun.
// ---------------------------------------------------------------------------

@Composable
fun CookModeIllustration(active: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val steps = listOf(
        stringResource(R.string.onboarding_cook_step_1),
        stringResource(R.string.onboarding_cook_step_2),
        stringResource(R.string.onboarding_cook_step_3),
    )
    var stepIndex by remember { mutableIntStateOf(if (animate) 0 else 1) }
    val sweep = remember { Animatable(if (animate) 0f else 0.65f) }

    LaunchedEffect(active, animate) {
        if (!animate) {
            stepIndex = 1
            sweep.snapTo(0.65f)
            return@LaunchedEffect
        }
        if (!active) {
            stepIndex = 0
            sweep.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(2600, easing = LinearEasing))
            stepIndex = (stepIndex + 1) % steps.size
        }
    }

    val glow by if (animate) {
        rememberInfiniteTransition(label = "sun").animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glow",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(modifier.size(IllustrationWidth, IllustrationHeight), contentAlignment = Alignment.Center) {
        val phoneShape = RoundedCornerShape(34.dp)
        Column(
            modifier = Modifier
                .size(210.dp, 292.dp)
                .clip(phoneShape)
                .background(colors.card)
                .border(3.dp, colors.cardBorder, phoneShape)
                .padding(18.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(46.dp, 6.dp)
                    .clip(CircleShape)
                    .background(colors.cardBorder),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_cook_step_label, stepIndex + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
            )
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                },
                label = "cookStep",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            ) { index ->
                Text(
                    text = steps[index],
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimerRing(progress = sweep.value, size = 78.dp)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SunDoodle(color = colors.pastels.amber.container, glow = glow, size = 34.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_cook_screen_on),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerRing(progress: Float, size: Dp) {
    val colors = AppTheme.colors
    val totalSeconds = 5 * 60
    val remaining = ((1f - progress.coerceIn(0f, 1f)) * totalSeconds).roundToInt()
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(colors.cardBorder, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "%d:%02d".format(remaining / 60, remaining % 60),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
    }
}

// ---------------------------------------------------------------------------
// Page 5: weekly counter counts up, a no-ads badge stamps in, confetti drifts.
// ---------------------------------------------------------------------------

private class ConfettiDot(
    val x: Float,
    val y: Float,
    val radius: Float,
    val colorIndex: Int,
    val phase: Float,
    val speed: Float,
)

@Composable
fun FreePlanIllustration(active: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val weeklyLimit = 7
    val count = remember { Animatable(if (animate) 0f else weeklyLimit.toFloat()) }
    val stamp = remember { Animatable(if (animate) 0f else 1f) }
    val phase = rememberLoopPhase(animate, durationMillis = 7000)
    val dots = remember {
        val random = Random(7)
        List(18) { index ->
            ConfettiDot(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 3f + random.nextFloat() * 4f,
                colorIndex = index % 5,
                phase = random.nextFloat() * TwoPi,
                speed = 0.6f + random.nextFloat() * 0.8f,
            )
        }
    }

    LaunchedEffect(active, animate) {
        if (!animate) {
            count.snapTo(weeklyLimit.toFloat())
            stamp.snapTo(1f)
            return@LaunchedEffect
        }
        if (!active) {
            count.snapTo(0f)
            stamp.snapTo(0f)
            return@LaunchedEffect
        }
        delay(250)
        count.animateTo(weeklyLimit.toFloat(), tween(1100, easing = FastOutSlowInEasing))
        stamp.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(modifier.size(IllustrationWidth, IllustrationHeight), contentAlignment = Alignment.Center) {
        val pastels = colors.pastels.all
        Canvas(Modifier.fillMaxSize()) {
            dots.forEach { dot ->
                val t = phase.value * dot.speed + dot.phase
                val center = Offset(
                    x = dot.x * size.width + cos(t) * 6.dp.toPx(),
                    y = dot.y * size.height + sin(t) * 10.dp.toPx(),
                )
                drawCircle(pastels[dot.colorIndex].container, radius = dot.radius.dp.toPx(), center = center)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier
                    .width(256.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.onboarding_counter_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_counter_value, count.value.roundToInt(), weeklyLimit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                ) {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(colors.cardBorder, cornerRadius = radius)
                    val fraction = (count.value / weeklyLimit).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        drawRoundRect(
                            color = colors.accent,
                            size = Size((size.width * fraction).coerceAtLeast(size.height), size.height),
                            cornerRadius = radius,
                        )
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .graphicsLayer {
                        val v = stamp.value
                        val scale = 2.4f - 1.4f * v
                        scaleX = scale
                        scaleY = scale
                        alpha = v.coerceIn(0f, 1f)
                        rotationZ = -12f
                    }
                    .clip(CircleShape)
                    .background(colors.pastels.green.container)
                    .border(3.dp, colors.pastels.green.content, CircleShape)
                    .padding(6.dp)
                    .border(1.5.dp, colors.pastels.green.content.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_badge_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.pastels.green.content,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_badge_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.pastels.green.content,
                    )
                }
            }
        }
    }
}
