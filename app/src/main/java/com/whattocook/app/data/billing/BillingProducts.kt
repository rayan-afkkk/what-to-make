package com.whattocook.app.data.billing

/**
 * Play Console product configuration. Create ONE subscription with TWO base plans
 * and put the exact IDs here. Prices are never hardcoded: the UI shows the
 * localized price that Google Play returns for each base plan.
 */
object BillingProducts {
    /** Subscription product ID in Play Console (Monetize > Subscriptions). */
    const val PRO_SUBSCRIPTION = "what_to_cook_pro"

    /** Base plan IDs inside that subscription. */
    const val BASE_PLAN_MONTHLY = "pro-monthly"
    const val BASE_PLAN_YEARLY = "pro-yearly"
}
