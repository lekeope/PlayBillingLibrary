package com.edgedevstudio.testoutbilling

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

class MainActivity : AppCompatActivity(), View.OnClickListener, BillingManager.BillingUpdatesListener {
    companion object {
        val TAG = "MainActivity"
    }
    var buy_gas: Button? = null
    var buy_car: Button? = null
    var sub_monthly: Button? = null
    var sub_yearly: Button? = null
    var billingManager: BillingManager? = null

    val BUY_GAS_SKU_ID = "buy_gas"
    val BUY_CAR_SKU_ID = "buy_car"
    val MONTH_SUB_SKU_ID = "monthly_sub"
    val YEAR_SUB_SKU_ID = "yearly_sub"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buy_gas = findViewById(R.id.remove_ads_btn)
        buy_car = findViewById(R.id.donate_btn)
        sub_monthly = findViewById(R.id.monthly_sub_btn)
        sub_yearly = findViewById(R.id.yearly_sub_btn)

        buy_gas?.setOnClickListener(this)
        buy_car?.setOnClickListener(this)
        sub_monthly?.setOnClickListener(this)
        sub_yearly?.setOnClickListener(this)

        billingManager = BillingManager(this, this)

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.remove_ads_btn -> startPurchaseFlow(BUY_GAS_SKU_ID, BillingClient.SkuType.INAPP)
            R.id.donate_btn -> startPurchaseFlow(BUY_CAR_SKU_ID, BillingClient.SkuType.INAPP)
            R.id.monthly_sub_btn -> startPurchaseFlow(MONTH_SUB_SKU_ID, BillingClient.SkuType.SUBS)
            R.id.yearly_sub_btn -> startPurchaseFlow(YEAR_SUB_SKU_ID, BillingClient.SkuType.SUBS)
        }
    }

    fun startPurchaseFlow(skuId: String, @BillingClient.SkuType skuType: String) {
        billingManager?.launchPurchaseFlow(skuId, skuType)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
        billingManager?.destroyBillingClient()
    }


    override fun onPurchaseUpdated(purchases: List<Purchase>, responseCode: Int) {
        Log.i(TAG, "onPurchaseUpdated, responseCode = $responseCode, size of purchases = ${purchases.size}")
        for (purchase in purchases) {
            Log.i(TAG, purchase.toString())
        }

    }

    override fun onConsumeFinished(token: String, responseCode: Int) {
        Log.i(TAG, "onConsumeFinished")
        Log.i(TAG, "BillingResponseCode = $responseCode")
        Log.i(TAG, "token = $token")

    }

    override fun onQueryPurchasesFinished(purchases: List<Purchase>) {
        Log.i(TAG, "onQueryPurchasesFinished, size of Purchases = ${purchases.size}")
        for (purchase in purchases) {
            Log.i(TAG, purchase.toString())
        }
    }
}
