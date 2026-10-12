package com.whattocook.app.ui.account

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.BuildConfig
import com.whattocook.app.R
import com.whattocook.app.domain.UsageSummary
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.ThemeMode
import com.whattocook.app.ui.components.AnimatedProgressBar
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.PulsingDots
import com.whattocook.app.ui.components.ScreenHeader
import com.whattocook.app.ui.components.SectionLabel
import com.whattocook.app.ui.components.SegmentOption
import com.whattocook.app.ui.components.SegmentedPill
import com.whattocook.app.ui.components.SettingsRow
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.errorMessage
import com.whattocook.app.ui.components.formatDate
import com.whattocook.app.ui.components.quantityString
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing

@Composable
fun AccountScreen(
    onOpenPlan: () -> Unit,
    onViewOnboarding: () -> Unit,
    onOpenAuth: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by rememberSaveable { mutableStateOf(AccountDialog.NONE) }

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

    val appName = stringResource(R.string.app_name)
    val feedbackSubject = stringResource(R.string.account_feedback_subject, appName, BuildConfig.VERSION_NAME)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.account_title))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            ) {
                ProfileCard(
                    plan = state.plan,
                    userEmail = state.userEmail,
                    isTursoConfigured = state.isTursoConfigured,
                )

                if (state.userEmail == null) {
                    Spacer(Modifier.height(Spacing.md))
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.pastels.blue.container),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudSync,
                                        contentDescription = null,
                                        tint = colors.pastels.blue.content,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.account_turso_sign_in_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = stringResource(R.string.account_turso_sign_in_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onOpenAuth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.account_turso_sign_in_action),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                UsageCard(
                    usage = state.usage,
                    loading = state.usageLoading,
                    errorText = state.usageError?.let { errorMessage(it) },
                    onRetry = viewModel::refreshUsage,
                )

                Spacer(Modifier.height(Spacing.xxl))
                SectionLabel(stringResource(R.string.account_appearance))
                Spacer(Modifier.height(Spacing.md))
                SegmentedPill(
                    options = listOf(
                        SegmentOption(ThemeMode.SYSTEM, stringResource(R.string.theme_system), Icons.Outlined.PhoneAndroid),
                        SegmentOption(ThemeMode.LIGHT, stringResource(R.string.theme_light), Icons.Outlined.LightMode),
                        SegmentOption(ThemeMode.DARK, stringResource(R.string.theme_dark), Icons.Outlined.DarkMode),
                    ),
                    selected = state.themeMode,
                    onSelect = viewModel::setTheme,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(R.string.account_appearance_hint, appName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(Spacing.xl))
                val usage = state.usage
                SettingsRow(
                    icon = Icons.Outlined.Bolt,
                    title = stringResource(R.string.account_plan),
                    subtitle = when {
                        state.plan == Plan.PRO -> stringResource(R.string.account_plan_pro_subtitle)
                        usage != null -> quantityString(R.plurals.usage_left_this_week, usage.remaining, usage.remaining, usage.limit)
                        else -> null
                    },
                    onClick = onOpenPlan,
                )
                SettingsRow(
                    icon = Icons.Outlined.PlayCircleOutline,
                    title = stringResource(R.string.account_view_onboarding),
                    onClick = onViewOnboarding,
                )
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = stringResource(R.string.account_help),
                    onClick = { context.sendFeedbackEmail(feedbackSubject) },
                )
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.account_privacy_terms),
                    onClick = { dialog = AccountDialog.LEGAL },
                )
                SettingsRow(
                    icon = Icons.Outlined.VpnKey,
                    title = stringResource(R.string.account_turso_secrets_title),
                    subtitle = if (state.isTursoConfigured) "Connected" else "Configure secrets & keys",
                    onClick = { dialog = AccountDialog.TURSO_SECRETS },
                )
                if (state.userEmail != null) {
                    SettingsRow(
                        icon = Icons.Outlined.SwitchAccount,
                        title = stringResource(R.string.account_turso_switch_account),
                        onClick = onOpenAuth,
                    )
                }
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = stringResource(R.string.account_sign_out),
                    destructive = true,
                    onClick = { dialog = AccountDialog.SIGN_OUT },
                )
                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.account_delete),
                    destructive = true,
                    onClick = { dialog = AccountDialog.DELETE },
                )
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    stringResource(R.string.account_version, appName, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(Spacing.xl))
            }
        }

        if (state.busy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(Spacing.lg))
    }

    when (dialog) {
        AccountDialog.NONE -> Unit
        AccountDialog.TURSO_SECRETS -> {
            var url by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                viewModel.getEffectiveTursoUrl { url = it }
            }
            TursoSecretsDialog(
                isTursoConfigured = state.isTursoConfigured,
                initialUrl = url,
                onDismiss = { dialog = AccountDialog.NONE },
                onTestConnection = { testUrl, testToken, onResult ->
                    viewModel.testTursoConnection(testUrl, testToken, onResult)
                },
                onSaveCredentials = { saveUrl, saveToken, onComplete ->
                    viewModel.saveTursoCredentials(saveUrl, saveToken, onComplete)
                },
                onClearCredentials = {
                    viewModel.clearTursoCredentials()
                },
            )
        }
        AccountDialog.TURSO_AUTH -> TursoAuthDialog(
            isTursoConfigured = state.isTursoConfigured,
            isBusy = state.busy,
            onDismiss = { dialog = AccountDialog.NONE },
            onSignIn = { email, pass ->
                viewModel.signInWithEmail(email, pass) { dialog = AccountDialog.NONE }
            },
            onSignUp = { email, pass ->
                viewModel.signUpWithEmail(email, pass) { dialog = AccountDialog.NONE }
            },
            onConfigureSecrets = { dialog = AccountDialog.TURSO_SECRETS },
        )
        AccountDialog.LEGAL -> AlertDialog(
            onDismissRequest = { dialog = AccountDialog.NONE },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.account_privacy_terms)) },
            text = { Text(stringResource(R.string.account_legal_message)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = AccountDialog.NONE
                    context.openUrl(BuildConfig.PRIVACY_URL)
                }) { Text(stringResource(R.string.legal_privacy), color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialog = AccountDialog.NONE
                    context.openUrl(BuildConfig.TERMS_URL)
                }) { Text(stringResource(R.string.legal_terms), color = colors.accent) }
            },
        )
        AccountDialog.SIGN_OUT -> ConfirmDialog(
            title = stringResource(R.string.account_sign_out_title),
            message = stringResource(R.string.account_sign_out_message),
            confirm = stringResource(R.string.account_sign_out),
            onConfirm = {
                dialog = AccountDialog.NONE
                viewModel.signOut()
            },
            onDismiss = { dialog = AccountDialog.NONE },
        )
        AccountDialog.DELETE -> ConfirmDialog(
            title = stringResource(R.string.account_delete_title),
            message = stringResource(R.string.account_delete_message),
            confirm = stringResource(R.string.account_delete_confirm),
            onConfirm = {
                dialog = AccountDialog.NONE
                viewModel.deleteAccount()
            },
            onDismiss = { dialog = AccountDialog.NONE },
        )
    }
}

