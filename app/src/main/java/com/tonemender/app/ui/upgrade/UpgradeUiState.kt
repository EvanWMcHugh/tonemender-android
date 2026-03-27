package com.tonemender.app.ui.upgrade

data class UpgradeUiState(
    val title: String = "Upgrade to Pro",
    val description: String = "Unlock unlimited rewrites and premium features.",
    val productName: String = "ToneMender Pro",
    val monthlyPriceLabel: String = "",
    val yearlyPriceLabel: String = "",
    val selectedPlan: UpgradePlan = UpgradePlan.MONTHLY,
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null
) {
    val hasMonthlyPlan: Boolean
        get() = monthlyPriceLabel.isNotBlank()

    val hasYearlyPlan: Boolean
        get() = yearlyPriceLabel.isNotBlank()

    val hasPlans: Boolean
        get() = hasMonthlyPlan || hasYearlyPlan
}
enum class UpgradePlan {
    MONTHLY,
    YEARLY
}