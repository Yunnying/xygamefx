package com.shxy.gamesdk.Billing;

import static java.lang.Math.max;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.BaseSdk.BaseSdk;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

/**
 * @author: 翟宇翔
 * @date: 2023/9/20
 * 类说明：实现Google play支付（内购）功能
 */

enum BillingState{
    UnReady,
    Ready,
    QueryINAPP,
    QuerySUSB,
    Purchasing,
    RestoringINAPP,
    RestoringSUBS
}

public class BillingManager {
    private static final String TAG = "BillingLib";
    private static BillingClient billingClient = null;
    private static boolean hasInited = false;//初始化标记位
    private static Activity mActivity = null;
    /*private static boolean isConnecting = false;//BillingClient是否连接，仅内部判断使用*/
    private static HashMap<String, ProductDetails> mInappDetailsList = new HashMap<>();//从GooglePlay获取所有Inapp类型物品的详细信息
    private static HashMap<String, ProductDetails> mSubsDetailsList = new HashMap<>();//从GooglePlay获取所有订阅的详细信息
    private static HashMap<String, Purchase> mPurchasesList = new HashMap<>();//将用户购买的信息存在这个HashMap中
    private static HashSet<String> mConsumeIds = new HashSet<>();//储存consume类型的产品id
    private static HashSet<String> mNonConsumeIds = new HashSet<>();//储存non-consume类型的产品id
    private static HashSet<String> mSubscribeIds = new HashSet<>();//储存订阅类型的产品id
    private static BillingState mBillingState = BillingState.UnReady;

    private static int mInPurchaseCount = 0;

    private static final String PREIOD_P1W = "P1W";
    private static final String PREIOD_P4W = "P4W";
    private static final String PREIOD_P1M = "P1M";
    private static final String PREIOD_P3M = "P3M";
    private static final String PREIOD_P6M = "P6M";
    private static final String PREIOD_P1Y = "P1Y";

    /*-------------------------------------------------------------------------- 初始化Billing -----------------------------------------------------------------*/

    /**
     * 初始化BillingClient
     * @param activity 调用billing的activity
     * @param productIds 初始化应用的内购Id列表使用的字符串
     */
    public static void initBilling(Activity activity, String productIds){
        mActivity = activity;
        initProductIds(productIds);
        billingClient = BillingClient.newBuilder(mActivity.getApplicationContext())
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        hasInited = true;
    }

    /**
     * 初始化应用的id列表，将传入的字符串中的产品id提取并分别储存到对应的List中
     * @param productIds 以字符串的形式传递所有的产品id，格式为“consumeid1&consumeid2|nonconsumeid1&nonconsumeid2|subscribeid1&subscribeid2”
     */
    private static void initProductIds(String productIds){
        int gidx = 0;

        for(String sub_skus : productIds.split("\\|")){
            for(String sku : sub_skus.split("&")){
                Log.i(TAG, "In class " + gidx + ", The product id is: " + sku);
                if(sku.length() == 0){
                    continue;
                }
                switch (gidx){
                    case 0: mConsumeIds.add(sku); break;
                    case 1: mNonConsumeIds.add(sku); break;
                    case 2: mSubscribeIds.add(sku); break;
                }
            }
            ++gidx;
        }
    }

