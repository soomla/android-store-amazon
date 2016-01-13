/*
 * Copyright (C) 2012 Soomla Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soomla.store.billing.amazon;

import com.soomla.SoomlaUtils;
import com.soomla.store.billing.IIabService;
import com.soomla.store.billing.IabCallbacks;
import com.soomla.store.billing.IabException;
import com.soomla.store.billing.IabHelper;
import com.soomla.store.billing.IabResult;
import com.soomla.store.billing.IabInventory;
import com.soomla.store.billing.IabPurchase;
import com.soomla.store.billing.IabSkuDetails;
import com.soomla.store.domain.PurchasableVirtualItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the Amazon plugin implementation of IIabService.
 *
 * see parent for more docs.
 */
public class AmazonIabService implements IIabService {

    public static final String VERSION = "2.0.6";

    /**
     * see parent
     */
    @Override
    public void initializeBillingService(final IabCallbacks.IabInitListener iabListener) {
        // Set up helper for the first time, querying and synchronizing inventory
        startIabHelper(new OnIabSetupFinishedListener(iabListener));
    }

    /**
     * see parent
     */
    @Override
    public void startIabServiceInBg(IabCallbacks.IabInitListener iabListener) {
        startIabHelper(new OnIabSetupFinishedListener(iabListener));
    }

    /**
     * see parent
     */
    @Override
    public void stopIabServiceInBg(IabCallbacks.IabInitListener iabListener) {
        SoomlaUtils.LogDebug(TAG, "stopIabServiceInBg method is not supported for Amazon IAP.");
    }

    /**
     * see parent
     */
    @Override
    public void configVerifyPurchases(Map<String, Object> verifyPurchases) {
        throw new UnsupportedOperationException("Purchase verification is not supported for Amazon");
    }

    /**
     * see parent
     */
    @Override
    public boolean shouldVerifyPurchases() {
        return false;
    }

    /**
     * see parent
     */
    @Override
    public void restorePurchasesAsync(IabCallbacks.OnRestorePurchasesListener restorePurchasesListener) {
        mAmazonIabHelper.restorePurchasesAsync(new RestorePurchasesFinishedListener(restorePurchasesListener));
    }

    /**
     * see parent
     */
    @Override
    public void fetchSkusDetailsAsync(List<String> skus, IabCallbacks.OnFetchSkusDetailsListener fetchSkusDetailsListener) {
        mAmazonIabHelper.fetchSkusDetailsAsync(skus, new FetchSkusDetailsFinishedListener(fetchSkusDetailsListener));
    }

    /**
     * see parent
     */
    @Override
    public boolean isIabServiceInitialized() {
        return (mAmazonIabHelper != null && mAmazonIabHelper.isSetupDone());
    }

    /**
     * see parent
     */
    @Override
    public void consume(IabPurchase purchase) throws IabException{
        SoomlaUtils.LogDebug(TAG, "consume method is not supported for Amazon IAP.");
    }

    /**
     * see parent
     */
    @Override
    public void consumeAsync(IabPurchase purchase, final IabCallbacks.OnConsumeListener consumeListener) {
        SoomlaUtils.LogDebug(TAG, "consumeAsync method is not supported for Amazon IAP.");
        consumeListener.success(purchase);
    }

