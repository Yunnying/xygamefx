package com.shxy.gamesdk.AdSdk;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import java.util.Locale;

/**
 * @author: 翟宇翔
 * @date: 2023/12/14
 */
public class MaxFullLibrary extends BaseFullLibrary {
    private MaxInterstitialAd mInterstitialAd = null;
    private int mFullTotalFailTimes = 0;//全屏广告请求失败的累计次数
    public static int MAX_FULL_REQUEST_FAIL_TIMES = 3;//全屏广告单轮请求失败的最大次数
    private static final String mFullAdId = AdManager.FullAdId();
    private Activity mActivity = null;
    private AdLoadStatus mFullAdLoadStatus = AdLoadStatus.als_Unload;
    private final InterstitialAdListener mInterstitialAdListener = new InterstitialAdListener();
    private final InterstitialAdRevenueListener mInterstitialAdRevenueListener = new InterstitialAdRevenueListener();
    private final String TAG = "MAX-FullAd";

    protected void initFullLib(Activity activity){
        mActivity = activity;
    }

    protected void initFullAd(){
        mActivity.runOnUiThread(()->{
            if(mInterstitialAd != null){
                mInterstitialAd.destroy();
            }
            mInterstitialAd = new MaxInterstitialAd(mFullAdId,mActivity);
            mInterstitialAd.setListener(mInterstitialAdListener);
            mInterstitialAd.setRevenueListener(mInterstitialAdRevenueListener);
        });
    }

    protected void loadFullAd(){

        mActivity.runOnUiThread(()->{
            //如果激励、横幅、开屏广告正在请求，则终止此次的全屏广告请求
            if (AdManager.isRewardAdLoading() || AdManager.isBannerAdLoading())
            {
                AdManager.addRequestAd("FullAd");
                return;
            }
            //如果状态不对，则终止此次广告请求，进行下一个广告请求
            if ((mInterstitialAd.isReady()) || (mFullAdLoadStatus != AdLoadStatus.als_Unload))
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

    private void requestFullAd(){
        mActivity.runOnUiThread(() -> {
            try {
                Log.d(TAG, "Full Ad Start to load");
                //Firebase事件记录
                FirebaseManager.logAdLoadEvent("ad_load_start", "Interstitial");

                mFullAdLoadStatus = AdLoadStatus.als_Loading;
                mInterstitialAd.loadAd();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    protected void showFullAd(){
        mActivity.runOnUiThread(() -> {
            if(!mInterstitialAd.isReady() || AdManager.getRemoveAdMode())
            {
                Log.e(TAG, "showFullAd: Full Ad is not ready!");
                AdManager.loadNextAd();
                return;
            }
            try
            {
                mInterstitialAd.showAd();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    protected boolean isFullAdLoading()
    {
        return (mFullAdLoadStatus == AdLoadStatus.als_Loading);
    }
    //全屏广告是否加载完成
    protected boolean isFullAdLoaded()
    {
        return  (mFullAdLoadStatus == AdLoadStatus.als_Loaded && mInterstitialAd.isReady());
    }

    class InterstitialAdListener implements MaxAdListener{

        @Override
        public void onAdLoaded(@NonNull MaxAd maxAd) {
            Log.d(TAG, "Full Ad load Success");
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_success", "Interstitial");
            //清空请求失败累计次数
            mFullTotalFailTimes = 0;
            //修改当前全屏广告状态为已加载
            mFullAdLoadStatus = AdLoadStatus.als_Loaded;
            AdManager.loadNextAd();
            //准备一个新的全屏广告
            //AdmobManager.addRequestAd("FullAd");
            AdSdk.onFullAdLoaded();
        }

        @Override
        public void onAdDisplayed(@NonNull MaxAd maxAd) {
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
        }

        @Override
        public void onAdHidden(@NonNull MaxAd maxAd) {
            AdSdk.onFullAdClosed();
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
            AdManager.loadNextAd();
            //准备一个新的全屏广告
            AdManager.addRequestAd("FullAd");
        }

        @Override
        public void onAdClicked(@NonNull MaxAd maxAd) {

        }

        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
            Log.e(TAG, "Full load listener:  FullAd load failed: " + maxError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "Interstitial");
            mFullTotalFailTimes ++;
            Log.d(TAG, String.format(Locale.getDefault(),"Full Ad load failed total times: %d", mFullTotalFailTimes));
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

        @Override
        public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
            // Called when fullscreen content failed to show.
            Log.e(TAG, "Full Ad failed to show: " + maxError.getMessage());
            mFullAdLoadStatus = AdLoadStatus.als_Unload;
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adf_show_fail");
            AdSdk.onFullAdClosed();
            AdManager.loadNextAd();
            //准备一个新的全屏广告
            AdManager.addRequestAd("FullAd");
        }
    }

    class InterstitialAdRevenueListener implements MaxAdRevenueListener {
        public void onAdRevenuePaid(@NonNull MaxAd maxAd) {
            double value = maxAd.getRevenue();
            Log.d(TAG, String.format(Locale.getDefault(),"interstitial onPaidEvent: %f USD", value));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(value,"interstitial");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adf_paid_0");
            }
            //开启子线程上报广告收益到Adjust（MAX后台已经上报，注释）
/*            Runnable runnable = () -> {
                Log.d(TAG, "Full AdjustAdRevenue");
                AdjustSdk.trackAdRevenue(value);
            };
            Thread thread = new Thread(runnable, "FullAdThread");
            thread.start();*/
        }
    }
}
