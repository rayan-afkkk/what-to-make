package com.whattocook.app.ui.paywall

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.BuildConfig
import com.whattocook.app.R
import com.whattocook.app.data.billing.PlanOffer
import com.whattocook.app.data.billing.PlanPeriod
import com.whattocook.app.domain.LibraryAccess
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.ui.components.AccentButton
import com.whattocook.app.ui.components.AppCard
import com.whattocook.app.ui.components.PrimaryButton
import com.whattocook.app.ui.components.PulsingDots
import com.whattocook.app.ui.components.Tag
import com.whattocook.app.ui.components.findActivity
import com.whattocook.app.ui.components.formatDate
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = state.message?.let { paywallMessageText(it) }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        if (messageText != null) snackbarHostState.showSnackbar(messageText)
        viewModel.messageShown()
        if (message == PaywallMessage.PURCHASE_SUCCESS || message == PaywallMessage.RESTORED) {
            delay(900)
            onClose()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = colors.textPrimary)
                }
            }
            Tag(
                text = stringResource(R.string.plan_pro),
                container = colors.accent,
                content = colors.onAccent,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )

            if (state.fromLibrary && state.plan == Plan.FREE) {
                Spacer(Modifier.height(Spacing.xl))
                LibraryBanner()
            } else if (state.limitReached && state.plan == Plan.FREE) {
                Spacer(Modifier.height(Spacing.xl))
                LimitBanner(weeklyLimit = state.weeklyLimit, resetsAtMillis = state.resetsAtMillis)
            }

            Spacer(Modifier.height(Spacing.xl))
            ComparisonTable(weeklyLimit = state.weeklyLimit)

            Spacer(Modifier.height(Spacing.xl))
            if (state.plan == Plan.PRO) {
                AppCard(Modifier.fillMaxWidth(), color = colors.pastels.green.container, borderColor = colors.pastels.green.container) {
                    Text(
                        stringResource(R.string.paywall_already_pro),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.pastels.green.content,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            } else {
                PlanOptions(
                    state = state,
                    onSelect = viewModel::select,
                    onRetry = viewModel::retryBilling,
                )
                Spacer(Modifier.height(Spacing.lg))
                AccentButton(
                    text = stringResource(R.string.paywall_continue),
                    onClick = { viewModel.purchase(context.findActivity()) },
                    enabled = state.billing.offers[state.selected] != null,
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            TextButton(
                onClick = viewModel::restore,
                enabled = !state.restoring,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(if (state.restoring) R.string.paywall_restoring else R.string.paywall_restore),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                stringResource(R.string.paywall_fine_print),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { context.openLink(BuildConfig.TERMS_URL) }) {
                    Text(stringResource(R.string.legal_terms), color = colors.textSecondary)
                }
                TextButton(onClick = { context.openLink(BuildConfig.PRIVACY_URL) }) {
                    Text(stringResource(R.string.legal_privacy), color = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(Spacing.lg))
    }
}

@Composable
private fun paywallMessageText(message: PaywallMessage): String = stringResource(
    when (message) {
        PaywallMessage.PURCHASE_SUCCESS -> R.string.paywall_msg_success
        PaywallMessage.PURCHASE_PENDING -> R.string.paywall_msg_pending
        PaywallMessage.PURCHASE_FAILED -> R.string.paywall_msg_failed
        PaywallMessage.VERIFICATION_FAILED -> R.string.paywall_msg_verify_failed
        PaywallMessage.RESTORED -> R.string.paywall_msg_restored
        PaywallMessage.NOTHING_TO_RESTORE -> R.string.paywall_msg_nothing_to_restore
        PaywallMessage.RESTORE_FAILED -> R.string.paywall_msg_restore_failed
        PaywallMessage.BILLING_UNAVAILABLE -> R.string.paywall_msg_unavailable
    },
)

@Composable
private fun LimitBanner(weeklyLimit: Int, resetsAtMillis: Long) {
    val colors = AppTheme.colors
    val amber = colors.pastels.amber
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(amber.container)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = amber.content)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(R.string.paywall_limit_title, weeklyLimit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amber.content,
            )
            Text(
                if (resetsAtMillis > 0) {
                    stringResource(R.string.paywall_limit_resets, formatDate(resetsAtMillis))
                } else {
                    stringResource(R.string.usage_resets_weekly)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = amber.content,
            )
        }
    }
}

@Composable
private fun LibraryBanner() {
    val purple = AppTheme.colors.pastels.purple
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(purple.container)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = purple.content)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(R.string.paywall_library_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = purple.content,
            )
            Text(
                stringResource(R.string.paywall_library_message, LibraryAccess.FREE_PER_CATEGORY),
                style = MaterialTheme.typography.bodyMedium,
                color = purple.content,
            )
        }
    }
}

