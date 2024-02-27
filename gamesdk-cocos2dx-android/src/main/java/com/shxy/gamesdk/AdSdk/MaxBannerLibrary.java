package com.shxy.gamesdk.AdSdk;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import java.util.Locale;

/**
 * @author: 翟宇翔
 * @date: 2023/12/14
 */
public class MaxBannerLibrary extends BaseBannerLibrary {
    private MaxAdView mAdView = null;
    private int mBannerTotalFailTimes = 0;//横幅广告请求失败的累计次数
    public static int MAX_BANNER_TOTAL_FAIL_TIMES = 3;//横幅广告请求失败的最大次数(超过次数则停止自动请求)
    private static final String mBannerAdId = AdManager.BannerAdId();
    private Activity mActivity = null;
    private FrameLayout mFrameLayout = null;
    private boolean mIsBannerVisible = false;//当前横幅广告是否可见
    private AdLoadStatus mBannerAdLoadStatus = AdLoadStatus.als_Unload;
    private final BannerAdListener mBannerAdListener = new BannerAdListener();
    private final BannerAdRevenueListener mBannerAdRevenueListener = new BannerAdRevenueListener();
    private int mBannerHeightPixels = 0;
    private final String TAG = "MAX-BannerAd";

    protected void initBannerLib(Activity activity, FrameLayout frameLayout)
    {
        mActivity = activity;
        mFrameLayout = frameLayout;
    }

