package com.edgedevstudio.testoutbilling

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView


class MainActivity : AppCompatActivity(), View.OnClickListener, BillingManager.BillingUpdatesListener {
    companion object {
        val TAG = "MainActivity"
    }

    private var remove_ads_perm_btn: Button? = null
    private var donate_btn: Button? = null
    private var sub_monthly: Button? = null
    private var sub_yearly: Button? = null
    private var billingManager: BillingManager? = null

    private val REMOVE_ADS_PERMANENTLY_SKU_ID = "remove_ads"
    private val DONATE_SKU_ID = "donate"
    private val MONTH_SUB_SKU_ID = "monthly_sub"
    private val YEAR_SUB_SKU_ID = "yearly_sub"
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adView = findViewById<View>(R.id.adView) as AdView
        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)

        remove_ads_perm_btn = findViewById(R.id.remove_ads_btn)
        donate_btn = findViewById(R.id.donate_btn)
        sub_monthly = findViewById(R.id.monthly_sub_btn)
        sub_yearly = findViewById(R.id.yearly_sub_btn)

        remove_ads_perm_btn?.setOnClickListener(this)
        donate_btn?.setOnClickListener(this)
        sub_monthly?.setOnClickListener(this)
        sub_yearly?.setOnClickListener(this)

        billingManager = BillingManager(this, this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.remove_ads_btn -> startPurchaseFlow(REMOVE_ADS_PERMANENTLY_SKU_ID, BillingClient.SkuType.INAPP)
            R.id.donate_btn -> startPurchaseFlow(DONATE_SKU_ID, BillingClient.SkuType.INAPP)
            R.id.monthly_sub_btn -> startPurchaseFlow(MONTH_SUB_SKU_ID, BillingClient.SkuType.SUBS)
            R.id.yearly_sub_btn -> startPurchaseFlow(YEAR_SUB_SKU_ID, BillingClient.SkuType.SUBS)
        }
    }

    fun startPurchaseFlow(skuId: String, @BillingClient.SkuType skuType: String) {
        billingManager?.launchPurchaseFlow(skuId, skuType)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager?.destroyBillingClient()
    }


    override fun onPurchaseUpdated(purchases: List<Purchase>, responseCode: Int) {
        Log.i(TAG, "onPurchaseUpdated, responseCode = $responseCode, size of purchases = ${purchases.size}")
        displayMsgForPurchasesCheck(purchases)
    }

    override fun onConsumeFinished(responseCode: Int, token: String?) {
        Log.i(TAG, "onConsumePurchase Successful > BillingResponseCode = $responseCode, token = $token")
        showToast("Thank You for Donating!\nYou may consider donating a few more times to see consumption of In-app purchases in action")
    }

    override fun onQueryPurchasesFinished(purchases: List<Purchase>) {
        Log.i(TAG, "onQueryPurchasesFinished, size of verified Purchases = ${purchases.size}")
        displayMsgForPurchasesCheck(purchases)

    }

    private fun displayMsgForPurchasesCheck(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (DONATE_SKU_ID.equals(purchase.sku)) {
                billingManager?.consumePurchase(purchase)
            } else {
                adView?.visibility = View.GONE
                showToast("Thank You for Purchase! Ads have been Eliminated!")
            }
        }
    }

    private fun showToast(msg: String, length: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, msg, length).show()
    }
}
