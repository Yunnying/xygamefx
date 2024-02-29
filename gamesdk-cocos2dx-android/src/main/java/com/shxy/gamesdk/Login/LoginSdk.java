package com.shxy.gamesdk.Login;

import android.app.Activity;

import com.facebook.CallbackManager;
import com.shxy.gamesdk.BaseSdk.BaseSdk;

/**
 * @author: 翟宇翔
 * @date: 2023/12/8
 */

public class LoginSdk {
    /**
     * 初始化Login SDK
     * @param activity
     * @param googleClientId Google客户端ID
     */
    public static void init(Activity activity, String googleClientId){
        LoginManager.init(activity, googleClientId);
    }

    /**
     * 进行第三方登录
     * @param thirdPartyType 第三方登录的类型
     */
    public static void login(int thirdPartyType){
        LoginManager.login(thirdPartyType);
    };

    /**
     * 登录结束回调
     * @param openId 本次登录获得的OpenId
     * @param loginType 本次的登录类型
     * @param errorType 本次登录的错误类型，无错误时返回0
     */
    public static void onLogined(String openId, int loginType, int errorType){
        BaseSdk.runOnGLThread(()->{
            onLoginedNative(openId, loginType, errorType);
        });
    }

    //登录结束的原生回调
    private static native void onLoginedNative(String openId, int loginType, int errorType );


    /**
     * 检查某种登录方式的登录状态
     * @param thirdPartyType 登录方式类型
     * @return true为已登录;false为未登录
     */
    public static boolean checkLogined(int thirdPartyType){
        return LoginManager.checkLogined(thirdPartyType);
    }

    /**
     * 获取某种登录方式的OpenId
     * @param thirdPartyType 登录方式类型
     * @return 一个String值，为该登录方式对应的OpenId
     */
    public static String getOpenId(int thirdPartyType){
        return LoginManager.getOpenId(thirdPartyType);
    }

    /**
     * 退出登录
     * @param thirdPartyType 登录方式类型
     */
    public static void logout(int thirdPartyType){
        LoginManager.logout(thirdPartyType);
    }

    /**
     * 供Facebook登录使用
     * @return 返回一个CallbackManager对象
     */
	public static CallbackManager getFacebookCallbackManager(){
		return FacebookLoginManager.getFacebookCallbackManager();
	}

    /**
     * 删除账户
     * @param thirdPartyType 登录方式类型
     */
    public static void deleteAccount(int thirdPartyType){

    }
}