    protected void initBannerAd(){
        mActivity.runOnUiThread(()->{
            if(mAdView != null){
                mAdView.destroy();
            }
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);
            mAdView = new MaxAdView(mBannerAdId, mActivity);
            mAdView.setListener(mBannerAdListener);
            mAdView.setRevenueListener(mBannerAdRevenueListener);
            // Stretch to the width of the screen for banners to be fully functional
            int width = FrameLayout.LayoutParams.WRAP_CONTENT;

            // Banner height on phones and tablets is 50 and 90, respectively
            mBannerHeightPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, mActivity.getApplication().getResources().getDisplayMetrics());

            FrameLayout.LayoutParams layoutParams  = new FrameLayout.LayoutParams(width, mBannerHeightPixels);
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            mAdView.setLayoutParams(layoutParams);

            // Set background or background color for banners to be fully functional
            //mAdView.setBackgroundColor( ... );
            mFrameLayout.addView( mAdView );
        });
    }

    protected void loadBannerAd(){
        if (mActivity == null || isBannerLoaded() || AdManager.getRemoveAdMode())
        {
            AdManager.loadNextAd();
            return;
        }

        mActivity.runOnUiThread(() -> {
            if(!AdManager.isConnectNetwork())
            {
                return;
            }
            //如果激励、全屏、开屏广告正在请求，则终止此次的横幅广告请求
            if (AdManager.isRewardAdLoading() || AdManager.isFullAdLoading())
            {
                AdManager.addRequestAd("BannerAd");
                return;
            }
            if(mBannerAdLoadStatus != AdLoadStatus.als_Unload)
            {
                Log.d(TAG,String.format("Banner Ad was loading or loaded: %s", mBannerAdLoadStatus.toString()));
                //加载下一个广告
                if (isBannerLoaded())
                {
                    AdManager.loadNextAd();
                }
                return;
            }
            requestBanner();
        });
    }

    private void requestBanner()
    {
        if (mActivity == null)
        {
            return;
        }
        mActivity.runOnUiThread(() -> {
            try {
                Log.d(TAG,"Banner Ad Start to load");
                //Firebase事件记录
                FirebaseManager.logAdLoadEvent("ad_load_start", "Banner");
                mBannerAdLoadStatus = AdLoadStatus.als_Loading;
                mAdView.loadAd();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    protected void showBannerAd(final boolean isVisible)
    {
        if (mActivity == null || AdManager.getRemoveAdMode())
        {
            AdManager.loadNextAd();
            return;
        }
        mActivity.runOnUiThread(() -> {
            mIsBannerVisible = isVisible;
            if(mAdView == null)
            {
                return;
            }
            if (!isBannerLoaded())
            {
                AdManager.addRequestAd("BannerAd");
                return;
            }
            mAdView.setVisibility(mIsBannerVisible ? View.VISIBLE : View.GONE);
            if (isVisible)
            {
                AdSdk.onBannerAdShown();
            }
        });
    }

    protected boolean isBannerLoaded()
    {
        return (mBannerAdLoadStatus == AdLoadStatus.als_Loaded);
    }
    //横幅广告是否正在加载
    protected boolean isBannerLoading()
    {
        return (mBannerAdLoadStatus == AdLoadStatus.als_Loading);
    }
    //横幅广告是否显示
    protected boolean isBannerAdVisible()
    {
        return mIsBannerVisible;
    }

    protected float getAdHeight(){
        return mBannerHeightPixels;
    }

    class BannerAdListener implements MaxAdViewAdListener{

        @Override
        public void onAdExpanded(@NonNull MaxAd maxAd) {

        }

        @Override
        public void onAdCollapsed(@NonNull MaxAd maxAd) {

        }

        @Override
        public void onAdLoaded(@NonNull MaxAd maxAd) {
            Log.d(TAG,"Banner Ad load Success");
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_success", "Banner");
            //清空请求失败累计次数
            mBannerTotalFailTimes = 0;
            //修改当前激励广告状态为已加载
            mBannerAdLoadStatus = AdLoadStatus.als_Loaded;
            //加载下一个广告
            AdManager.loadNextAd();
            //再次加入到广告队列
            AdManager.addRequestAd("BannerAd");
            //调用横幅广告加载完毕回调
            AdSdk.onBannerAdLoaded();
        }

        @Override
        public void onAdDisplayed(@NonNull MaxAd maxAd) {
            //马上废弃，不要使用
        }

        @Override
        public void onAdHidden(@NonNull MaxAd maxAd) {
            //马上废弃，不要使用
        }

        @Override
        public void onAdClicked(@NonNull MaxAd maxAd) {
            Log.d(TAG,"Banner Ad Click");
            mBannerAdLoadStatus = AdLoadStatus.als_Unload;
            AdSdk.onBannerAdClicked();
        }

        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
            Log.e(TAG,"Banner Ad load listener: Banner Ad load failed: " + maxError.getMessage());
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_failed", "Banner");

            mBannerTotalFailTimes ++;
            Log.d(TAG,String.format(Locale.getDefault(),"Banner Ad load failed total times: %d", mBannerTotalFailTimes));
            mBannerAdLoadStatus = AdLoadStatus.als_Unload;
            //再次加入广告请求队列
            if (mBannerTotalFailTimes < MAX_BANNER_TOTAL_FAIL_TIMES)
            {
                AdManager.addRequestAd("BannerAd");
            }
            else
            {
                Log.d(TAG,"Banner Ad load failed too many times. Stop to continue load Banner Ad");
                mBannerTotalFailTimes = 0;
                //加载下一个广告
                AdManager.loadNextAd();
                AdManager.addRequestAd("BannerAd");
            }
        }

        @Override
        public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {

        }
    }

    class BannerAdRevenueListener implements MaxAdRevenueListener {

        @Override
        public void onAdRevenuePaid(@NonNull MaxAd maxAd) {
            double value = maxAd.getRevenue();
            Log.d(TAG, String.format(Locale.getDefault(),"Banner onPaidEvent: %f USD", value));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(value,"banner");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adb_paid_0");
            }
            //开启子线程上报广告收益到Adjust（MAX后台已经上报，注释）
/*            Runnable runnable = () -> {
                Log.d(TAG, "Banner AdjustAdRevenue");
                AdjustSdk.trackAdRevenue(value);
            };
            Thread thread = new Thread(runnable, "BannerAdThread");
            thread.start();*/
        }
    }

}
