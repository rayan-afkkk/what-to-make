package com.whattocook.app.ui.recipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.LoadingState
import com.whattocook.app.ui.components.PrimaryButton
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.SectionLabel
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onCookMode: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val heartScale = remember { Animatable(1f) }

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
        Column(Modifier.fillMaxSize()) {
            val recipe = state.recipe
            DetailTopBar(title = "", onBack = onBack) {
                if (recipe != null) {
                    IconButton(onClick = {
                        viewModel.toggleSaved()
                        scope.launch {
                            heartScale.animateTo(1.3f, tween(120))
                            heartScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                        }
                    }) {
                        Icon(
                            if (recipe.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(if (recipe.isSaved) R.string.recipe_unsave else R.string.recipe_save),
                            tint = if (recipe.isSaved) colors.accent else colors.textPrimary,
                            modifier = Modifier.graphicsLayer {
                                scaleX = heartScale.value
                                scaleY = heartScale.value
                            },
                        )
                    }
                }
            }

            val error = state.error
            when {
                state.isLoading -> LoadingState(stringResource(R.string.recipe_loading))
                error != null -> ErrorState(error = error)
                state.notFound || recipe == null -> EmptyState(
                    icon = Icons.Outlined.RestaurantMenu,
                    title = stringResource(R.string.recipe_not_found_title),
                    message = stringResource(R.string.recipe_not_found_message),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
                else -> RecipeContent(
                    state = state,
                    onServings = viewModel::changeServings,
                    onAddMissing = viewModel::addMissingToShopping,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val recipe = state.recipe
        if (recipe != null && recipe.steps.isNotEmpty()) {
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }
}

@Composable
private fun RecipeContent(
    state: RecipeDetailUiState,
    onServings: (Int) -> Unit,
    onAddMissing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = state.recipe ?: return
    val colors = AppTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = 120.dp),
    ) {
        item(key = "header") {
            Column {
                Text(recipe.title, style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                if (recipe.summary.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(recipe.summary, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
                }
                Spacer(Modifier.height(Spacing.lg))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(text = stringResource(R.string.recipe_minutes, recipe.timeMinutes), icon = Icons.Outlined.Schedule)
                    Spacer(Modifier.weight(1f))
                    ServingsStepper(servings = state.servings, onChange = onServings)
                }
            }
        }

        item(key = "ingredients_label") {
            SectionLabel(stringResource(R.string.recipe_ingredients), Modifier.padding(top = Spacing.xl, bottom = Spacing.sm))
        }
        itemsIndexed(state.ingredients, key = { index, _ -> "ing_$index" }) { _, row ->
            IngredientRow(row)
        }

        if (state.missing.isNotEmpty()) {
            item(key = "missing") {
                Column(Modifier.padding(top = Spacing.xl).animateContentSize()) {
                    SectionLabel(stringResource(R.string.recipe_missing), Modifier.padding(bottom = Spacing.sm))
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            state.missing.forEachIndexed { index, missing ->
                                if (index > 0) Spacer(Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        missing.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (missing.amount.isNotBlank()) {
                                        Text(missing.amount, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                    }
                                }
                                if (missing.swap != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.pastels.green.container)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Outlined.SwapHoriz,
                                            contentDescription = null,
                                            tint = colors.pastels.green.content,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.recipe_swap, missing.swap),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.pastels.green.content,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    SecondaryButton(
                        text = stringResource(R.string.recipe_add_missing),
                        icon = Icons.Outlined.AddShoppingCart,
                        onClick = onAddMissing,
                    )
                }
            }
        }

        item(key = "steps_label") {
            SectionLabel(stringResource(R.string.recipe_steps), Modifier.padding(top = Spacing.xl, bottom = Spacing.sm))
        }
        itemsIndexed(recipe.steps, key = { index, _ -> "step_$index" }) { index, step ->
            Row(Modifier.padding(vertical = 10.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.cardRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(step, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IngredientRow(row: IngredientRowUi) {
    val colors = AppTheme.colors
    val circleColor by animateColorAsState(if (row.inPantry) colors.accent else colors.card, label = "haveColor")
    val stateText = stringResource(if (row.inPantry) R.string.recipe_in_pantry else R.string.recipe_not_in_pantry)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .semantics(mergeDescendants = true) { stateDescription = stateText }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(1.5.dp, if (row.inPantry) colors.accent else colors.textMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (row.inPantry) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            row.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (row.inPantry) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (row.amount.isNotBlank()) {
            Text(row.amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.textSecondary)
        }
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
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = { onChange(-1) }, enabled = servings > 1) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.recipe_fewer_servings), tint = colors.textPrimary)
        }
        Text(
            description,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
        IconButton(onClick = { onChange(1) }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recipe_more_servings), tint = colors.textPrimary)
        }
    }
}