@Composable
private fun ComparisonTable(weeklyLimit: Int) {
    val colors = AppTheme.colors
    val rows = listOf(
        Triple(stringResource(R.string.paywall_row_recipes), stringResource(R.string.paywall_row_recipes_free, weeklyLimit), stringResource(R.string.paywall_row_recipes_pro)),
        Triple(
            stringResource(R.string.paywall_row_library),
            stringResource(R.string.paywall_row_library_free, LibraryAccess.FREE_PER_CATEGORY),
            stringResource(R.string.paywall_row_library_pro),
        ),
        Triple(stringResource(R.string.paywall_row_pantry), CHECK, CHECK),
        Triple(stringResource(R.string.paywall_row_cook_mode), CHECK, CHECK),
        Triple(stringResource(R.string.paywall_row_no_ads), CHECK, CHECK),
        Triple(stringResource(R.string.paywall_row_support), DASH, CHECK),
    )
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            TableRow(
                feature = "",
                free = stringResource(R.string.plan_free),
                pro = stringResource(R.string.plan_pro),
                header = true,
            )
            rows.forEach { (feature, free, pro) ->
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                TableRow(feature = feature, free = free, pro = pro, header = false)
            }
        }
    }
}

private const val CHECK = "check"
private const val DASH = "dash"

@Composable
private fun TableRow(feature: String, free: String, pro: String, header: Boolean) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            feature,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1.6f),
        )
        TableCell(free, header, highlight = false, modifier = Modifier.weight(1f))
        TableCell(pro, header, highlight = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TableCell(value: String, header: Boolean, highlight: Boolean, modifier: Modifier) {
    val colors = AppTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        when (value) {
            CHECK -> Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.paywall_included),
                tint = if (highlight) colors.accent else colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            DASH -> Text("—", color = colors.textMuted, style = MaterialTheme.typography.bodyMedium)
            else -> Text(
                value,
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
                fontWeight = if (header || highlight) FontWeight.Bold else FontWeight.Normal,
                color = if (highlight) colors.accent else colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlanOptions(
    state: PaywallUiState,
    onSelect: (PlanPeriod) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = AppTheme.colors
    when {
        state.billing.isLoading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xl),
            contentAlignment = Alignment.Center,
        ) { PulsingDots() }
        !state.billing.isAvailable || state.billing.offers.isEmpty() -> AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.paywall_unavailable_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(R.string.paywall_unavailable_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.sm))
                PrimaryButton(text = stringResource(R.string.action_try_again), onClick = onRetry)
            }
        }
        else -> Column(
            Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.billing.offers[PlanPeriod.YEARLY]?.let { offer ->
                PlanOptionCard(
                    title = stringResource(R.string.paywall_yearly),
                    offer = offer,
                    periodLabel = stringResource(R.string.paywall_per_year),
                    badge = state.yearlySavingsPercent?.let { stringResource(R.string.paywall_save_percent, it) }
                        ?: stringResource(R.string.paywall_best_value),
                    selected = state.selected == PlanPeriod.YEARLY,
                    onClick = { onSelect(PlanPeriod.YEARLY) },
                )
            }
            state.billing.offers[PlanPeriod.MONTHLY]?.let { offer ->
                PlanOptionCard(
                    title = stringResource(R.string.paywall_monthly),
                    offer = offer,
                    periodLabel = stringResource(R.string.paywall_per_month),
                    badge = null,
                    selected = state.selected == PlanPeriod.MONTHLY,
                    onClick = { onSelect(PlanPeriod.MONTHLY) },
                )
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    offer: PlanOffer,
    periodLabel: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val border by animateColorAsState(if (selected) colors.accent else colors.cardBorder, label = "planBorder")
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.accent else colors.textMuted, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else colors.card),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Tag(text = badge, container = colors.accentSoft, content = colors.accent)
                }
            }
            Text(
                stringResource(R.string.paywall_price_per, offer.formattedPrice, periodLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
    }
}

private fun Context.openLink(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No browser available.
    }
}
