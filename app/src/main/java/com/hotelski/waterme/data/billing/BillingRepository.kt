package com.hotelski.waterme.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

interface BillingRepository {
    val purchaseUpdates: Flow<BillingPurchaseUpdate>

    suspend fun queryProducts(productIds: List<String>): BillingProductsResult

    suspend fun launchPurchase(activity: Activity, productId: String): BillingLaunchResult

    suspend fun queryUnconsumedPurchases(): BillingPurchasesResult

    suspend fun consumePurchase(purchase: Purchase): BillingConsumeResult
}

class PlayBillingRepository(
    context: Context,
) : BillingRepository, PurchasesUpdatedListener {
    private val productDetailsById = ConcurrentHashMap<String, ProductDetails>()
    private val connectionMutex = Mutex()
    private var pendingConnection: CompletableDeferred<BillingResult>? = null

    private val _purchaseUpdates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 8)
    override val purchaseUpdates: Flow<BillingPurchaseUpdate> = _purchaseUpdates.asSharedFlow()

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _purchaseUpdates.tryEmit(BillingPurchaseUpdate.Purchases(purchases.orEmpty()))
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseUpdates.tryEmit(BillingPurchaseUpdate.Canceled)
            }
            else -> {
                _purchaseUpdates.tryEmit(BillingPurchaseUpdate.Error(billingResult.asUserMessage()))
            }
        }
    }

    override suspend fun queryProducts(productIds: List<String>): BillingProductsResult {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return BillingProductsResult(errorMessage = connection.asUserMessage())
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resume(BillingProductsResult(errorMessage = billingResult.asUserMessage()))
                    return@queryProductDetailsAsync
                }

                val products = queryResult.productDetailsList.map { details ->
                    productDetailsById[details.productId] = details
                    details.toBillingProduct()
                }
                val unfetchedProductIds = queryResult.unfetchedProductList.map { product ->
                    product.productId
                }

                continuation.resume(
                    BillingProductsResult(
                        products = products,
                        unfetchedProductIds = unfetchedProductIds,
                    ),
                )
            }
        }
    }

    override suspend fun launchPurchase(
        activity: Activity,
        productId: String,
    ): BillingLaunchResult {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return BillingLaunchResult(errorMessage = connection.asUserMessage())
        }

        val details = productDetailsById[productId]
            ?: return BillingLaunchResult(errorMessage = "This support option is not available from Google Play yet.")
        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        val offerToken = details.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken
        if (!offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(offerToken)
        }

        val billingResult = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
                .build(),
        )

        return if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            BillingLaunchResult(started = true)
        } else {
            BillingLaunchResult(errorMessage = billingResult.asUserMessage())
        }
    }

    override suspend fun queryUnconsumedPurchases(): BillingPurchasesResult {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return BillingPurchasesResult(errorMessage = connection.asUserMessage())
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(BillingPurchasesResult(purchases = purchases))
                } else {
                    continuation.resume(BillingPurchasesResult(errorMessage = billingResult.asUserMessage()))
                }
            }
        }
    }

    override suspend fun consumePurchase(purchase: Purchase): BillingConsumeResult {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return BillingConsumeResult(errorMessage = connection.asUserMessage())
        }

        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.consumeAsync(params) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(BillingConsumeResult(consumed = true))
                } else {
                    continuation.resume(BillingConsumeResult(errorMessage = billingResult.asUserMessage()))
                }
            }
        }
    }

    private suspend fun ensureConnected(): BillingResult {
        if (billingClient.isReady) return okBillingResult()

        val connection = connectionMutex.withLock {
            if (billingClient.isReady) return okBillingResult()

            pendingConnection ?: CompletableDeferred<BillingResult>().also { deferred ->
                pendingConnection = deferred
                billingClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            deferred.complete(billingResult)
                        }

                        override fun onBillingServiceDisconnected() {
                            if (!deferred.isCompleted) {
                                deferred.complete(
                                    BillingResult.newBuilder()
                                        .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                                        .setDebugMessage("Google Play billing service disconnected.")
                                        .build(),
                                )
                            }
                        }
                    },
                )
            }
        }

        return connection.await().also {
            connectionMutex.withLock {
                if (pendingConnection === connection) {
                    pendingConnection = null
                }
            }
        }
    }

    private fun okBillingResult(): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()
}

data class BillingProduct(
    val productId: String,
    val formattedPrice: String,
)

data class BillingProductsResult(
    val products: List<BillingProduct> = emptyList(),
    val unfetchedProductIds: List<String> = emptyList(),
    val errorMessage: String? = null,
)

data class BillingLaunchResult(
    val started: Boolean = false,
    val errorMessage: String? = null,
)

data class BillingPurchasesResult(
    val purchases: List<Purchase> = emptyList(),
    val errorMessage: String? = null,
)

data class BillingConsumeResult(
    val consumed: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface BillingPurchaseUpdate {
    data class Purchases(val purchases: List<Purchase>) : BillingPurchaseUpdate
    data object Canceled : BillingPurchaseUpdate
    data class Error(val message: String) : BillingPurchaseUpdate
}

private fun ProductDetails.toBillingProduct(): BillingProduct =
    BillingProduct(
        productId = productId,
        formattedPrice = oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.formattedPrice
            .orEmpty(),
    )

private fun BillingResult.asUserMessage(): String =
    debugMessage.takeIf { it.isNotBlank() }
        ?: "Google Play Billing is not available right now."
