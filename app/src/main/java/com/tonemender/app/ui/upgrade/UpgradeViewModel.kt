package com.tonemender.app.ui.upgrade

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.tonemender.app.data.billing.BillingManager
import com.tonemender.app.data.billing.BillingPlanType
import com.tonemender.app.data.billing.BillingPurchaseEvent
import com.tonemender.app.data.local.session.SessionStore
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.ui.common.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpgradeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val billingManager = BillingManager(application.applicationContext)
    private val authRepository = AuthRepository()
    private val sessionStore = SessionStore(application.applicationContext)

    private val _uiState = MutableStateFlow(UpgradeUiState())
    val uiState: StateFlow<UpgradeUiState> = _uiState

    init {
        observeBilling()
        billingManager.connect {
            billingManager.loadProducts()
        }
    }

    private fun observeBilling() {
        viewModelScope.launch {
            billingManager.uiModel.collect { model ->
                _uiState.value = _uiState.value.copy(
                    monthlyPriceLabel = model.monthlyPlan?.formattedPrice ?: "",
                    yearlyPriceLabel = model.yearlyPlan?.formattedPrice ?: "",
                    isLoading = model.isConnecting || model.isLoadingProducts,
                    errorMessage = model.errorMessage
                )
            }
        }

        viewModelScope.launch {
            billingManager.purchaseEvent.collect { event ->
                when (event) {
                    BillingPurchaseEvent.None -> Unit

                    BillingPurchaseEvent.Cancelled -> {
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            errorMessage = null
                        )
                        UiMessageManager.showMessage("Purchase cancelled.")
                        billingManager.consumePurchaseEvent()
                    }

                    is BillingPurchaseEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            errorMessage = event.message
                        )
                        billingManager.consumePurchaseEvent()
                    }

                    is BillingPurchaseEvent.Success -> {
                        handleSuccessfulPurchase(event.purchase)
                        billingManager.consumePurchaseEvent()
                    }
                }
            }
        }
    }

    private fun handleSuccessfulPurchase(purchase: Purchase) {
        billingManager.acknowledgePurchase(purchase) { acknowledged, acknowledgeMessage ->
            if (!acknowledged) {
                _uiState.value = _uiState.value.copy(
                    isPurchasing = false,
                    errorMessage = acknowledgeMessage ?: "Could not acknowledge purchase."
                )
                return@acknowledgePurchase
            }

            viewModelScope.launch {
                try {
                    val purchaseToken = purchase.purchaseToken
                    val productId = purchase.products.firstOrNull()

                    if (purchaseToken.isBlank() || productId.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            errorMessage = "Missing purchase details."
                        )
                        return@launch
                    }

                    val basePlanId = when (_uiState.value.selectedPlan) {
                        UpgradePlan.MONTHLY -> BillingManager.MONTHLY_BASE_PLAN_ID
                        UpgradePlan.YEARLY -> BillingManager.YEARLY_BASE_PLAN_ID
                    }

                    val response = authRepository.verifyGooglePlayPurchase(
                        purchaseToken = purchaseToken,
                        productId = productId,
                        basePlanId = basePlanId
                    )

                    if (response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            errorMessage = null
                        )

                        sessionStore.triggerRefresh()
                        UiMessageManager.showMessage("Purchase verified successfully.")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Purchase verification failed."
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        errorMessage = e.message ?: "Purchase verification failed."
                    )
                }
            }
        }
    }

    fun selectPlan(plan: UpgradePlan) {
        _uiState.value = _uiState.value.copy(
            selectedPlan = plan,
            errorMessage = null
        )
    }

    fun startPurchase(activity: Activity) {
        val state = _uiState.value
        if (state.isLoading || state.isPurchasing) return

        val billingPlanType = when (state.selectedPlan) {
            UpgradePlan.MONTHLY -> BillingPlanType.MONTHLY
            UpgradePlan.YEARLY -> BillingPlanType.YEARLY
        }

        _uiState.value = state.copy(
            isPurchasing = true,
            errorMessage = null
        )

        val launched = billingManager.launchPurchase(activity, billingPlanType)
        if (!launched) {
            _uiState.value = _uiState.value.copy(
                isPurchasing = false,
                errorMessage = "Could not start purchase flow."
            )
        }
    }

    fun refreshPlans() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
        billingManager.loadProducts()
    }

    override fun onCleared() {
        billingManager.endConnection()
        super.onCleared()
    }
}