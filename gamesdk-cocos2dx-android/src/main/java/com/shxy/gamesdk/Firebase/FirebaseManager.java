package com.shxy.gamesdk.Firebase;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdValue;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.shxy.gamesdk.BaseSdk.BaseSdk;
import com.shxy.gamesdk.R;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

public class FirebaseManager
{
    private static Activity mActivity = null;
    private static FirebaseAnalytics mFirebaseAnalytics = null;
    private static FirebaseRemoteConfig mFirebaseRemoteConfig = null;
    private static final String KEY_UPDATE_TIME = "last_update_time";//initTaichiDaily()使用，上次初始化Taichi2.5数据的时间
    private static final String KEY_AD_REVENUE_ONE_DAY = "key_ad_one_day_revenue"; //广告一天总收益的存储key(Taichi 2.5)
    private static final String KEY_TAICHI_LOG_IDX = "key_taichi_log_idx"; //广告打点索引值(Taichi 2.5，0-4)
    private static final String KEY_AD_REVENUE_TOTAL = "key_ad_total_revenue"; //广告累计总收益的存储key(Taichi 3.0)
    private static String EVENT_AD_REVENUE_IMPRESSION = "Ad_Impression_Revenue";//Taichi 1.0 Event Name
    private static final String EVENT_AD_REVENUE_TOTAL_001 = "Total_Ads_Revenue_001";//Taichi 3.0 Event Name
    private static final String[] mKeyTopPercentList = {"AdLTV_OneDay_Top50Percent", "AdLTV_OneDay_Top40Percent", "AdLTV_OneDay_Top30Percent", "AdLTV_OneDay_Top20Percent", "AdLTV_OneDay_Top10Percent"};
    private static String mVersionName = "";
    private static String mVersionCode = "";
    private static String mCountryCode = "";

    private static String TAG = "FirebaseManager";

    /**
     * 初始化Firebase
     * @param activity
     */
    public static void initManager(Activity activity)
    {
        mActivity = activity;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(mActivity);
        mCountryCode = mActivity.getResources().getConfiguration().locale.getCountry();
        //初始化App信息
        //initAppInfo();
        // Obtain the FirebaseAnalytics instance.
/*        mFirebaseAnalytics.setUserProperty("is_first_open", isFirstOpen()?"1":"0");
        mFirebaseAnalytics.setUserProperty("ram", getDeviceTotalRamString());
        mFirebaseAnalytics.setUserProperty("version", mVersionName);*/
        //init Taichi Daily.
        initTaichiDaily();
        // Obtain the FirebaseRemoteConfig instance.
        if(!FirebaseApp.getApps(mActivity).isEmpty())
        {
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        }
        if (mFirebaseRemoteConfig != null)
        {
            // FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build();//设置每小时更新一次，不加这句更新时长为12小时一次
            // mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(mActivity, task -> {
                if (task.isSuccessful())
                {
                    boolean updated = task.getResult();
                    Log.i(TAG,"firebase Config params updated: " + updated);
                    //FirebaseSdk.onRemoteConfigLoaded();
                }
                else
                {
                    Log.i(TAG,"firebase Config params updated: is fail");
                }
            });
        }
    }

