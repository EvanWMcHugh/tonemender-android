package com.tonemender.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    context: Context
) : PurchasesUpdatedListener {

    companion object {
        const val SUBSCRIPTION_PRODUCT_ID = "tonemender_pro"
        const val MONTHLY_BASE_PLAN_ID = "monthly"
        const val YEARLY_BASE_PLAN_ID = "yearly"
    }

    private val appContext = context.applicationContext

    private val _uiModel = MutableStateFlow(BillingUiModel())
    val uiModel: StateFlow<BillingUiModel> = _uiModel.asStateFlow()

    private val _purchaseEvent = MutableStateFlow<BillingPurchaseEvent>(BillingPurchaseEvent.None)
    val purchaseEvent: StateFlow<BillingPurchaseEvent> = _purchaseEvent.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private var cachedProductDetails: ProductDetails? = null

    fun connect(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            updateUiModel {
                copy(
                    isReady = true,
                    isConnecting = false,
                    errorMessage = null
                )
            }
            queryExistingPurchases()
            onConnected?.invoke()
            return
        }

        updateUiModel {
            copy(
                isConnecting = true,
                errorMessage = null
            )
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    updateUiModel {
                        copy(
                            isReady = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                    queryExistingPurchases()
                    onConnected?.invoke()
                } else {
                    updateUiModel {
                        copy(
                            isReady = false,
                            isConnecting = false,
                            errorMessage = billingResult.debugMessage.ifBlank {
                                "Billing setup failed."
                            }
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                updateUiModel {
                    copy(
                        isReady = false,
                        isConnecting = false,
                        errorMessage = "Billing service disconnected."
                    )
                }
            }
        })
    }

    fun loadProducts() {
        if (!billingClient.isReady) {
            connect { loadProducts() }
            return
        }

        updateUiModel {
            copy(
                isLoadingProducts = true,
                errorMessage = null
            )
        }

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, queryResult ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                updateUiModel {
                    copy(
                        isLoadingProducts = false,
                        errorMessage = billingResult.debugMessage.ifBlank {
                            "Could not load subscription options."
                        }
                    )
                }
                return@queryProductDetailsAsync
            }

            val productDetails = queryResult.productDetailsList.firstOrNull()
            if (productDetails == null) {
                cachedProductDetails = null
                updateUiModel {
                    copy(
                        isLoadingProducts = false,
                        monthlyPlan = null,
                        yearlyPlan = null,
                        errorMessage = "No subscription product found."
                    )
                }
                return@queryProductDetailsAsync
            }

            cachedProductDetails = productDetails

            val subscriptionOffers = productDetails.subscriptionOfferDetails.orEmpty()

            val monthlyPlan = subscriptionOffers
                .find { it.basePlanId == MONTHLY_BASE_PLAN_ID }
                ?.toBillingPlan(
                    planType = BillingPlanType.MONTHLY,
                    productDetails = productDetails
                )

            val yearlyPlan = subscriptionOffers
                .find { it.basePlanId == YEARLY_BASE_PLAN_ID }
                ?.toBillingPlan(
                    planType = BillingPlanType.YEARLY,
                    productDetails = productDetails
                )

            updateUiModel {
                copy(
                    isLoadingProducts = false,
                    monthlyPlan = monthlyPlan,
                    yearlyPlan = yearlyPlan,
                    errorMessage = if (monthlyPlan == null && yearlyPlan == null) {
                        "No valid subscription plans found."
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun launchPurchase(activity: Activity, planType: BillingPlanType): Boolean {
        val productDetails = cachedProductDetails
        if (productDetails == null) {
            emitPurchaseEvent(
                BillingPurchaseEvent.Error("Subscription details are not loaded yet.")
            )
            return false
        }

        val selectedPlan = when (planType) {
            BillingPlanType.MONTHLY -> _uiModel.value.monthlyPlan
            BillingPlanType.YEARLY -> _uiModel.value.yearlyPlan
        }

        if (selectedPlan == null) {
            emitPurchaseEvent(
                BillingPurchaseEvent.Error("Selected plan is unavailable.")
            )
            return false
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(selectedPlan.offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingResponseCode.OK) {
            emitPurchaseEvent(
                BillingPurchaseEvent.Error(
                    billingResult.debugMessage.ifBlank {
                        "Could not launch purchase flow."
                    }
                )
            )
            return false
        }

        return true
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null) {
                    emitPurchaseEvent(BillingPurchaseEvent.Success(purchase))
                } else {
                    emitPurchaseEvent(
                        BillingPurchaseEvent.Error(
                            "Purchase completed but no purchase data was returned."
                        )
                    )
                }
            }

            BillingResponseCode.USER_CANCELED -> {
                emitPurchaseEvent(BillingPurchaseEvent.Cancelled)
            }

            else -> {
                emitPurchaseEvent(
                    BillingPurchaseEvent.Error(
                        billingResult.debugMessage.ifBlank { "Purchase failed." }
                    )
                )
            }
        }
    }

    private fun queryExistingPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync

            val existing = purchases.firstOrNull { purchase ->
                purchase.products.contains(SUBSCRIPTION_PRODUCT_ID)
            } ?: return@queryPurchasesAsync

            emitPurchaseEvent(BillingPurchaseEvent.Success(existing))
        }
    }

    fun acknowledgePurchase(
        purchase: Purchase,
        onAcknowledged: (success: Boolean, message: String?) -> Unit
    ) {
        if (purchase.isAcknowledged) {
            onAcknowledged(true, null)
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                onAcknowledged(true, null)
            } else {
                onAcknowledged(
                    false,
                    billingResult.debugMessage.ifBlank {
                        "Could not acknowledge purchase."
                    }
                )
            }
        }
    }

    fun consumePurchaseEvent() {
        emitPurchaseEvent(BillingPurchaseEvent.None)
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun emitPurchaseEvent(event: BillingPurchaseEvent) {
        _purchaseEvent.value = event
    }

    private inline fun updateUiModel(
        transform: BillingUiModel.() -> BillingUiModel
    ) {
        _uiModel.value = _uiModel.value.transform()
    }

    private fun ProductDetails.SubscriptionOfferDetails.toBillingPlan(
        planType: BillingPlanType,
        productDetails: ProductDetails
    ): BillingPlan? {
        val pricingPhase = pricingPhases.pricingPhaseList.firstOrNull() ?: return null

        return BillingPlan(
            planType = planType,
            basePlanId = basePlanId,
            offerToken = offerToken,
            formattedPrice = pricingPhase.formattedPrice,
            billingPeriod = pricingPhase.billingPeriod,
            productId = productDetails.productId
        )
    }
}