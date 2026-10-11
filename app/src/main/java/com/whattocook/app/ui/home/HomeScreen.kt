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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.theme.Pastel
import java.util.Calendar
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RestaurantMenu
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
import com.whattocook.app.ui.components.RecipePhoto
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
    onImport: () -> Unit,
    onOpenDiscover: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.home_title), showDivider = false) {
            IconButton(onClick = onImport) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_add_recipe),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(onClick = { viewModel.setFilter(HomeFilter.SAVED) }) {
                Icon(
                    if (state.filter == HomeFilter.SAVED) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.home_show_saved),
                    tint = if (state.filter == HomeFilter.SAVED) colors.accent else colors.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        val error = state.error
        when {
            state.isLoading -> LoadingState(stringResource(R.string.home_loading))
            error != null -> ErrorState(error = error, onRetry = viewModel::retry)
            else -> HomeGrid(
                state = state,
                onFilter = viewModel::setFilter,
                onOpenRecipe = onOpenRecipe,
                onOpenPantry = onOpenPantry,
                onFindRecipes = onFindRecipes,
                onImport = onImport,
                onOpenDiscover = onOpenDiscover,
            )
        }
    }
}

@Composable
private fun HomeGrid(
    state: HomeUiState,
    onFilter: (HomeFilter) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onOpenPantry: () -> Unit,
    onFindRecipes: () -> Unit,
    onImport: () -> Unit,
    onOpenDiscover: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.xs, bottom = Spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "greeting") {
            Greeting(state)
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "quick") {
            QuickStartTiles(
                onFromPantry = onFindRecipes,
                onFromReel = onImport,
                onBrowse = onOpenDiscover,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "section") {
            Column(Modifier.padding(top = Spacing.md)) {
                Text(
                    stringResource(R.string.home_your_recipes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.md))
                SegmentedPill(
                    options = listOf(
                        SegmentOption(HomeFilter.ALL, stringResource(R.string.home_filter_all)),
                        SegmentOption(HomeFilter.RECIPES, stringResource(R.string.home_filter_recipes), Icons.Outlined.Restaurant),
                        SegmentOption(HomeFilter.SAVED, stringResource(R.string.home_filter_saved), Icons.Outlined.FavoriteBorder),
                    ),
                    selected = state.filter,
                    onSelect = onFilter,
                )
            }
        }

        items(state.recipes, key = { it.id }) { recipe ->
            RecipeGridCard(
                recipe = recipe,
                onClick = { onOpenRecipe(recipe.id) },
                modifier = Modifier.animateItem(),
            )
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

/** "Good evening" plus how many AI recipes are left this week. */
@Composable
private fun Greeting(state: HomeUiState) {
    val colors = AppTheme.colors
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    Column {
        Text(
            greeting,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
        )
        val left = state.recipesLeft
        if (state.isPro || left != null) {
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (state.isPro) colors.accent else colors.accentSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = if (state.isPro) colors.onAccent else colors.accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (state.isPro || left == null) {
                        stringResource(R.string.home_pro_badge)
                    } else {
                        quantityString(R.plurals.usage_left_this_week, left, left, state.weeklyLimit)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isPro) colors.onAccent else colors.accent,
                )
            }
        }
    }
}

/** Three colourful ways to start: from the pantry, from a reel, or from the library. */
@Composable
private fun QuickStartTiles(
    onFromPantry: () -> Unit,
    onFromReel: () -> Unit,
    onBrowse: () -> Unit,
) {
    val pastels = AppTheme.colors.pastels
    Row(
        modifier = Modifier.padding(top = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickTile(
            icon = Icons.Outlined.Kitchen,
            title = stringResource(R.string.home_tile_pantry),
            subtitle = stringResource(R.string.home_tile_pantry_sub),
            pastel = pastels.green,
            onClick = onFromPantry,
            modifier = Modifier.weight(1f),
        )
        QuickTile(
            icon = Icons.Outlined.SmartDisplay,
            title = stringResource(R.string.home_tile_reel),
            subtitle = stringResource(R.string.home_tile_reel_sub),
            pastel = pastels.purple,
            onClick = onFromReel,
            modifier = Modifier.weight(1f),
        )
        QuickTile(
            icon = Icons.Outlined.MenuBook,
            title = stringResource(R.string.home_tile_browse),
            subtitle = stringResource(R.string.home_tile_browse_sub),
            pastel = pastels.amber,
            onClick = onBrowse,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    pastel: Pastel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 118.dp)
            .pressClickable(onClick = onClick, pressedScale = 0.95f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(lerp(pastel.container, Color.White, 0.22f), pastel.container)),
            )
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(pastel.content.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = pastel.content, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = pastel.content,
            maxLines = 2,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = pastel.content.copy(alpha = 0.8f),
            maxLines = 2,
        )
    }
}

@Composable
private fun RecipeGridCard(recipe: RecipeCardUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val tint = colors.recipeTints[recipe.id.hashCode().absoluteValue % colors.recipeTints.size]
    AppCard(
        modifier = modifier.fillMaxWidth(),
        color = tint,
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_open_recipe),
    ) {
        Column(Modifier.padding(16.dp)) {
            val imageUrl = recipe.imageUrl
            if (imageUrl != null) {
                RecipePhoto(
                    url = imageUrl,
                    contentDescription = null,
                    thumbnail = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            } else {
                RecipeGlyph(kind = glyphFor(recipe.title), color = colors.textPrimary, size = 40.dp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = recipe.summary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
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
            .dashedBorder(colors.cardBorder, cornerRadius = 20.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}
