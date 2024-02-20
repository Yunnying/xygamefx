package com.shxy.gamesdk.Login;
/**
 * @author: 翟宇翔
 * @date: 2023/12/8
 */

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;

public class FacebookLoginManager {
    private static Activity mActivity = null;
    private static CallbackManager mCallbackManager = CallbackManager.Factory.create();
    private static String TAG = "LoginWithFacebookManager";

    protected static void init(Activity activity) {
        mActivity = activity;
    }

    /**
     * 账户登录
     */
    protected static void login() {
        if(!FacebookSdk.isInitialized()){
            Log.e(TAG, "login: Facebook SDK is not initialized!");
        }

        LoginManager.getInstance().setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);
        LoginManager.getInstance().logInWithReadPermissions(mActivity, Arrays.asList("public_profile", "email"));
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                //登录成功回调
                Log.i(TAG, "onSuccess: Login Success!");
                LoginSdk.onLogined(loginResult.getAccessToken().getUserId(),LoginType.FaceBook.ordinal(),ErrorType.NoError.ordinal());
            }

            @Override
            public void onCancel() {
                //登录取消回调
                Log.i(TAG, "onCancel: Login Cancel!");
                LoginSdk.onLogined("",LoginType.FaceBook.ordinal(),ErrorType.UserCanceled.ordinal());
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                //登录错误回调
                Log.e(TAG, "onError: "+ e.getMessage());
                LoginSdk.onLogined("", LoginType.FaceBook.ordinal(),ErrorType.OtherError.ordinal());
            }
        });
    }

    /**
     * 判断登录状态
     * @return true 已登录，false 未登录
     */
    protected static boolean checkLogined() {
        if(!FacebookSdk.isInitialized()){
            return false;
        }

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken!= null && !accessToken.isExpired()){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    protected static String getUid() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken!= null &&!accessToken.isExpired()){
            return accessToken.getUserId();
        }else{
            return "";
        }
    }

    /**
     * 退出登录
     */
    protected static void logout() {
        LoginManager.getInstance().logOut();
    }

    /**
     * 删除账户
     */
    protected static void deleteAccount() {
        //待实现
    }
}
