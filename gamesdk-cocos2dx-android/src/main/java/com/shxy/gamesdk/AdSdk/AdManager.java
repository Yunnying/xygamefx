package com.shxy.gamesdk.AdSdk;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.facebook.ads.AdSettings;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.shxy.gamesdk.BaseSdk.BaseSdk;
import com.shxy.gamesdk.GDPR.MaxGdprManager;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

//广告加载状态
enum AdLoadStatus
{
    als_Unload, //未加载
    als_Loading,//加载中
    als_Loaded, //已加载
}

enum MediationName
{
    m_Admob,
    m_AppLovin,
}

public class AdManager
{
    private static Activity mActivity = null; //
    private static FrameLayout mFrameLayout = null;
    private static MediationName mMediationName = MediationName.m_Admob;//默认为Admob
    private static BaseRewardLibrary mAdRewardLib = null;
    private static BaseFullLibrary mAdFullLib = null;
    private static BaseBannerLibrary mAdBannerLib = null;
    private static BaseOpenLibrary mAdOpenLib = null;
    private static Vector mAdLoadList = new Vector();//当前需要加载的广告列表
    private static String mRewardAdId = "ca-app-pub-3940256099942544/5224354917";//奖励广告id，默认值是测试广告
    private static String mFullAdId = "ca-app-pub-3940256099942544/1033173712";//全屏广告id，默认值是测试广告
    private static String mOpenAdId = "ca-app-pub-3940256099942544/9257395921";//开屏广告id，默认值是测试广告
    private static String mBannerAdId = "ca-app-pub-3940256099942544/6300978111";//横幅广告id，默认值是测试广告
    private static boolean mIsDebug = false;
    private static boolean mHasInitedAds = false;
    private static String TAG = "Ad-Manager";


    /**
     * 初始化AdmobSDK
     * @param activity
     * @param frameLayout
     * @param mediationName 广告聚合商名称，可使用"Admob"和"AppLovin"
     * @param isDebug 是否开启debug模式
     * @param rewardAdId 奖励广告id
     * @param fullAdId 全屏广告id
     * @param bannerAdId 横幅广告id
     */
    protected static void initManager(Activity activity, FrameLayout frameLayout, String mediationName, String rewardAdId, String fullAdId, String bannerAdId, boolean isDebug, String testDeviceId)
    {
        mActivity = activity;
        mFrameLayout = frameLayout;
        setDebug(isDebug);
        setAdId(rewardAdId, fullAdId, bannerAdId);

        if(mediationName.equals("Admob")){
            mMediationName = MediationName.m_Admob;
        }else if(mediationName.equals("AppLovin")){
            mMediationName = MediationName.m_AppLovin;
        }

        if(mMediationName == MediationName.m_Admob){
            mAdRewardLib = new AdmobRewardLibrary();
            mAdRewardLib.initRewardLib(activity);

            mAdFullLib = new AdmobFullLibrary();
            mAdFullLib.initFullLib(activity);

            mAdBannerLib = new AdmobBannerLibrary();
            mAdBannerLib.initBannerLib(activity, frameLayout);

            mAdOpenLib = new AdmobOpenLibrary();
            mAdOpenLib.initAdOpenLib(activity);

            if(mIsDebug){
                /*List<String> testDeviceIds = Arrays.asList("96B7A4F965B951AC1679B186C10D4EAB");//这里可以考虑传参进来后进行更改*/
                List<String> testDeviceIds = Arrays.asList(testDeviceId);//这里可以考虑传参进来后进行更改
                RequestConfiguration configuration =
                        new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
                MobileAds.setRequestConfiguration(configuration);
            }
        }else if(mMediationName == MediationName.m_AppLovin){
            mAdRewardLib = new MaxRewardLibrary();
            mAdRewardLib.initRewardLib(activity);

            mAdFullLib = new MaxFullLibrary();
            mAdFullLib.initFullLib(activity);

            mAdBannerLib = new MaxBannerLibrary();
            mAdBannerLib.initBannerLib(activity, frameLayout);

            mAdOpenLib = new MaxOpenLibrary();
            mAdOpenLib.initAdOpenLib(activity);
            if(mIsDebug){
                AdSettings.addTestDevice(testDeviceId);
            }
        }
    }

