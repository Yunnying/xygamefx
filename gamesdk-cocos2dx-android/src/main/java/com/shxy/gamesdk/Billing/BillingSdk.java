package com.shxy.gamesdk.Billing;
import android.app.Activity;

import com.shxy.gamesdk.BaseSdk.BaseSdk;

/**
 * @author: 翟宇翔
 * @date: 2023/9/20
 * billing_version = "6.0.1"
 * 本sdk提供了使用GooglePlay结算服务的一系列方法，我们推荐在使用本sdk前首先阅读谷歌的官方文档：<a href="https://developer.android.com/google/play/billing/integrate?hl=zh-cn">...</a>。
 */
public class BillingSdk {
    /* ---------------------------------- Callback JNI Functions ------------------------------------*/

    /**
     * BillingClient尝试连接到GooglePlay回调
     * @param responseCode 为0时说明连接成功，其他情况均为连接失败，请参考 https://developer.android.com/google/play/billing/errors?hl=zh-cn
     */
    public static void onConnectionFinished(int responseCode){
        BaseSdk.runOnGLThread(()->{
            onConnectionFinishedNative(responseCode);
        });
    }
    private static native void onConnectionFinishedNative(int responseCode);


    /**
     * BillingClient与GooglePlay断开连接回调
     */
    public static void onConnectionLost(){
        BaseSdk.runOnGLThread(()->{
            onConnectionLostNative();
        });
    }
    private static native void onConnectionLostNative();


    /**
     * 查询商品列表成功回调
     */
    public static void onQueryProductDetailsSuccess(){
        BaseSdk.runOnGLThread(()->{
            onQueryProductDetailsSuccessNative();
        });
    }
    private static native void onQueryProductDetailsSuccessNative();


    /**
     * 查询商品列表失败回调，错误代码含义请参考 https://developer.android.com/google/play/billing/errors?hl=zh-cn
     * @param errorCode 回调失败的错误代码
     */
    public static void onQueryProductDetailsFailed(int errorCode){
        BaseSdk.runOnGLThread(()->{
            onQueryProductDetailsFailedNative(errorCode);
        });
    }
    private static native void onQueryProductDetailsFailedNative(int errorCode);


    /**
     * 恢复购买成功回调
     */
    public static void onHandleQueryPurchasesSuccess(){
        BaseSdk.runOnGLThread(()->{
            onHandleQueryPurchasesSuccessNative();
        });
    }
    private static native void onHandleQueryPurchasesSuccessNative();


    /**
     * 恢复购买失败回调
     * @param errorCode 返回的错误码，参考 https://developer.android.com/google/play/billing/errors?hl=zh-cn
     */
    public static void onHandleQueryPurchasesFailed(int errorCode){
        BaseSdk.runOnGLThread(()->{
            onHandleQueryPurchasesFailedNative(errorCode);
        });
    }
    private static native void onHandleQueryPurchasesFailedNative(int errorCode);


    /**
     * 启动购买弹窗失败回调
     * @param productId 要购买的商品id
     * @param errorCode 返回的错误码，参考 https://developer.android.com/google/play/billing/errors?hl=zh-cn
     */
    public static void onLaunchBillingFlowFailed(String productId, int errorCode){
        BaseSdk.runOnGLThread(()->{
            onLaunchBillingFlowFailedNative(productId, errorCode);
        });
    }
    private static native void onLaunchBillingFlowFailedNative(String productId, int errorCode);


    /**
     * 监听购买更新失败回调
     * @param errorCode 返回的错误码，参考 https://developer.android.com/google/play/billing/errors?hl=zh-cn
     */
    public static void onPurchasesUpdatedFailed(int errorCode){
        BaseSdk.runOnGLThread(()->{
            onPurchasesUpdatedFailedNative(errorCode);
        });
    }
    private static native void onPurchasesUpdatedFailedNative(int errorCode);


    /**
     * 购买状态异常回调，这说明此次购买不成功，可能是用户未支付或其他原因
     * @param productId 购买的产品id
     * @param stateCode 返回的状态码，状态代码可能为2(Pending)或0(UNSPECIFIED_STATE)
     */
    public static void onHandlePurchaseState(String productId, int stateCode){
        BaseSdk.runOnGLThread(()->{
            onHandlePurchaseStateNative(productId, stateCode);
        });
    }
    private static native void onHandlePurchaseStateNative(String productId, int stateCode);


