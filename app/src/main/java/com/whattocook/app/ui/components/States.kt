package com.whattocook.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whattocook.app.R
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.ui.theme.AppTheme

/** Three softly pulsing dots plus a message. */
@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PulsingDots()
        Spacer(Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PulsingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, delayMillis = index * 140),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = scale
                    }
                    .clip(CircleShape)
                    .background(AppTheme.colors.accent),
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.width(260.dp))
        }
    }
}

@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(error.icon(), contentDescription = null, tint = AppTheme.colors.accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = errorTitle(error),
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage(error),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = stringResource(R.string.action_try_again), onClick = onRetry, modifier = Modifier.width(220.dp))
        }
    }
}

private fun AppError.icon(): ImageVector = when (this) {
    AppError.NoNetwork -> Icons.Outlined.WifiOff
    AppError.Timeout -> Icons.Outlined.Schedule
    AppError.Unauthorized, AppError.NotConfigured -> Icons.Outlined.Lock
    is AppError.Server -> Icons.Outlined.CloudOff
    is AppError.LimitReached -> Icons.Outlined.Lock
    is AppError.Unprocessable, is AppError.Unknown -> Icons.Outlined.ErrorOutline
}

@Composable
fun errorTitle(error: AppError): String = stringResource(
    when (error) {
        AppError.NoNetwork -> R.string.error_no_network_title
        AppError.Timeout -> R.string.error_timeout_title
        AppError.Unauthorized -> R.string.error_unauthorized_title
        AppError.NotConfigured -> R.string.error_not_configured_title
        is AppError.LimitReached -> R.string.error_limit_title
        is AppError.Server -> R.string.error_server_title
        is AppError.Unprocessable -> when (error.reason) {
            "no_recipe_found" -> R.string.error_no_recipe_title
            "unsupported_link", "invalid_link" -> R.string.error_bad_link_title
            "not_found" -> R.string.error_not_found_title
            else -> R.string.error_unknown_title
        }
        is AppError.Unknown -> R.string.error_unknown_title
    },
)

@Composable
fun errorMessage(error: AppError): String = when (error) {
    AppError.NoNetwork -> stringResource(R.string.error_no_network_message)
    AppError.Timeout -> stringResource(R.string.error_timeout_message)
    AppError.Unauthorized -> stringResource(R.string.error_unauthorized_message)
    AppError.NotConfigured -> stringResource(R.string.error_not_configured_message)
    is AppError.LimitReached -> stringResource(R.string.error_limit_message)
    is AppError.Server -> stringResource(R.string.error_server_message, error.code)
    is AppError.Unprocessable -> stringResource(
        when (error.reason) {
            "no_recipe_found" -> R.string.error_no_recipe_message
            "unsupported_link", "invalid_link" -> R.string.error_bad_link_message
            "not_found" -> R.string.error_not_found_message
            else -> R.string.error_unknown_message
        },
    )
    is AppError.Unknown -> error.message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_unknown_message)
}
