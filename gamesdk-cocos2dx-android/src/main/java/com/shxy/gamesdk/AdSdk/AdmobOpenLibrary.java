package com.shxy.gamesdk.AdSdk;

import android.util.Log;

import androidx.annotation.NonNull;

/*import com.appsflyer.adrevenue.AppsFlyerAdRevenue;
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork;*/
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;

import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import android.app.Activity;

import java.util.Locale;

public class AdmobOpenLibrary extends BaseOpenLibrary
{
    private Activity mActivity = null;//cocos2d的Activity
    /** Open Ad **/
    public static int MAX_OPEN_TOTAL_FAIL_TIMES = 3;//开屏广告请求失败的最大次数(超过次数则停止自动请求)
    private static final String mOpenAdId = AdManager.AdmobOpenAdId();
    private AppOpenAd mOpenAd = null;//开屏广告对象
    private AdLoadStatus mOpenAdLoadStatus = AdLoadStatus.als_Unload;//当前开屏广告的加载状态
    private ResponseInfo mOpenAdResponseInfo = null;
    private final AdOpenLoadListener mAdOpenLoadListener = new AdOpenLoadListener();//开屏广告加载监听
    private final AdOpenShowListener mAdOpenShowListener = new AdOpenShowListener();//开屏广告展示监听
    private final OpenAdPaidListener mOpenAdPaidListener = new OpenAdPaidListener();//开屏广告收益监听
    private int mOpenTotalFailTimes = 0;//开屏广告请求失败的累计次数
    private long loadTime = 0;//广告加载的时刻，用来判断广告是否过期


    private final String TAG = "AdMob-OpenAd";

    protected void initAdOpenLib(Activity activity)
    {
        mActivity = activity;
    }

