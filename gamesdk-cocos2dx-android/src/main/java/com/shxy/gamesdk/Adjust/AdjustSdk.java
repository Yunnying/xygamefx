package com.shxy.gamesdk.Adjust;

/**
 * @author: 翟宇翔
 * @date: 2023/10/18
 * Modified at: 2023/12/25
 */

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.AdjustThirdPartySharing;
import com.adjust.sdk.LogLevel;
import com.google.android.gms.ads.AdValue;

import java.util.HashSet;


public class AdjustSdk {

    private static String mInAppPurchaseToken = "";
    private static boolean mIsDMACountry = false;

    private static final HashSet<String> DMACountrySet = new HashSet<String>() {{
        add("AT");//Austria
        add("BE");//Belgium
        add("BG");//Bulgaria
        add("HR");//Croatia
        add("CY");//Cyprus
        add("CZ");//Czech Republic
        add("DK");//Denmark
        add("EE");//Estonia
        add("FI");//Finland
        add("FR");//France
        add("DE");//Germany
        add("GR");//Greece
        add("HU");//Hungary
        add("IE");//Ireland
        add("IT");//Italy
        add("LV");//Latvia
        add("LT");//Lithuania
        add("LU");//Luxembourg
        add("MT");//Malta
        add("NL");//Netherlands
        add("PL");//Poland
        add("PT");//Portugal
        add("RO");//Romania
        add("SK");//Slovakia
        add("SI");//Slovenia
        add("ES");//Spain
        add("SE");//Sweden
        add("GB");//United Kingdom
        add("CH");//Switzerland
        add("NO");//Norway
        add("IS");//Iceland
    }};
    /**
     * 初始化Adjust
     * @param application 初始化使用的application
     * @param appToken 应用的appToken
     * @param inAppPurchaseToken 应用的inAppPuechaseToken
     * @param isDebug 是否开启debug模式
     */
    public static void init(Application application, String appToken, String inAppPurchaseToken, boolean isDebug){
        String environment = "";
        mInAppPurchaseToken = inAppPurchaseToken;
        AdjustConfig config;
        if(isDebug)
        {
            config = new AdjustConfig(application, appToken, AdjustConfig.ENVIRONMENT_SANDBOX);
            config.setLogLevel(LogLevel.VERBOSE); //设置日志 关键日志 tag:Adjust  Install tracked
        }
        else
        {
            config = new AdjustConfig(application.getApplicationContext(), appToken, AdjustConfig.ENVIRONMENT_PRODUCTION);
            config.setLogLevel(LogLevel.SUPRESS); //设置日志 关键日志 tag:Adjust  Install tracked
        }
        String countryCode = application.getResources().getConfiguration().locale.getCountry();
        mIsDMACountry = DMACountrySet.contains(countryCode);

        new Thread(() -> {
            trackThirdPartySharing(mIsDMACountry, true, true); //设置第三方追踪状态
            Adjust.onCreate(config);
        }).start();
    }

    /**
     * 跟踪Admob收益事件
     * @param adValue Advalue对象
     */
    public static void trackAdRevenue(AdValue adValue){
        final Double value = adValue.getValueMicros() / 1000000.0;
        AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
        adRevenue.setRevenue(value, adValue.getCurrencyCode());
        Adjust.trackAdRevenue(adRevenue);
    }

    /**
     * 跟踪收益事件
     * @param revenue 收益
     * @param currencyCode 币种
     */
    public static void trackPurchaseRevenue(double revenue, String currencyCode){
        if(mInAppPurchaseToken.isEmpty()){
            Log.d("AdjustSdk", "trackPurchaseRevenue: Empty InAppPuechaseToken!");
            return;
        }
        AdjustEvent adjustEvent = new AdjustEvent(mInAppPurchaseToken);
        adjustEvent.setRevenue(revenue,currencyCode);
        Adjust.trackEvent(adjustEvent);
    }
    /**
     * 跟踪AppLovin收益事件
     * @param value 收益金额
     */
    public static void trackAdRevenue(double value){
        AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX);
        adRevenue.setRevenue(value, "USD");
        Adjust.trackAdRevenue(adRevenue);
    }

    /**
     * 上传事件
     * @param eventName 预设的事件id
     */
    public static void trackEvent(String eventName){
        AdjustEvent adjustEvent = new AdjustEvent(eventName);
        Adjust.trackEvent(adjustEvent);
    }

    /**
     * 向 Google 提供许可数据 (《数字市场法案》合规)
     */
    public static void trackThirdPartySharing(boolean isEEAUser, boolean canUsePersonalizedData, boolean canUseUserData){
        AdjustThirdPartySharing adjustThirdPartySharing = new AdjustThirdPartySharing(null);
        adjustThirdPartySharing.addGranularOption("google_dma", "eea", (isEEAUser)?"1":"0");
        adjustThirdPartySharing.addGranularOption("google_dma", "ad_personalization", (canUsePersonalizedData)?"1":"0");
        adjustThirdPartySharing.addGranularOption("google_dma", "ad_user_data", (canUseUserData)?"1":"0");
        Adjust.trackThirdPartySharing(adjustThirdPartySharing);
    }

    public static final class AdjustLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {}
        @Override
        public void onActivityStarted(Activity activity) {}
        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        @Override
        public void onActivityStopped(Activity activity) {}
        @Override
        public void onActivityDestroyed(Activity activity) {}
        @Override
        public void onActivityResumed(Activity activity) {
            Adjust.onResume();
        }
        @Override
        public void onActivityPaused(Activity activity) {
            Adjust.onPause();
        }

        //...
    }
}
