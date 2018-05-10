package com.edgedevstudio.testoutbilling

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import com.edgedevstudio.testoutbilling.MainActivity.Companion.TAG

/**
 * Created by opeyemi on 24/04/2018.
 */
class BillingManager(val activity: Activity, val billingUpdatesListener: BillingUpdatesListener) : PurchasesUpdatedListener {
    private val billingClient: BillingClient
    private val verifiedPurchaseList = ArrayList<Purchase>()

    init {
        billingClient = BillingClient.newBuilder(activity).setListener(this).build()
        queryPurchases()
    }

    private fun startServiceConnectionIfNeeded(runnable: Runnable?) {
        if (billingClient.isReady) {
            runnable?.run()
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(responseCode: Int) {
                    Log.i(TAG, "onBillingSetupFinished")
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        Log.i(TAG, "Response Code == OK")
                        runnable?.run()
                    } else {
                        Log.i(TAG, "Response Code != OK, RESPONSE CODE = $responseCode")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.i(TAG, "onBillingServiceDisconnected")
                }
            })
        }

    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        verifiedPurchaseList.clear()
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
        billingUpdatesListener.onPurchaseUpdated(verifiedPurchaseList, responseCode)
    }

    fun launchPurchaseFlow(skuId: String, @BillingClient.SkuType skuType: String) {
        val billingParams = BillingFlowParams.newBuilder().setSku(skuId).setType(skuType).build()
        val runnable = Runnable {
            billingClient.launchBillingFlow(activity, billingParams)
        }
        startServiceConnectionIfNeeded(runnable)
    }

    fun queryPurchases() {
        val purchaseQueryRunnable = Runnable {
            Log.d(TAG, "query Purchases")
            verifiedPurchaseList.clear() // We cleared the verified purchase list, we'll talk more about this in sTAe 4
            val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP) // querying for in app purchases

            // if response was good
            if (purchasesResult.responseCode == BillingClient.BillingResponse.OK) {
                purchasesResult.purchasesList.addAll(purchasesResult.purchasesList)
            }

            // Not all clients support subscriptions so we have to check
            // If there are subscriptions supported, we add subscription rows as well
            if (areSubscriptionsSupported()) {
                val subscriptionResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                if (subscriptionResult.responseCode == BillingClient.BillingResponse.OK) {
                        //a succinct way of adding all elements to a list instead of using for each loop
                        purchasesResult.purchasesList.addAll(subscriptionResult.purchasesList)
                }
            } else {
                Log.d(TAG, "Subscription are not supported for this client!")
            }
            if (purchasesResult != null)
                for (purchase in purchasesResult.purchasesList) {
                    handlePurchase(purchase)
                }
            billingUpdatesListener.onQueryPurchasesFinished(verifiedPurchaseList)
        }
        startServiceConnectionIfNeeded(purchaseQueryRunnable)
    }

    private fun handlePurchase(purchase: Purchase?) {

    }

    fun areSubscriptionsSupported(): Boolean {
        // Checks if subscriptions are supported for current client
        val responseCode = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (responseCode != BillingClient.BillingResponse.OK) {
            Log.i(TAG, "got an error response: " + responseCode)
        }
        return responseCode == BillingClient.BillingResponse.OK
    }

    interface BillingUpdatesListener {
        fun onPurchaseUpdated(purchases: List<Purchase>, responseCode: Int)

        fun onConsumeFinished(token: String, @BillingClient.BillingResponse responseCode: Int)

        fun onQueryPurchasesFinished(purchases: List<Purchase>)

    }

    fun destroyBillingClient() {
        billingClient.endConnection()
    }
}