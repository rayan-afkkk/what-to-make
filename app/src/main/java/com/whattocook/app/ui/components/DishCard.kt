package com.whattocook.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whattocook.app.domain.LibraryDish
import com.whattocook.app.ui.theme.AppTheme

private val PhotoShape = RoundedCornerShape(18.dp)

/** Photo + name card for a library dish. */
@Composable
fun DishCard(
    dish: LibraryDish,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.pressClickable(onClick = onClick, role = Role.Button, pressedScale = 0.96f)) {
        RecipePhoto(
            url = dish.imageUrl,
            contentDescription = null,
            thumbnail = true,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(PhotoShape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = dish.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Softly pulsing grey block shown while a row of dishes loads. */
@Composable
fun DishCardPlaceholder(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "placeholder").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "placeholderAlpha",
    )
    Column(modifier.graphicsLayer { alpha = pulse }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(PhotoShape)
                .background(AppTheme.colors.cardRaised),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(AppTheme.colors.cardRaised),
        )
    }
}
