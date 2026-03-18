package com.tonemender.app.data.billing

import com.android.billingclient.api.Purchase

enum class BillingPlanType {
    MONTHLY,
    YEARLY
}

data class BillingPlan(
    val planType: BillingPlanType,
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriod: String,
    val productId: String
)

data class BillingUiModel(
    val isReady: Boolean = false,
    val isConnecting: Boolean = false,
    val isLoadingProducts: Boolean = false,
    val monthlyPlan: BillingPlan? = null,
    val yearlyPlan: BillingPlan? = null,
    val errorMessage: String? = null
)

sealed class BillingPurchaseEvent {
    data object None : BillingPurchaseEvent()
    data class Success(val purchase: Purchase) : BillingPurchaseEvent()
    data class Error(val message: String) : BillingPurchaseEvent()
    data object Cancelled : BillingPurchaseEvent()
}