/*    //获取版本名称和版本号
    private static void initAppInfo()
    {
        if (mActivity == null)
        {
            return;
        }
        setAppVersion();

    }

    private static void setAppVersion(){
        PackageManager manager = mActivity.getPackageManager();
        PackageInfo apkInfo;
        try
        {
            apkInfo = manager.getPackageInfo(mActivity.getPackageName(), 0);
            mVersionName = apkInfo.versionName;
            mVersionCode = String.format(Locale.getDefault(),"%d", apkInfo.versionCode);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    //获取设备总内存
    private static long getTotalMemory()
    {
        ActivityManager manager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        //设备内存
        manager.getMemoryInfo(mi);
        return mi.totalMem;
    }
    //获取设备总内存字符串
    private static String getDeviceTotalRamString()
    {
        long memory = getTotalMemory() / 1000000;
        return String.format(Locale.getDefault(),"%.1f", memory / 1024.0);
    }

    *//**
 * 是否同时启动
 * @return 为”1“时
 *//*
    private static boolean isFirstOpen()
    {
        long lastOpenTime = Cocos2dxHelper.getLongForKey("last_open_time", 0L);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        Cocos2dxHelper.setLongForKey("last_open_time",currentTime);
        if(lastOpenTime > 0L){
            return false;
        }else{
            return true;
        }
    }*/

    /**
     * 设置新的 EVENT_AD_REVENUE_IMPRESSION 名称
     * @param eventAdRevenueImpression 已有项目使用的EVENT_AD_REVENUE_IMPRESSION值
     */
    public static void setEventAdRevenueImpression(String eventAdRevenueImpression){
        EVENT_AD_REVENUE_IMPRESSION = eventAdRevenueImpression;
    }


    /******** Firebase 事件上传  begin **********/
    //无参数上传Firebase
    public static void logNullParamEvent(final String eventKey)
    {
        Log.i(TAG,"logNullParamEvent: " + eventKey);
        mActivity.runOnUiThread(() -> {
            if(mFirebaseAnalytics != null)
            {
                mFirebaseAnalytics.logEvent(eventKey, null);
            }
        });
    }
    //单个参数上传Firebase
    public static void logParamsEvent(final String eventKey, final Bundle paramBundle)
    {
        mActivity.runOnUiThread(() -> {
            if(mFirebaseAnalytics != null)
            {
                mFirebaseAnalytics.logEvent(eventKey, paramBundle);
            }
        });
    }


    /******** Firebase 广告收益(Taichi)  begin **********/
    //记录广告收益(Taichi 1.0 Ad_revenue_impression)
    public static void logAdRevenue(AdValue adValue, String AdType)
    {
        if (mFirebaseAnalytics != null)
        {
            double value = (double)adValue.getValueMicros() / 1000000.0;
            String currencyCode = adValue.getCurrencyCode();

            Bundle bundle = new Bundle();
            bundle.putDouble(FirebaseAnalytics.Param.VALUE, value);
            bundle.putString(FirebaseAnalytics.Param.CURRENCY, currencyCode);
            bundle.putString("ad_format",AdType);
            bundle.putString("CountryCode",mCountryCode);
            bundle.putInt("Ad_Level",BaseSdk.getIntegerForKey("Ad_Level",-1));
            mFirebaseAnalytics.logEvent(EVENT_AD_REVENUE_IMPRESSION, bundle);
            //上报Firebase(Taichi 2.5)
            logAdOneDayRevenue(value, currencyCode);
            //上报Firebase(Taichi 3.0)
            logAdTotalRevenue(value, currencyCode);
        }
    }
    //（MAX使用） 记录广告收益(Taichi 1.0 Ad_revenue_impression)
    public static void logAdRevenue(double value, String AdType)
    {
        if (mFirebaseAnalytics != null)
        {
            String currencyCode = "USD";
            Bundle bundle = new Bundle();
            bundle.putDouble(FirebaseAnalytics.Param.VALUE, value);
            bundle.putString(FirebaseAnalytics.Param.CURRENCY, currencyCode);
            bundle.putString("ad_format",AdType);
            bundle.putString("CountryCode",mCountryCode);
            bundle.putInt("Ad_Level",BaseSdk.getIntegerForKey("Ad_Level",-1));
            mFirebaseAnalytics.logEvent(EVENT_AD_REVENUE_IMPRESSION, bundle);
            //上报Firebase(Taichi 2.5)
            logAdOneDayRevenue(value, currencyCode);
            //上报Firebase(Taichi 3.0)
            logAdTotalRevenue(value, currencyCode);
        }
    }
    //每日初始化Taichi 2.5的数据记录
    public static void initTaichiDaily(){
        long curTime = System.currentTimeMillis()/1000L;//获取当前的UNIX时间戳(s)
        long lastUpdateTime = BaseSdk.getLongForKey(KEY_UPDATE_TIME,0L);
        long curZeroTime = curTime - ( curTime % 86400L);
        if ((curTime - lastUpdateTime) / 3600 >= 24){
            BaseSdk.setLongForKey(KEY_UPDATE_TIME, curZeroTime);
            BaseSdk.setFloatForKey(KEY_AD_REVENUE_ONE_DAY, 0.f);
            BaseSdk.setIntegerForKey(KEY_AD_REVENUE_ONE_DAY, 0);
        }
    }
    //记录广告收益(Taichi 2.5 AdLTV_OneDay_Top%dPercent)
    public static void logAdOneDayRevenue(double revenue, String currencyCode)
    {
        if (mFirebaseRemoteConfig == null)
        {
            return;
        }

        int idx = BaseSdk.getIntegerForKey(KEY_TAICHI_LOG_IDX, 0);
        if (idx >= mKeyTopPercentList.length)
        {
            return;
        }

        float curValue = BaseSdk.getFloatForKey(KEY_AD_REVENUE_ONE_DAY, 0.f);
        curValue += revenue;
        BaseSdk.setFloatForKey(KEY_AD_REVENUE_ONE_DAY, curValue);

        String eventKey = mKeyTopPercentList[idx];
        float value = getFloatRemoteValue(eventKey);
        if (curValue >= value)
        {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CURRENCY, currencyCode);
            bundle.putFloat(FirebaseAnalytics.Param.VALUE, curValue);
            mFirebaseAnalytics.logEvent(eventKey, bundle);
            idx ++;
            BaseSdk.setIntegerForKey(KEY_TAICHI_LOG_IDX, idx);
            //再次校验
            logAdOneDayRevenue(0, currencyCode);
        }
    }
    //记录广告收益(Taichi 3.0 Total_Ads_Revenue_001)
    public static void logAdTotalRevenue(double revenue, String currencyCode)
    {
        if (mFirebaseRemoteConfig != null)
        {
            float curValue = BaseSdk.getFloatForKey(KEY_AD_REVENUE_TOTAL, 0.f);
            curValue += revenue;

            if (curValue >= 0.01)
            {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CURRENCY, currencyCode);
                bundle.putDouble(FirebaseAnalytics.Param.VALUE, curValue);
                mFirebaseAnalytics.logEvent(EVENT_AD_REVENUE_TOTAL_001, bundle);
                curValue = 0.f;
            }
            BaseSdk.setFloatForKey(KEY_AD_REVENUE_TOTAL, curValue);
        }
    }
    /******** Firebase 广告收益(Taichi)  end **********/
    //记录广告加载状态
    public static void logAdLoadEvent(final String eventKey, final String adType)
    {
        if (mFirebaseAnalytics != null)
        {
            Bundle bundle = new Bundle();
            bundle.putString("AdType", adType);
            mFirebaseAnalytics.logEvent(eventKey, bundle);
        }
    }

    /******** Firebase RemoteConfig Functions  begin **********/
    //获取int
    public static int getIntRemoteConfig(final String key)
    {
        if (mFirebaseRemoteConfig == null)
        {
            return 0;
        }
        long value = mFirebaseRemoteConfig.getLong(key);
        return (int) value;
    }
    //获取float
    public static float getFloatRemoteValue(String configKey)
    {
        if (mFirebaseRemoteConfig == null)
        {
            return 0.f;
        }
        double value = mFirebaseRemoteConfig.getDouble(configKey);
        return (float) value;
    }
    //获取double
    public static double getDoubleRemoteConfig(final String key)
    {
        if (mFirebaseRemoteConfig == null)
        {
            return 0.f;
        }
        return mFirebaseRemoteConfig.getDouble(key);
    }
    //获取string
    public static String getStringRemoteConfig(final String key)
    {
        if (mFirebaseRemoteConfig == null)
        {
            return "";
        }
        return mFirebaseRemoteConfig.getString(key);
    }

}
