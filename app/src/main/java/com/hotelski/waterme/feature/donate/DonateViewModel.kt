package com.hotelski.waterme.feature.donate

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.billing.BillingPurchaseUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DonateViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val billingRepository = WaterMeAppContainer.billingRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        DonateUiState(
            tiers = SupportTier.entries.map { it.toUiState() },
            isLoadingProducts = true,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        observePurchaseUpdates()
        loadProducts()
    }

    fun onRetryClicked() {
        loadProducts()
    }

    fun onSupportTierClicked(
        productId: String,
        activity: Activity?,
    ) {
        if (activity == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Google Play checkout cannot open from this screen.",
                    successMessage = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPurchasing = true,
                    selectedProductId = productId,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            val result = billingRepository.launchPurchase(activity, productId)
            if (!result.started) {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        selectedProductId = null,
                        errorMessage = result.errorMessage ?: "Google Play checkout could not start.",
                    )
                }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingProducts = true,
                    errorMessage = null,
                    setupMessage = null,
                )
            }

            val result = billingRepository.queryProducts(SupportTier.productIds)
            val productsById = result.products.associateBy { it.productId }
            val tiers = SupportTier.entries.map { tier ->
                val product = productsById[tier.productId]
                tier.toUiState(
                    formattedPrice = product?.formattedPrice,
                    isAvailable = product != null,
                )
            }

            val setupMessage = when {
                result.errorMessage != null -> result.errorMessage
                result.products.isEmpty() -> ProductSetupMessage
                result.unfetchedProductIds.isNotEmpty() -> MissingProductsMessage
                else -> null
            }

            _uiState.update {
                it.copy(
                    tiers = tiers,
                    isLoadingProducts = false,
                    setupMessage = setupMessage,
                )
            }

            consumeUnfinishedPurchases()
        }
    }

    private fun observePurchaseUpdates() {
        viewModelScope.launch {
            billingRepository.purchaseUpdates.collect { update ->
                when (update) {
                    BillingPurchaseUpdate.Canceled -> {
                        _uiState.update {
                            it.copy(
                                isPurchasing = false,
                                selectedProductId = null,
                                successMessage = "Google Play checkout was canceled.",
                                errorMessage = null,
                            )
                        }
                    }
                    is BillingPurchaseUpdate.Error -> {
                        _uiState.update {
                            it.copy(
                                isPurchasing = false,
                                selectedProductId = null,
                                errorMessage = update.message,
                                successMessage = null,
                            )
                        }
                    }
                    is BillingPurchaseUpdate.Purchases -> processPurchases(update.purchases)
                }
            }
        }
    }

    private fun consumeUnfinishedPurchases() {
        viewModelScope.launch {
            val result = billingRepository.queryUnconsumedPurchases()
            if (result.errorMessage == null && result.purchases.isNotEmpty()) {
                processPurchases(result.purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        viewModelScope.launch {
            val purchased = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            val pending = purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }

            if (pending) {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        selectedProductId = null,
                        successMessage = "Google Play marked the payment as pending.",
                        errorMessage = null,
                    )
                }
            }

            if (purchased.isEmpty()) {
                return@launch
            }

            val failedConsumption = purchased
                .map { purchase -> billingRepository.consumePurchase(purchase) }
                .firstOrNull { !it.consumed }

            _uiState.update {
                if (failedConsumption == null) {
                    it.copy(
                        isPurchasing = false,
                        selectedProductId = null,
                        successMessage = "Thank you for supporting WaterMe.",
                        errorMessage = null,
                    )
                } else {
                    it.copy(
                        isPurchasing = false,
                        selectedProductId = null,
                        errorMessage = failedConsumption.errorMessage
                            ?: "The purchase completed, but WaterMe could not finish processing it.",
                        successMessage = null,
                    )
                }
            }
        }
    }
}

data class DonateUiState(
    val tiers: List<DonateTierUiState> = SupportTier.entries.map { it.toUiState() },
    val isLoadingProducts: Boolean = false,
    val isPurchasing: Boolean = false,
    val selectedProductId: String? = null,
    val setupMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class DonateTierUiState(
    val productId: String,
    val title: String,
    val description: String,
    val amount: String,
    val isAvailable: Boolean,
)

enum class SupportTier(
    val productId: String,
    val fallbackAmount: String,
    val title: String,
    val description: String,
) {
    WateringCan(
        productId = "support_water_2",
        fallbackAmount = "$2",
        title = "Watering can",
        description = "A small thank you for keeping the garden tidy.",
    ),
    FreshGrowth(
        productId = "support_growth_5",
        fallbackAmount = "$5",
        title = "Fresh growth",
        description = "Support new care quality-of-life improvements.",
    ),
    GardenBoost(
        productId = "support_garden_10",
        fallbackAmount = "$10",
        title = "Garden boost",
        description = "Help fund richer characters and extra polish.",
    );

    fun toUiState(
        formattedPrice: String? = null,
        isAvailable: Boolean = false,
    ): DonateTierUiState =
        DonateTierUiState(
            productId = productId,
            title = title,
            description = description,
            amount = formattedPrice?.takeIf { it.isNotBlank() } ?: fallbackAmount,
            isAvailable = isAvailable,
        )

    companion object {
        val productIds: List<String> = entries.map { it.productId }
    }
}

private const val ProductSetupMessage =
    "Create and activate the support products in Play Console, then install WaterMe from an internal testing track."

private const val MissingProductsMessage =
    "Some support products are not active in Play Console yet."
