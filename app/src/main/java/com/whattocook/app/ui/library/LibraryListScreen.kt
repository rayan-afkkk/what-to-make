package com.whattocook.app.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.LibraryAccess
import com.whattocook.app.ui.components.DishCard
import com.whattocook.app.ui.components.DishCardPlaceholder
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.recipe.DetailTopBar
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing

/** "See all" / search results: a photo grid of library dishes. */
@Composable
fun LibraryListScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onLockedDish: () -> Unit,
    viewModel: LibraryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = viewModel.title, onBack = onBack)
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "libraryList",
            modifier = Modifier.weight(1f),
        ) { current ->
            when (current) {
                LibraryListUiState.Loading -> DishGrid {
                    items(8) { DishCardPlaceholder() }
                }
                is LibraryListUiState.Failed -> ErrorState(error = current.error, onRetry = viewModel::load)
                is LibraryListUiState.Loaded -> if (current.dishes.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.RestaurantMenu,
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(R.string.library_empty_message),
                    )
                } else {
                    DishGrid {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val locked = LibraryAccess.lockedCount(current.dishes.size, isPro)
                            Text(
                                if (locked > 0) {
                                    quantityString(R.plurals.library_count_locked, locked, LibraryAccess.FREE_PER_CATEGORY, locked)
                                } else {
                                    quantityString(R.plurals.library_count, current.dishes.size, current.dishes.size)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary,
                            )
                        }
                        itemsIndexed(current.dishes, key = { _, dish -> dish.recipeId }) { index, dish ->
                            val locked = LibraryAccess.isLocked(index, isPro)
                            DishCard(
                                dish = dish,
                                locked = locked,
                                onClick = { if (locked) onLockedDish() else onOpenRecipe(dish.recipeId) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DishGrid(content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.sm, bottom = Spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        content = content,
    )
}
