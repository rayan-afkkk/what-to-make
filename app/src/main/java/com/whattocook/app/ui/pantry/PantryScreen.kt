package com.whattocook.app.ui.pantry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.domain.model.CatalogIngredient
import com.whattocook.app.domain.model.RecipePreferences
import com.whattocook.app.ui.components.AccentButton
import com.whattocook.app.ui.components.EmptyState
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.IngredientChip
import com.whattocook.app.ui.components.LoadingState
import com.whattocook.app.ui.components.PillTextField
import com.whattocook.app.ui.components.ScreenHeader
import com.whattocook.app.ui.components.SectionLabel
import com.whattocook.app.ui.components.categoryLabel
import com.whattocook.app.ui.components.pressClickable
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing

@Composable
fun PantryScreen(
    onFindRecipes: () -> Unit,
    viewModel: PantryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateGroup by rememberSaveable { mutableStateOf(false) }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var groupToDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    val messageText = when (val message = state.message) {
        is PantryMessage.Added -> stringResource(R.string.pantry_added, message.name)
        is PantryMessage.AlreadyAdded -> stringResource(R.string.pantry_already_added, message.name)
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
            ScreenHeader(title = stringResource(R.string.pantry_title)) {
                IconButton(onClick = { showCreateGroup = true }) {
                    Icon(
                        Icons.Outlined.CreateNewFolder,
                        contentDescription = stringResource(R.string.pantry_create_group),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            val error = state.error
            when {
                state.isLoading -> LoadingState(stringResource(R.string.pantry_loading))
                error != null -> ErrorState(error = error, onRetry = viewModel::retry)
                else -> PantryContent(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onAdd = viewModel::addIngredient,
                    onRemove = viewModel::removeItem,
                    onSelectTarget = viewModel::setAddTarget,
                    onDeleteGroup = { groupToDelete = it },
                    modifier = Modifier.weight(1f),
                )
            }

            // Find recipes + options, pinned above the bottom navigation.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccentButton(
                    text = stringResource(R.string.pantry_find_recipes),
                    icon = Icons.Outlined.AutoAwesome,
                    onClick = onFindRecipes,
                    enabled = state.itemCount > 0,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.md))
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, CircleShape)
                        .pressClickable(
                            onClick = { showOptions = true },
                            onClickLabel = stringResource(R.string.pantry_options),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = stringResource(R.string.pantry_options),
                        tint = colors.textPrimary,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp),
        )
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onCreate = {
                viewModel.createGroup(it)
                showCreateGroup = false
            },
        )
    }

    groupToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.pantry_delete_group_title)) },
            text = { Text(stringResource(R.string.pantry_delete_group_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(id)
                    groupToDelete = null
                }) { Text(stringResource(R.string.action_delete), color = colors.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text(stringResource(R.string.action_cancel), color = colors.textPrimary)
                }
            },
        )
    }

    if (showOptions) {
        RecipeOptionsSheet(
            preferences = state.preferences,
            onChange = viewModel::updatePreferences,
            onDismiss = { showOptions = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PantryContent(
    state: PantryUiState,
    onQueryChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (com.whattocook.app.domain.model.PantryItem) -> Unit,
    onSelectTarget: (Long?) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        item(key = "search") {
            Column(Modifier.animateContentSize()) {
                PillTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.pantry_search_hint),
                    leadingIcon = Icons.Filled.Search,
                    onSubmit = { onAdd(state.suggestions.firstOrNull()?.name ?: state.query) },
                    imeAction = ImeAction.Done,
                )
                AnimatedVisibility(
                    visible = state.query.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    SuggestionList(
                        query = state.query.trim(),
                        suggestions = state.suggestions,
                        canAddCustom = state.canAddCustom,
                        onAdd = onAdd,
                    )
                }
            }
        }

        if (state.groups.isNotEmpty()) {
            item(key = "targets") {
                Column(Modifier.padding(top = Spacing.lg)) {
                    Text(
                        stringResource(R.string.pantry_adding_to),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        SelectChip(
                            label = stringResource(R.string.pantry_target_all),
                            selected = state.addTargetGroupId == null,
                            onClick = { onSelectTarget(null) },
                        )
                        state.groups.forEach { group ->
                            SelectChip(
                                label = group.name,
                                selected = state.addTargetGroupId == group.id,
                                onClick = { onSelectTarget(group.id) },
                            )
                        }
                    }
                }
            }
        }

        if (state.itemCount == 0 && state.groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.Kitchen,
                    title = stringResource(R.string.pantry_empty_title),
                    message = stringResource(R.string.pantry_empty_message),
                )
            }
        }

        items(state.sections, key = { it.key }) { section ->
            Column(
                Modifier
                    .padding(top = Spacing.xl)
                    .animateContentSize(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val title = when (val t = section.title) {
                        is PantrySectionTitle.Group -> t.name
                        is PantrySectionTitle.Category -> categoryLabel(t.category)
                    }
                    SectionLabel(title, Modifier.weight(1f))
                    val groupId = section.groupId
                    if (groupId != null) {
                        IconButton(onClick = { onDeleteGroup(groupId) }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.pantry_delete_group_cd, title),
                                tint = colors.textMuted,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                if (section.items.isEmpty()) {
                    Text(
                        stringResource(R.string.pantry_group_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        section.items.forEach { item ->
                            IngredientChip(
                                label = item.name,
                                onRemove = { onRemove(item) },
                                removeDescription = stringResource(R.string.pantry_remove_cd, item.name),
                            )
                        }
                    }
                }
            }
        }
        item(key = "bottom_space") { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun SuggestionList(
    query: String,
    suggestions: List<CatalogIngredient>,
    canAddCustom: Boolean,
    onAdd: (String) -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .padding(top = Spacing.sm)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
    ) {
        suggestions.forEach { suggestion ->
            SuggestionRow(
                title = suggestion.name,
                subtitle = categoryLabel(suggestion.category),
                onClick = { onAdd(suggestion.name) },
            )
        }
        if (canAddCustom) {
            SuggestionRow(
                title = stringResource(R.string.pantry_add_custom, query),
                subtitle = null,
                onClick = { onAdd(query) },
            )
        }
        if (suggestions.isEmpty() && !canAddCustom) {
            Text(
                stringResource(R.string.pantry_no_matches),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(18.dp),
            )
        }
    }
}

@Composable
private fun SuggestionRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pressClickable(onClick = onClick, pressedScale = 0.98f)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
    }
}

@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressClickable(onClick = onClick, role = Role.RadioButton, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.card)
            .border(1.dp, if (selected) colors.accent else colors.cardBorder, CircleShape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.onAccent else colors.textPrimary,
        )
    }
}

