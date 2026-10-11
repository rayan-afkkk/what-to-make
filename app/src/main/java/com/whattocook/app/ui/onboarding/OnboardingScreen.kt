package com.whattocook.app.ui.onboarding

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whattocook.app.R
import com.whattocook.app.ui.components.PrimaryButton
import com.whattocook.app.ui.components.rememberReducedMotion
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
)

private val Pages = listOf(
    OnboardingPage(R.string.onboarding_1_title, R.string.onboarding_1_subtitle),
    OnboardingPage(R.string.onboarding_2_title, R.string.onboarding_2_subtitle),
    OnboardingPage(R.string.onboarding_3_title, R.string.onboarding_3_subtitle),
    OnboardingPage(R.string.onboarding_4_title, R.string.onboarding_4_subtitle),
    OnboardingPage(R.string.onboarding_5_title, R.string.onboarding_5_subtitle),
)

/** Signed distance of [page] from the centre of the pager, in pages. */
@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.offsetFor(page: Int): Float = (currentPage - page) + currentPageOffsetFraction

/**
 * Five-page onboarding. The current page survives rotation and process death
 * because [rememberPagerState] is saveable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val pagerState = rememberPagerState { Pages.size }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val reducedMotion = rememberReducedMotion()
    val isLastPage = pagerState.currentPage == Pages.lastIndex

    // Soft tick on every page change (not on the initial composition).
    LaunchedEffect(pagerState, view) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }

    fun finish(confirm: Boolean) {
        if (confirm) {
            val strong = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(strong)
        }
        onFinished()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        // Skip, top right, on pages 1-4.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = Spacing.sm),
        ) {
            val skipAlpha by animateFloatAsState(if (isLastPage) 0f else 1f, label = "skipAlpha")
            if (skipAlpha > 0.01f) {
                TextButton(
                    onClick = { finish(confirm = false) },
                    enabled = !isLastPage,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer { alpha = skipAlpha },
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            OnboardingPageContent(
                page = page,
                pagerState = pagerState,
                reducedMotion = reducedMotion,
            )
        }

        PageIndicator(
            count = Pages.size,
            selected = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = Spacing.lg),
        )

        PrimaryButton(
            text = stringResource(if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next),
            onClick = {
                if (isLastPage) {
                    finish(confirm = true)
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.padding(horizontal = Spacing.xl),
        )

        val footerAlpha by animateFloatAsState(if (isLastPage) 1f else 0f, label = "footerAlpha")
        Text(
            text = stringResource(R.string.onboarding_free_footer),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md, bottom = Spacing.lg)
                .graphicsLayer { alpha = footerAlpha },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPageContent(
    page: Int,
    pagerState: PagerState,
    reducedMotion: Boolean,
) {
    val info = Pages[page]
    val active = pagerState.settledPage == page
    val animate = !reducedMotion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (reducedMotion) {
                    // Hold the page still and simply crossfade.
                    val offset = pagerState.offsetFor(page)
                    translationX = offset * size.width
                    alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Illustration layer: moves at ~55% of the swipe speed, fades and shrinks a little.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    if (!reducedMotion) {
                        val offset = pagerState.offsetFor(page)
                        val distance = offset.absoluteValue.coerceIn(0f, 1f)
                        translationX = offset * size.width * 0.45f
                        alpha = 1f - distance * 0.7f
                        val scale = 1f - distance * 0.12f
                        scaleX = scale
                        scaleY = scale
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val scale = min(1f, min(maxWidth / IllustrationWidth, maxHeight / IllustrationHeight))
            Box(
                modifier = Modifier
                    .requiredSize(IllustrationWidth, IllustrationHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    0 -> PantryShelfIllustration(active = active, animate = animate)
                    1 -> TypingSearchIllustration(active = active, animate = animate)
                    2 -> RecipeCardIllustration(active = active, animate = animate)
                    3 -> CookModeIllustration(active = active, animate = animate)
                    else -> FreePlanIllustration(active = active, animate = animate)
                }
            }
        }

        // Text layer: moves at ~85% of the swipe speed, so it drifts against the art.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .graphicsLayer {
                    if (!reducedMotion) {
                        val offset = pagerState.offsetFor(page)
                        translationX = offset * size.width * 0.15f
                        alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(info.title),
                style = MaterialTheme.typography.displaySmall,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(info.subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

/** Dots where the active one stretches into an orange pill with a springy motion. */
@Composable
fun PageIndicator(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.onboarding_page_indicator, selected + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isSelected = index == selected
            val width by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.accent else AppTheme.colors.textMuted.copy(alpha = 0.5f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
