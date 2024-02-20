package com.shxy.gamesdk.Login;

import android.app.Activity;

/**
 * @author: 翟宇翔
 * @date: 2023/12/8
 */

enum LoginType{
    Apple,
    FaceBook,
    Google,
    Unknown
};

enum ErrorType{
    NoError,
    UserCanceled,
    NotSupport,
    OtherError
};
public class LoginManager {
    /**
     * 初始化
     * @param activity
     */
    public static void init(Activity activity, String googleClientId){
        GoogleLoginManager.init(activity, googleClientId);
        FacebookLoginManager.init(activity);
    }
    /**
     * 登录
     * @param thirdPartyType 第三方类型
     */
    public static void login(int thirdPartyType){
        switch (LoginType.values()[thirdPartyType]){
            case FaceBook:
                FacebookLoginManager.login();
                break;
            case Google:
                GoogleLoginManager.login();
                break;
            case Apple:
                //LoginWithAppleIDManager.login();
                break;
        }
    };

    /**
     * 检查是否登录
     * @param thirdPartyType 第三方类型
     * @return true 已登录，false 未登录
     */
    public static boolean checkLogined(int thirdPartyType){
        switch (LoginType.values()[thirdPartyType]){
            case FaceBook:
                return FacebookLoginManager.checkLogined();
            case Google:
                return GoogleLoginManager.checkLogined();
            case Apple:
                return false;
        }
        return false;
    }

    /**
     * 获取用户id
     * @param thirdPartyType 第三方类型
     * @return 用户id
     */
    public static String getOpenId(int thirdPartyType){
        switch (LoginType.values()[thirdPartyType]){
            case FaceBook:
                return FacebookLoginManager.getUid();
            case Google:
                return GoogleLoginManager.getUid();
            case Apple:
                return "LoginWithAppleIDManager.getUid();";
        }
        return "";
    }

    /**
     * 退出登录
     * @param thirdPartyType 第三方类型
     */
    public static void logout(int thirdPartyType){
        switch (LoginType.values()[thirdPartyType]){
            case FaceBook:
                FacebookLoginManager.logout();
                break;
            case Google:
                GoogleLoginManager.logout();
                break;
            case Apple:
                //LoginWithAppleIDManager.logout();
                break;
        }
    }

    /**
     * 删除账号
     * @param thirdPartyType 第三方类型
     */
    public static void deleteAccount(int thirdPartyType){
        switch (LoginType.values()[thirdPartyType]){
            case FaceBook:
                FacebookLoginManager.deleteAccount();
                break;
            case Google:
                //LoginWithGoogleManager.deleteAccount();
                break;
            case Apple:
                //LoginWithAppleIDManager.deleteAccount();
                break;
        }
    }
}
