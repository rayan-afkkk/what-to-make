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
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.whattocook.app.ui.components.AccentButton
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.SegmentOption
import com.whattocook.app.ui.components.SegmentedPill
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Radii
import com.whattocook.app.ui.theme.Spacing

private enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun TursoAuthDialog(
    isTursoConfigured: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSignIn: (email: String, pass: String) -> Unit,
    onSignUp: (email: String, pass: String) -> Unit,
    onConfigureSecrets: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val focusManager = LocalFocusManager.current
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp)),
            color = colors.card,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                // Top row with status badge and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isTursoConfigured) {
                        Tag(
                            text = "Turso Connected",
                            container = colors.pastels.green.container,
                            content = colors.pastels.green.content,
                        )
                    } else {
                        Tag(
                            text = "Demo Mode",
                            container = colors.pastels.amber.container,
                            content = colors.pastels.amber.content,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
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

                // Serif Display Title matching the app's editorial aesthetic
                Text(
                    text = if (mode == AuthMode.SIGN_IN) "Welcome back" else "Create account",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Sync your kitchen pantry, recipes, and preferences across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(Spacing.lg))

                // App's native animated SegmentedPill toggle
                SegmentedPill(
                    options = listOf(
                        SegmentOption(
                            value = AuthMode.SIGN_IN,
                            label = "Sign in",
                            icon = Icons.AutoMirrored.Outlined.Login,
                        ),
                        SegmentOption(
                            value = AuthMode.SIGN_UP,
                            label = "Create account",
                            icon = Icons.Outlined.PersonAdd,
                        ),
                    ),
                    selected = mode,
                    onSelect = {
                        mode = it
                        localError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.lg))

                // Email Input
                ThemedInputField(
                    label = "Email address",
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                    },
                    leadingIcon = Icons.Outlined.Email,
                    placeholder = "you@example.com",
                    enabled = !isBusy,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onAction = { focusManager.moveFocus(FocusDirection.Down) },
                )

                Spacer(Modifier.height(Spacing.md))

                // Password Input
                ThemedInputField(
                    label = "Password",
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                    },
                    leadingIcon = Icons.Outlined.Lock,
                    placeholder = "At least 6 characters",
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    enabled = !isBusy,
                    keyboardType = KeyboardType.Password,
                    imeAction = if (mode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done,
                    onAction = {
                        if (mode == AuthMode.SIGN_UP) {
                            focusManager.moveFocus(FocusDirection.Down)
                        } else {
                            focusManager.clearFocus()
                        }
                    },
                )

                // Confirm Password (when in Sign Up mode)
                if (mode == AuthMode.SIGN_UP) {
                    Spacer(Modifier.height(Spacing.md))
                    ThemedInputField(
                        label = "Confirm password",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localError = null
                        },
                        leadingIcon = Icons.Outlined.Lock,
                        placeholder = "Re-enter your password",
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        enabled = !isBusy,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onAction = { focusManager.clearFocus() },
                    )
                }

                // Error State
                AnimatedVisibility(
                    visible = localError != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    localError?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = colors.destructive,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = err,
                                color = colors.destructive,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))

                // Main Action Pill Button
                AccentButton(
                    text = if (mode == AuthMode.SIGN_IN) "Sign in" else "Create account",
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                            localError = "Please enter a valid email address."
                            return@AccentButton
                        }
                        if (password.length < 6) {
                            localError = "Password must be at least 6 characters long."
                            return@AccentButton
                        }
                        if (mode == AuthMode.SIGN_UP && password != confirmPassword) {
                            localError = "Passwords do not match."
                            return@AccentButton
                        }

                        localError = null
                        if (mode == AuthMode.SIGN_IN) {
                            onSignIn(trimmedEmail, password)
                        } else {
                            onSignUp(trimmedEmail, password)
                        }
                    },
                    loading = isBusy,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.md))

                // Security & Privacy Guarantee Card
                AppCard(
                    color = colors.cardRaised,
                    borderColor = colors.cardBorder,
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Security guarantee: Passwords are cryptographically salted and hashed with PBKDF2 before storage. Database tokens remain private and hidden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }

                // If not yet configured, provide helpful guidance and a 1-tap demo shortcut
                if (!isTursoConfigured) {
                    Spacer(Modifier.height(Spacing.md))
                    AppCard(
                        color = colors.cardRaised,
                        borderColor = colors.cardBorder,
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.VpnKey,
                                    contentDescription = null,
                                    tint = colors.pastels.amber.content,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Ready to paste Turso keys?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Use the Secrets panel (🔑 key icon in the AI Studio sidebar) to paste TURSO_DATABASE_URL and TURSO_AUTH_TOKEN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            SecondaryButton(
                                text = "Fill demo account (demo@example.com)",
                                onClick = {
                                    email = "demo@example.com"
                                    password = "password123"
                                    confirmPassword = "password123"
                                    mode = AuthMode.SIGN_IN
                                    localError = null
                                },
                            )
                            if (onConfigureSecrets != null) {
                                Spacer(Modifier.height(8.dp))
                                SecondaryButton(
                                    text = "Configure Turso Cloud Secrets 🔑",
                                    onClick = onConfigureSecrets,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom tactile input field styled consistently with What to Cook's warm culinary aesthetic.
 */
@Composable
private fun ThemedInputField(
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
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
