package com.whattocook.app.data.billing

import android.app.Activity
import com.whattocook.app.domain.model.Plan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class PlanPeriod { MONTHLY, YEARLY }

/** A purchasable base plan with the price string exactly as Google Play formatted it. */
data class PlanOffer(
    val period: PlanPeriod,
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    val offerToken: String,
)

data class BillingState(
    val isLoading: Boolean = true,
    val isAvailable: Boolean = false,
    val offers: Map<PlanPeriod, PlanOffer> = emptyMap(),
)

sealed interface PurchaseEvent {
    data class Success(val plan: Plan) : PurchaseEvent
    data object Pending : PurchaseEvent
    data object Cancelled : PurchaseEvent
    data object VerificationFailed : PurchaseEvent
    data class Error(val code: Int) : PurchaseEvent
}

enum class RestoreResult { RESTORED, NOTHING_TO_RESTORE, FAILED }

interface BillingRepository {
    val state: StateFlow<BillingState>
    val events: Flow<PurchaseEvent>
    suspend fun refreshProducts()
    /** Opens the Google Play purchase sheet. Returns false if it could not be shown. */
    fun launchPurchase(activity: Activity, period: PlanPeriod): Boolean
    suspend fun restorePurchases(): RestoreResult
}
