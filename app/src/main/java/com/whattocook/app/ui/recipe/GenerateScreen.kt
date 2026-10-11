package com.whattocook.app.ui.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.PulsingDots
import com.whattocook.app.ui.components.RecipeGlyph
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.glyphFor
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.components.rememberReducedMotion
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun GenerateScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onOpenPantry: () -> Unit,
    onLimitReached: (resetsAtMillis: Long) -> Unit,
    viewModel: GenerateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val current = state
        if (current is GenerateUiState.LimitReached) onLimitReached(current.resetsAtMillis)
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = stringResource(R.string.generate_title), onBack = onBack)
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "generateState",
            modifier = Modifier.weight(1f),
        ) { current ->
            when (current) {
                GenerateUiState.Loading -> CookingLoader()
                GenerateUiState.PantryEmpty -> EmptyState(
                    icon = Icons.Outlined.Kitchen,
                    title = stringResource(R.string.generate_pantry_empty_title),
                    message = stringResource(R.string.generate_pantry_empty_message),
                    actionLabel = stringResource(R.string.generate_go_to_pantry),
                    onAction = onOpenPantry,
                )
                is GenerateUiState.Error -> ErrorState(error = current.error, onRetry = viewModel::retry)
                is GenerateUiState.LimitReached -> CookingLoader()
                is GenerateUiState.Success -> if (current.recipes.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.RestaurantMenu,
                        title = stringResource(R.string.generate_none_title),
                        message = stringResource(R.string.generate_none_message),
                        actionLabel = stringResource(R.string.action_try_again),
                        onAction = viewModel::retry,
                    )
                } else {
                    ResultsList(
                        recipes = current.recipes,
                        onOpenRecipe = onOpenRecipe,
                        onToggleSaved = viewModel::setSaved,
                    )
                }
            }
        }
    }
}

/** Friendly rotating status lines while the backend thinks. */
@Composable
private fun CookingLoader() {
    val messages = listOf(
        stringResource(R.string.generate_loading_1),
        stringResource(R.string.generate_loading_2),
        stringResource(R.string.generate_loading_3),
    )
    var index by remember { mutableIntStateOf(0) }
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_800)
            index = (index + 1) % messages.size
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RecipeGlyph(kind = glyphFor("pot"), color = AppTheme.colors.textPrimary, size = 72.dp)
        Spacer(Modifier.height(24.dp))
        PulsingDots()
        Spacer(Modifier.height(20.dp))
        if (reducedMotion) {
            Text(messages[index], style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary, textAlign = TextAlign.Center)
        } else {
            Crossfade(targetState = index, label = "loaderText") { i ->
                Text(
                    messages[i],
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ResultsList(
    recipes: List<Recipe>,
    onOpenRecipe: (String) -> Unit,
    onToggleSaved: (String, Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                quantityString(R.plurals.generate_results_count, recipes.size, recipes.size),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
        items(recipes, key = { it.id }) { recipe ->
            AppCard(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth(),
                onClick = { onOpenRecipe(recipe.id) },
                onClickLabel = stringResource(R.string.home_open_recipe),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        RecipeGlyph(kind = glyphFor(recipe.title), color = colors.textPrimary, size = 38.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(recipe.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(recipe.summary, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                        IconButton(onClick = { onToggleSaved(recipe.id, !recipe.isSaved) }) {
                            Icon(
                                if (recipe.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(if (recipe.isSaved) R.string.recipe_unsave else R.string.recipe_save),
                                tint = if (recipe.isSaved) colors.accent else colors.textSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Tag(
                            text = stringResource(R.string.recipe_minutes, recipe.timeMinutes),
                            icon = Icons.Outlined.Schedule,
                        )
                        if (recipe.missing.isEmpty()) {
                            Tag(
                                text = stringResource(R.string.recipe_have_everything),
                                icon = Icons.Outlined.CheckCircle,
                                container = colors.pastels.green.container,
                                content = colors.pastels.green.content,
                            )
                        } else {
                            Tag(
                                text = quantityString(R.plurals.recipe_missing_count, recipe.missing.size, recipe.missing.size),
                                container = colors.pastels.pink.container,
                                content = colors.pastels.pink.content,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.navigationBarsPadding().size(8.dp)) }
    }
}