private enum class AccountDialog { NONE, LEGAL, SIGN_OUT, DELETE, TURSO_AUTH, TURSO_SECRETS }

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        properties = DialogProperties(),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = colors.destructive, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textPrimary) }
        },
    )
}

@Composable
private fun ProfileCard(
    plan: Plan,
    userEmail: String?,
    isTursoConfigured: Boolean,
) {
    val colors = AppTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val initial = userEmail?.firstOrNull()?.uppercaseChar()?.toString()
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (userEmail != null) colors.pastels.blue.container else colors.pastels.amber.container),
                contentAlignment = Alignment.Center,
            ) {
                if (initial != null) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.pastels.blue.content,
                    )
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = colors.pastels.amber.content, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = userEmail ?: stringResource(R.string.account_guest),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                Text(
                    text = when {
                        userEmail != null && isTursoConfigured -> stringResource(R.string.account_turso_connected)
                        userEmail != null -> stringResource(R.string.account_turso_demo)
                        else -> stringResource(R.string.account_guest_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Tag(
                text = stringResource(if (plan == Plan.PRO) R.string.plan_pro else R.string.plan_free),
                container = if (plan == Plan.PRO) colors.accent else colors.cardRaised,
                content = if (plan == Plan.PRO) colors.onAccent else colors.textPrimary,
            )
        }
    }
}

@Composable
private fun UsageCard(
    usage: UsageSummary?,
    loading: Boolean,
    errorText: String?,
    onRetry: () -> Unit,
) {
    val colors = AppTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(18.dp)
                .animateContentSize(),
        ) {
            when {
                usage != null -> {
                    Text(
                        quantityString(R.plurals.usage_remaining, usage.remaining, usage.remaining),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(if (usage.plan == Plan.PRO) R.string.usage_pro_label else R.string.usage_free_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.usage_fraction, usage.remaining, usage.limit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    AnimatedProgressBar(progress = usage.remainingFraction, height = 8.dp)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = resetText(usage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                loading -> Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.lg),
                    horizontalArrangement = Arrangement.Center,
                ) { PulsingDots() }
                else -> Column {
                    Text(
                        stringResource(R.string.usage_unavailable),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    if (errorText != null) {
                        Text(errorText, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.action_try_again), color = colors.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun resetText(usage: UsageSummary): String = when {
    usage.resetsAtMillis <= 0L -> stringResource(R.string.usage_resets_weekly)
    usage.daysUntilReset <= 0 -> stringResource(R.string.usage_resets_today)
    usage.daysUntilReset <= 7 -> quantityString(R.plurals.usage_resets_in_days, usage.daysUntilReset, usage.daysUntilReset)
    else -> stringResource(R.string.usage_resets_on, formatDate(usage.resetsAtMillis))
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No browser installed; nothing sensible to do.
    }
}

private fun Context.sendFeedbackEmail(subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        openUrl("mailto:${BuildConfig.SUPPORT_EMAIL}")
    }
}