    /**
     * 初始化广告
     */
    protected static void initAds(){
        if(mAdBannerLib == null || mAdFullLib == null || mAdRewardLib == null){
            Log.e(TAG, "initAds: You may not initialize Ad SDK!" );
            return;
        }
        if(mHasInitedAds){
            Log.d(TAG, "initAds: Ads has been initialized!");
            return;
        }
        Log.d(TAG, "initAds: Begin Init Ads");
        if(mMediationName == MediationName.m_Admob){
            MobileAds.initialize(mActivity,initializationStatus -> {
                onAdInitComplete();
            });
        } else if (mMediationName == MediationName.m_AppLovin) {
            //AppLovin MAX聚合的初始化在CMP完成后自动进行
            if(!MaxGdprManager.getHasInitMax()){
                Log.d(TAG, "initAds: Init AppLovin Max");
                AppLovinSdk.getInstance( mActivity ).setMediationProvider( "max" );
                AppLovinSdk.initializeSdk( mActivity, new AppLovinSdk.SdkInitializationListener() {
                    @Override
                    public void onSdkInitialized(final AppLovinSdkConfiguration configuration)
                    {
                        mAdRewardLib.initRewardAd();
                        mAdBannerLib.initBannerAd();
                        mAdFullLib.initFullAd();
                        mAdOpenLib.initOpenAd();
                        onAdInitComplete();
                    }
                } );
            }else{
                mAdRewardLib.initRewardAd();
                mAdBannerLib.initBannerAd();
                mAdFullLib.initFullAd();
                mAdOpenLib.initOpenAd();
                Log.d(TAG, "initAds: For AppLovin Max mediation, the CMP performs its initialization automatically.");
            }
        }
    }

    protected static boolean hasInitedAds(){
        return mHasInitedAds;
    }

    /**
     * 设置debug标志位
     * @param isDebug 为true时表示是测试模式，切换到测试广告；为false时为正式模式，使用正式广告
     */
    private static void setDebug(boolean isDebug){
        mIsDebug = isDebug;
    }

    /**
     * 设置移除广告标志位
     * @param isRemoveAd true表示移除广告，false表示不移除广告
     */
    protected static void setRemoveAdMode(boolean isRemoveAd){
        BaseSdk.setBoolForKey("Remove_Ad_Mode",isRemoveAd);
    }

    /**
     * 获取移除广告标志位
     * @return true表示移除广告，false表示不移除广告
     */
    protected static boolean getRemoveAdMode(){
        return BaseSdk.getBoolForKey("Remove_Ad_Mode",false);
    }
    //Ad初始化回调
    protected static void onAdInitComplete()
    {
        mHasInitedAds = true;
        if(mMediationName == MediationName.m_AppLovin && mIsDebug){
            AppLovinSdk.getInstance(mActivity).showMediationDebugger();
        }
        AdSdk.onAdInitialized();
    }

    /**
     * 设置广告是否静音
     * @param enable  true表示广告静音，false表示广告不静音
     */
    protected static void setAdmobVolumeEnable(boolean enable)
    {
        if (mActivity == null)
        {
            return;
        }
        mActivity.runOnUiThread(()-> MobileAds.setAppMuted(!enable));
    }

