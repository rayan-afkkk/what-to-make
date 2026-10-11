package com.whattocook.app.domain

import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.UsageStatus
import kotlin.math.ceil
import kotlin.math.max

/** Everything the UI needs to render the usage counter, the progress bar and the paywall. */
data class UsageSummary(
    val plan: Plan,
    val used: Int,
    val limit: Int,
    val remaining: Int,
    /** Fraction of the allowance still available, 0f..1f (a full bar means nothing used). */
    val remainingFraction: Float,
    val daysUntilReset: Int,
    val isLimitReached: Boolean,
    val resetsAtMillis: Long,
)

object UsageLimit {

    const val FREE_WEEKLY_LIMIT = 7
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Anything below this is treated as epoch seconds (1e11 ms is in 1973). */
    private const val SECONDS_THRESHOLD = 100_000_000_000L

    fun summarize(status: UsageStatus, nowMillis: Long): UsageSummary {
        val resetPassed = status.resetsAtMillis in 1..nowMillis
        // Once the reset date has passed the server will start a new week; show it as fresh.
        val used = if (resetPassed) 0 else status.used.coerceAtLeast(0)
        val limit = status.limit.coerceAtLeast(0)
        val remaining = max(0, limit - used)
        return UsageSummary(
            plan = status.plan,
            used = used,
            limit = limit,
            remaining = remaining,
            remainingFraction = if (limit == 0) 0f else remaining.toFloat() / limit,
            daysUntilReset = daysUntil(status.resetsAtMillis, nowMillis),
            isLimitReached = remaining == 0,
            resetsAtMillis = status.resetsAtMillis,
        )
    }

    /** Whole days until [resetsAtMillis], rounded up so "in 3 hours" reads as 1 day. */
    fun daysUntil(resetsAtMillis: Long, nowMillis: Long): Int {
        if (resetsAtMillis <= nowMillis) return 0
        return ceil((resetsAtMillis - nowMillis).toDouble() / DAY_MILLIS).toInt()
    }

    /** True when a new recipe request is allowed. Unknown usage defers to the server. */
    fun canRequest(status: UsageStatus?, nowMillis: Long): Boolean {
        if (status == null) return true
        return !summarize(status, nowMillis).isLimitReached
    }

    /** The backend may send seconds or milliseconds; always return milliseconds. */
    fun normalizeEpochMillis(value: Long): Long =
        if (value in 1 until SECONDS_THRESHOLD) value * 1000 else value
}
