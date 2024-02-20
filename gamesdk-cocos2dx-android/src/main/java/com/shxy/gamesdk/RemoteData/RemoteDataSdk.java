package com.shxy.gamesdk.RemoteData;

import android.util.Log;

import com.shxy.gamesdk.BaseSdk.BaseSdk;

/**
 * @author: 翟宇翔
 * @date: 2023/12/28
 */
public class RemoteDataSdk {

    /**
     * 初始化SDK
     * @param url 服务器地址
     * @param appId 应用ID
     */
    public static void init(String url, String appId) {
        RemoteDataManager.init(url, appId);
    }

    /**
     * 根据uid获取用户数据
     * @param uid 用户ID
     */
    public static void getUserDataByUid(String uid) {
        RemoteDataManager.getUserDataByUid(uid);
    }

    /**
     * 根据openId获取用户数据
     * @param loginType 第三方登录类型
     * @param openId 第三方登录ID
     */
    public static void getUserDataByOpenId(String loginType, String openId) {
        RemoteDataManager.getUserDataByOpenId(loginType, openId);
    }

    /**
     * getUserDataByUid回调
     * @param success 是否成功
     */
    public static void onGetUserData(boolean success, int responseCode){
        BaseSdk.runOnGLThread(()->{
            onGetUserDataNative(success, responseCode);
        });
    }
    private static native void onGetUserDataNative(boolean success, int responseCode);

    /**
     * 上传用户数据
     * @param uid 用户ID
     * @param data 数据
     * @param properties 自定义属性
     */
    public static void postUserData(String uid, String data, String properties) {
        RemoteDataManager.postUserData(uid, data, properties);
    }

    /**
     * postUserData回调
     * @param success 是否成功
     */
    public static void onPostUserData(boolean success, int responseCode){
        BaseSdk.runOnGLThread(()->{
            onPostUserDataNative(success, responseCode);
        });
    }
    private static native void onPostUserDataNative(boolean success, int responseCode);
    /**
     * 删除用户数据
     * @param uid 用户ID
     */
    public static void deleteUserData(String uid) {
        RemoteDataManager.deleteUserData(uid);
    }
    /**
     * deleteUserData回调
     * @param success 是否成功
     */
    public static void onDeleteUserData(boolean success, int responseCode){
        BaseSdk.runOnGLThread(()->{
            onDeleteUserDataNative(success, responseCode);
        });
    }

    private static native void onDeleteUserDataNative(boolean success, int responseCode);

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public static String getUid(){
        return RemoteDataManager.getStringValue("id");
    }

    /**
     * 获取int类型数据
     * @param key 键
     * @return int类型数据，默认返回0
     */
    public static int getIntValue(String key){
        return RemoteDataManager.getIntValue(key);
    }

    /**
     * 获取double类型数据
     * @param key 键
     * @return double类型数据，默认返回0.0
     */
    public static double getDoubleValue(String key){
        return RemoteDataManager.getDoubleValue(key);
    }

    /**
     * 获取String类型数据
     * @param key 键
     * @return String类型数据，默认返回""
     */
    public static String getStringValue(String key){
        return RemoteDataManager.getStringValue(key);
    }

    /**
     * 获取boolean类型数据
     * @param key 键
     * @return boolean类型数据，默认返回false
     */
    public static boolean getBooleanValue(String key){
        return RemoteDataManager.getBooleanValue(key);
    }

    /**
     * 从玩家的存档的properties中获取String类型的数据
     *
     * @param key 存档信息的key
     * @return String类型的数据，默认返回""
     */
    public static String getProperty(String key){
        return RemoteDataManager.getProperty(key);
    }

    /**
     * 从玩家的存档信息中获取openId
     *
     * @param loginType 登录类型，包括"Apple"、"GP"、"FB"
     * @return openId 对应登录类型的OpenId，不存在时返回""
     */
    public static String getOpenId(String loginType){
        return RemoteDataManager.getOpenId(loginType);
    }
}
