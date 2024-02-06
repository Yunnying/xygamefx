package com.shxy.gamesdk.AdSdk;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

/*import com.appsflyer.adrevenue.AppsFlyerAdRevenue;
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork;*/
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;

import com.shxy.gamesdk.Adjust.AdjustSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import android.app.Activity;

import java.util.Locale;

public class AdmobBannerLibrary extends BaseBannerLibrary
{
    private Activity mActivity = null;//cocos2d的Activity
    private FrameLayout mFrameLayout = null;
    /** Banner Ad **/
    public static int MAX_BANNER_TOTAL_FAIL_TIMES = 3;//横幅广告请求失败的最大次数(超过次数则停止自动请求)
    private static final String mBannerAdId = AdManager.BannerAdId();
    private AdView mAdView = null;//横幅广告对象
    private boolean mIsBannerVisible = false;//当前横幅广告是否可见
    private AdLoadStatus mBannerAdLoadStatus = AdLoadStatus.als_Unload;//当前横幅广告的加载状态
    private final BannerAdListener mBannerAdListener = new BannerAdListener();//横幅广告监听
    private final BannerAdPaidListener mBannerAdPaidListener = new BannerAdPaidListener();//横幅广告收益监听
    private int mBannerTotalFailTimes = 0;//横幅广告请求失败的累计次数
    private int mWinWidth = 0;//当前屏幕的宽度
    private AdSize mAdSize = null;//广告尺寸
    private final String TAG = "AdMob-BannerAd";

    protected void initBannerLib(Activity activity, FrameLayout frameLayout)
    {
        mActivity = activity;
        mFrameLayout = frameLayout;
        mAdSize = getAdSize();
    }

    //开始加载横幅广告
    protected void loadBannerAd()
    {
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
            if (AdManager.isRewardAdLoading() || AdManager.isFullAdLoading() || AdManager.isOpenAdLoading())
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
            if(mAdView != null)
            {
                mAdView.setAdListener(null);
                mFrameLayout.removeView(mAdView);
                mAdView.destroy();
                mAdView = null;
            }
            requestBanner();
        });
    }
    //请求横幅广告(runOnUiThread)
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
                mAdView = new AdView(mActivity);
                mAdView.setAdUnitId(mBannerAdId);
/*                if(mAdSize == null){
                    mAdSize = getAdSize();
                }*/
                mAdView.setAdSize(mAdSize);
                FrameLayout.LayoutParams layoutInfo = new FrameLayout.LayoutParams(mAdSize.getWidthInPixels(mActivity), mAdSize.getHeightInPixels(mActivity));
                layoutInfo.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                mAdView.setLayoutParams(layoutInfo);
                mAdView.setAdListener(mBannerAdListener);
                mFrameLayout.addView(mAdView);

                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }
    //展示横幅广告(runOnUiThread)
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
    //横幅广告是否加载完成
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
    //获取广告尺寸
    private AdSize getAdSize()
    {
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        mWinWidth = outMetrics.widthPixels;
        float widthPixels = (float)mWinWidth;
        if (AdManager.isChromeBook())
        {
            float heightPixels = outMetrics.heightPixels;
            widthPixels = 720.f * heightPixels / 1280.f * 1.2f;
        }
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);

        return AdSize.getPortraitAnchoredAdaptiveBannerAdSize(mActivity, adWidth);
    }

    protected float getAdHeight() {
/*        if (mWinWidth == 0){
            DisplayMetrics displayMetrics = new DisplayMetrics();
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            mWinWidth = displayMetrics.widthPixels;
        }*/
        int pixelHeight = mAdSize.getHeightInPixels(mActivity);
        int pixelWidth = mAdSize.getWidthInPixels(mActivity);
        float scale = (float)pixelWidth / (float)mWinWidth;
        return (float) pixelHeight / scale;
    }

/*    protected void setWinWidth(int winWidth)
    {
*//*        Log.d(TAG, "Cocos2dx获得的宽度为："+winWidth);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        Log.d(TAG, "Java方法获得的宽度为："+ screenWidth);*//*
        mWinWidth = winWidth;
        //在这里设置广告尺寸
        mAdSize = getAdSize();
    }*/

    public void onPause()
    {
        if(mAdView != null)
        {
            mAdView.pause();
        }
    }

    public void onResume()
    {
        if(mAdView != null)
        {
            mAdView.resume();
        }
    }

    /** Banner Ad load listener **/
    class BannerAdListener extends AdListener {
        @Override//(runOnGLThread)
        public void onAdLoaded()
        {
            super.onAdLoaded();
            Log.d(TAG,"Banner Ad load Success");
            //Firebase事件记录
            //FirebaseManager.logAdLoadEvent("ad_load_success", "Banner");
            //清空请求失败累计次数
            mBannerTotalFailTimes = 0;
            //修改当前激励广告状态为已加载
            mBannerAdLoadStatus = AdLoadStatus.als_Loaded;
            if (mAdView != null)
            {
                mAdView.setOnPaidEventListener(mBannerAdPaidListener);
            }
            //加载下一个广告
            AdManager.loadNextAd();
            //再次加入到广告队列
            AdManager.addRequestAd("BannerAd");
            //调用横幅广告加载完毕回调
            AdSdk.onBannerAdLoaded();
        }
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError)
        {
            super.onAdFailedToLoad(loadAdError);
            Log.e(TAG,"Banner Ad load listener: Banner Ad load failed: " + loadAdError.getMessage());
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
        public void onAdClosed()
        {
            super.onAdClosed();
            mBannerAdLoadStatus = AdLoadStatus.als_Unload;
            AdManager.addRequestAd("BannerAd");
        }

        @Override
        public void onAdOpened()
        {
            super.onAdOpened();
        }

        @Override
        public void onAdClicked()
        {
            super.onAdClicked();
            Log.d(TAG,"Banner Ad Click");
            mBannerAdLoadStatus = AdLoadStatus.als_Unload;
            AdSdk.onBannerAdClicked();
        }
    }
    /** Banner ad paid event listener **/
    class BannerAdPaidListener implements OnPaidEventListener {
        @Override
        public void onPaidEvent(@NonNull AdValue adValue)
        {
            final double value = adValue.getValueMicros() / 1000000.0;
            //Debug Log
            Log.d(TAG,String.format(Locale.getDefault(),"Banner onPaidEvent: %f %s", value, adValue.getCurrencyCode()));
            //上报Firebase(Taichi 1.0, 1.0自动调用2.5和3.0)
            FirebaseManager.logAdRevenue(adValue,"banner");
            //Firebase事件记录
            if (value <= 0)
            {
                FirebaseManager.logNullParamEvent("adb_paid_0");
            }
            //开启子线程上报广告收益到AppsFlyer
            if(mAdView != null){
                Runnable runnable = () -> {
                    /*String currency = adValue.getCurrencyCode();*/
                    Log.d(TAG, "Banner AdjustAdRevenue");
                    //Appsflyer接入
/*                    String sourceName =mAdView.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName();
                    AppsFlyerAdRevenue.logAdRevenue(sourceName, MediationNetwork.googleadmob, Currency.getInstance(currency), value, null);*/

                    AdjustSdk.trackAdRevenue(adValue);
                };

                Thread thread = new Thread(runnable, "BannerAdThread");
                thread.start();
            }else{
                Log.e(TAG, "mAdView is Null!");
            }
        }
    }
}
