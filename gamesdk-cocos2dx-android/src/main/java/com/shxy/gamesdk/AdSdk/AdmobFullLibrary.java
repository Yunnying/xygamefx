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
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;
import android.app.Activity;

import java.util.Locale;

public class AdmobFullLibrary extends BaseFullLibrary
{
    private Activity mActivity = null;//cocos2d的Activity
    /** Full Ad **/
    public static int MAX_FULL_REQUEST_FAIL_TIMES = 3;//全屏广告单轮请求失败的最大次数
    private static final String mFullAdId = AdManager.FullAdId();
    private InterstitialAd  mInterstitialAd = null;//全屏广告对象
    private AdLoadStatus mFullAdLoadStatus = AdLoadStatus.als_Unload;//当前全屏广告的加载状态
    private ResponseInfo mInterstitialAdResponseInfo = null;
    private final FullAdLoadListener mFullAdLoadListener = new FullAdLoadListener();//全屏广告加载监听
    private final FullAdShowListener mFullAdShowListener = new FullAdShowListener();//全屏广告展示监听
    private final FullAdPaidListener mFullAdPaidListener = new FullAdPaidListener();//全屏广告收益监听
    private int mFullTotalFailTimes = 0;//全屏广告请求失败的累计次数
    private final String TAG = "Admob-FullAd";
    protected void initFullLib(Activity activity)
    {
        mActivity = activity;
    }

    //加载全屏广告。所有状态判定完全后，调用请求全屏广告逻辑(runOnUiThread)
    protected void loadFullAd()
    {
        mActivity.runOnUiThread(() -> {
            if(AdManager.getRemoveAdMode() || !AdManager.isConnectNetwork()){
                AdManager.loadNextAd();
                return;
            }
            //如果激励、横幅、开屏广告正在请求，则终止此次的全屏广告请求
            if (AdManager.isRewardAdLoading() || AdManager.isBannerAdLoading() || AdManager.isOpenAdLoading())
            {
                AdManager.addRequestAd("FullAd");
                return;
            }
            //如果状态不对，则终止此次广告请求，进行下一个广告请求
            if ((mInterstitialAd != null) || (mFullAdLoadStatus != AdLoadStatus.als_Unload))
            {
                Log.d(TAG, String.format("Full Ad was loading or loaded: %s", mFullAdLoadStatus.toString()));
                //加载下一个广告
                if (isFullAdLoaded())
                {
                    AdManager.loadNextAd();
                }
                return;
            }
            requestFullAd();
        });
    }

