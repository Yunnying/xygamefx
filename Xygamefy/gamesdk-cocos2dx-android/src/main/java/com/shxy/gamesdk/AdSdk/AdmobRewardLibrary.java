package com.shxy.gamesdk.AdSdk;

import android.util.Log;

import androidx.annotation.NonNull;

import android.app.Activity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import java.util.Locale;

public class AdmobRewardLibrary extends BaseRewardLibrary
{
    private Activity mActivity = null;//Activity
    /** Reward Ad **/
    public static int MAX_REWARD_REQUEST_FAIL_TIMES = 3;//激励广告单轮请求失败的最大次数
    private static final String mRewardAdId = AdManager.RewardAdId();
    private RewardedAd mRewardAd = null;//激励广告对象
    private AdLoadStatus mRewardAdLoadStatus = AdLoadStatus.als_Unload;//当前激励广告的加载状态
    private ResponseInfo mRewardAdResponseInfo = null;
    private boolean mIsEarnedReward = false; //是否获取奖励
    private final RewardAdLoadListener mRewardAdLoadListener = new RewardAdLoadListener();//激励广告加载监听
    private final RewardAdShowListener mRewardAdShowListener = new RewardAdShowListener();//激励广告展示监听
    private final RewardAdPaidListener mRewardAdPaidListener = new RewardAdPaidListener();//全屏广告收益监听
    private int mRewardTotalFailTimes = 0;//激励广告请求失败的累计次数

    private final String TAG = "AdMob-RewardAd";

    public void initRewardLib(Activity activity)
    {
        mActivity = activity;
    }

