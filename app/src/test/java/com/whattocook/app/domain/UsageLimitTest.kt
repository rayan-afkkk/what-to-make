package com.whattocook.app.domain

import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.UsageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageLimitTest {

    private val now = 1_700_000_000_000L
    private val hour = 60L * 60 * 1000
    private val day = 24 * hour

    private fun status(used: Int, limit: Int = 7, resetsAt: Long = now + 3 * day, plan: Plan = Plan.FREE) =
        UsageStatus(plan = plan, used = used, limit = limit, resetsAtMillis = resetsAt)

    @Test
    fun `fresh week shows all recipes left and a full bar`() {
        val summary = UsageLimit.summarize(status(used = 0), now)
        assertEquals(7, summary.remaining)
        assertEquals(1f, summary.remainingFraction, 0.0001f)
        assertEquals(3, summary.daysUntilReset)
        assertFalse(summary.isLimitReached)
    }

    @Test
    fun `partially used week`() {
        val summary = UsageLimit.summarize(status(used = 3), now)
        assertEquals(4, summary.remaining)
        assertEquals(4f / 7f, summary.remainingFraction, 0.0001f)
        assertFalse(summary.isLimitReached)
    }

    @Test
    fun `all seven used at once hits the limit`() {
        val summary = UsageLimit.summarize(status(used = 7), now)
        assertEquals(0, summary.remaining)
        assertEquals(0f, summary.remainingFraction, 0.0001f)
        assertTrue(summary.isLimitReached)
        assertFalse(UsageLimit.canRequest(status(used = 7), now))
    }

    @Test
    fun `over-use never shows negative numbers`() {
        val summary = UsageLimit.summarize(status(used = 9), now)
        assertEquals(0, summary.remaining)
        assertTrue(summary.isLimitReached)
    }

    @Test
    fun `once the reset date passes the week is fresh again`() {
        val expired = status(used = 7, resetsAt = now - 1_000)
        val summary = UsageLimit.summarize(expired, now)
        assertEquals(7, summary.remaining)
        assertEquals(0, summary.daysUntilReset)
        assertTrue(UsageLimit.canRequest(expired, now))
    }

    @Test
    fun `days until reset round up`() {
        assertEquals(1, UsageLimit.daysUntil(now + hour, now))
        assertEquals(1, UsageLimit.daysUntil(now + day, now))
        assertEquals(2, UsageLimit.daysUntil(now + day + 1, now))
        assertEquals(0, UsageLimit.daysUntil(now - day, now))
    }

    @Test
    fun `unknown usage lets the server decide`() {
        assertTrue(UsageLimit.canRequest(null, now))
    }

    @Test
    fun `zero limit is treated as reached`() {
        val summary = UsageLimit.summarize(status(used = 0, limit = 0), now)
        assertEquals(0f, summary.remainingFraction, 0.0001f)
        assertTrue(summary.isLimitReached)
    }

    @Test
    fun `reset timestamps in seconds are converted to milliseconds`() {
        assertEquals(1_700_000_000_000L, UsageLimit.normalizeEpochMillis(1_700_000_000L))
        assertEquals(1_700_000_000_000L, UsageLimit.normalizeEpochMillis(1_700_000_000_000L))
        assertEquals(0L, UsageLimit.normalizeEpochMillis(0L))
    }
}
