package com.whattocook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Pastel

/** Soft pastel chip, slightly tilted, used on Discover. */
@Composable
fun TiltedPastelChip(
    label: String,
    pastel: Pastel,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .rotate(rotation)
            .pressClickable(onClick = onClick, pressedScale = 0.92f)
            .clip(shape)
            .background(pastel.container)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = pastel.content, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = pastel.content,
            maxLines = 1,
        )
    }
}

/** Removable ingredient chip used in the pantry. */
@Composable
fun IngredientChip(
    label: String,
    onRemove: () -> Unit,
    removeDescription: String,
    modifier: Modifier = Modifier,
    container: Color = AppTheme.colors.card,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, AppTheme.colors.cardBorder, CircleShape)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
        )
        // 48dp touch target for the remove button.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(42.dp)
                .pressClickable(onClick = onRemove, onClickLabel = removeDescription, pressedScale = 0.85f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = removeDescription,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Small rounded tag such as "2 missing" or "3/6". */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = AppTheme.colors.cardRaised,
    content: Color = AppTheme.colors.textSecondary,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}
