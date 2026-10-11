package com.whattocook.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.billing.BillingRepository
import com.whattocook.app.data.billing.BillingState
import com.whattocook.app.data.billing.PlanPeriod
import com.whattocook.app.data.billing.PurchaseEvent
import com.whattocook.app.data.billing.RestoreResult
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PaywallMessage {
    PURCHASE_SUCCESS,
    PURCHASE_PENDING,
    PURCHASE_FAILED,
    VERIFICATION_FAILED,
    RESTORED,
    NOTHING_TO_RESTORE,
    RESTORE_FAILED,
    BILLING_UNAVAILABLE,
}

data class PaywallUiState(
    val billing: BillingState = BillingState(),
    val selected: PlanPeriod = PlanPeriod.YEARLY,
    val plan: Plan = Plan.FREE,
    val limitReached: Boolean = false,
    /** Opened by tapping a locked library dish. */
    val fromLibrary: Boolean = false,
    val resetsAtMillis: Long = 0L,
    val weeklyLimit: Int = UsageLimit.FREE_WEEKLY_LIMIT,
    /** Yearly savings vs. 12 monthly payments, computed from Play's real prices. */
    val yearlySavingsPercent: Int? = null,
    val restoring: Boolean = false,
    val message: PaywallMessage? = null,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    usageRepository: UsageRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val argResetsAt = savedStateHandle.get<Long>(Routes.ARG_RESETS_AT) ?: 0L
    private val fromLibrary = savedStateHandle.get<String>(Routes.ARG_REASON) == Routes.REASON_LIBRARY
    private val selected = savedStateHandle.getStateFlow(KEY_SELECTED, PlanPeriod.YEARLY.name)
    private val restoring = MutableStateFlow(false)
    private val message = MutableStateFlow<PaywallMessage?>(null)

    val uiState: StateFlow<PaywallUiState> = combine(
        billingRepository.state,
        usageRepository.usage,
        selected,
        restoring,
        message,
    ) { billing, usage, period, isRestoring, msg ->
        val now = System.currentTimeMillis()
        val resetsAt = argResetsAt.takeIf { it > 0 } ?: usage?.resetsAtMillis ?: 0L
        PaywallUiState(
            billing = billing,
            selected = PlanPeriod.valueOf(period),
            plan = usage?.plan ?: Plan.FREE,
            limitReached = argResetsAt > 0 || (usage != null && !UsageLimit.canRequest(usage, now)),
            fromLibrary = fromLibrary,
            resetsAtMillis = resetsAt,
            weeklyLimit = usage?.takeIf { it.plan == Plan.FREE }?.limit ?: UsageLimit.FREE_WEEKLY_LIMIT,
            yearlySavingsPercent = savings(billing),
            restoring = isRestoring,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallUiState())

    init {
        viewModelScope.launch { billingRepository.refreshProducts() }
        viewModelScope.launch {
            billingRepository.events.collect { event ->
                message.value = when (event) {
                    is PurchaseEvent.Success -> PaywallMessage.PURCHASE_SUCCESS
                    PurchaseEvent.Pending -> PaywallMessage.PURCHASE_PENDING
                    PurchaseEvent.Cancelled -> null
                    PurchaseEvent.VerificationFailed -> PaywallMessage.VERIFICATION_FAILED
                    is PurchaseEvent.Error -> PaywallMessage.PURCHASE_FAILED
                }
            }
        }
    }

    fun select(period: PlanPeriod) {
        savedStateHandle[KEY_SELECTED] = period.name
    }

    fun purchase(activity: Activity?) {
        val launched = activity != null && billingRepository.launchPurchase(activity, uiState.value.selected)
        if (!launched) message.value = PaywallMessage.BILLING_UNAVAILABLE
    }

    fun restore() {
        if (restoring.value) return
        viewModelScope.launch {
            restoring.value = true
            message.value = when (billingRepository.restorePurchases()) {
                RestoreResult.RESTORED -> PaywallMessage.RESTORED
                RestoreResult.NOTHING_TO_RESTORE -> PaywallMessage.NOTHING_TO_RESTORE
                RestoreResult.FAILED -> PaywallMessage.RESTORE_FAILED
            }
            restoring.value = false
        }
    }

    fun retryBilling() {
        viewModelScope.launch { billingRepository.refreshProducts() }
    }

    fun messageShown() {
        message.value = null
    }

    private fun savings(billing: BillingState): Int? {
        val monthly = billing.offers[PlanPeriod.MONTHLY] ?: return null
        val yearly = billing.offers[PlanPeriod.YEARLY] ?: return null
        if (monthly.currencyCode != yearly.currencyCode || monthly.priceMicros <= 0) return null
        val percent = ((1.0 - yearly.priceMicros.toDouble() / (monthly.priceMicros * 12.0)) * 100).roundToInt()
        return percent.takeIf { it in 1..90 }
    }

    private companion object {
        const val KEY_SELECTED = "paywall_selected"
    }
}
