package com.whattocook.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.di.ApplicationScope
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Google Play Billing for the Pro subscription. Every purchase is verified by your
 * backend (POST /verify-purchase) before the app treats the user as Pro.
 */
@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val usageRepository: UsageRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : BillingRepository, PurchasesUpdatedListener {

    private val _state = MutableStateFlow(BillingState())
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 8)
    override val events: Flow<PurchaseEvent> = _events.asSharedFlow()

    private val connectMutex = Mutex()
    private var productDetails: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private suspend fun ensureConnected(): Boolean = connectMutex.withLock {
        if (client.isReady) return true
        suspendCancellableCoroutine<Boolean> { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    override suspend fun refreshProducts() {
        _state.value = _state.value.copy(isLoading = true)
        if (!ensureConnected()) {
            _state.value = BillingState(isLoading = false, isAvailable = false)
            return
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProducts.PRO_SUBSCRIPTION)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        val details = result.productDetailsList?.firstOrNull()
        productDetails = details
        _state.value = BillingState(
            isLoading = false,
            isAvailable = details != null,
            offers = details?.let(::offersFrom) ?: emptyMap(),
        )
    }

    private fun offersFrom(details: ProductDetails): Map<PlanPeriod, PlanOffer> {
        val offers = details.subscriptionOfferDetails.orEmpty()
        fun offerFor(basePlanId: String, period: PlanPeriod): PlanOffer? {
            // Use the plain base plan (no promotional offer) so the price matches the plan.
            val offer = offers.firstOrNull { it.basePlanId == basePlanId && it.offerId == null }
                ?: offers.firstOrNull { it.basePlanId == basePlanId }
                ?: return null
            val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return null
            return PlanOffer(
                period = period,
                formattedPrice = phase.formattedPrice,
                priceMicros = phase.priceAmountMicros,
                currencyCode = phase.priceCurrencyCode,
                offerToken = offer.offerToken,
            )
        }
        return buildMap<PlanPeriod, PlanOffer> {
            offerFor(BillingProducts.BASE_PLAN_MONTHLY, PlanPeriod.MONTHLY)?.let { put(PlanPeriod.MONTHLY, it) }
            offerFor(BillingProducts.BASE_PLAN_YEARLY, PlanPeriod.YEARLY)?.let { put(PlanPeriod.YEARLY, it) }
        }
    }

    override fun launchPurchase(activity: Activity, period: PlanPeriod): Boolean {
        val details = productDetails ?: return false
        val offer = _state.value.offers[period] ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        .build(),
                ),
            )
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                purchases.orEmpty().forEach { purchase -> _events.emit(handlePurchase(purchase)) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(PurchaseEvent.Cancelled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch {
                _events.emit(
                    if (restorePurchases() == RestoreResult.RESTORED) PurchaseEvent.Success(Plan.PRO)
                    else PurchaseEvent.Error(result.responseCode),
                )
            }
            else -> _events.tryEmit(PurchaseEvent.Error(result.responseCode))
        }
    }

    private suspend fun handlePurchase(purchase: Purchase): PurchaseEvent {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) return PurchaseEvent.Pending
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return PurchaseEvent.Cancelled

        val productId = purchase.products.firstOrNull() ?: BillingProducts.PRO_SUBSCRIPTION
        return when (val verified = usageRepository.verifyPurchase(purchase.purchaseToken, productId)) {
            is AppResult.Success -> {
                if (!purchase.isAcknowledged) {
                    val ack = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val ackResult = client.acknowledgePurchase(ack)
                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "Acknowledge failed: ${ackResult.debugMessage}")
                    }
                }
                PurchaseEvent.Success(verified.data)
            }
            is AppResult.Failure -> PurchaseEvent.VerificationFailed
        }
    }

    override suspend fun restorePurchases(): RestoreResult {
        if (!ensureConnected()) return RestoreResult.FAILED
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return RestoreResult.FAILED
        }
        val active = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active.isEmpty()) return RestoreResult.NOTHING_TO_RESTORE
        val outcomes = active.map { handlePurchase(it) }
        return if (outcomes.any { it is PurchaseEvent.Success }) RestoreResult.RESTORED else RestoreResult.FAILED
    }

    private companion object {
        const val TAG = "Billing"
    }
}