    /**
     * 初始化BillingClient和GooglePlay的连接,连接成功后，会自动查询物品信息
     */
    private static void startConnection(){
        if (!isBillingClientReady()) {
            Log.d(TAG, "BillingClient: Start connection...");

            billingClient.startConnection(new BillingClientStateListener() {
                //连接成功后从GooglePlay获取商品详情；当连接状态有错误时，根据不同responseCode进行处理
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    int responseCode = billingResult.getResponseCode();
                    String debugMessage = billingResult.getDebugMessage();
                    Log.d(TAG, "onBillingSetupFinished: " + responseCode + " " + debugMessage);
                    if (responseCode == BillingClient.BillingResponseCode.OK) {

                        if(mBillingState == BillingState.QueryINAPP){
                            queryInappProductDetails();
                        }
                        else if (mBillingState == BillingState.QuerySUSB) {
                            querySubsProductDetails();
                        }
                        else if (mBillingState == BillingState.RestoringINAPP){
                            queryInappStates();
                        }
                        else if (mBillingState ==BillingState.RestoringSUBS){
                            querySubsStates();
                        }
                        //说明初始化支付流程成功，可以自定义打点或者进行其他操作
                        mBillingState = BillingState.Ready;

                    } else{
                        //其他情况返回给游戏，由游戏处理
                        mBillingState = BillingState.UnReady;

                    }
                    BillingSdk.onConnectionFinished(responseCode);
                }

                //连接失败时返回，需要尝试重连，谨慎处理，有可能造成ANR或者CRASH
                @Override
                public void onBillingServiceDisconnected() {
                    Log.e(TAG, "onBillingServiceDisconnected");
                    //retryBillingServiceConnection();
                    BillingSdk.onConnectionLost();
                }
            });
        }else{
            Log.e(TAG, "BillingClient is already set up.");
        }
    }

    /*------------------------------------------------------------------------ 入口查询函数 ------------------------------------------------------------------------*/
    /**
     * 从GooglePlay获取INAPP类商品详情，在购买INAPP类型商品前使用
     */
    public static void queryInappProductDetails(){
        mActivity.runOnUiThread(()->{
            if(hasGetInappProductDetails()){
                return;
            }
            mBillingState = BillingState.QueryINAPP;
            if(isBillingClientReady()){
                ArrayList<QueryProductDetailsParams.Product> products = new ArrayList<>();
                for(String productId : mConsumeIds){
                    Log.d(TAG,"Consume Id is " + productId);
                    products.add(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build());
                }
                for(String productId : mNonConsumeIds){
                    Log.d(TAG,"NonConsume Id is " + productId);
                    products.add(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build());
                }
                QueryProductDetailsParams queryProductDetailsParams =
                        QueryProductDetailsParams.newBuilder()
                                .setProductList(products)
                                .build();
                mBillingState = BillingState.QueryINAPP;
                billingClient.queryProductDetailsAsync(queryProductDetailsParams,productDetailsResponseListener);
            }else{
                startConnection();
            }
        });
    }

    /**
     * 从GooglePlay获取SUBS类商品详情，在进行订阅前使用
     */
    public static void querySubsProductDetails(){
        mActivity.runOnUiThread(()->{
            //mSubscribeIds为空时，直接返回不进行查询
            if(mSubscribeIds.isEmpty() || hasGetSubsProductDetails()){
                return;
            }
            mBillingState = BillingState.QuerySUSB;
            if(isBillingClientReady()){
                ArrayList<QueryProductDetailsParams.Product> products = new ArrayList<>();
                for(String productId : mSubscribeIds){
                    Log.d(TAG,"Subscribe Id is " + productId);
                    products.add(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build());
                }
                QueryProductDetailsParams queryProductDetailsParams =
                        QueryProductDetailsParams.newBuilder()
                                .setProductList(products)
                                .build();

                billingClient.queryProductDetailsAsync(queryProductDetailsParams,productDetailsResponseListener);
            }else{
                startConnection();
            }
        });
    }

    /**
     * queryInappProductDetails和querySubsProductDetails的结果监听函数
     */
    private static ProductDetailsResponseListener productDetailsResponseListener = new ProductDetailsResponseListener(){
        @Override
        public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
            int responseCode = billingResult.getResponseCode();
            String debugMessage = billingResult.getDebugMessage();
            Log.d(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);

            if(responseCode == BillingClient.BillingResponseCode.OK){
                for(ProductDetails productDetails:productDetailsList){
                    String productId = productDetails.getProductId();
                    Log.i(TAG,"ProductRequestListener Product Id: " + productId);

                    if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS))
                    {
                        mSubsDetailsList.put(productId, productDetails);
                    }
                    else if (productDetails.getProductType().equals(BillingClient.ProductType.INAPP))
                    {
                        Log.d(TAG,"Put" + productId + "In to List!");
                        mInappDetailsList.put(productId, productDetails);
                    }
                }
                mBillingState = BillingState.Ready;
                BillingSdk.onQueryProductDetailsSuccess();
            }else{
                BillingSdk.onQueryProductDetailsFailed(responseCode);
            }
        }
    };


    /**
     * 查询INAPP类型商品的购买状态，用于恢复INAPP类型的购买
     */
    public static void queryInappStates(){
        mActivity.runOnUiThread(()->{
            mBillingState = BillingState.RestoringINAPP;
            if(isBillingClientReady()){
                billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), purchasesResponseListener);
            }else{
                startConnection();
            }
        });
    }

    /**
     * 查询SUBS类型商品的购买状态，用于恢复订阅
     */
    public static void querySubsStates(){
        mActivity.runOnUiThread(()->{
            mBillingState = BillingState.RestoringSUBS;
            if(isBillingClientReady()){
                billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), purchasesResponseListener);
            }else{
                startConnection();
            }
        });
    }

    /**
     * 监听queryInappStates和querySubsStates的处理结果
     */
    private static final PurchasesResponseListener purchasesResponseListener = new PurchasesResponseListener(){

        @Override
        public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
            if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK){
                for (Purchase purchase:purchases){
                    handlePurchase(purchase);
                }
                BillingSdk.onHandleQueryPurchasesSuccess();
            }else{
                //未收到购买信息或者responseCode不为ok，返回responseCode
                BillingSdk.onHandleQueryPurchasesFailed(billingResult.getResponseCode());
            }
        }
    };


    /*--------------------------------------------------------------------------- 工具函数 --------------------------------------------------------------------*/
    /**
     * 返回BillingClient是否已经成功连接到GooglePlay
     * @return 为true时BillingClient已经连接到GooglePlay，否则未连接
     */
    public static boolean isBillingClientReady(){
        Log.e(TAG, "BillingClient isReady: " + billingClient.isReady());
        return billingClient.isReady();
    }

    /**
     * INAPP类型的产品详情列表是否完成初始化
     * @return 为true时完成了初始化，否则未完成
     */
    private static boolean hasGetInappProductDetails(){
        return !mInappDetailsList.isEmpty();
    }

    /**
     * SUBS类型的产品详情列表是否完成初始化
     * @return 为true时完成了初始化，否则未完成
     */
    private static boolean hasGetSubsProductDetails(){
        return !mSubsDetailsList.isEmpty();
    }

    /**
     * 判断是否是Consume类型的商品
     * @param productId 商品id
     * @return 为true时是Consume类型的商品；为false时不是
     */
    private static boolean isConsumeProduct(String productId){
        return mConsumeIds.contains(productId);
    }

    /**
     * 判断是否是NonConsume类型的商品
     * @param productId 商品id
     * @return 为true时是NonConsume类型的商品；为false时不是
     */
    private static boolean isNonConsumeProduct(String productId){
        return mNonConsumeIds.contains(productId);
    }

    /**
     * 判断是否时订阅类型的商品
     * @param productId 商品id
     * @return 为true时该商品为订阅；为false时不是
     */
    private static boolean isSubscribeProduct(String productId) {
        return mSubscribeIds.contains(productId);
    }

    /* --------------------------------------------------------------------------------------- 购买函数 -----------------------------------------------------------------------*/
    /**
     * 购买商品，将购买事件上传给GooglePlay，得到反馈后启动支付弹窗，在调用前应该检查BillingClient的初始化情况以及其是否就绪
     * @param productId 购买商品的Id
     */
    public static void startBilling(String productId){
        mActivity.runOnUiThread(()->{
            //检查是否已经完成了初始化
            if(!isBillingClientReady()){
                return;
            }
            ProductDetails details = null;
            //获取产品的细节，并以此构建billingFlowParam
            if(isConsumeProduct(productId) || isNonConsumeProduct(productId)){
                details = mInappDetailsList.get(productId);
            }else{
                details = mSubsDetailsList.get(productId);
            }

            if(details == null){
                Log.e(TAG,"Get product details fails. ProductId is " + productId);
                return;
            }
            BillingFlowParams.ProductDetailsParams productDetailsParams;
            if(isSubscribeProduct(productId)){
                if(details.getSubscriptionOfferDetails() != null)
                {
                    String offerToken = details.getSubscriptionOfferDetails().get(0).getOfferToken();

                    productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offerToken)
                            .build();
                }else{
                    Log.e(TAG,"Get Subscription Offer Details fails. ProductId is " + productId);
                    return;
                }
            }else {
                productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build();
            }
            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList= new ArrayList<>();
            productDetailsParamsList.add(productDetailsParams);
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();
            //发起购买请求
            BillingResult billingResult = billingClient.launchBillingFlow(mActivity, billingFlowParams);
            //请求失败处理
            if(billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK){
                Log.e(TAG,"launchBillingFlow fails.");
                BillingSdk.onLaunchBillingFlowFailed(productId, billingResult.getResponseCode());
            }
        });

    }

    /**
     * startBilling(launchBillingFlow)的监听方法
     * 使用此方法监听购买交易更新
     */
    private static final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
            mInPurchaseCount++;
            int responseCode = billingResult.getResponseCode();
            String debugMessage = billingResult.getDebugMessage();
            Log.d(TAG, "onPurchasesUpdated: $responseCode $debugMessage" + responseCode + " " + debugMessage);
            if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else {
                BillingSdk.onPurchasesUpdatedFailed(responseCode);
            }
        }
    };

    /**
     * 处理普通商品类/订阅类的成功购买结果，再次向服务器验证是否已经购买成功
     */
    private static void handlePurchase(final Purchase purchase) {
        //首先获取购买信息
        //mPurchasesList.put(purchase.getOrderId(),purchase);
        mPurchasesList.put(purchase.getProducts().get(0),purchase);
        String productId = purchase.getProducts().get(0);
        /*if(mProductDetail == null){
            Log.e(TAG, "handlePurchase: ProductDetail of " + productId + " is empty!");
            return;
        }*/
        //根据购买状态进行处理
        //购买成功，进行验证和后续操作
        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
            mInPurchaseCount--;
            BaseSdk.setIntegerForKey("In_Purchase_count", mInPurchaseCount);

            ProductDetails mProductDetail = null;
            if(isConsumeProduct(productId) || isNonConsumeProduct(productId)){
                mProductDetail = mInappDetailsList.get(productId);
            }else{
                mProductDetail = mSubsDetailsList.get(productId);

            }
            if(!purchase.isAcknowledged()){
                Log.d(TAG, "handlePurchase: Begin Acknowledge");
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                ProductDetails finalMProductDetail = mProductDetail;
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG,"handlePurchase: Acknowledge Success");
                            if (isSubscribeProduct(productId)) {
                                //如果是订阅产品，更新到期时间后进行成功购买回调
                                updateSubscriptionExpireTime(purchase);
                            }else{
                                assert finalMProductDetail != null;
                                Log.i(TAG, "handlePurchase: Product tile is: " + finalMProductDetail.getTitle());
                                if(!finalMProductDetail.getTitle().toLowerCase().contains("test".toLowerCase())){
                                    double price = finalMProductDetail.getOneTimePurchaseOfferDetails().getPriceAmountMicros()/1000000.0;
                                    String currencyCode = finalMProductDetail.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
                                    AdjustSdk.trackPurchaseRevenue(price,currencyCode);
                                }
                            }

                            BillingSdk.onHandlePurchaseSuccess(productId);
                        }else{
                            Log.d(TAG,"handlePurchase: Acknowledge Fail");
                            BillingSdk.onHandlePurchaseFailed(productId,billingResult.getResponseCode());
                        }
                    }
                });
            } else {
                Log.d(TAG, "handlePurchase: Has Acknowledged");
                if(isSubscribeProduct(productId)){
                    //如果是订阅产品，需要更新到期时间
                    updateSubscriptionExpireTime(purchase);
                }else{
                    assert mProductDetail != null;
                    Log.i(TAG, "handlePurchase: Product tile is: " + mProductDetail.getTitle());
                    if(!mProductDetail.getTitle().toLowerCase().contains("test".toLowerCase())){
                        double price = mProductDetail.getOneTimePurchaseOfferDetails().getPriceAmountMicros()/1000000.0;
                        String currencyCode = mProductDetail.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
                        AdjustSdk.trackPurchaseRevenue(price,currencyCode);
                    }
                }
                BillingSdk.onHandlePurchaseSuccess(productId);
            }
        }else {
            BillingSdk.onHandlePurchaseState(productId,purchase.getPurchaseState());
        }
    }

    /**
     * 处理consume类型的购买
     * @param productId 产品id
     */
    public static void handleConsumePurchase(String productId){
        Purchase purchase = mPurchasesList.get(productId);
        Log.d(TAG, "handleConsumePurchase: " + mPurchasesList);
        if(purchase != null){
            ConsumeParams consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            ConsumeResponseListener listener = new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(BillingResult billingResult, @NonNull String purchaseToken) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        //返回商品购买状态
                        Log.d(TAG, "handleConsumePurchase: Consume " + productId + " Success!");
                        BillingSdk.onHandleConsumePurchaseSuccess(productId);
                    }else{
                        Log.d(TAG, "handleConsumePurchase: Consume " + productId + " Fail!");
                        BillingSdk.onHandleConsumePurchaseFailed(productId,billingResult.getResponseCode());
                    }
                }
            };
            if(billingClient!= null){
                billingClient.consumeAsync(consumeParams, listener);//consume这个商品
            }
        }else{
            Log.d(TAG, "handleConsumePurchase: Purchase is Empty!");
        }
    }

    /**
     * 更新订阅到期时间
     * @param purchase 订阅的购买信息
     */
    private static void updateSubscriptionExpireTime(Purchase purchase){
        for(String productId : purchase.getProducts()){
            ProductDetails productDetails = mSubsDetailsList.get(productId);
            if(productDetails != null && productDetails.getProductType().equals(BillingClient.ProductType.SUBS)){
                ProductDetails.SubscriptionOfferDetails subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails().get(0);
                //subscriptionOfferDetails.getOfferTags("sada");
                ProductDetails.PricingPhase pricingPhase = subscriptionOfferDetails.getPricingPhases().getPricingPhaseList().get(0);

                //根据付款时间和有效期限计算到期时间
                long purchaseTimeInMillis = purchase.getPurchaseTime();
                long curSubExpireTimeInMillis = getSubscribeExpiresTimeInMillis(productId);
                long newSubStartTimeInMillis = max(purchaseTimeInMillis, curSubExpireTimeInMillis);
                String period = pricingPhase.getBillingPeriod();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getDefault());
                calendar.setTimeInMillis(newSubStartTimeInMillis);

                switch (period) {
                    case PREIOD_P1W:
                        calendar.add(Calendar.WEEK_OF_MONTH, 1);
                        break;
                    case PREIOD_P4W:
                        calendar.add(Calendar.WEEK_OF_MONTH, 4);
                        break;
                    case PREIOD_P1M:
                        calendar.add(Calendar.MONTH, 1);
                        break;
                    case PREIOD_P3M:
                        calendar.add(Calendar.MONTH, 3);
                        break;
                    case PREIOD_P6M:
                        calendar.add(Calendar.MONTH, 6);
                        break;
                    case PREIOD_P1Y:
                        calendar.add(Calendar.YEAR, 1);
                        break;
                }

                long expireTimeInMillis = calendar.getTimeInMillis();

                setLocalSubExpireTimeInMillis(productId, expireTimeInMillis);
            }
        }
    }

    private static void setLocalSubExpireTimeInMillis(String productId, long val){
        String nKey = productId + "_expire_time";
        BaseSdk.setLongForKey(nKey, val);
    }

    public static long getSubscribeExpiresTimeInMillis(String productId){
        String nKey = productId + "_expire_time";
        return BaseSdk.getLongForKey(nKey, 0);
    }

}
