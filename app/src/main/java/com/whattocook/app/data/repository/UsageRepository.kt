package com.whattocook.app.data.repository

import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.UsageStatus
import kotlinx.coroutines.flow.StateFlow

/** Usage counting lives on the server; this holds the last known value. */
interface UsageRepository {
    val usage: StateFlow<UsageStatus?>
    suspend fun refresh(): AppResult<UsageStatus>
    fun onUsageReported(used: Int, limit: Int, resetsAtMillis: Long)
    fun onLimitReached(resetsAtMillis: Long)
    suspend fun verifyPurchase(purchaseToken: String, productId: String): AppResult<Plan>
    fun clear()
}
