package com.whattocook.app.domain.model

enum class Plan {
    FREE,
    PRO;

    companion object {
        fun fromApi(value: String?): Plan =
            if (value.equals("pro", ignoreCase = true)) PRO else FREE
    }
}

/** Server-reported usage. [resetsAtMillis] is always epoch milliseconds. */
data class UsageStatus(
    val plan: Plan,
    val used: Int,
    val limit: Int,
    val resetsAtMillis: Long,
)