    /**
     * see parent
     */
    @Override
    public void launchPurchaseFlow(String sku,
                                   final IabCallbacks.OnPurchaseListener purchaseListener,
                                   String extraData) {

        mAmazonIabHelper.launchPurchaseFlow(null, sku, new IabHelper.OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, IabPurchase purchase) {

                /**
                 * Wait to see if the purchase succeeded, then start the consumption process.
                 */
                SoomlaUtils.LogDebug(TAG, "IabPurchase finished: " + result + ", purchase: " + purchase);
                switch (result.getResponse()) {
                    case IabResult.BILLING_RESPONSE_RESULT_OK:
                        purchaseListener.success(purchase);
                        break;
                    case IabResult.BILLING_RESPONSE_RESULT_USER_CANCELED:
                        purchaseListener.cancelled(purchase);
                        break;
                    case IabResult.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
                        purchaseListener.alreadyOwned(purchase);
                        break;
                    default:
                        purchaseListener.fail(result.getMessage());
                        break;
                }
            }
        }, extraData);
    }



    /*====================   Private Utility Methods   ====================*/

    /**
     * Create a new IAB helper and set it up.
     *
     * @param onIabSetupFinishedListener is a callback that lets users to add their own implementation for when the Iab is started
     */
    private synchronized void startIabHelper(OnIabSetupFinishedListener onIabSetupFinishedListener) {
        if (mAmazonIabHelper == null) {
            SoomlaUtils.LogDebug(TAG, "Creating Purchasing Observer.");
            mAmazonIabHelper = new AmazonIabHelper();
        }

        mAmazonIabHelper.startSetup(onIabSetupFinishedListener);
    }

    /**
     * Handle Restore Purchases processes
     */
    private class RestorePurchasesFinishedListener implements IabHelper.RestorePurchasessFinishedListener {


        private IabCallbacks.OnRestorePurchasesListener mRestorePurchasesListener;

        public RestorePurchasesFinishedListener(IabCallbacks.OnRestorePurchasesListener restorePurchasesListener) {
            this.mRestorePurchasesListener            = restorePurchasesListener;
        }

        @Override
        public void onRestorePurchasessFinished(IabResult result, IabInventory inventory) {
            SoomlaUtils.LogDebug(TAG, "Restore Purchases succeeded");
            if (result.getResponse() == IabResult.BILLING_RESPONSE_RESULT_OK && mRestorePurchasesListener != null) {
                // fetching owned items
                List<String> itemSkus = inventory.getAllOwnedSkus(IabHelper.ITEM_TYPE_INAPP);
                List<IabPurchase> purchases = new ArrayList<IabPurchase>();
                for (String sku : itemSkus) {
                    IabPurchase purchase = inventory.getPurchase(sku);
                    purchases.add(purchase);
                }

                this.mRestorePurchasesListener.success(purchases);
            } else {
                SoomlaUtils.LogError(TAG, "Wither mRestorePurchasesListener==null OR Restore purchases error: " + result.getMessage());
                if (this.mRestorePurchasesListener != null) this.mRestorePurchasesListener.fail(result.getMessage());
            }
        }
    }

    /**
     * Handle Fetch Skus Details processes
     */
    private class FetchSkusDetailsFinishedListener implements IabHelper.FetchSkusDetailsFinishedListener {


        private IabCallbacks.OnFetchSkusDetailsListener mFetchSkusDetailsListener;

        public FetchSkusDetailsFinishedListener(IabCallbacks.OnFetchSkusDetailsListener fetchSkusDetailsListener) {
            this.mFetchSkusDetailsListener            = fetchSkusDetailsListener;
        }

        @Override
        public void onFetchSkusDetailsFinished(IabResult result, IabInventory inventory) {
            SoomlaUtils.LogDebug(TAG, "Restore Purchases succeeded");
            if (result.getResponse() == IabResult.BILLING_RESPONSE_RESULT_OK && mFetchSkusDetailsListener != null) {

                // @lassic (May 1st): actually, here (query finished) it only makes sense to get the details
                // of the SKUs we already queried for
                List<String> skuList = inventory.getAllQueriedSkus(false);
                List<IabSkuDetails> skuDetails = new ArrayList<IabSkuDetails>();
                for (String sku : skuList) {
                    IabSkuDetails skuDetail = inventory.getSkuDetails(sku);
                    if (skuDetail != null) {
                        skuDetails.add(skuDetail);
                    }
                }

                this.mFetchSkusDetailsListener.success(skuDetails);
            } else {
                SoomlaUtils.LogError(TAG, "Wither mFetchSkusDetailsListener==null OR Fetching details error: " + result.getMessage());
                if (this.mFetchSkusDetailsListener != null) this.mFetchSkusDetailsListener.fail(result.getMessage());
            }
        }
    }


    /**
     * Handle Setup processes finished
     */
    private class OnIabSetupFinishedListener implements IabHelper.OnIabSetupFinishedListener {

        private IabCallbacks.IabInitListener mIabInitListener;

        public IabCallbacks.IabInitListener getIabInitListener() {
            return mIabInitListener;
        }

        public OnIabSetupFinishedListener(IabCallbacks.IabInitListener iabListener) {
            this.mIabInitListener = iabListener;
        }

        @Override
        public void onIabSetupFinished(IabResult result) {

            SoomlaUtils.LogDebug(TAG, "IAB helper Setup finished.");
            if (result.isFailure()) {
                if (mIabInitListener != null) mIabInitListener.fail(result.getMessage());
                return;
            }
            if (mIabInitListener != null) mIabInitListener.success(false);
        }
    }


    /* Private Members */
    private static final String TAG = "SOOMLA AmazonIabService";
    private AmazonIabHelper mAmazonIabHelper;
}
