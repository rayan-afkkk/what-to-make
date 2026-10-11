package com.whattocook.app.data.repository

import com.whattocook.app.data.remote.ApiCaller
import com.whattocook.app.data.remote.ApiService
import com.whattocook.app.data.remote.dto.VerifyPurchaseRequestDto
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.UsageStatus
import com.whattocook.app.domain.model.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class DefaultUsageRepository @Inject constructor(
    private val api: ApiService,
    private val apiCaller: ApiCaller,
) : UsageRepository {

    private val _usage = MutableStateFlow<UsageStatus?>(null)
    override val usage: StateFlow<UsageStatus?> = _usage.asStateFlow()

    override suspend fun refresh(): AppResult<UsageStatus> {
        val result = apiCaller.call { api.usage() }.map {
            UsageStatus(
                plan = Plan.fromApi(it.plan),
                used = it.used,
                limit = it.limit,
                resetsAtMillis = UsageLimit.normalizeEpochMillis(it.resetsAt),
            )
        }
        if (result is AppResult.Success) _usage.value = result.data
        return result
    }

    override fun onUsageReported(used: Int, limit: Int, resetsAtMillis: Long) {
        _usage.update { current ->
            (current ?: UsageStatus(Plan.FREE, used, limit, resetsAtMillis))
                .copy(used = used, limit = limit, resetsAtMillis = resetsAtMillis)
        }
    }

    override fun onLimitReached(resetsAtMillis: Long) {
        _usage.update { current ->
            val base = current ?: UsageStatus(
                Plan.FREE, UsageLimit.FREE_WEEKLY_LIMIT, UsageLimit.FREE_WEEKLY_LIMIT, resetsAtMillis,
            )
            base.copy(
                used = maxOf(base.used, base.limit),
                resetsAtMillis = if (resetsAtMillis > 0) resetsAtMillis else base.resetsAtMillis,
            )
        }
    }

    override suspend fun verifyPurchase(purchaseToken: String, productId: String): AppResult<Plan> {
        val result = apiCaller.call {
            api.verifyPurchase(VerifyPurchaseRequestDto(purchaseToken, productId))
        }.map { Plan.fromApi(it.plan) }
        if (result is AppResult.Success) {
            _usage.update { it?.copy(plan = result.data) }
            refresh()
        }
        return result
    }

    override fun clear() {
        _usage.value = null
    }
}
