package com.whattocook.app.ui.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.RecipeReading
import com.whattocook.app.domain.StepTimerParser
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeOrigin
import com.whattocook.app.ui.components.AnimatedProgressBar
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.LoadingState
import com.whattocook.app.ui.components.PrimaryButton
import com.whattocook.app.ui.components.RecipeGlyph
import com.whattocook.app.ui.components.RecipePhoto
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.SegmentOption
import com.whattocook.app.ui.components.SegmentedPill
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.glyphFor
import com.whattocook.app.ui.components.pressClickable
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private enum class RecipeTab { INGREDIENTS, STEPS }

@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onCookMode: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = when (val message = state.message) {
        is DetailMessage.AddedToShopping -> if (message.count == 0) {
            stringResource(R.string.recipe_already_on_list)
        } else {
            quantityString(R.plurals.recipe_added_to_list, message.count, message.count)
        }
        null -> null
    }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        val recipe = state.recipe
        val error = state.error
        when {
            state.isLoading -> Column(Modifier.fillMaxSize()) {
                DetailTopBar(title = "", onBack = onBack)
                LoadingState(stringResource(R.string.recipe_loading))
            }
            error != null -> Column(Modifier.fillMaxSize()) {
                DetailTopBar(title = "", onBack = onBack)
                ErrorState(error = error, onRetry = viewModel::retry)
            }
            state.notFound || recipe == null -> Column(Modifier.fillMaxSize()) {
                DetailTopBar(title = "", onBack = onBack)
                EmptyState(
                    icon = Icons.Outlined.RestaurantMenu,
                    title = stringResource(R.string.recipe_not_found_title),
                    message = stringResource(R.string.recipe_not_found_message),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
            }
            else -> {
                RecipeContent(
                    recipe = recipe,
                    state = state,
                    onServings = viewModel::changeServings,
                    onAddMissing = viewModel::addMissingToShopping,
                    onToggleStep = viewModel::toggleStep,
                )
                FloatingTopButtons(
                    isSaved = recipe.isSaved,
                    onBack = onBack,
                    onToggleSaved = viewModel::toggleSaved,
                )
                if (recipe.steps.isNotEmpty()) {
                    PrimaryButton(
                        text = stringResource(R.string.recipe_cook_mode),
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onCookMode(recipe.id) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }
}

/** Back and save buttons floating over the photo header. */
@Composable
private fun FloatingTopButtons(
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val heartScale = remember { Animatable(1f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = Color.White,
            onClick = onBack,
        )
        CircleIconButton(
            icon = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(if (isSaved) R.string.recipe_unsave else R.string.recipe_save),
            tint = if (isSaved) colors.accent else Color.White,
            onClick = {
                onToggleSaved()
                scope.launch {
                    heartScale.animateTo(1.3f, tween(120))
                    heartScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                }
            },
            iconModifier = Modifier.graphicsLayer {
                scaleX = heartScale.value
                scaleY = heartScale.value
            },
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f)),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = iconModifier)
    }
}

@Composable
private fun RecipeContent(
    recipe: Recipe,
    state: RecipeDetailUiState,
    onServings: (Int) -> Unit,
    onAddMissing: () -> Unit,
    onToggleStep: (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(RecipeTab.INGREDIENTS) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        item(key = "hero") { Hero(recipe) }
        item(key = "overview") {
            Overview(recipe = recipe, state = state)
        }
        item(key = "tabs") {
            SegmentedPill(
                options = listOf(
                    SegmentOption(
                        RecipeTab.INGREDIENTS,
                        stringResource(R.string.recipe_tab_ingredients, recipe.ingredients.size),
                    ),
                    SegmentOption(RecipeTab.STEPS, stringResource(R.string.recipe_tab_steps, recipe.steps.size)),
                ),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.xl),
            )
        }
        item(key = "tab_content") {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "recipeTab",
            ) { current ->
                when (current) {
                    RecipeTab.INGREDIENTS -> IngredientsTab(state, onServings, onAddMissing)
                    RecipeTab.STEPS -> StepsTab(recipe.steps, state.doneSteps, onToggleStep)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun Hero(recipe: Recipe) {
    val colors = AppTheme.colors
    val imageUrl = recipe.imageUrl
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (imageUrl != null) 320.dp else 240.dp),
    ) {
        if (imageUrl != null) {
            RecipePhoto(
                url = imageUrl,
                contentDescription = recipe.title,
                modifier = Modifier
                    .matchParentSize(),
            )
        } else {
            val tint = colors.recipeTints[recipe.id.hashCode().absoluteValue % colors.recipeTints.size]
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(tint, colors.background))),
                contentAlignment = Alignment.Center,
            ) {
                RecipeGlyph(
                    kind = glyphFor(recipe.title),
                    color = colors.textPrimary.copy(alpha = 0.85f),
                    size = 96.dp,
                    modifier = Modifier.padding(bottom = 48.dp),
                )
            }
        }
        // Darken the bottom so the title is always readable.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.3f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to colors.background,
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            if (recipe.tags.isNotEmpty()) {
                Text(
                    recipe.tags.joinToString("  ·  ").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(
                recipe.title,
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Overview(recipe: Recipe, state: RecipeDetailUiState) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(horizontal = Spacing.screen)) {
        if (recipe.summary.isNotBlank()) {
            Text(recipe.summary, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            Spacer(Modifier.height(Spacing.lg))
        }

        // Quick facts: time, servings, difficulty, steps.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FactTile(
                icon = Icons.Outlined.Schedule,
                value = if (recipe.timeMinutes > 0) stringResource(R.string.recipe_minutes, recipe.timeMinutes) else "—",
                label = stringResource(R.string.recipe_fact_time),
                modifier = Modifier.weight(1f),
            )
            FactTile(
                icon = Icons.Outlined.People,
                value = state.servings.toString(),
                label = stringResource(R.string.recipe_fact_servings),
                modifier = Modifier.weight(1f),
            )
            FactTile(
                icon = Icons.Outlined.SignalCellularAlt,
                value = stringResource(
                    when (state.difficulty) {
                        RecipeReading.Difficulty.EASY -> R.string.difficulty_easy
                        RecipeReading.Difficulty.MEDIUM -> R.string.difficulty_medium
                        RecipeReading.Difficulty.HARD -> R.string.difficulty_hard
                    },
                ),
                label = stringResource(R.string.recipe_fact_level),
                modifier = Modifier.weight(1f),
            )
            FactTile(
                icon = Icons.Outlined.FormatListNumbered,
                value = recipe.steps.size.toString(),
                label = stringResource(R.string.recipe_fact_steps),
                modifier = Modifier.weight(1f),
            )
        }

        // "You have 6 of 9 ingredients"
        val total = state.ingredients.size
        if (total > 0) {
            Spacer(Modifier.height(Spacing.lg))
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.recipe_have_progress, state.haveCount, total),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.haveCount == total) {
                            Tag(
                                text = stringResource(R.string.recipe_ready_to_cook),
                                container = colors.pastels.green.container,
                                content = colors.pastels.green.content,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    AnimatedProgressBar(
                        progress = state.haveCount.toFloat() / total,
                        color = colors.pastels.green.content,
                        height = 8.dp,
                    )
                }
            }
        }

        if (state.mayNotBeHalal || recipe.sourceUrl != null) {
            Spacer(Modifier.height(Spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (state.mayNotBeHalal) {
                    Tag(
                        text = stringResource(R.string.recipe_not_halal_warning),
                        icon = Icons.Outlined.Warning,
                        container = colors.pastels.amber.container,
                        content = colors.pastels.amber.content,
                    )
                }
            }
            val sourceUrl = recipe.sourceUrl
            if (sourceUrl != null) {
                TextButton(onClick = { uriHandler.openSafely(sourceUrl) }) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(
                            if (recipe.origin == RecipeOrigin.LINK) R.string.recipe_open_reel else R.string.recipe_open_source,
                        ),
                        color = colors.accent,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun FactTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Ingredients tab
// ---------------------------------------------------------------------------

@Composable
private fun IngredientsTab(
    state: RecipeDetailUiState,
    onServings: (Int) -> Unit,
    onAddMissing: () -> Unit,
) {
    val colors = AppTheme.colors
    val have = state.ingredients.filter { it.inPantry }
    val need = state.ingredients.filterNot { it.inPantry }
    val swaps = state.missing.associate { it.name.lowercase() to it.swap }

    Column(Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.recipe_servings_for),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            ServingsStepper(servings = state.servings, onChange = onServings)
        }

        if (need.isNotEmpty()) {
            SectionTitle(stringResource(R.string.recipe_you_need, need.size), dot = colors.accent)
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    need.forEachIndexed { index, row ->
                        IngredientLine(row = row, swap = swaps[row.name.lowercase()], showDivider = index > 0)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = quantityString(R.plurals.recipe_add_to_list, need.size, need.size),
                icon = Icons.Outlined.AddShoppingCart,
                onClick = onAddMissing,
            )
        }

        if (have.isNotEmpty()) {
            SectionTitle(stringResource(R.string.recipe_you_have, have.size), dot = colors.pastels.green.content)
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    have.forEachIndexed { index, row ->
                        IngredientLine(row = row, swap = null, showDivider = index > 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, dot: Color) {
    Row(
        Modifier.padding(top = Spacing.xl, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Amount first in bold, then the ingredient: easy to scan while shopping or cooking. */
@Composable
private fun IngredientLine(row: IngredientRowUi, swap: String?, showDivider: Boolean) {
    val colors = AppTheme.colors
    val stateText = stringResource(if (row.inPantry) R.string.recipe_in_pantry else R.string.recipe_not_in_pantry)
    Column {
        if (showDivider) {
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { stateDescription = stateText }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (row.inPantry) colors.pastels.green.container else colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (row.inPantry) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = null,
                    tint = if (row.inPantry) colors.pastels.green.content else colors.accent,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = row.amount.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.width(86.dp),
                maxLines = 2,
            )
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        if (swap != null) {
            Row(
                modifier = Modifier
                    .padding(start = 50.dp, end = 16.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.pastels.green.container)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = colors.pastels.green.content, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.recipe_swap, swap),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.pastels.green.content,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Steps tab
// ---------------------------------------------------------------------------

@Composable
private fun StepsTab(
    steps: List<String>,
    doneSteps: Set<Int>,
    onToggleStep: (Int) -> Unit,
) {
    val colors = AppTheme.colors
    Column(Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.recipe_steps_done, doneSteps.count { it < steps.size }, steps.size),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        AnimatedProgressBar(
            progress = if (steps.isEmpty()) 0f else doneSteps.count { it < steps.size }.toFloat() / steps.size,
            height = 6.dp,
            durationMillis = 400,
        )
        Text(
            stringResource(R.string.recipe_step_tip),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
        )
        steps.forEachIndexed { index, step ->
            StepCard(
                number = index + 1,
                text = step,
                done = index in doneSteps,
                onToggle = { onToggleStep(index) },
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun StepCard(number: Int, text: String, done: Boolean, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val border by animateColorAsState(if (done) colors.pastels.green.content else colors.cardBorder, label = "stepBorder")
    val stateText = stringResource(if (done) R.string.recipe_step_done else R.string.recipe_step_not_done)
    val timers = remember(text) { StepTimerParser.parse(text) }
    val highlighted = rememberHighlighted(text, done)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .pressClickable(onClick = onToggle, role = Role.Checkbox, pressedScale = 0.98f)
            .semantics(mergeDescendants = true) { stateDescription = stateText }
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (done) colors.pastels.green.container else colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.pastels.green.content, modifier = Modifier.size(18.dp))
            } else {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onAccent)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) colors.textMuted else colors.textPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (timers.isNotEmpty() && !done) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    timers.forEach { timer ->
                        Tag(
                            text = timer.label,
                            icon = Icons.Outlined.Timer,
                            container = colors.accentSoft,
                            content = colors.accent,
                        )
                    }
                }
            }
        }
    }
}

/** Times and temperatures in bold accent so the important numbers jump out. */
@Composable
private fun rememberHighlighted(text: String, done: Boolean): AnnotatedString {
    val accent = AppTheme.colors.accent
    return remember(text, done, accent) {
        if (done) return@remember AnnotatedString(text)
        buildAnnotatedString {
            var cursor = 0
            RecipeReading.highlights(text).forEach { range ->
                append(text.substring(cursor, range.first))
                pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold))
                append(text.substring(range))
                pop()
                cursor = range.last + 1
            }
            append(text.substring(cursor))
        }
    }
}

private fun UriHandler.openSafely(url: String) {
    try {
        openUri(url)
    } catch (e: IllegalArgumentException) {
        // No app can open this link.
    } catch (e: android.content.ActivityNotFoundException) {
        // No browser installed.
    }
}

@Composable
private fun ServingsStepper(servings: Int, onChange: (Int) -> Unit) {
    val colors = AppTheme.colors
    val description = quantityString(R.plurals.recipe_servings, servings, servings)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, CircleShape)
            .semantics { stateDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = { onChange(-1) }, enabled = servings > 1) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.recipe_fewer_servings), tint = colors.textPrimary)
        }
        Text(description, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        IconButton(onClick = { onChange(1) }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recipe_more_servings), tint = colors.textPrimary)
        }
    }
}
