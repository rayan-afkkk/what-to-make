package com.whattocook.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.LoadingState
import com.whattocook.app.ui.components.RecipeGlyph
import com.whattocook.app.ui.components.ScreenHeader
import com.whattocook.app.ui.components.SegmentOption
import com.whattocook.app.ui.components.SegmentedPill
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.dashedBorder
import com.whattocook.app.ui.components.glyphFor
import com.whattocook.app.ui.components.pressClickable
import com.whattocook.app.ui.components.relativeDate
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    onOpenRecipe: (String) -> Unit,
    onOpenPantry: () -> Unit,
    onFindRecipes: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.home_title), showDivider = false) {
            IconButton(onClick = onFindRecipes) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_new_recipe),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
            IconButton(onClick = { viewModel.setFilter(HomeFilter.SAVED) }) {
                Icon(
                    if (state.filter == HomeFilter.SAVED) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.home_show_saved),
                    tint = if (state.filter == HomeFilter.SAVED) colors.accent else colors.textPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        SegmentedPill(
            options = listOf(
                SegmentOption(HomeFilter.ALL, stringResource(R.string.home_filter_all)),
                SegmentOption(HomeFilter.RECIPES, stringResource(R.string.home_filter_recipes), Icons.Outlined.Restaurant),
                SegmentOption(HomeFilter.SAVED, stringResource(R.string.home_filter_saved), Icons.Outlined.FavoriteBorder),
            ),
            selected = state.filter,
            onSelect = viewModel::setFilter,
            modifier = Modifier.padding(horizontal = Spacing.screen),
        )
        Spacer(Modifier.height(Spacing.md))
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        val error = state.error
        when {
            state.isLoading -> LoadingState(stringResource(R.string.home_loading))
            error != null -> ErrorState(error = error, onRetry = viewModel::retry)
            else -> HomeGrid(
                state = state,
                onOpenRecipe = onOpenRecipe,
                onOpenPantry = onOpenPantry,
                onFindRecipes = onFindRecipes,
            )
        }
    }
}

@Composable
private fun HomeGrid(
    state: HomeUiState,
    onOpenRecipe: (String) -> Unit,
    onOpenPantry: () -> Unit,
    onFindRecipes: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(state.recipes, key = { it.id }) { recipe ->
            RecipeGridCard(recipe = recipe, onClick = { onOpenRecipe(recipe.id) })
        }

        if (state.recipes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                if (state.filter == HomeFilter.SAVED) {
                    EmptyState(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = stringResource(R.string.home_empty_saved_title),
                        message = stringResource(R.string.home_empty_saved_message),
                    )
                } else {
                    EmptyState(
                        icon = Icons.Outlined.RestaurantMenu,
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                    )
                }
            }
        }

        if (state.showAddIngredientsCard) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "start_pantry") {
                GetStartedCard(
                    title = stringResource(R.string.home_start_pantry_title),
                    subtitle = stringResource(R.string.home_start_pantry_subtitle),
                    onClick = onOpenPantry,
                )
            }
        }
        if (state.showFirstRecipeCard) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "start_recipe") {
                GetStartedCard(
                    title = stringResource(R.string.home_start_recipe_title),
                    subtitle = stringResource(R.string.home_start_recipe_subtitle),
                    onClick = onFindRecipes,
                )
            }
        }
        val latest = state.latestRecipeId
        if (state.showSaveCard && latest != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "start_save") {
                GetStartedCard(
                    title = stringResource(R.string.home_start_save_title),
                    subtitle = stringResource(R.string.home_start_save_subtitle),
                    onClick = { onOpenRecipe(latest) },
                )
            }
        }
    }
}

@Composable
private fun RecipeGridCard(recipe: RecipeCardUi, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val tint = colors.recipeTints[recipe.id.hashCode().absoluteValue % colors.recipeTints.size]
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = tint,
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_open_recipe),
    ) {
        Column(Modifier.padding(18.dp)) {
            RecipeGlyph(kind = glyphFor(recipe.title), color = colors.textPrimary, size = 48.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = recipe.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(
                    text = stringResource(R.string.home_ingredient_progress, recipe.haveCount, recipe.totalCount),
                    icon = Icons.Outlined.Kitchen,
                    container = colors.textPrimary.copy(alpha = 0.10f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = relativeDate(recipe.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GetStartedCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickable(onClick = onClick, pressedScale = 0.97f)
            .dashedBorder(colors.cardBorder)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}