    //加载激励广告。所有状态判定完全后，调用请求激励广告逻辑(runOnUiThread)
    protected void loadRewardAd()
    {
        mActivity.runOnUiThread(() -> {
            if(!AdManager.isConnectNetwork())
            {
                return;
            }
            //如果全屏、横幅、开屏广告正在请求，则终止此次激励广告请求
            if (AdManager.isFullAdLoading() || AdManager.isBannerAdLoading() || AdManager.isOpenAdLoading())
            {
                AdManager.addRequestAd("RewardAd");
                return;
            }
            //如果状态不对，则终止此次广告请求，进行下一个广告请求
            if((mRewardAd != null) || (mRewardAdLoadStatus != AdLoadStatus.als_Unload))
            {
                Log.d(TAG, String.format("Reward Ad was loading or loaded: %s", mRewardAdLoadStatus.toString()));
                //加载下一个广告
                if (isRewardAdLoaded())
                {
                    AdManager.loadNextAd();
                }
                return;
            }
            requestRewardAd();
        });
    }
    //请求激励广告(runOnUiThread)
    private void requestRewardAd()
    {
        mActivity.runOnUiThread(() -> {
            try {
                Log.d(TAG, "Reward Ad Start to load");
                //Firebase事件记录
                FirebaseManager.logAdLoadEvent("ad_load_start", "Rewarded");

                mRewardAdLoadStatus = AdLoadStatus.als_Loading;
                AdRequest adRequest = new AdRequest.Builder().build();
                RewardedAd.load(mActivity, mRewardAdId, adRequest, mRewardAdLoadListener);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //展示激励广告(runOnUiThread)
    protected void showRewardAd()
    {
        mActivity.runOnUiThread(() -> {
            if(mRewardAd == null)
            {
                return;
            }
            if (!isRewardAdLoaded())
            {
                return;
            }
            try {
                mRewardAd.show(mActivity, rewardItem -> {
                    //int rewardAmount = rewardItem.getAmount();
                    //String rewardType = rewardItem.getType();
                    mIsEarnedReward = true;
                });
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //激励广告是否正在加载
    protected boolean isRewardAdLoading()
    {
        return (mRewardAdLoadStatus == AdLoadStatus.als_Loading);
    }
    //激励广告是否加载完成
    protected boolean isRewardAdLoaded()
    {
        return (mRewardAdLoadStatus == AdLoadStatus.als_Loaded);
    }

    /** Reward Ad load listener **/
    class RewardAdLoadListener extends RewardedAdLoadCallback {
        @Override//(runOnGLThread)
        public void onAdLoaded(@NonNull final RewardedAd rewardedAd)
        {
            Log.d(TAG, "Reward Ad load Success");
            //Firebase事件记录
            /*FirebaseManager.logAdLoadEvent("ad_load_success", "Rewarded");*/
            //清空请求失败累计次数
            mRewardTotalFailTimes = 0;
            //设置激励广告播放对象
            mRewardAd = rewardedAd;
            //修改当前激励广告状态为已加载
            mRewardAdLoadStatus = AdLoadStatus.als_Loaded;
            //设置激励广告播放监听
            mRewardAd.setFullScreenContentCallback(mRewardAdShowListener);
            //设置激励广告收益监听
            mRewardAd.setOnPaidEventListener(mRewardAdPaidListener);
            //加载下一个广告
            AdManager.loadNextAd();
            //AdmobManager.addRequestAd("RewardAd");
            //调用激励广告加载完毕回调
            AdSdk.onRewardAdLoaded();
        }
        @Override//(runOnGLThread)
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError)
        {
            // Handle the error.
            Log.e(TAG, "Reward Ad load listener: Reward Ad load failed: " + loadAdError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "Rewarded");

            mRewardTotalFailTimes ++;
            Log.d(TAG, String.format(Locale.getDefault(),"Reward Ad load failed total times: %d", mRewardTotalFailTimes));
            //重置数据
            mRewardAd = null;
            mRewardAdLoadStatus = AdLoadStatus.als_Unload;
            if (mRewardTotalFailTimes < MAX_REWARD_REQUEST_FAIL_TIMES)
            {
                AdManager.addRequestAd("RewardAd");
            }
            else
            {
                Log.d(TAG, "Reward Ad load failed too many times. Stop to continue load Reward Ad");
                mRewardTotalFailTimes = 0;
                //加载下一个广告
                AdManager.loadNextAd();
                //准备一个新的激励广告
                AdManager.addRequestAd("RewardAd");
            }
            //调用激励广告加载失败回调
            AdSdk.onRewardAdLoadFailed();
        }
    }
    /** Reward Ad show listener **/
    class RewardAdShowListener extends FullScreenContentCallback{
        @Override
        public void onAdShowedFullScreenContent()
        {
            //重置数据前获得RewardAdResponseInfo
            mRewardAdResponseInfo = mRewardAd.getResponseInfo();
            //重置数据，防止二次调用
            mRewardAd = null;
            mRewardAdLoadStatus = AdLoadStatus.als_Unload;
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adv_show_success");
        }
        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError)
        {
            // Called when ad fails to show.
            Log.e(TAG, "Reward Ad failed to show: " + adError.getMessage());
            //重置数据
            mRewardAd = null;
            mRewardAdLoadStatus = AdLoadStatus.als_Unload;
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adv_show_fail");
           
            AdSdk.onRewardAdCanceled();
            mIsEarnedReward = false;

            AdManager.loadNextAd();
            //准备新一个激励广告
            AdManager.addRequestAd("RewardAd");
        }
        @Override//(runOnGLThread)
        public void onAdDismissedFullScreenContent()
        {
            //Firebase事件记录
/*            Bundle bundle = new Bundle();
            bundle.putBoolean("IsEarnedReward", mIsEarnedReward);*/
            //FirebaseManager.logParamsEvent("adv_close", bundle);

            if(mIsEarnedReward)
            {
                AdSdk.onRewardAdViewed();
            }
            else
            {
                AdSdk.onRewardAdCanceled();
            }
            mIsEarnedReward = false;
            AdManager.loadNextAd();
            //准备新一个激励广告
            AdManager.addRequestAd("RewardAd");
        }
    }
    /** Reward ad paid event listener **/
    class RewardAdPaidListener implements OnPaidEventListener {
        @Override
        public void onPaidEvent(@NonNull AdValue adValue)
        {
            final double value = adValue.getValueMicros() / 1000000.0;
            //Debug Log
            Log.d(TAG, String.format(Locale.getDefault(),"reward onPaidEvent: %f %s", value, adValue.getCurrencyCode()));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(adValue,"rewarded");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adv_paid_0");
            }

            //开启子线程上报广告收益到AppsFlyer
            if(mRewardAdResponseInfo != null){
                Runnable runnable = () -> {
/*                    String currency = adValue.getCurrencyCode();
                    Log.d(TAG, "Reward AppsflyerAdRevenue");
                    //Appsflyer接入
                    String sourceName = mRewardAdResponseInfo.getLoadedAdapterResponseInfo().getAdSourceName();
                    Log.d(TAG, "SourceName:"+sourceName);*/
                    /*AppsFlyerAdRevenue.logAdRevenue(sourceName, MediationNetwork.googleadmob, Currency.getInstance(currency), value, null);*/
                    mRewardAdResponseInfo = null;
                    Log.d(TAG, "Reward AdjustAdRevenue");
                    AdjustSdk.trackAdRevenue(adValue);

                };
                Thread thread = new Thread(runnable, "RewardAdThread");
                thread.start();
            }else{
                Log.e(TAG, "mRewardAdResponseInfo is Null!");
            }
        }
    }
}