    /**
     * 设置广告ID
     * @param rewardAdId reward广告ID
     * @param fullAdId full广告ID
     * @param bannerAdId Banner广告ID
     */
    protected static void setAdId(String rewardAdId, String fullAdId, String bannerAdId){
        mRewardAdId = rewardAdId;
        mFullAdId = fullAdId;
        mBannerAdId = bannerAdId;
    }
    /**
     * AdMob Banner Ad Id
     * Debug: ca-app-pub-3940256099942544/6300978111
     * Release: ca-app-pub-7488334025024124/8015233925
     * Release: H:, M:, L:
     **/
    protected static String BannerAdId()
    {
        if (mIsDebug && mMediationName == MediationName.m_Admob)
        {
            return "ca-app-pub-3940256099942544/6300978111";
        }
        else
        {
            return mBannerAdId;
        }
    }
    /**
     * AdMob Full Ad Id
     * Debug: ca-app-pub-3940256099942544/1033173712
     * Release: ca-app-pub-7488334025024124/7436100686
     **/
    protected static String FullAdId()
    {
        if (mIsDebug && mMediationName == MediationName.m_Admob)
        {
            return "ca-app-pub-3940256099942544/1033173712";
        }
        else
        {
            return mFullAdId;
        }
    }
    /**
     * AdMob Reward Ad Id
     * Debug: ca-app-pub-3940256099942544/5224354917
     * Release: ca-app-pub-7488334025024124/4538474602
     **/
    protected static String RewardAdId()
    {
        if (mIsDebug && mMediationName == MediationName.m_Admob)
        {
            return "ca-app-pub-3940256099942544/5224354917";
        }
        else
        {
            return mRewardAdId;
        }
    }
    /**
     * AdMob Open Ad Id
     * Debug: ca-app-pub-3940256099942544/3419835294
     * Release:
     **/
    protected static String AdmobOpenAdId()
    {
        if (mIsDebug && mMediationName == MediationName.m_Admob)
        {
            return "ca-app-pub-3940256099942544/3419835294";
        }
        else
        {
            return mOpenAdId;
        }
    }
    //添加需要加载广告(FullAd,RewardAd,BannerAd,OpenAd)
    protected static void addRequestAd(final String adName)
    {
        Log.i(TAG, "addRequestAd: " + adName);
        mAdLoadList.addElement(adName);

        if (isRewardAdLoading() || isFullAdLoading() || isBannerAdLoading() || isOpenAdLoading())
        {
            return;
        }

        loadNextAd();
    }
    //获取当前要加载的广告
    protected static String getNextAdToLoad()
    {
        if (mAdLoadList.size() == 0)
        {
            return "";
        }
        if (isRewardAdLoading() || isFullAdLoading() || isBannerAdLoading() || isOpenAdLoading())
        {
            return "";
        }
        String adName = (String)mAdLoadList.elementAt(0);
        mAdLoadList.removeElementAt(0);
        return adName;
    }
    //依次从广告加载列表里面加载下一个广告
    protected static void loadNextAd()
    {
        if (isRewardAdLoading() || isFullAdLoading() || isBannerAdLoading() || isOpenAdLoading())
        {
            return ;
        }

        new Handler().postDelayed(()->{
            String adName = getNextAdToLoad();
            if (adName.equals(""))
            {
                return;
            }
            Log.i(TAG, "Get Next Ad To Load: " + adName);

            if (adName.equals("FullAd"))
            {
                loadFullAd();
            }
            if (adName.equals("RewardAd"))
            {
                loadRewardAd();
            }
            if (adName.equals("BannerAd"))
            {
                loadBannerAd();
            }
            if (adName.equals("OpenAd"))
            {
                loadOpenAd();
            }
        },5000);
    }
    /** Full Ad Function **/
    //加载FullAd
    protected static void loadFullAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdFullLib != null)
            {
                mAdFullLib.loadFullAd();
            }
        });
    }
    //显示FullAd
    protected static void showFullAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdFullLib != null)
            {
                mAdFullLib.showFullAd();
            }
        });
    }

    /**
     * 判断FullAd是否正在加载中
     * @return true表示正在加载中，false表示加载完成
     */
    protected static boolean isFullAdLoading()
    {
        if (mAdFullLib != null)
        {
            return mAdFullLib.isFullAdLoading();
        }
        return false;
    }

    /**
     * 判断FullAd是否加载完成
     * @return true表示加载完成，false表示正在加载中
     */
    protected static boolean isFullAdLoaded()
    {
        if (mAdFullLib != null)
        {
            return mAdFullLib.isFullAdLoaded();
        }
        return false;
    }
    /** Reward Ad Function **/
    protected static void loadRewardAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdRewardLib != null)
            {
                mAdRewardLib.loadRewardAd();
            }
        });
    }
    protected static void showRewardAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdRewardLib != null)
            {
                mAdRewardLib.showRewardAd();
            }
        });
    }
    protected static boolean isRewardAdLoading()
    {
        if (mAdRewardLib != null)
        {
            return mAdRewardLib.isRewardAdLoading();
        }
        return false;
    }
    protected static boolean isRewardAdLoaded()
    {
        if (mAdRewardLib != null)
        {
            return mAdRewardLib.isRewardAdLoaded();
        }
        return false;
    }
    /** Banner Ad Function **/
    protected static void loadBannerAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdBannerLib != null)
            {
                mAdBannerLib.loadBannerAd();
            }
        });
    }
    protected static void showBannerAd(boolean isVisible)
    {
        if (mAdBannerLib != null)
        {
            mAdBannerLib.showBannerAd(isVisible);
        }
    }
    protected static boolean isBannerAdLoading()
    {
        if (mAdBannerLib != null)
        {
            return mAdBannerLib.isBannerLoading();
        }
        return false;
    }
    protected static boolean isBannerLoaded()
    {
        if (mAdBannerLib != null)
        {
            return mAdBannerLib.isBannerLoaded();
        }
        return false;
    }
    protected static boolean isBannerAdVisible()
    {
        if (mAdBannerLib != null)
        {
            return mAdBannerLib.isBannerAdVisible();
        }
        return false;
    }
    protected static float getBannerAdHeight()
    {
        if (mAdBannerLib != null)
        {
            return mAdBannerLib.getAdHeight();
        }
        return 0;
    }