    //请求全屏广告(runOnUiThread)
    private void requestFullAd()
    {
        mActivity.runOnUiThread(() -> {
            try {
                Log.d(TAG, "Full Ad Start to load");
                //Firebase事件记录
                FirebaseManager.logAdLoadEvent("ad_load_start", "Interstitial");

                mFullAdLoadStatus = AdLoadStatus.als_Loading;
                AdRequest adRequest = new AdRequest.Builder().build();
                InterstitialAd.load(mActivity, mFullAdId, adRequest, mFullAdLoadListener);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //展示全屏广告(runOnUiThread)
    protected void showFullAd()
    {
        mActivity.runOnUiThread(() -> {
            if(mInterstitialAd == null || AdManager.getRemoveAdMode() || !isFullAdLoaded())
            {
                AdManager.loadNextAd();
                return;
            }

            try
            {
                mInterstitialAd.show(mActivity);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //全屏广告是否正在加载
    protected boolean isFullAdLoading()
    {
        return (mFullAdLoadStatus == AdLoadStatus.als_Loading);
    }
    //全屏广告是否加载完成
    protected boolean isFullAdLoaded()
    {
        return  (mFullAdLoadStatus == AdLoadStatus.als_Loaded);
    }

    /** Full Ad load listener **/
    class FullAdLoadListener extends InterstitialAdLoadCallback {
        @Override
        public void onAdLoaded(@NonNull final InterstitialAd interstitialAd)
        {
            Log.d(TAG, "Full Ad load Success");
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_success", "Interstitial");
            //清空请求失败累计次数
            mFullTotalFailTimes = 0;
            //设置全屏广告播放对象
            mInterstitialAd = interstitialAd;
            //修改当前全屏广告状态为已加载
            mFullAdLoadStatus = AdLoadStatus.als_Loaded;
            //设置全屏广告播放监听
            mInterstitialAd.setFullScreenContentCallback(mFullAdShowListener);
            //设置全屏广告收益监听
            mInterstitialAd.setOnPaidEventListener(mFullAdPaidListener);
            AdManager.loadNextAd();
            //准备一个新的全屏广告
            //AdmobManager.addRequestAd("FullAd");
            AdSdk.onFullAdLoaded();
        }
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError)
        {
            Log.e(TAG, "Full load listener:  FullAd load failed: " + loadAdError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "Interstitial");

            mFullTotalFailTimes ++;
            Log.d(TAG, String.format(Locale.getDefault(),"Full Ad load failed total times: %d", mFullTotalFailTimes));
            //重置数据
            mInterstitialAd = null;
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
            if (mFullTotalFailTimes < MAX_FULL_REQUEST_FAIL_TIMES)
            {
                AdManager.addRequestAd("FullAd");
            }
            else
            {
                Log.d(TAG, "Full Ad load failed too many times. Stop to continue load Full Ad");
                mFullTotalFailTimes = 0;
                AdManager.loadNextAd();
                //准备一个新的全屏广告
                AdManager.addRequestAd("FullAd");
            }
            AdSdk.onFullAdLoadFailed();
        }
    }
    /** Full Ad show listener **/
    class FullAdShowListener extends FullScreenContentCallback
    {
        @Override
        public void onAdShowedFullScreenContent()
        {
            mInterstitialAdResponseInfo = mInterstitialAd.getResponseInfo();
            // 重置数据，防止二次调用
            mInterstitialAd = null;
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adf_show_success");
        }
        @Override//(runOnGLThread)
        public void onAdFailedToShowFullScreenContent(AdError adError)
        {
            // Called when fullscreen content failed to show.
            Log.e(TAG, "Full Ad failed to show: " + adError.getMessage());
            // 重置数据
            mInterstitialAd = null;
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adf_show_fail");
            AdSdk.onFullAdClosed();
            AdManager.loadNextAd();
            //准备一个新的全屏广告
            AdManager.addRequestAd("FullAd");

        }
        @Override//(runOnGLThread)
        public void onAdDismissedFullScreenContent()
        {
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adf_close");
            //调用全屏广告关闭回调
            AdSdk.onFullAdClosed();
            // 重置数据，防止二次调用
            mInterstitialAd = null;
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
            AdManager.loadNextAd();
            //准备一个新的全屏广告
            AdManager.addRequestAd("FullAd");
        }
    }
    /** Full ad paid event listener **/
    class FullAdPaidListener implements OnPaidEventListener {
        @Override
        public void onPaidEvent(@NonNull AdValue adValue)
        {
            final double value = adValue.getValueMicros() / 1000000.0;
            //Debug Log
            Log.d(TAG, String.format(Locale.getDefault(),"full onPaidEvent: %f %s", value, adValue.getCurrencyCode()));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(adValue,"interstitial");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adf_paid_0");
            }
            //开启子线程上报广告收益到AppsFlyer
            if(mInterstitialAdResponseInfo != null){
                Runnable runnable = () -> {
/*                    String currency = adValue.getCurrencyCode();
                    Log.d(TAG, "Full AppsflyerAdRevenue");
                    //Appsflyer接入
                    String sourceName = mInterstitialAdResponseInfo.getLoadedAdapterResponseInfo().getAdSourceName();
                    Log.d(TAG, "SourceName:"+sourceName);*/
                    /*AppsFlyerAdRevenue.logAdRevenue(sourceName, MediationNetwork.googleadmob, Currency.getInstance(currency), value, null);*/
                    mInterstitialAdResponseInfo = null;
                    Log.d(TAG, "Full AdjustAdRevenue");
                    AdjustSdk.trackAdRevenue(adValue);
                };

                Thread thread = new Thread(runnable, "FullAdThread");
                thread.start();
            }else{
                Log.e(TAG, "mInterstitialAdResponseInfo is Null!");
            }
        }
    }
}
