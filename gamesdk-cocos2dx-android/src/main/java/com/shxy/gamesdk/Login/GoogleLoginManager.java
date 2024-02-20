package com.shxy.gamesdk.Login;

import android.app.Activity;
import android.os.CancellationSignal;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.shxy.gamesdk.BaseSdk.BaseSdk;

import java.security.SecureRandom;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author: 翟宇翔
 * @date: 2023/12/8
 */
public class GoogleLoginManager {
    private static Activity mActivity = null;
    private static final String TAG = "LoginWithGoogleManager";
    private static String mNonce = null;

    private static String mGoogleClientId = null;

    protected static void init(Activity activity, String googleClientId){
        mActivity = activity;
        mGoogleClientId = googleClientId;
    }

    /**
     * 生成随机字符串作为Nonce
     * @return 一个随机字符串
     */
    private static String generateNonce() {
        Log.i(TAG, "generateNonce: get Nonce");
        // 随机数生成器
        SecureRandom secureRandom = new SecureRandom();
        // 生成字节数组
        byte[] nonceBytes = new byte[32]; // 至少16字节，Base64后会扩展
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        Log.i(TAG, "generateNonce: Nonce is " + nonce);
        // Base64编码
        return nonce;
    }

    /**
     * 登录方法
     */
    protected static void login(){
        Log.i(TAG, "login: start login");
        if(mActivity == null){
            Log.e(TAG, "login: mActivity is null");
            return;
        }
        // 生成随机数
        mNonce = generateNonce();
        // 创建GetGoogleIdOption对象，并设置相关参数
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // 设置通过授权账户筛选
                .setNonce(mNonce) // 设置随机数
                .setServerClientId(mGoogleClientId)
                .setRequestVerifiedPhoneNumber(true)
                .build();
        // 创建GetCredentialRequest对象，并添加凭证选项
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption) // 添加凭证选项
                .build();
        // 创建CredentialManager对象
        CredentialManager credentialManager = CredentialManager.create(mActivity);
        // 创建取消信号
        CancellationSignal cancellationSignal = new CancellationSignal();
        // 创建执行器，如果要在当前线程执行，可以设置为null
        Executor executor = Executors.newSingleThreadExecutor();
        // 异步获取凭证
        credentialManager.getCredentialAsync(
                mActivity,
                request,
                cancellationSignal,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        Log.i(TAG, "onResult: login success");
                        //handleSignIn(getCredentialResponse); // 处理登录
                        String uid = (String) getCredentialResponse.getCredential().getData().get("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID");
                        BaseSdk.setStringForKey("google_key_id", uid);
                        LoginSdk.onLogined(uid, LoginType.Google.ordinal(),ErrorType.NoError.ordinal());
                    }
                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG,"Error: " + e.getErrorMessage()); // 输出错误信息
                        LoginSdk.onLogined("",LoginType.Google.ordinal(),ErrorType.OtherError.ordinal());
                    }
                }
        );
    }

/*    *//**
     * 处理登录操作。
     * @param result 登录结果，包含获取到的凭证信息。
     *//*
    private static void handleSignIn(GetCredentialResponse result) {
        // 处理成功返回的凭证
        Credential credential = result.getCredential();
        // 将获取到的凭证信息保存到本地
        BaseSdk.setStringForKey("google_key_id", (String) credential.getData().get("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID"));
        Log.i(TAG, "handleSignIn: The result type is " + credential.getType());
        Log.i(TAG, "handleSignIn: The result date is " + credential.getData());
        Log.i(TAG, "handleSignIn: The KEY_ID is " + credential.getData().get("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID"));
        if (credential instanceof PublicKeyCredential) {
            String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
            // 将responseJson（即GetCredentialResponse对象）分享到你的服务器上进行验证和身份验证
        } else if (credential instanceof PasswordCredential) {
            String username = ((PasswordCredential) credential).getId();
            Log.i(TAG, "handleSignIn: username is" + username);
            String password = ((PasswordCredential) credential).getPassword();
            Log.i(TAG, "handleSignIn: password is" + password);
            // 使用id和password到你的服务器进行验证和身份验证
        } else if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                // 使用googleIdTokenCredential提取id在你的服务器上进行验证和身份验证
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
            } else {
                // 在这里捕获任何不被识别的自定义凭证类型
                Log.e(TAG, "未预期的凭证类型");
            }
        } else {
            // 在这里捕获任何不被识别的凭证类型
            Log.e(TAG, "未预期的凭证类型");
        }
    }*/
    /**
     * 获取用户ID
     * @return 用户ID
     */
    protected static String getUid(){
        String uid = BaseSdk.getStringForKey("google_key_id", "");
        Log.i(TAG, "getUid: Uid is " + uid);
        return uid;
    }

    /**
     * 检查是否登录
     * @return 是否登录
     */
    protected static boolean checkLogined(){
        Log.i(TAG, "checkLogined: into checkLogined, isLogined: " + !getUid().equals(""));
        return !getUid().equals("");
    }

    /**
     * 退出登录
     */
    protected static void logout(){
        Log.i(TAG, "logout: into logout");
        if (!checkLogined()) {
            Log.i(TAG, "logout: not logined");
            return;
        }
        // 删除本地保存的凭证信息
        BaseSdk.deleteValueForKey("google_key_id");
        if(mActivity == null){
            Log.e(TAG, "logout: mActivity is null");
            return;
        }
        CredentialManager credentialManager = CredentialManager.create(mActivity);
        ClearCredentialStateRequest clearCredentialStateRequest = new ClearCredentialStateRequest();
        // 创建取消信号
        CancellationSignal cancellationSignal = new CancellationSignal();
        // 创建执行器，如果要在当前线程执行，可以设置为null
        Executor executor = Executors.newSingleThreadExecutor();
        credentialManager.clearCredentialStateAsync(
            clearCredentialStateRequest,
            cancellationSignal,
            executor,
            new CredentialManagerCallback<Void, ClearCredentialException>() {
                @Override
                public void onResult(Void unused) {
                    Log.i(TAG, "onResult: Logout success!");
                }

                @Override
                public void onError(@NonNull ClearCredentialException e) {
                    Log.e(TAG,"onError: Logout Error！ The Message is : " + e.getErrorMessage()); // 输出错误信息
                }
            }
        );
    }


}
