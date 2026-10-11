package com.whattocook.app.data.repository

import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.UsageStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory usage counter used when BuildConfig.USE_FAKE_BACKEND is true.
 * It enforces the same 7-per-week rule as the real backend so the paywall can be tested.
 */
@Singleton
class FakeUsageRepository @Inject constructor() : UsageRepository {

    private val _usage = MutableStateFlow<UsageStatus?>(freshWeek(Plan.FREE))
    override val usage: StateFlow<UsageStatus?> = _usage.asStateFlow()

    override suspend fun refresh(): AppResult<UsageStatus> {
        delay(300)
        rollOverIfNeeded()
        return AppResult.Success(_usage.value ?: freshWeek(Plan.FREE).also { _usage.value = it })
    }

    /** Called by [FakeRecipeRepository] for each generation. */
    fun consumeOne(): AppResult<UsageStatus> {
        rollOverIfNeeded()
        val current = _usage.value ?: freshWeek(Plan.FREE)
        if (!UsageLimit.canRequest(current, System.currentTimeMillis())) {
            return AppResult.Failure(AppError.LimitReached(current.resetsAtMillis))
        }
        val updated = current.copy(used = current.used + 1)
        _usage.value = updated
        return AppResult.Success(updated)
    }

    override fun onUsageReported(used: Int, limit: Int, resetsAtMillis: Long) {
        _usage.update { it?.copy(used = used, limit = limit, resetsAtMillis = resetsAtMillis) }
    }

    override fun onLimitReached(resetsAtMillis: Long) {
        _usage.update { it?.copy(used = it.limit) }
    }

    override suspend fun verifyPurchase(purchaseToken: String, productId: String): AppResult<Plan> {
        delay(500)
        _usage.value = freshWeek(Plan.PRO)
        return AppResult.Success(Plan.PRO)
    }

    override fun clear() {
        _usage.value = freshWeek(Plan.FREE)
    }

    private fun rollOverIfNeeded() {
        val current = _usage.value ?: return
        if (current.resetsAtMillis <= System.currentTimeMillis()) {
            _usage.value = freshWeek(current.plan)
        }
    }

    private fun freshWeek(plan: Plan): UsageStatus {
        val nextMonday = LocalDate.now()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val limit = if (plan == Plan.PRO) PRO_DEMO_LIMIT else UsageLimit.FREE_WEEKLY_LIMIT
        return UsageStatus(plan = plan, used = 0, limit = limit, resetsAtMillis = nextMonday)
    }

    private companion object {
        /** Demo-only number; the real Pro allowance comes from your backend. */
        const val PRO_DEMO_LIMIT = 100
    }
}
