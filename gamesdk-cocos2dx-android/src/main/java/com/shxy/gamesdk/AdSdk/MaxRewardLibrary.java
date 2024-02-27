package com.shxy.gamesdk.AdSdk;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import java.util.Locale;

/**
 * MAX聚合使用的激励广告库
 * @author: 翟宇翔
 * @date: 2023/12/14
 */
public class MaxRewardLibrary extends BaseRewardLibrary{
    private MaxRewardedAd mRewardedAd = null;
    private int mRewardTotalFailTimes = 0;
    public static int MAX_REWARD_REQUEST_FAIL_TIMES = 3;
    private static final String mRewardAdId = AdManager.RewardAdId();
    private Activity mActivity = null;
    private AdLoadStatus mRewardAdLoadStatus = AdLoadStatus.als_Unload;
    private final RewardedAdListener mRewardedAdListener = new RewardedAdListener();
    private final RewardedAdRevenueListener mRewardedAdRevenueListener = new RewardedAdRevenueListener();
    private final String TAG = "MAX-RewardAd";

    /**
     * 初始化激励广告SDK
     * @param activity 当前Activity
     */
    public void initRewardLib(Activity activity) {
        mActivity = activity;
    }

    public void initRewardAd(){
        mActivity.runOnUiThread(()->{
            if (mRewardedAd!= null) {
                mRewardedAd.destroy();
            }
            mRewardedAd = MaxRewardedAd.getInstance(mRewardAdId, mActivity);
            mRewardedAd.setListener(mRewardedAdListener);
            mRewardedAd.setRevenueListener(mRewardedAdRevenueListener);
        });

    }

    /**
     * 加载激励广告
     */
    protected void loadRewardAd(){
        mActivity.runOnUiThread(()->{
            //如果全屏、横幅、开屏广告正在请求，则终止此次激励广告请求
            if (AdManager.isFullAdLoading() || AdManager.isBannerAdLoading())
            {
                AdManager.addRequestAd("RewardAd");
                return;
            }
            //如果状态不对，则终止此次广告请求，进行下一个广告请求
            if((mRewardedAd.isReady()) || (mRewardAdLoadStatus != AdLoadStatus.als_Unload))
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

    /**
     * 请求激励广告
     */
    private void requestRewardAd(){
        mActivity.runOnUiThread(() -> {
            try {
                Log.d(TAG, "Reward Ad Start to load");
                //Firebase事件记录
                FirebaseManager.logAdLoadEvent("ad_load_start", "Rewarded");

                mRewardAdLoadStatus = AdLoadStatus.als_Loading;
                mRewardedAd.loadAd();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    /**
     * 显示激励广告
     */
    protected void showRewardAd(){
        mActivity.runOnUiThread(() -> {
            if (!mRewardedAd.isReady())
            {
                Log.e(TAG, "showRewardAd: Reward Ad is not ready!");
                return;
            }
            try {
                mRewardedAd.showAd();
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
        return (mRewardAdLoadStatus == AdLoadStatus.als_Loaded && mRewardedAd.isReady());
    }


    //激励广告收益监听
    class RewardedAdListener implements MaxRewardedAdListener {

        /**
         * 用户观看完成激励广告
         * @param maxAd
         * @param maxReward
         */
        @Override
        public void onUserRewarded(@NonNull MaxAd maxAd, @NonNull MaxReward maxReward) {

            Log.d(TAG, "Reward Ad shown Success");

            AdSdk.onRewardAdViewed();
            //加载下一个广告
            AdManager.loadNextAd();
            //准备新一个激励广告
            AdManager.addRequestAd("RewardAd");
        }

        @Override
        public void onRewardedVideoStarted(@NonNull MaxAd maxAd) {
            //已废弃方法
        }

        @Override
        public void onRewardedVideoCompleted(@NonNull MaxAd maxAd) {
            //已废弃方法
        }

        /**
         * 加载激励广告成功
         * @param maxAd
         */
        @Override
        public void onAdLoaded(@NonNull MaxAd maxAd) {
            Log.d(TAG, "Reward Ad load Success");
            //Firebase事件记录
            /*FirebaseManager.logAdLoadEvent("ad_load_success", "Rewarded");*/
            //清空请求失败累计次数
            mRewardTotalFailTimes = 0;
            //修改当前激励广告状态为已加载
            mRewardAdLoadStatus = AdLoadStatus.als_Loaded;
            //加载下一个广告
            AdManager.loadNextAd();
            //调用激励广告加载完毕回调
            AdSdk.onRewardAdLoaded();
        }

        /**
         * 显示激励广告
         * @param maxAd
         */
        @Override
        public void onAdDisplayed(@NonNull MaxAd maxAd) {
            mRewardAdLoadStatus = AdLoadStatus.als_Unload;
        }

        /**
         * 关闭激励广告
         * @param maxAd
         */
        @Override
        public void onAdHidden(@NonNull MaxAd maxAd) {
            Log.d(TAG, "Reward Ad hidden");
            //用户关闭了广告
            AdManager.loadNextAd();
            //准备新一个激励广告
            AdManager.addRequestAd("RewardAd");
            //用户取消了广告
            AdSdk.onRewardAdCanceled();
        }

        @Override
        public void onAdClicked(@NonNull MaxAd maxAd) {

        }

        /**
         * 加载激励广告失败
         * @param s
         * @param maxError
         */
        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
            // Handle the error.
            Log.e(TAG, "Reward Ad load listener: Reward Ad load failed: " + maxError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "Rewarded");
            mRewardTotalFailTimes ++;
            Log.d(TAG, String.format(Locale.getDefault(),"Reward Ad load failed total times: %d", mRewardTotalFailTimes));
            mRewardAdLoadStatus = AdLoadStatus.als_Unload;
            if (mRewardTotalFailTimes < MAX_REWARD_REQUEST_FAIL_TIMES) {
                AdManager.addRequestAd("RewardAd");
            }
            else {
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

        /**
         * 显示激励广告失败
         * @param maxAd
         * @param maxError
         */
        @Override
        public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
            // Called when ad fails to show.
            Log.e(TAG, "Reward Ad failed to show: " + maxError.getMessage());
            //重置数据
            mRewardAdLoadStatus = AdLoadStatus.als_Unload;
            //Firebase事件记录
            //FirebaseManager.logNullParamEvent("adv_show_fail");
            //展示广告失败，视为取消展示
            AdSdk.onRewardAdCanceled();

            AdManager.loadNextAd();
            //准备新一个激励广告
            AdManager.addRequestAd("RewardAd");
        }
    }

    /**
     * 广告收益监听
     */
    class RewardedAdRevenueListener implements MaxAdRevenueListener {
        @Override
        public void onAdRevenuePaid(@NonNull MaxAd maxAd) {
            double value = maxAd.getRevenue();
            Log.d(TAG, String.format(Locale.getDefault(),"reward onPaidEvent: %f USD", value));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(value,"rewarded");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adv_paid_0");
            }
            //开启子线程上报广告收益到Adjust（MAX后台已经上报，注释）
/*            Runnable runnable = () -> {
                Log.d(TAG, "Reward AdjustAdRevenue");
                AdjustSdk.trackAdRevenue(value);
            };
            Thread thread = new Thread(runnable, "RewardAdThread");
            thread.start();*/
        }
    }
}
