package com.whattocook.app.ui.importer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.R
import com.whattocook.app.ui.components.AccentButton
import com.whattocook.app.ui.components.ErrorState
import com.whattocook.app.ui.components.PillTextField
import com.whattocook.app.ui.components.PulsingDots
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.rememberReducedMotion
import com.whattocook.app.ui.recipe.DetailTopBar
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Paste or share a reel / video / recipe link and get a recipe back. */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onRecipeReady: (recipeId: String) -> Unit,
    onFindRecipes: () -> Unit,
    onLimitReached: (resetsAtMillis: Long) -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.status) {
        when (val status = state.status) {
            is ImportStatus.Done -> {
                onRecipeReady(status.recipeId)
                viewModel.onNavigated()
            }
            is ImportStatus.LimitReached -> {
                onLimitReached(status.resetsAtMillis)
                viewModel.onNavigated()
            }
            else -> Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = stringResource(R.string.import_title), onBack = onBack)
        AnimatedContent(
            targetState = state.status is ImportStatus.Working,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "importWorking",
            modifier = Modifier.weight(1f),
        ) { working ->
            if (working) {
                ImportProgress()
            } else {
                ImportForm(
                    state = state,
                    onInputChange = viewModel::onInputChange,
                    onStart = viewModel::start,
                    onFindRecipes = onFindRecipes,
                )
            }
        }
    }
}

@Composable
private fun ImportForm(
    state: ImportUiState,
    onInputChange: (String) -> Unit,
    onStart: () -> Unit,
    onFindRecipes: () -> Unit,
) {
    val colors = AppTheme.colors
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .navigationBarsPadding()
            .padding(bottom = Spacing.xl),
    ) {
        Spacer(Modifier.height(Spacing.md))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.pastels.purple.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SmartDisplay, contentDescription = null, tint = colors.pastels.purple.content, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            stringResource(R.string.import_headline),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.import_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.xl))

        PillTextField(
            value = state.input,
            onValueChange = onInputChange,
            placeholder = stringResource(R.string.import_hint),
            leadingIcon = Icons.Outlined.Link,
            onSubmit = onStart,
            imeAction = ImeAction.Go,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SecondaryButton(
                text = stringResource(R.string.import_paste),
                icon = Icons.Outlined.ContentPaste,
                onClick = { clipboard.getText()?.text?.let(onInputChange) },
                modifier = Modifier.weight(1f),
            )
            AccentButton(
                text = stringResource(R.string.import_go),
                icon = Icons.Outlined.AutoAwesome,
                onClick = onStart,
                enabled = state.input.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }

        val status = state.status
        if (status is ImportStatus.Failed) {
            ErrorState(error = status.error, onRetry = onStart)
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            stringResource(R.string.import_tip_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(Spacing.xs))
        listOf(R.string.import_tip_1, R.string.import_tip_2, R.string.import_tip_3).forEachIndexed { index, tip ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.cardRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text((index + 1).toString(), style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                }
                Spacer(Modifier.width(Spacing.md))
                Text(stringResource(tip), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        Text(
            stringResource(R.string.import_counts),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        Spacer(Modifier.height(Spacing.xl))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(Spacing.xl))
        SecondaryButton(
            text = stringResource(R.string.import_cook_from_pantry),
            icon = Icons.Outlined.Kitchen,
            onClick = onFindRecipes,
        )
    }
}

@Composable
private fun ImportProgress() {
    val messages = listOf(
        stringResource(R.string.import_progress_1),
        stringResource(R.string.import_progress_2),
        stringResource(R.string.import_progress_3),
    )
    var index by remember { mutableIntStateOf(0) }
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_200)
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AppTheme.colors.pastels.purple.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.SmartDisplay,
                contentDescription = null,
                tint = AppTheme.colors.pastels.purple.content,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        PulsingDots()
        Spacer(Modifier.height(20.dp))
        val textModifier = Modifier.fillMaxWidth()
        if (reducedMotion) {
            Text(messages[index], style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary, textAlign = TextAlign.Center, modifier = textModifier)
        } else {
            Crossfade(targetState = index, label = "importProgress") { i ->
                Text(messages[i], style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary, textAlign = TextAlign.Center, modifier = textModifier)
            }
        }
    }
}
