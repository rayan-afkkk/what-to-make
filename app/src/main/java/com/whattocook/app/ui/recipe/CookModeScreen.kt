package com.whattocook.app.ui.recipe

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.StepTimer
import com.whattocook.app.ui.components.AnimatedProgressBar
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.LoadingState
import com.whattocook.app.ui.components.PrimaryButton
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.pressClickable
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Sans
import com.whattocook.app.ui.theme.Spacing
import androidx.compose.material.icons.outlined.RestaurantMenu
import kotlinx.coroutines.launch

@Composable
fun CookModeScreen(
    onClose: () -> Unit,
    viewModel: CookModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep the screen awake while cooking.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .systemBarsPadding(),
    ) {
        when {
            state.isLoading -> LoadingState(stringResource(R.string.recipe_loading))
            state.notFound || state.steps.isEmpty() -> EmptyState(
                icon = Icons.Outlined.RestaurantMenu,
                title = stringResource(R.string.recipe_not_found_title),
                message = stringResource(R.string.recipe_not_found_message),
                actionLabel = stringResource(R.string.action_close),
                onAction = onClose,
            )
            else -> CookPager(
                state = state,
                onClose = onClose,
                onStepChanged = viewModel::onStepChanged,
                onToggleTimer = viewModel::toggleTimer,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CookPager(
    state: CookUiState,
    onClose: () -> Unit,
    onStepChanged: (Int) -> Unit,
    onToggleTimer: (step: Int, index: Int, seconds: Int) -> Unit,
) {
    val colors = AppTheme.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = state.initialStep.coerceIn(0, state.steps.lastIndex)) {
        state.steps.size
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onStepChanged(page)
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    // Buzz when a running timer reaches zero.
    val finishedCount = state.runningTimers.keys.count { state.remainingSeconds(it) == 0 }
    LaunchedEffect(finishedCount) {
        if (finishedCount > 0) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cook_exit), tint = colors.textPrimary)
            }
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.cook_step_of, pagerState.currentPage + 1, state.steps.size),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
                modifier = Modifier.padding(end = Spacing.md),
            )
        }
        AnimatedProgressBar(
            progress = (pagerState.currentPage + 1f) / state.steps.size,
            modifier = Modifier.padding(horizontal = Spacing.screen),
            height = 6.dp,
            durationMillis = 400,
        )

        // Swipe left/right to move between steps.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
            ) {
                Text(
                    text = state.steps[page],
                    fontFamily = Sans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.sp,
                    lineHeight = 35.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                val timers = state.timers.getOrNull(page).orEmpty()
                if (timers.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xl))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        timers.forEachIndexed { index, timer ->
                            TimerChip(
                                timer = timer,
                                remaining = state.remainingSeconds(timerKey(page, index)),
                                onClick = { onToggleTimer(page, index, timer.seconds) },
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SecondaryButton(
                text = stringResource(R.string.cook_previous),
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.weight(1f),
            )
            val isLast = pagerState.currentPage == state.steps.lastIndex
            PrimaryButton(
                text = stringResource(if (isLast) R.string.cook_finish else R.string.cook_next),
                onClick = {
                    if (isLast) onClose() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TimerChip(timer: StepTimer, remaining: Int?, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val running = remaining != null && remaining > 0
    val done = remaining == 0
    val container by animateColorAsState(
        targetValue = when {
            done -> colors.pastels.green.container
            running -> colors.accent
            else -> colors.card
        },
        label = "timerColor",
    )
    val content = when {
        done -> colors.pastels.green.content
        running -> colors.onAccent
        else -> colors.textPrimary
    }
    val label = when {
        done -> stringResource(R.string.cook_timer_done)
        remaining != null -> "%d:%02d".format(remaining / 60, remaining % 60)
        else -> timer.label
    }
    val actionLabel = stringResource(if (remaining == null) R.string.cook_timer_start else R.string.cook_timer_stop)
    Row(
        modifier = Modifier
            .heightIn(min = 56.dp)
            .pressClickable(onClick = onClick, onClickLabel = actionLabel, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, if (remaining == null) colors.cardBorder else container, CircleShape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Timer, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
    }
}