@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val colors = AppTheme.colors
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.pantry_create_group)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.pantry_group_hint)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.cardBorder,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_create), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textPrimary) }
        },
    )
}

private val DietOptions = listOf(
    null to R.string.option_any,
    "vegetarian" to R.string.option_vegetarian,
    "vegan" to R.string.option_vegan,
    "halal" to R.string.option_halal,
    "high-protein" to R.string.option_high_protein,
    "gluten-free" to R.string.option_gluten_free,
)
private val TimeOptions = listOf(null, 15, 30, 45, 60)
private val EquipmentOptions = listOf(
    "oven" to R.string.equipment_oven,
    "stovetop" to R.string.equipment_stovetop,
    "microwave" to R.string.equipment_microwave,
    "air fryer" to R.string.equipment_air_fryer,
    "blender" to R.string.equipment_blender,
    "pressure cooker" to R.string.equipment_pressure_cooker,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecipeOptionsSheet(
    preferences: RecipePreferences,
    onChange: (RecipePreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding()
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                stringResource(R.string.options_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                stringResource(R.string.options_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel(stringResource(R.string.options_diet))
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DietOptions.forEach { (value, label) ->
                    SelectChip(
                        label = stringResource(label),
                        selected = preferences.diet == value,
                        onClick = { onChange(preferences.copy(diet = value)) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel(stringResource(R.string.options_time))
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TimeOptions.forEach { minutes ->
                    SelectChip(
                        label = if (minutes == null) stringResource(R.string.option_any) else stringResource(R.string.option_minutes, minutes),
                        selected = preferences.maxTimeMinutes == minutes,
                        onClick = { onChange(preferences.copy(maxTimeMinutes = minutes)) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel(stringResource(R.string.options_missing))
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.options_missing_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onChange(preferences.copy(allowMissing = (preferences.allowMissing - 1).coerceAtLeast(0))) },
                    enabled = preferences.allowMissing > 0,
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.action_decrease), tint = colors.textPrimary)
                }
                Text(
                    preferences.allowMissing.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                IconButton(
                    onClick = { onChange(preferences.copy(allowMissing = (preferences.allowMissing + 1).coerceAtMost(5))) },
                    enabled = preferences.allowMissing < 5,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_increase), tint = colors.textPrimary)
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel(stringResource(R.string.options_equipment))
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                EquipmentOptions.forEach { (value, label) ->
                    val selected = value in preferences.equipment
                    SelectChip(
                        label = stringResource(label),
                        selected = selected,
                        onClick = {
                            val updated = if (selected) preferences.equipment - value else preferences.equipment + value
                            onChange(preferences.copy(equipment = updated))
                        },
                    )
                }
            }
        }
    }
}
