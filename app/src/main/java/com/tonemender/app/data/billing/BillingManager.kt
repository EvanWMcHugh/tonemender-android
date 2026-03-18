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
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    private var cachedProductDetails: ProductDetails? = null

    fun connect(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            _uiModel.value = _uiModel.value.copy(
                isReady = true,
                isConnecting = false,
                errorMessage = null
            )
            onConnected?.invoke()
            queryExistingPurchases()
            return
        }

        _uiModel.value = _uiModel.value.copy(
            isConnecting = true,
            errorMessage = null
        )

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    _uiModel.value = _uiModel.value.copy(
                        isReady = true,
                        isConnecting = false,
                        errorMessage = null
                    )
                    queryExistingPurchases()
                    onConnected?.invoke()
                } else {
                    _uiModel.value = _uiModel.value.copy(
                        isReady = false,
                        isConnecting = false,
                        errorMessage = billingResult.debugMessage.ifBlank {
                            "Billing setup failed."
                        }
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiModel.value = _uiModel.value.copy(
                    isReady = false,
                    isConnecting = false,
                    errorMessage = "Billing service disconnected."
                )
            }
        })
    }

    fun loadProducts() {
        if (!billingClient.isReady) {
            connect {
                loadProducts()
            }
            return
        }

        _uiModel.value = _uiModel.value.copy(
            isLoadingProducts = true,
            errorMessage = null
        )

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
                _uiModel.value = _uiModel.value.copy(
                    isLoadingProducts = false,
                    errorMessage = billingResult.debugMessage.ifBlank {
                        "Could not load subscription options."
                    }
                )
                return@queryProductDetailsAsync
            }

            val productDetails = queryResult.productDetailsList.firstOrNull()

            if (productDetails == null) {
                _uiModel.value = _uiModel.value.copy(
                    isLoadingProducts = false,
                    errorMessage = "No subscription product found."
                )
                return@queryProductDetailsAsync
            }

            cachedProductDetails = productDetails

            val subscriptionOffers = productDetails.subscriptionOfferDetails.orEmpty()

            val monthlyOffer = subscriptionOffers.find { it.basePlanId == MONTHLY_BASE_PLAN_ID }
            val yearlyOffer = subscriptionOffers.find { it.basePlanId == YEARLY_BASE_PLAN_ID }

            val monthlyPlan = monthlyOffer?.toBillingPlan(
                planType = BillingPlanType.MONTHLY,
                productDetails = productDetails
            )

            val yearlyPlan = yearlyOffer?.toBillingPlan(
                planType = BillingPlanType.YEARLY,
                productDetails = productDetails
            )

            _uiModel.value = _uiModel.value.copy(
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

    fun launchPurchase(activity: Activity, planType: BillingPlanType): Boolean {
        val productDetails = cachedProductDetails
        if (productDetails == null) {
            _purchaseEvent.value =
                BillingPurchaseEvent.Error("Subscription details are not loaded yet.")
            return false
        }

        val selectedPlan = when (planType) {
            BillingPlanType.MONTHLY -> _uiModel.value.monthlyPlan
            BillingPlanType.YEARLY -> _uiModel.value.yearlyPlan
        }

        if (selectedPlan == null) {
            _purchaseEvent.value = BillingPurchaseEvent.Error("Selected plan is unavailable.")
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
            _purchaseEvent.value = BillingPurchaseEvent.Error(
                billingResult.debugMessage.ifBlank { "Could not launch purchase flow." }
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
                    _purchaseEvent.value = BillingPurchaseEvent.Success(purchase)
                } else {
                    _purchaseEvent.value = BillingPurchaseEvent.Error(
                        "Purchase completed but no purchase data was returned."
                    )
                }
            }

            BillingResponseCode.USER_CANCELED -> {
                _purchaseEvent.value = BillingPurchaseEvent.Cancelled
            }

            else -> {
                _purchaseEvent.value = BillingPurchaseEvent.Error(
                    billingResult.debugMessage.ifBlank { "Purchase failed." }
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

            _purchaseEvent.value = BillingPurchaseEvent.Success(existing)
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
                    billingResult.debugMessage.ifBlank { "Could not acknowledge purchase." }
                )
            }
        }
    }

    fun consumePurchaseEvent() {
        _purchaseEvent.value = BillingPurchaseEvent.None
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
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