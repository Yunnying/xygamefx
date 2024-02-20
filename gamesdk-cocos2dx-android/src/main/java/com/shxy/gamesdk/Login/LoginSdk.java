package com.shxy.gamesdk.Login;

import android.app.Activity;

import com.shxy.gamesdk.BaseSdk.BaseSdk;

/**
 * @author: 翟宇翔
 * @date: 2023/12/8
 */

public class LoginSdk {
    public static void init(Activity activity, String googleClientId){
        LoginManager.init(activity, googleClientId);
    }
    public static void login(int thirdPartyType){
        LoginManager.login(thirdPartyType);
    };

    public static void onLogined(String openId, int loginType, int errorType){
        BaseSdk.runOnGLThread(()->{
            onLoginedNative(openId, loginType, errorType);
        });
    }
    private static native void onLoginedNative(String openId, int loginType, int errorType );


    public static boolean checkLogined(int thirdPartyType){
        return LoginManager.checkLogined(thirdPartyType);
    }

    public static String getOpenId(int thirdPartyType){
        return LoginManager.getOpenId(thirdPartyType);
    }

    public static void logout(int thirdPartyType){
        LoginManager.logout(thirdPartyType);
    }

    public static void deleteAccount(String thirdPartyType){

    }
}