    //加载开屏广告(加载索引值自+1)
    protected void loadOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            if(!AdManager.isConnectNetwork())
            {
                return;
            }
            //如果激励、全屏、横幅广告正在请求，则终止此次开屏广告请求
            if (AdManager.isRewardAdLoading() || AdManager.isFullAdLoading() || AdManager.isBannerAdLoading())
            {
                AdManager.addRequestAd("OpenAd");
                return;
            }
            //如果状态不对，则终止此次广告请求，进行下一个广告请求
            if ((isAdAvailable()) || (mOpenAdLoadStatus != AdLoadStatus.als_Unload))
            {

                Log.d(TAG, String.format("Open Ad was loading or loaded: %s", mOpenAdLoadStatus.toString()));
                //加载下一个广告
                if (isOpenAdLoaded())
                {
                    AdManager.loadNextAd();
                }
                return;
            }
            requestOpenAd();
        });
    }
    //请求开屏广告(runOnUiThread)
    private void requestOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            try
            {
                Log.d(TAG, "Open Ad Start to load");
                //Firebase事件记录
                //FirebaseManager.logAdLoadEvent("ad_load_start", "AppOpen");

                mOpenAdLoadStatus = AdLoadStatus.als_Loading;
                AdRequest request = new AdRequest.Builder().build();
                AppOpenAd.load(mActivity, mOpenAdId, request, mAdOpenLoadListener);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //展示开屏广告(runOnUiThread)
    protected void showOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            if(!isAdAvailable())
            {
                return;
            }
            if (!isOpenAdLoaded())
            {
                return;
            }
            try
            {
                mOpenAd.show(mActivity);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //开屏广告是否正在加载
    protected boolean isOpenAdLoading()
    {
        return (mOpenAdLoadStatus == AdLoadStatus.als_Loading);
    }
    //开屏广告是否加载完成
    protected boolean isOpenAdLoaded()
    {
        return (mOpenAdLoadStatus == AdLoadStatus.als_Loaded);
    }

    /**
     * 判断开屏广告是否可用，过期时自动加载开屏广告
     * @return true:可用，false:不可用
     */
    private boolean isAdAvailable()
    {
        if(mOpenAd == null){
            return false;
        }
        if((System.currentTimeMillis() - loadTime) < 1000 * 3600 * 4){
            return true;
        }else{
            mOpenAd = null;
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
            AdManager.addRequestAd("OpenAd");
            return false;
        }
    }

    /** Open Ad load listener **/
    class AdOpenLoadListener extends AppOpenAd.AppOpenAdLoadCallback {
        @Override//(runOnGLThread)
        public void onAdLoaded(@NonNull AppOpenAd ad)
        {
            Log.d(TAG, "Open load listener: Open Ad load Success");
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_success", "AppOpen");
            //清空请求失败累计次数
            mOpenTotalFailTimes = 0;
            //设置开屏广告播放对象
            mOpenAd = ad;
            //修改当前开屏广告状态为已加载
            mOpenAdLoadStatus = AdLoadStatus.als_Loaded;
            //设置开票广告播放监听
            mOpenAd.setFullScreenContentCallback(mAdOpenShowListener);
            //设置开屏广告播放监听
            mOpenAd.setOnPaidEventListener(mOpenAdPaidListener);
            loadTime = System.currentTimeMillis();
            //加载下一个广告
            AdManager.loadNextAd();
            //调用开屏广告加载完毕回调
            AdSdk.onOpenAdLoaded();
        }
        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError)
        {
            Log.e(TAG, "Open Ad load listener: Open Ad load failed: " + loadAdError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "AppOpen");

            mOpenTotalFailTimes ++;
            Log.d(TAG, String.format(Locale.getDefault(),"Open Ad load failed total times: %d", mOpenTotalFailTimes));
            //重置数据
            mOpenAd = null;
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
            if (mOpenTotalFailTimes < MAX_OPEN_TOTAL_FAIL_TIMES)
            {
                AdManager.addRequestAd("OpenAd");
            }
            else
            {
                Log.d(TAG, "Open Ad load failed too many times. Stop to continue load Open Ad");
                mOpenTotalFailTimes = 0;
                //加载下一个广告
                AdManager.loadNextAd();
            }
            AdSdk.onOpenAdLoadFailed();
        }
    }
    /** Open Ad show listener **/
    class AdOpenShowListener extends FullScreenContentCallback {
        @Override
        public void onAdShowedFullScreenContent()
        {
            mOpenAdResponseInfo = mOpenAd.getResponseInfo();
            //重置数据，防止二次调用
            mOpenAd = null;
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
        }
        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError)
        {
            Log.e(TAG, "Full Ad failed to show: " + adError.getMessage());
            // 重置数据
            mOpenAd = null;
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
            AdSdk.onOpenAdClosed();
            AdManager.loadNextAd();
            //准备新一个开屏广告
            AdManager.addRequestAd("OpenAd");
        }
        @Override
        public void onAdDismissedFullScreenContent()
        {
            AdSdk.onOpenAdClosed();
            // 重置数据
            mOpenAd = null;
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
            AdManager.loadNextAd();
            //准备新一个开屏广告
            AdManager.addRequestAd("OpenAd");
        }
    }
    /** Open ad paid event listener **/
    class OpenAdPaidListener implements OnPaidEventListener {
        @Override
        public void onPaidEvent(@NonNull AdValue adValue)
        {
            final double value = adValue.getValueMicros() / 1000000.0;
            //Debug Log
            Log.d(TAG, String.format(Locale.getDefault(),"open onPaidEvent: %f %s", value, adValue.getCurrencyCode()));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            //FirebaseManager.logAdRevenue(adValue);
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adb_paid_0");
            }
            //开启子线程上报广告收益到AppsFlyer
            if(mOpenAdResponseInfo != null){
                Runnable runnable = () -> {
/*                    String currency = adValue.getCurrencyCode();
                    Log.d(TAG, "Open AppsflyerAdRevenue");
                    //Appsflyer接入
                    String sourceName = mOpenAdResponseInfo.getLoadedAdapterResponseInfo().getAdSourceName();*/
                    /*AppsFlyerAdRevenue.logAdRevenue(sourceName, MediationNetwork.googleadmob, Currency.getInstance(currency), value, null);*/
                    mOpenAdResponseInfo = null;
                    Log.d(TAG, "Open AdjustAdRevenue");
                    AdjustSdk.trackAdRevenue(adValue);
                };
                Thread thread = new Thread(runnable, "OpenAdThread");
                thread.start();
            }else{
                Log.e(TAG, "mOpenAdResponseInfo is Null!");
            }
        }
    }
}