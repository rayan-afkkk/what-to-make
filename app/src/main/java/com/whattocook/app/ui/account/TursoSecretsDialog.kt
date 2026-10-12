package com.whattocook.app.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.ui.components.AccentButton
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Radii
import com.whattocook.app.ui.theme.Spacing

@Composable
fun TursoSecretsDialog(
    isTursoConfigured: Boolean,
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onTestConnection: (url: String, token: String, onResult: (AppResult<Unit>) -> Unit) -> Unit,
    onSaveCredentials: (url: String, token: String, onComplete: (AppResult<Unit>) -> Unit) -> Unit,
    onClearCredentials: () -> Unit,
) {
    val colors = AppTheme.colors
    val focusManager = LocalFocusManager.current

    var databaseUrl by rememberSaveable { mutableStateOf(initialUrl) }
    var authToken by rememberSaveable { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(false) }

    LaunchedEffect(initialUrl) {
        if (databaseUrl.isEmpty() && initialUrl.isNotEmpty()) {
            databaseUrl = initialUrl
        }
    }

    Dialog(
        onDismissRequest = { if (!isTesting && !isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp)),
            color = colors.card,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isTursoConfigured) {
                        Tag(
                            text = "Turso Active",
                            container = colors.pastels.green.container,
                            content = colors.pastels.green.content,
                        )
                    } else {
                        Tag(
                            text = "Demo Storage",
                            container = colors.pastels.amber.container,
                            content = colors.pastels.amber.content,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isTesting && !isSaving,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // Serif Display Title
                Text(
                    text = "Turso Database Secrets",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Configure your libSQL cloud database. All tokens remain private, masked, and never logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(Spacing.lg))

                // Option 1: AI Studio Secrets Panel Card
                AppCard(
                    color = colors.cardRaised,
                    borderColor = colors.cardBorder,
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.VpnKey,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Option 1: AI Studio Secrets Panel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "In AI Studio's left sidebar, click the Secrets icon (🔑) and add:\n• TURSO_DATABASE_URL = https://your-db.turso.io\n• TURSO_AUTH_TOKEN = your_turso_token\nGradle automatically injects these into BuildConfig.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // Option 2: In-App Direct Entry
                Text(
                    text = "Option 2: Direct Setup (Immediate)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Paste your credentials below to connect immediately in this app session without rebuilding:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(Spacing.md))

                // Database URL input
                SecretInputField(
                    label = "Turso Database URL",
                    value = databaseUrl,
                    onValueChange = {
                        databaseUrl = it
                        statusMessage = null
                    },
                    leadingIcon = Icons.Outlined.CloudSync,
                    placeholder = "https://your-db-org.turso.io",
                    enabled = !isTesting && !isSaving,
                    imeAction = ImeAction.Next,
                    onAction = { focusManager.moveFocus(FocusDirection.Down) },
                )

                Spacer(Modifier.height(Spacing.md))

                // Auth Token input (Masked by default!)
                SecretInputField(
                    label = "Turso Auth Token (Hidden)",
                    value = authToken,
                    onValueChange = {
                        authToken = it
                        statusMessage = null
                    },
                    leadingIcon = Icons.Outlined.Lock,
                    placeholder = "Paste your Turso auth token",
                    isPassword = true,
                    passwordVisible = tokenVisible,
                    onPasswordToggle = { tokenVisible = !tokenVisible },
                    enabled = !isTesting && !isSaving,
                    imeAction = ImeAction.Done,
                    onAction = { focusManager.clearFocus() },
                )

                // Status banner (Feedback on test/save)
                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    statusMessage?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSuccessStatus) colors.pastels.green.container else colors.accentSoft
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isSuccessStatus) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccessStatus) colors.pastels.green.content else colors.destructive,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = if (isSuccessStatus) colors.pastels.green.content else colors.destructive,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton(
                        text = if (isTesting) "Testing…" else "Test Connection",
                        onClick = {
                            val trimmedUrl = databaseUrl.trim()
                            val trimmedToken = authToken.trim()
                            if (trimmedUrl.isEmpty()) {
                                statusMessage = "Please enter a database URL."
                                isSuccessStatus = false
                                return@SecondaryButton
                            }
                            if (trimmedToken.isEmpty()) {
                                statusMessage = "Please enter an auth token."
                                isSuccessStatus = false
                                return@SecondaryButton
                            }
                            isTesting = true
                            statusMessage = null
                            onTestConnection(trimmedUrl, trimmedToken) { result ->
                                isTesting = false
                                when (result) {
                                    is AppResult.Success -> {
                                        statusMessage = "Connection successful! Turso is reachable."
                                        isSuccessStatus = true
                                    }
                                    is AppResult.Failure -> {
                                        val errText = (result.error as? AppError.Unknown)?.message ?: "Connection failed."
                                        statusMessage = errText
                                        isSuccessStatus = false
                                    }
                                }
                            }
                        },
                        enabled = !isTesting && !isSaving,
                        modifier = Modifier.weight(1f),
                    )

                    AccentButton(
                        text = if (isSaving) "Saving…" else "Save & Connect",
                        onClick = {
                            val trimmedUrl = databaseUrl.trim()
                            val trimmedToken = authToken.trim()
                            if (trimmedUrl.isEmpty() || trimmedToken.isEmpty()) {
                                statusMessage = "Please fill in both URL and Token."
                                isSuccessStatus = false
                                return@AccentButton
                            }
                            isSaving = true
                            statusMessage = null
                            onSaveCredentials(trimmedUrl, trimmedToken) { result ->
                                isSaving = false
                                when (result) {
                                    is AppResult.Success -> {
                                        statusMessage = "Connected and saved! Turso cloud is active."
                                        isSuccessStatus = true
                                    }
                                    is AppResult.Failure -> {
                                        val errText = (result.error as? AppError.Unknown)?.message ?: "Failed to save credentials."
                                        statusMessage = errText
                                        isSuccessStatus = false
                                    }
                                }
                            }
                        },
                        loading = isSaving,
                        enabled = !isTesting && !isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // Reset button
                TextButton(
                    onClick = {
                        databaseUrl = ""
                        authToken = ""
                        statusMessage = "Credentials reset to defaults."
                        isSuccessStatus = true
                        onClearCredentials()
                    },
                    enabled = !isTesting && !isSaving,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Reset to Demo / Defaults",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecretInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onAction: () -> Unit = {},
) {
    val colors = AppTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radii.medium))
                .background(colors.cardRaised)
                .border(
                    width = 1.dp,
                    color = if (isFocused) colors.accent else colors.cardBorder,
                    shape = RoundedCornerShape(Radii.medium),
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) colors.accent else colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onAction() },
                        onNext = { onAction() },
                    ),
                )
            }
            if (isPassword && onPasswordToggle != null) {
                IconButton(
                    onClick = onPasswordToggle,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide secret" else "Show secret",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
