package com.edgedevstudio.testoutbilling

import android.app.Activity
import android.util.Base64
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.util.BillingHelper
import com.edgedevstudio.testoutbilling.MainActivity.Companion.TAG
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Created by opeyemi on 24/04/2018.
 */
class BillingManager(val activity: Activity, val billingUpdatesListener: BillingUpdatesListener) : PurchasesUpdatedListener {
    private val billingClient: BillingClient
    private val verifiedPurchases = ArrayList<Purchase>()
    private val BASE_64_ENCODED_PUBLIC_KEY = "your_base_64_encoded_public_key"
    private val SIGNATURE_ALGORITHM = "SHA1withRSA"
    private val KEY_FACTORY_ALGORITHM = "RSA"

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
                    Log.d(TAG, "onBillingSetupFinished, responseCode = $responseCode")
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        runnable?.run()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.d(TAG, "onBillingServiceDisconnected")
                }
            })
        }

    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        verifiedPurchases.clear()
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
        billingUpdatesListener.onPurchaseUpdated(verifiedPurchases, responseCode)
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
            Log.d(TAG, "querying Purchases")
            verifiedPurchases.clear() // We cleared the verified purchase list, we'll talk more about this in sTAe 4
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
            Log.d(TAG, "found ${purchasesResult.purchasesList.size} unverified products")
            for (purchase in purchasesResult.purchasesList) {
                handlePurchase(purchase)
            }
            billingUpdatesListener.onQueryPurchasesFinished(verifiedPurchases)
        }
        startServiceConnectionIfNeeded(purchaseQueryRunnable)
    }

    // Checks if subscriptions are supported for current client
    fun areSubscriptionsSupported(): Boolean {
        val responseCode = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        return responseCode == BillingClient.BillingResponse.OK
    }

    interface BillingUpdatesListener {
        fun onPurchaseUpdated(purchases: List<Purchase>, responseCode: Int)

        fun onConsumeFinished(@BillingClient.BillingResponse responseCode: Int, token: String?)

        fun onQueryPurchasesFinished(purchases: List<Purchase>)

    }

    private fun handlePurchase(purchase: Purchase) {
        if (isValidSignature(purchase.originalJson, purchase.signature)) {
            verifiedPurchases.add(purchase)
        }
    }

    private fun isValidSignature(signedData: String, signature: String): Boolean {

        val publicKey = generatePublicKey(BASE_64_ENCODED_PUBLIC_KEY)


        val signatureBytes: ByteArray
        try {
            signatureBytes = Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            BillingHelper.logWarn(TAG, "Base64 decoding failed.")
            return false
        }

        try {
            val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            if (!signatureAlgorithm.verify(signatureBytes)) {
                BillingHelper.logWarn(TAG, "Signature verification failed.")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            BillingHelper.logWarn(TAG, "Invalid key specification.")
        } catch (e: SignatureException) {
            BillingHelper.logWarn(TAG, "Signature exception.")
        }

        return false
    }

    @Throws(IOException::class)
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            val msg = "key specification: $e"
            Log.d(TAG, msg)
            BillingHelper.logWarn(TAG, msg)
            throw IOException(msg)
        }
    }

    fun destroyBillingClient() {
        billingClient.endConnection()
    }

    fun consumePurchase(purchase: Purchase) {
        val consumePurchaseRunnable = Runnable {
            billingClient.consumeAsync(purchase.purchaseToken, ConsumeResponseListener { responseCode, purchaseToken ->
                if (responseCode == BillingClient.BillingResponse.OK) {
                    billingUpdatesListener.onConsumeFinished(responseCode, purchaseToken)
                }
            })
        }
        startServiceConnectionIfNeeded(consumePurchaseRunnable)
    }
}