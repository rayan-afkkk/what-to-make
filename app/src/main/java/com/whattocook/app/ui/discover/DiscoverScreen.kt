package com.whattocook.app.ui.discover

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BreakfastDining
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Microwave
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.Cuisine
import com.whattocook.app.domain.Mood
import com.whattocook.app.ui.components.PrimaryButton
import com.whattocook.app.ui.components.SectionLabel
import com.whattocook.app.ui.components.SegmentOption
import com.whattocook.app.ui.components.SegmentedPill
import com.whattocook.app.ui.components.TiltedPastelChip
import com.whattocook.app.ui.components.pressClickable
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.components.rememberReducedMotion
import com.whattocook.app.ui.navigation.Routes
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Pastel
import com.whattocook.app.ui.theme.Spacing
import kotlinx.coroutines.isActive

private data class MoodChip(
    val mood: Mood,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val MoodRowOne = listOf(
    MoodChip(Mood.QUICK, R.string.mood_quick, Icons.Outlined.Bolt),
    MoodChip(Mood.COMFORT, R.string.mood_comfort, Icons.Outlined.Weekend),
    MoodChip(Mood.HIGH_PROTEIN, R.string.mood_high_protein, Icons.Outlined.FitnessCenter),
    MoodChip(Mood.ONE_PAN, R.string.mood_one_pan, Icons.Outlined.LocalDining),
    MoodChip(Mood.BUDGET, R.string.mood_budget, Icons.Outlined.Savings),
    MoodChip(Mood.NO_OVEN, R.string.mood_no_oven, Icons.Outlined.Microwave),
    MoodChip(Mood.VEGETARIAN, R.string.mood_vegetarian, Icons.Outlined.Eco),
)

private val MoodRowTwo = listOf(
    MoodChip(Mood.HALAL, R.string.mood_halal, Icons.Outlined.Verified),
    MoodChip(Mood.UNDER_15, R.string.mood_under_15, Icons.Outlined.Timer),
    MoodChip(Mood.VEGAN, R.string.mood_vegan, Icons.Outlined.Spa),
    MoodChip(Mood.BREAKFAST, R.string.mood_breakfast, Icons.Outlined.BreakfastDining),
    MoodChip(Mood.SPICY, R.string.mood_spicy, Icons.Outlined.LocalFireDepartment),
    MoodChip(Mood.LIGHT, R.string.mood_light, Icons.Outlined.Grass),
    MoodChip(Mood.KID_FRIENDLY, R.string.mood_kid_friendly, Icons.Outlined.ChildCare),
)

private val ChipTilts = listOf(-3f, 2f, -1.5f, 3f, -2.5f, 1.5f)

@Composable
fun DiscoverScreen(
    onStartRequest: (route: String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    val usePantry = state.mode == DiscoverMode.PANTRY

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(vertical = Spacing.xl),
    ) {
        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Text(
                text = stringResource(R.string.discover_title),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.discover_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(Spacing.xl))
            SegmentedPill(
                options = listOf(
                    SegmentOption(DiscoverMode.PANTRY, stringResource(R.string.discover_mode_pantry), Icons.Outlined.Kitchen),
                    SegmentOption(DiscoverMode.ANYTHING, stringResource(R.string.discover_mode_anything), Icons.Outlined.Explore),
                ),
                selected = state.mode,
                onSelect = viewModel::setMode,
            )
            Spacer(Modifier.height(Spacing.sm))
            AnimatedContent(
                targetState = Pair(state.mode, state.pantryCount),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "modeHint",
            ) { (mode, count) ->
                Text(
                    text = when {
                        mode == DiscoverMode.ANYTHING -> stringResource(R.string.discover_hint_anything)
                        count == 0 -> stringResource(R.string.discover_hint_empty_pantry)
                        else -> quantityString(R.plurals.discover_hint_pantry, count, count)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        AutoScrollingChipRow(
            chips = MoodRowOne,
            pastelOffset = 0,
            reverse = false,
            animate = !reducedMotion,
            onClick = { onStartRequest(Routes.generate(mood = it.name, usePantry = usePantry)) },
        )
        AutoScrollingChipRow(
            chips = MoodRowTwo,
            pastelOffset = 2,
            reverse = true,
            animate = !reducedMotion,
            onClick = { onStartRequest(Routes.generate(mood = it.name, usePantry = usePantry)) },
        )

        Spacer(Modifier.height(Spacing.xxl))
        Column(Modifier.padding(horizontal = Spacing.screen)) {
            SectionLabel(stringResource(R.string.discover_cuisine_label))
            Spacer(Modifier.height(Spacing.lg))
            val cuisines = listOf(
                Triple(Cuisine.DESI, R.string.cuisine_desi, Icons.Filled.RiceBowl) to colors.pastels.pink,
                Triple(Cuisine.ITALIAN, R.string.cuisine_italian, Icons.Filled.LocalPizza) to colors.pastels.blue,
                Triple(Cuisine.ASIAN, R.string.cuisine_asian, Icons.Filled.RamenDining) to colors.pastels.purple,
                Triple(Cuisine.MEDITERRANEAN, R.string.cuisine_mediterranean, Icons.Filled.Eco) to colors.pastels.green,
            )
            cuisines.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    row.forEach { (info, pastel) ->
                        val (cuisine, label, icon) = info
                        CuisineCard(
                            label = stringResource(label),
                            icon = icon,
                            pastel = pastel,
                            onClick = { onStartRequest(Routes.generate(cuisine = cuisine.name, usePantry = usePantry)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(
                text = stringResource(R.string.discover_surprise),
                icon = Icons.Filled.Shuffle,
                onClick = { onStartRequest(Routes.generate(surprise = true, usePantry = usePantry)) },
            )
        }
    }
}

/**
 * A row of tilted pastel chips that drifts sideways forever. When animations are
 * disabled it becomes a normal, manually scrollable row.
 */
@Composable
private fun AutoScrollingChipRow(
    chips: List<MoodChip>,
    pastelOffset: Int,
    reverse: Boolean,
    animate: Boolean,
    onClick: (Mood) -> Unit,
) {
    val pastels = AppTheme.colors.pastels.all
    // Start in the middle of a very long list so the row can drift either way.
    val startIndex = if (animate) (Int.MAX_VALUE / 2).let { it - it % chips.size } else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val speedPxPerSecond = with(LocalDensity.current) { 28.dp.toPx() }

    if (animate) {
        LaunchedEffect(listState, reverse) {
            var lastFrame = 0L
            while (isActive) {
                val delta = androidx.compose.runtime.withFrameNanos { now ->
                    val elapsed = if (lastFrame == 0L) 0L else now - lastFrame
                    lastFrame = now
                    elapsed / 1_000_000_000f * speedPxPerSecond
                }
                listState.scrollBy(if (reverse) -delta else delta)
            }
        }
    }

    LazyRow(
        state = listState,
        userScrollEnabled = !animate,
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val count = if (animate) Int.MAX_VALUE else chips.size
        items(count) { index ->
            val chip = chips[index % chips.size]
            TiltedPastelChip(
                label = stringResource(chip.label),
                icon = chip.icon,
                pastel = pastels[(index + pastelOffset) % pastels.size],
                rotation = ChipTilts[index % ChipTilts.size],
                onClick = { onClick(chip.mood) },
            )
        }
    }
}

@Composable
private fun CuisineCard(
    label: String,
    icon: ImageVector,
    pastel: Pastel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val top = lerp(pastel.container, Color.White, 0.25f)
    Box(
        modifier = modifier
            .heightIn(min = 96.dp)
            .pressClickable(onClick = onClick, pressedScale = 0.95f)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, pastel.container)))
            .drawBehind {
                // Two small decorative studs in opposite corners.
                val r = 5.dp.toPx()
                val inset = 14.dp.toPx()
                val stud = pastel.content.copy(alpha = 0.25f)
                drawCircle(stud, r, Offset(inset, inset))
                drawCircle(stud, r, Offset(size.width - inset, size.height - inset))
            }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(pastel.content.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = pastel.content, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = pastel.content,
                maxLines = 1,
            )
        }
    }
}
