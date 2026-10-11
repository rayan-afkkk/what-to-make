package com.whattocook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Radii

/** Rounded card with a subtle 1dp border, matching the app's card style. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.card,
    borderColor: Color = AppTheme.colors.cardBorder,
    cornerRadius: Dp = Radii.xLarge,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) {
        Modifier.pressClickable(onClick = onClick, onClickLabel = onClickLabel, pressedScale = 0.97f)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(clickModifier)
            .clip(shape)
            .background(color)
            .border(1.dp, borderColor, shape),
        content = content,
    )
}