/*    //设置屏幕宽度尺寸(单位：像素)
    public static void setWinWidth(final int winWidth)
    {
        mActivity.runOnUiThread(() -> {
            if (mAdBannerLib != null)
            {
                mAdBannerLib.setWinWidth(winWidth);
            }
        });
    }*/
    /** Open Ad Function **/
    protected static void loadOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdOpenLib != null)
            {
                mAdOpenLib.loadOpenAd();
            }
        });
    }
    protected static void showOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            if (mAdOpenLib != null)
            {
                mAdOpenLib.showOpenAd();
            }
        });
    }
    protected static boolean isOpenAdLoading()
    {
        if (mAdOpenLib != null)
        {
            return mAdOpenLib.isOpenAdLoading();
        }
        return false;
    }
    protected static boolean isOpenAdLoaded()
    {
        if (mAdOpenLib != null)
        {
            return mAdOpenLib.isOpenAdLoaded();
        }
        return false;
    }

    /**
     * 检查是否建立了网络连接
     * @return 是否建立了网络连接
     */
    protected static boolean isConnectNetwork(){
        boolean isConnect = false;
        ConnectivityManager conManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = conManager.getActiveNetworkInfo();
        if(network != null)
        {
            isConnect = conManager.getActiveNetworkInfo().isAvailable();
            Log.d(TAG, "isConnectNetwork: network is connected");
        }
        else
        {
            Log.d(TAG, "isConnectNetwork: network is not connected");
        }
        return isConnect;
    }

    //获取设备品牌(Chromebook返回 google)
    private static String getDeviceBrand()
    {
        String deviceBrand = Build.BRAND.toLowerCase();
        Log.d(TAG, "getDeviceBrand: " + deviceBrand);
        return deviceBrand;
    }
    //获取设备制造商(Chromebook返回 google)
    private static String getDeviceManufacturer()
    {
        String deviceManufacturer = Build.MANUFACTURER.toLowerCase();
        Log.d(TAG, "getDeviceManufacturer:" + deviceManufacturer);
        return deviceManufacturer;
    }
    //是否Pad
    private static boolean isPad()
    {
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y); // 屏幕尺寸
        Log.d(TAG, "screenInches: "+ screenInches);
        return screenInches >= 11.0;
    }
    //是否ChromeBook
    protected static boolean isChromeBook()
    {
        return  (getDeviceBrand().equals("google") && getDeviceManufacturer().equals("google") && isPad());
    }
}