    /**
     * 购买成功回调，可以在此方法中向用户发放其购买的物品/订阅等商品
     * @param productId 购买的产品id
     */
    public static void onHandlePurchaseSuccess(String productId){
        BaseSdk.runOnGLThread(()->{
            onHandlePurchaseSuccessNative(productId);
        });
    }
    private static native void onHandlePurchaseSuccessNative(String productId);


    /**
     * 购买失败回调，
     * @param productId 要购买的产品id，为"null"时说明用户在支付时未能成功从GooglePlay获取信息；为实际产品id时说明用户的购买未能进行验证
     * @param errorCode 返回的错误码，参考 https://developer.android.com/google/play/billing/errors?hl=zh-cn
     */
    public static void onHandlePurchaseFailed(String productId, int errorCode){
        BaseSdk.runOnGLThread(()->{
            onHandlePurchaseFailedNative(productId, errorCode);
        });
    }
    private static native void onHandlePurchaseFailedNative(String productId, int errorCode);


    /**
     * Consume成功回调
     */
    public static void onHandleConsumePurchaseSuccess(String productId){
        BaseSdk.runOnGLThread(()->{
            onHandleConsumePurchaseSuccessNative(productId);
        });
    }
    private static native void onHandleConsumePurchaseSuccessNative(String productId);


    /**
     * Consume失败回调
     * @param productId 产品id
     * @param errorCode 返回的错误码
     */
    public static void onHandleConsumePurchaseFailed(String productId, int errorCode){
        BaseSdk.runOnGLThread(()->{
            onHandleConsumePurchaseFailedNative(productId, errorCode);
        });
    }
    private static native void onHandleConsumePurchaseFailedNative(String productId, int errorCode);


    /* -------------------------------------- Billing Init Functions ---------------------------------------*/
    /**
     * 初始化BillingClient，在使用所有内购方法前应该调用此方法。该方法会在本地建立商品表.
     * @param activity 初始化所使用的activity
     * @param productIds 以字符串的形式传递产品id，格式为“consumeid1&consumeid2|nonconsumeid1&nonconsumeid2|subscribeid1&subscribeid2”
     */
    public static void init(Activity activity, String productIds){
        BillingManager.initBilling(activity, productIds);
    }

    /**
     * 返回BillingClient是否已经就绪
     * @return 为true时BillingClient已经连接到GooglePlay，并且成功获得商品的详细信息。否则未连接或者获得商品详细信息失败。为false时请使用 startConnection() 尝试重新连接
     */
    public static boolean isReady(){
        return BillingManager.isBillingClientReady();
    }

    /* ------------------------------------------ Billing Purchase Functions -------------------------------------------*/

    /**
     * 查询INAPP类商品详情，如果尚未建立连接会自动建立连接
     */
    public static void queryInappProductDetails(){
        BillingManager.queryInappProductDetails();
    }

    /**
     * 查询SUBS类商品详情，如果尚未建立连接会自动建立连接
     */
    public static void querySubsProductDetails(){
        BillingManager.querySubsProductDetails();
    }

    /**
     * 查询INAPP类商品状态，如果尚未建立连接会自动建立连接，在恢复购买时使用
     */
    public static void queryInappStates(){
        BillingManager.queryInappStates();
    }

    /**
     * 查询SUBS类商品状态，如果尚未建立连接会自动建立连接，在恢复购买或查询订阅状态时使用
     */
    public static void querySubsStates(){
        BillingManager.querySubsStates();
    }
    /**
     * 开始购买流程，该流程会发起购买弹窗，购买成功会回调onHandlePurchaseSuccess，购买失败会回调onHandlePurchaseFailed。
     * @param productId 要购买的商品id
     */
    public static void startBilling(String productId){
        BillingManager.startBilling(productId);
    }

    /**
     *处理consume类型的购买
     *@param productId 产品id
     */
    public static void handleConsumePurchase(String productId){
        BillingManager.handleConsumePurchase(productId);
    }
    /**
     * 对于订阅类型商品，获得本地储存的订阅到期毫秒时间戳
     * @param productId 商品id
     * @return 商品id对应的订阅到期毫秒时间戳；如果未订阅或者商品不为订阅类型，返回0
     */
    public static long getSubscribeExpiresTimeInMillis(String productId){
        return BillingManager.getSubscribeExpiresTimeInMillis(productId);
    }


}
