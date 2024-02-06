package com.shxy.gamesdk.Adjust;

/**
 * @author: 翟宇翔
 * @date: 2023/10/18
 */

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.LogLevel;
import com.google.android.gms.ads.AdValue;


public class AdjustSdk {
    /**
     * 初始化Adjust
     * @param application 初始化使用的application
     * @param appToken 应用的appToken
     * @param isDebug 是否开启debug模式
     */
    public static void init(Application application, String appToken, boolean isDebug){
        String environment = "";
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

        Adjust.onCreate(config);
    }

    /**
     * 跟踪收益事件
     * @param adValue Advalue对象
     */
    public static void trackAdRevenue(AdValue adValue){
        final Double value = adValue.getValueMicros() / 1000000.0;
        AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
        adRevenue.setRevenue(value, adValue.getCurrencyCode());
        Adjust.trackAdRevenue(adRevenue);
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
