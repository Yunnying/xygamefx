/**
 * @author: 翟宇翔
 * @date: 2023/8/25
 */
package com.shxy.gamesdk.Firebase;

import android.os.Bundle;

import android.app.Activity;

import com.shxy.gamesdk.BaseSdk.BaseSdk;

import java.security.PublicKey;

public class FirebaseSdk {
    /*----------------------------------------------- Firebase Init Functions --------------------------------------------*/

    /**
     * 远程配置加载回调
     */
    public static void onRemoteConfigLoaded(){
        BaseSdk.runOnGLThread(()->{
            onRemoteConfigLoadedNative();
        });
    }
    private static native void onRemoteConfigLoadedNative(); //远程配置加载回调

    /**
     * 初始化FirebaseSdk
     * @param activity 当前activity
     */
    public static void init(Activity activity){
        FirebaseManager.initManager(activity);
    }

    /**
     * 已有项目使用，设置EVENT_AD_REVENUE_IMPRESSION值为现有值（默认值为 "Ad_Impression_Revenue"）
     * @param eventAdRevenueImpression 已有项目使用的EVENT_AD_REVENUE_IMPRESSION值
     */
    public static void setEventAdRevenueImpression(String eventAdRevenueImpression){
        FirebaseManager.setEventAdRevenueImpression(eventAdRevenueImpression);
    }

    /*----------------------------------------------- Firebase Analytics (Event upload) Functions --------------------------------------------*/

    /**
     * 上传无参数事件到Firebase
     * @param name 事件名称
     */
    public static void logEvent(final String name){
        FirebaseManager.logNullParamEvent(name);
    }

    /**
     * 有参数事件上传至Firebase，参数名称和参数值均以Bundle的形式传递
     * @param name 事件名称
     * @param parameter 事件参数
     */
    public static void logEvent(final String name, Bundle parameter){
        FirebaseManager.logParamsEvent(name, parameter);
    }

    /*----------------------------------------------- Firebase Remote Config Functions ---------------------------------------------------------*/

    /**
     * 从远端获取int类型
     * @param key 要获得的key值
     * @return key值对应的返回值
     */
    public static int getIntRemoteConfig(final String key){
        return FirebaseManager.getIntRemoteConfig(key);
    }

    /**
     * 从远端获取double类型
     * @param key 要获得的key值
     * @return key值对应的返回值
     */
    public static double getDoubleRemoteConfig(final String key){
        return FirebaseManager.getDoubleRemoteConfig(key);
    }

    /**
     * 从远端获取String类型
     * @param key 要获得的key值
     * @return key值对应的返回值
     */
    public static String getStringRemoteConfig(final String key){
        return FirebaseManager.getStringRemoteConfig(key);
    }
}
