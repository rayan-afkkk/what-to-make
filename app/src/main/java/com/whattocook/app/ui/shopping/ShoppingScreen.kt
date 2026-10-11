package com.whattocook.app.ui.shopping

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.model.ShoppingItem
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.LoadingState
import com.whattocook.app.ui.components.PillTextField
import com.whattocook.app.ui.components.ScreenHeader
import com.whattocook.app.ui.components.SectionLabel
import com.whattocook.app.ui.components.categoryLabel
import com.whattocook.app.ui.components.pressClickable
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing

@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.shopping_title)) {
            if (state.checkedCount > 0) {
                TextButton(onClick = viewModel::clearCompleted) {
                    Text(
                        stringResource(R.string.shopping_clear_completed),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillTextField(
                value = state.input,
                onValueChange = viewModel::onInputChange,
                placeholder = stringResource(R.string.shopping_add_hint),
                leadingIcon = Icons.Outlined.AddShoppingCart,
                onSubmit = viewModel::addFromInput,
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.md))
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .pressClickable(
                        onClick = viewModel::addFromInput,
                        enabled = state.input.isNotBlank(),
                        onClickLabel = stringResource(R.string.shopping_add),
                    )
                    .clip(CircleShape)
                    .background(if (state.input.isNotBlank()) colors.accent else colors.card),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.shopping_add),
                    tint = if (state.input.isNotBlank()) colors.onAccent else colors.textMuted,
                )
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        val error = state.error
        when {
            state.isLoading -> LoadingState(stringResource(R.string.shopping_loading))
            error != null -> ErrorState(error = error, onRetry = viewModel::retry)
            state.totalCount == 0 -> EmptyState(
                icon = Icons.Outlined.ShoppingCart,
                title = stringResource(R.string.shopping_empty_title),
                message = stringResource(R.string.shopping_empty_message),
            )
            else -> ShoppingList(
                state = state,
                onToggle = viewModel::toggle,
                onDelete = viewModel::delete,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingList(
    state: ShoppingUiState,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
    ) {
        state.sections.forEach { section ->
            item(key = "header_${section.category.key}") {
                SectionLabel(
                    categoryLabel(section.category),
                    Modifier
                        .animateItem()
                        .padding(top = Spacing.lg, bottom = Spacing.xs),
                )
            }
            items(section.items, key = { it.id }) { item ->
                ShoppingRow(
                    item = item,
                    onToggle = { onToggle(item) },
                    onDelete = { onDelete(item) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "bottom") { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val circle by animateColorAsState(if (item.checked) colors.accent else colors.card, label = "checkColor")
    val textColor by animateColorAsState(if (item.checked) colors.textMuted else colors.textPrimary, label = "textColor")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp)
                .toggleable(value = item.checked, role = Role.Checkbox, onValueChange = { onToggle() }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(circle)
                    .border(1.5.dp, if (item.checked) colors.accent else colors.textMuted, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (item.checked) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (item.quantity.isNotBlank()) {
                    Text(item.quantity, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.shopping_remove_cd, item.name),
                tint = colors.textMuted,
            )
        }
    }
}
