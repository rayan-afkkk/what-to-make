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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.ui.components.AccentButton
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.SecondaryButton
import com.whattocook.app.ui.components.SegmentOption
import com.whattocook.app.ui.components.SegmentedPill
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.errorMessage
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Radii
import com.whattocook.app.ui.theme.Spacing

private enum class AuthPageMode { SIGN_IN, SIGN_UP }

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var mode by rememberSaveable { mutableStateOf(AuthPageMode.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var showSecretsDialog by rememberSaveable { mutableStateOf(false) }
    var initialTursoUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.getEffectiveTursoUrl { initialTursoUrl = it }
    }

    val actionError = state.actionError
    val actionErrorText = actionError?.let { errorMessage(it) }
    LaunchedEffect(actionError) {
        if (actionErrorText != null) {
            snackbarHostState.showSnackbar(actionErrorText)
            viewModel.actionErrorShown()
        }
    }

    val actionSuccess = state.actionSuccess
    LaunchedEffect(actionSuccess) {
        if (actionSuccess != null) {
            snackbarHostState.showSnackbar(actionSuccess)
            viewModel.actionSuccessShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.cardRaised),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(
                        text = "What to Cook",
                        container = colors.pastels.amber.container,
                        content = colors.pastels.amber.content,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (state.isTursoConfigured) {
                        Tag(
                            text = "Turso Cloud",
                            container = colors.pastels.green.container,
                            content = colors.pastels.green.content,
                        )
                    } else {
                        Tag(
                            text = "Demo Mode",
                            container = colors.pastels.blue.container,
                            content = colors.pastels.blue.content,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // Hero Graphic & Title
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.pastels.pink.container)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.OutdoorGrill,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = if (mode == AuthPageMode.SIGN_IN) "Welcome back" else "Create your account",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Sync your kitchen ingredients, saved recipes, and weekly shopping lists safely.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(Spacing.xl))

            // Segmented Switcher
            SegmentedPill(
                options = listOf(
                    SegmentOption(
                        value = AuthPageMode.SIGN_IN,
                        label = "Sign in",
                        icon = Icons.AutoMirrored.Outlined.Login,
                    ),
                    SegmentOption(
                        value = AuthPageMode.SIGN_UP,
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

            // Email Address Input
            ThemedAuthField(
                label = "Email address",
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                leadingIcon = Icons.Outlined.Email,
                placeholder = "you@example.com",
                enabled = !state.busy,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onAction = { focusManager.moveFocus(FocusDirection.Down) },
            )

            Spacer(Modifier.height(Spacing.md))

            // Password Input
            ThemedAuthField(
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
                enabled = !state.busy,
                keyboardType = KeyboardType.Password,
                imeAction = if (mode == AuthPageMode.SIGN_UP) ImeAction.Next else ImeAction.Done,
                onAction = {
                    if (mode == AuthPageMode.SIGN_UP) {
                        focusManager.moveFocus(FocusDirection.Down)
                    } else {
                        focusManager.clearFocus()
                    }
                },
            )

            // Confirm Password (in Sign Up mode)
            if (mode == AuthPageMode.SIGN_UP) {
                Spacer(Modifier.height(Spacing.md))
                ThemedAuthField(
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
                    enabled = !state.busy,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onAction = { focusManager.clearFocus() },
                )
            }

            // Error display
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

            // Main Action Button
            AccentButton(
                text = if (mode == AuthPageMode.SIGN_IN) "Sign In to What to Cook" else "Create Free Account",
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
                    if (mode == AuthPageMode.SIGN_UP && password != confirmPassword) {
                        localError = "Passwords do not match."
                        return@AccentButton
                    }

                    localError = null
                    if (mode == AuthPageMode.SIGN_IN) {
                        viewModel.signInWithEmail(trimmedEmail, password) {
                            onBack()
                        }
                    } else {
                        viewModel.signUpWithEmail(trimmedEmail, password) {
                            onBack()
                        }
                    }
                },
                loading = state.busy,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.md))

            // Quick Demo Account Pill
            SecondaryButton(
                text = "Fill demo account (demo@example.com)",
                onClick = {
                    email = "demo@example.com"
                    password = "password123"
                    confirmPassword = "password123"
                    mode = AuthPageMode.SIGN_IN
                    localError = null
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.xl))

            // Privacy & Password Protection Callout
            AppCard(
                color = colors.cardRaised,
                borderColor = colors.cardBorder,
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zero-Knowledge Protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Passwords are cryptographic salted and hashed using PBKDF2 with HMAC-SHA256 before transmission. Turso database tokens remain private and hidden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Turso Database Credentials & Secrets Card
            AppCard(
                color = colors.card,
                borderColor = colors.cardBorder,
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CloudSync,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Turso Cloud Database",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                        }

                        if (state.isTursoConfigured) {
                            Tag(
                                text = "Connected",
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
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Turso powers real-time SQLite cloud synchronization. You can paste secrets into the AI Studio Secrets panel or configure them right here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    SecondaryButton(
                        text = "Configure Turso Cloud Secrets 🔑",
                        onClick = { showSecretsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xxl))
        }

        // Loading overlay
        if (state.busy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.lg),
        )
    }

    if (showSecretsDialog) {
        TursoSecretsDialog(
            isTursoConfigured = state.isTursoConfigured,
            initialUrl = initialTursoUrl,
            onDismiss = { showSecretsDialog = false },
            onTestConnection = { url, token, onResult ->
                viewModel.testTursoConnection(url, token, onResult)
            },
            onSaveCredentials = { url, token, onComplete ->
                viewModel.saveTursoCredentials(url, token) { result ->
                    onComplete(result)
                    if (result is com.whattocook.app.domain.model.AppResult.Success) {
                        viewModel.getEffectiveTursoUrl { initialTursoUrl = it }
                    }
                }
            },
            onClearCredentials = {
                viewModel.clearTursoCredentials()
                initialTursoUrl = ""
            },
        )
    }
}

@Composable
private fun ThemedAuthField(
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
