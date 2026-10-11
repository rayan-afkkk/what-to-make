package com.whattocook.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing

/** Large serif screen title with optional trailing actions and a hairline divider. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.screen, end = Spacing.sm, top = Spacing.md, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        if (showDivider) HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)
    }
}

/** Small uppercase section label, e.g. "APPEARANCE". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = AppTheme.colors.textSecondary,
        modifier = modifier.semantics { heading() },
    )
}
