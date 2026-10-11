package com.whattocook.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whattocook.app.ui.theme.AppTheme

data class SegmentOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

/** Pill-shaped segmented toggle with a sliding, springy selection indicator. */
@Composable
fun <T> SegmentedPill(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "segmentIndicator",
    )
    val count = options.size.coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, CircleShape)
            .padding(5.dp)
            .drawBehind {
                val segmentWidth = size.width / count
                val radius = CornerRadius(size.height / 2f)
                val topLeft = Offset(segmentWidth * indicatorPosition, 0f)
                val indicatorSize = Size(segmentWidth, size.height)
                drawRoundRect(colors.cardRaised, topLeft, indicatorSize, radius)
                drawRoundRect(
                    color = colors.cardBorder,
                    topLeft = topLeft,
                    size = indicatorSize,
                    cornerRadius = radius,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
                label = "segmentColor",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(option.value) },
                        role = Role.Tab,
                    )
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (option.icon != null) {
                    Icon(option.icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
