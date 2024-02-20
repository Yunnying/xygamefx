/**
 * @author: 翟宇翔
 * @date: 2023/08/21
 * 提供广告接入能力的SDK
 */

package com.shxy.gamesdk.AdSdk;

import android.widget.FrameLayout;

import com.shxy.gamesdk.BaseSdk.BaseSdk;

import android.app.Activity;

public class AdSdk {

    /* ------------------------------------------ JNI Function to C++ ---------------------------*/
    /** Call JNI Function to C++ Begin **/
    //开屏广告加载完成回调
    public static void onOpenAdLoaded(){
        BaseSdk.runOnGLThread(()->{
            onOpenAdLoadedNative();
        });
    }
    private static native void onOpenAdLoadedNative();


    //开屏广告关闭回调
    public static void onOpenAdClosed(){
        BaseSdk.runOnGLThread(()->{
            onOpenAdClosedNative();
        });
    }
    private static native void onOpenAdClosedNative();

    public static void onOpenAdLoadFailed(){
        BaseSdk.runOnGLThread(()->{
            onOpenAdLoadFailedNative();
        });
    }
    private static native void onOpenAdLoadFailedNative();
    /** Call JNI Function to C++ End **/

    /*---------------------------------------------- Basic Functions ------------------------------------*/

    /**
     * 初始化AdSDK
     * @param activity 当前的activity
     * @param frameLayout 当前的frameLayout
     * @param rewardAdId 激励广告id
     * @param fullAdId 全屏广告id
     * @param bannerAdId 横幅广告id
     * @param isDebug 是否开启debug模式
     * @param testDeviceId 测试设备id
     */
    public static void init(Activity activity, FrameLayout frameLayout, String mediationName, String rewardAdId, String fullAdId, String bannerAdId, boolean isDebug, String testDeviceId){
        AdManager.initManager(activity,frameLayout,mediationName,rewardAdId,fullAdId,bannerAdId,isDebug,testDeviceId);
    }

    /**
     * 初始化广告内容
     */
    public static void initAds(){
        AdManager.initAds();
    };

    /**
     * 设置去除广告标记位
     * @param isRemoveAd true为去除广告；false为不去除广告
     */
    public static void setRemoveAdMode(boolean isRemoveAd){
        AdManager.setRemoveAdMode(isRemoveAd);
    }

    /**
     * 获取去除广告标记位
     * @return true为去除广告；false为不去除广告，默认为false
     */
    public static boolean getRemoveAdMode(){
        return AdManager.getRemoveAdMode();
    }


    /**
     * 广告内容初始化完成回调
     */
    public static void onAdInitialized(){
        BaseSdk.runOnGLThread(()->{
            onAdInitializedNative();
        });
    }
    private static native void onAdInitializedNative();


    /*---------------------------------------------- RewardAd Functions ---------------------------------*/

    /**
     * 加载激励广告
     */
    public static void loadRewardAd(){
        AdManager.loadRewardAd();
    }
    /**
     * 判断激励广告是否加载成功
     * @return 为true时激励广告已经加载成功；否则没有加载成功
     */
    public static boolean isRewardAdLoaded(){
        return AdManager.isRewardAdLoaded();
    }
    /**
     * 展示激励广告
     */
    public static void showRewardAd(){
        AdManager.showRewardAd();
    }


    //激励广告加载完成回调
    public static void onRewardAdLoaded(){
        BaseSdk.runOnGLThread(()->{
            onRewardAdLoadedNative();
        });
    }
    private static native void onRewardAdLoadedNative();


    //激励广告加载失败回调
    public static void onRewardAdLoadFailed(){
        BaseSdk.runOnGLThread(()->{
            onRewardAdLoadFailedNative();
        });
    }
    private static native void onRewardAdLoadFailedNative();


    //激励广告观看完成回调
    public static void onRewardAdViewed(){
        BaseSdk.runOnGLThread(()->{
            onRewardAdViewedNative();
        });
    }
    private static native void onRewardAdViewedNative();


    //激励广告观看取消回调
    public static void onRewardAdCanceled(){
        BaseSdk.runOnGLThread(()->{
            onRewardAdCanceledNative();
        });
    }
    private static native void onRewardAdCanceledNative();

    /*---------------------------------------------- FullAd Functions ---------------------------------*/

    /**
     * 加载全屏广告
     */
    public static void loadFullAd(){
        AdManager.loadFullAd();
    }
    /**
     * 判断全屏广告是否加载成功
     * @return 为true时说明全屏广告已经加载成功；否则未加载成功
     */
    public static boolean isFullAdLoaded(){
        return AdManager.isFullAdLoaded();
    }
    /**
     * 展示全屏广告
     */
    public static void showFullAd(){
        AdManager.showFullAd();
    }


    //全屏广告加载完成回调
    public static void onFullAdLoaded(){
        BaseSdk.runOnGLThread(()->{
            onFullAdLoadedNative();
        });
    }
    private static native void onFullAdLoadedNative();


    //全屏广告加载失败回调
    public static void onFullAdLoadFailed(){
        BaseSdk.runOnGLThread(()->{
            onFullAdLoadFailedNative();
        });
    }
    private static native void onFullAdLoadFailedNative();


    //全屏广告关闭回调
    public static void onFullAdClosed(){
        BaseSdk.runOnGLThread(()->{
            onFullAdClosedNative();
        });
    }
    private static native void onFullAdClosedNative();

    /*-----------------------------------------------BannerAd Functions ---------------------------------*/

    /**
     * 加载横幅广告
     */
    public static void loadBannerAd(){
        AdManager.loadBannerAd();
    }
    /**
     * 判断横幅广告是否加载成功
     * @return 为true时说明横幅广告加载成功；否则说明未加载成功
     */
    public static boolean isBannerLoaded(){
        return AdManager.isBannerLoaded();
    }

    /**
     * 设置横幅广告是否可见
     * @param isVisible 为true时横幅广告可见；为false时横幅广告不可见
     */
    public static void showBannerAd(boolean isVisible){
        AdManager.showBannerAd(isVisible);
    }

    /**
     * 判断横幅广告是否可见
     * @return 为true时可见；为false时不可见
     */
    public static boolean isBannerAdVisible(){
        return AdManager.isBannerAdVisible();
    }

    /**
     * 获取横幅广告高度
     * @return 返回一个float值为横幅广告的高度
     */
    public static float getBannerAdHeight(){return AdManager.getBannerAdHeight();}


    //横幅广告加载完成回调
    public static void onBannerAdLoaded(){
        BaseSdk.runOnGLThread(()->{
            onBannerAdLoadedNative();
        });
    }
    private static native void onBannerAdLoadedNative();


    //横幅广告点击回调
    public static void onBannerAdClicked(){
        BaseSdk.runOnGLThread(()->{
            onBannerAdClickedNative();
        });
    }
    private static native void onBannerAdClickedNative();


    // 横幅广告展示回调
    public static void onBannerAdShown(){
        BaseSdk.runOnGLThread(()->{
            onBannerAdShownNative();
        });
    }
    private static native void onBannerAdShownNative();
}
