package com.shxy.gamesdk.AdSdk;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import java.util.Locale;

/**
 * @author: 翟宇翔
 * @date: 2023/12/15
 */
public class MaxOpenLibrary extends BaseOpenLibrary{
    private Activity mActivity = null;//cocos2d的Activity
    private int mOpenTotalFailTimes = 0;//开屏广告请求失败的累计次数
    /** Open Ad **/
    public static int MAX_OPEN_TOTAL_FAIL_TIMES = 3;//开屏广告请求失败的最大次数(超过次数则停止自动请求)
    private static final String mOpenAdId = AdManager.AdmobOpenAdId();
    private static MaxAppOpenAd mOpenAd = null;//开屏广告对象
    private AdLoadStatus mOpenAdLoadStatus = AdLoadStatus.als_Unload;//当前开屏广告的加载状态
    private final OpenAdListener mOpenAdListener = new OpenAdListener();
    private final OpenAdRevenueListener mOpenAdRevenueListener = new OpenAdRevenueListener();
    private final String TAG = "MAX-OpenAd";
    protected void initAdOpenLib(Activity activity) {
        mActivity = activity;
    }

    protected void initOpenAd(){
        mActivity.runOnUiThread(()->{
            if(mOpenAd != null){
                mOpenAd.destroy();
            }
            mOpenAd = new MaxAppOpenAd(mOpenAdId,mActivity);
            mOpenAd.setListener(mOpenAdListener);
            mOpenAd.setRevenueListener(mOpenAdRevenueListener);
        });
    }

    protected void loadOpenAd() {
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
            if (mOpenAd.isReady() || (mOpenAdLoadStatus != AdLoadStatus.als_Unload))
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

    private void requestOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            try
            {
                Log.d(TAG, "Open Ad Start to load");
                //Firebase事件记录
                //FirebaseManager.logAdLoadEvent("ad_load_start", "AppOpen");

                mOpenAdLoadStatus = AdLoadStatus.als_Loading;
                mOpenAd.loadAd();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    protected void showOpenAd()
    {
        mActivity.runOnUiThread(() -> {
            if(!mOpenAd.isReady())
            {
                return;
            }
            if (!isOpenAdLoaded())
            {
                return;
            }
            try
            {
                mOpenAd.showAd();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    protected boolean isOpenAdLoading() {
        return mOpenAdLoadStatus == AdLoadStatus.als_Loading;
    }


    protected boolean isOpenAdLoaded() {
        return mOpenAdLoadStatus == AdLoadStatus.als_Loaded;
    }

    class OpenAdListener implements MaxAdListener {

        @Override
        public void onAdLoaded(@NonNull MaxAd maxAd) {
            Log.d(TAG, "Open load listener: Open Ad load Success");
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_success", "AppOpen");
            //清空请求失败累计次数
            mOpenTotalFailTimes = 0;
            //修改当前开屏广告状态为已加载
            mOpenAdLoadStatus = AdLoadStatus.als_Loaded;
            //加载下一个广告
            AdManager.loadNextAd();
            //调用开屏广告加载完毕回调
            AdSdk.onOpenAdLoaded();
        }

        @Override
        public void onAdDisplayed(@NonNull MaxAd maxAd) {
            mOpenAdLoadStatus= AdLoadStatus.als_Unload;
        }

        @Override
        public void onAdHidden(@NonNull MaxAd maxAd) {
            AdSdk.onOpenAdClosed();
            // 重置数据
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
            AdManager.loadNextAd();
            //准备新一个开屏广告
            AdManager.addRequestAd("OpenAd");
        }

        @Override
        public void onAdClicked(@NonNull MaxAd maxAd) {

        }

        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
            Log.e(TAG, "Open Ad load listener: Open Ad load failed: " + maxError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "AppOpen");

            mOpenTotalFailTimes ++;
            Log.d(TAG, String.format(Locale.getDefault(),"Open Ad load failed total times: %d", mOpenTotalFailTimes));
            //重置数据
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
                AdManager.addRequestAd("OpenAd");
            }
            AdSdk.onOpenAdLoadFailed();
        }

        @Override
        public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
            Log.e(TAG, "Full Ad failed to show: " + maxError.getMessage());
            // 重置数据
            mOpenAdLoadStatus = AdLoadStatus.als_Unload;
            AdSdk.onOpenAdClosed();
            AdManager.loadNextAd();
            //准备新一个开屏广告
            AdManager.addRequestAd("OpenAd");
        }
    }

    class OpenAdRevenueListener implements MaxAdRevenueListener {

        @Override
        public void onAdRevenuePaid(@NonNull MaxAd maxAd) {
            double value = maxAd.getRevenue();
            Log.d(TAG, String.format(Locale.getDefault(),"Open onPaidEvent: %f USD", value));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(value,"open");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adb_paid_0");
            }
            //开启子线程上报广告收益到Adjust
            Runnable runnable = () -> {
                Log.d(TAG, "Open AdjustAdRevenue");
                AdjustSdk.trackAdRevenue(value);
            };
            Thread thread = new Thread(runnable, "OpenAdThread");
            thread.start();
        }
    }
}
