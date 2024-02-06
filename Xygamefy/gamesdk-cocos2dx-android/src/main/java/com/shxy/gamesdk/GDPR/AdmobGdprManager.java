package com.shxy.gamesdk.GDPR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import android.app.Activity;
import com.shxy.gamesdk.BaseSdk.BaseSdk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import com.shxy.gamesdk.Firebase.FirebaseManager;

/**
 * Admob的CMP管理类
 * @author: 翟宇翔
 * @date: 2023/10/17
 */
public class AdmobGdprManager extends BaseGdprManager {
    private Activity mActivity = null;
    private static ConsentInformation consentInformation;
    private static final String TAG = "GdprManager";
    private static boolean mIsGDPRCountry = false; //用户是否是欧盟/英国用户
    private static boolean mIsGdprPolicyValid = false;//CMP弹窗是否到达展示时间
    private static boolean hasrequestGdpr = false;//本次启动游戏是否已经请求过GDPR弹窗
    private static boolean mIsDebug = false;
    private static String mTestDeviceHashedId = "";
    private static boolean mIsFromStartGDPR = false;//本次进入updateGDPR是否为从startGDPR进入的
    private static boolean mIsShowedThisTime = false; //本次启动CMP是否成功弹出弹窗

/*    public static void SetGdprWindowCloseHandler(IGdprWindowClose handler)
    {
        onGdprWindowCloseHandler = handler;
    }*/

    //需要符合GDPR标准的国家列表
    private static final HashSet<String> GdprCountrySet = new HashSet<String>() {{
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
    }};

    /**
     * 初始化GdprManager，并返回GdprSdk以供回调使用
     * @param activity 使用sdk的activity
     */
    protected void init(Activity activity, boolean isDebug){
        Log.e(TAG, "初始化GDPR");
        mActivity = activity;
        String countryCode = mActivity.getResources().getConfiguration().locale.getCountry();
        mIsGDPRCountry = GdprCountrySet.contains(countryCode);
        Bundle bundle = new Bundle();
        bundle.putString("gdpr_user_country",countryCode);
        FirebaseManager.logParamsEvent("gdpr_user_country",bundle);
        Bundle bundle1 = new Bundle();
        bundle1.putBoolean("gdpr_is_country",mIsGDPRCountry);
        FirebaseManager.logParamsEvent("gdpr_is_country",bundle1);
        setDebug(isDebug);
    }

    protected boolean hasShowedThisTime(){
        return mIsShowedThisTime;
    }

    private void setDebug(boolean isDebug){
        if(isDebug){
            mIsDebug = true;
            mTestDeviceHashedId = getDeviceId();
        }
        setGdprPolicyValid();
    }

    /**
     * 获取设备ID
     * @return 设备ID的MD5值
     */
    private String getDeviceId(){
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(mActivity.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String Md5DeviceId = getMd5Value(deviceId);
        Log.i(TAG, "ANDROID_ID:" + Md5DeviceId);
        return Md5DeviceId;
    }

    /**
     * 获取MD5值
     * @param str 要进行MD5加密的字符串
     * @return MD5加密后的字符串
     */
    private static String getMd5Value(final String str) {
        try {
            MessageDigest bmd5 = MessageDigest.getInstance("MD5");
            bmd5.update(str.getBytes());
            int i;
            StringBuilder buf = new StringBuilder();
            byte[] b = bmd5.digest();
            for (byte value : b) {
                i = value;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 判断是否为GDPR国家
     * @return 如果是GDPR国家并且GDPR策略有效，则返回true；否则返回false
     */
    protected boolean isGDPRCountry(){
        if(mIsDebug){
            return true;
        }
        return mIsGDPRCountry && mIsGdprPolicyValid;
    }

    /**
     * 判断用户的国家/地区是否适用GDPR（欧盟、英国）
     * @param countryCode 用户的国家/地区代码（getResources().getConfiguration().locale.getCountry()）
     * @return 是否需要遵守GDPR
     */
/*    public static boolean isNecessary(String countryCode)
    {
        mIsGDPRCountry = GdprCountrySet.contains(countryCode);
        return mIsGDPRCountry && mIsGdprPolicyValid;
    }*/

    /**
     * 设置GDPR策略是否有效的方法
     */
    private static void setGdprPolicyValid(){
        if(mIsDebug){
            mIsGdprPolicyValid = true;
            return;
        }
        // 获取当前日期
        Calendar currentDate = Calendar.getInstance();
        // 指定比较日期（2024年1月7日）
        Calendar comparisonDate = Calendar.getInstance();
        comparisonDate.set(2024, Calendar.JANUARY, 7);
        // 比较日期
        // 当前日期晚于等于比较日期，设置GDPR策略有效
        mIsGdprPolicyValid = !currentDate.before(comparisonDate); // 当前日期早于比较日期，设置GDPR策略无效
    }

    /**
     * 获取当前的广告等级：
     * -1：未进行设置
     * 0：不能加载任何广告
     * 1：可以加载非个性化广告
     * 2：可以加载个性化广告
     * @return 广告等级
     */
    protected int getAdLevels(){
        return BaseSdk.getIntegerForKey("Ad_Level",-1);
    }


    private static boolean hasAttribute(String input, int index) {
        if (input == null) return false;
        return input.length() >= index && input.charAt(index-1) == '1';
    }

    private static boolean hasConsentFor(List<Integer> indexes, String purposeConsent, boolean hasVendorConsent) {
        for (Integer p: indexes) {
            if (!hasAttribute(purposeConsent, p)) {
                Log.e(TAG, "hasConsentFor: denied for purpose #" + p );
                return false;
            }
        }
        return hasVendorConsent;
    }
    private static boolean hasConsentOrLegitimateInterestFor(List<Integer> indexes, String purposeConsent, String purposeLI, boolean hasVendorConsent, boolean hasVendorLI){
        for (Integer p: indexes) {
            boolean purposeAndVendorLI = hasAttribute(purposeLI, p) && hasVendorLI;
            boolean purposeConsentAndVendorConsent = hasAttribute(purposeConsent, p) && hasVendorConsent;
            boolean isOk = purposeAndVendorLI || purposeConsentAndVendorConsent;
            if (!isOk){
                Log.e(TAG, "hasConsentOrLegitimateInterestFor: denied for #" + p);
                return false;
            }
        }
        return true;
    }

    /**
     * 根据用户的选择，设置用户当前选择的广告等级，谷歌仅仅会根据这个等级返回对应的广告
     *  2： 可以显示个性化广告
     *  1： 仅能显示非个性化广告
     *  0： 不可以显示任何广告
     */
    private void setAdLevels() {
        int mAdLevel = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents","");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        List<Integer> indexes = new ArrayList<>();
        indexes.add(1);
        indexes.add(3);
        indexes.add(4);

        List<Integer> indexesLI = new ArrayList<>();
        indexesLI.add(2);
        indexesLI.add(7);
        indexesLI.add(9);
        indexesLI.add(10);

        if(hasConsentFor(indexes, purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI)){
            mAdLevel = 2;
            Log.d(TAG, "setAdLevels: Ad Level is " + mAdLevel);
            BaseSdk.setIntegerForKey("Ad_Level",mAdLevel);
            return;
        }

        indexes.remove((Integer) 3);
        indexes.remove((Integer) 4);

        if(hasConsentFor(indexes, purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI)){
            mAdLevel =  1;
        }
        Log.d(TAG, "setAdLevels: Ad Level is " + mAdLevel);

        BaseSdk.setIntegerForKey("Ad_Level",mAdLevel);
    }

    /**
     * 启动CMP弹窗流程，应用启动时使用
     */
    protected void startGDPR(){

        if(mActivity == null || !isGDPRCountry()){
            Log.e(TAG, "startGDPR: GDPRSDK is not inited");
            return;
        }
        mActivity.runOnUiThread(()->{
            Log.d(TAG, "startGDPR: GDPR Country");
            boolean gdprEUShowed = BaseSdk.getBoolForKey("gdpr_EU_showed",false);
            if(gdprEUShowed && getAdLevels() == 2){
                //EEU用户之前展示过弹窗并且开启了所有广告，不弹出CMP
                //实装非个性化广告后，将后置条件替换为mAdLevel != 0
                Log.d(TAG,"has start CMP before, so skip request GDPR");
                Log.d(TAG, "startGDPR: Ad Level is " + getAdLevels());
                onGDPRCompleted(true);
            }else if(!gdprEUShowed) {
                //EEU用户第一次启动
                Log.d(TAG, "begin request GDPR");
                requestConsentInfoUpdate();
            }else {
                mIsFromStartGDPR = true;
                updateGDPR();
            }

        });
    }

    /**
     * 启动GDPR弹窗，首先进行request，然后执行loadandshow操作
     */
    private void requestConsentInfoUpdate(){
        Log.d(TAG, "requestConsentInfoUpdate: 进入GdprCmpManager");
        FirebaseManager.logNullParamEvent("gdpr_start_form");
        mActivity.runOnUiThread(()->{
            ConsentRequestParameters params = null;
            if(mIsDebug){
                ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(mActivity.getApplicationContext())
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        //.addTestDeviceHashedId("583A93B2E92FF74F416F09C52E0643F0")
                        .addTestDeviceHashedId(mTestDeviceHashedId)
                        .build();
                params = new ConsentRequestParameters
                        .Builder()
                        .setTagForUnderAgeOfConsent(false)
                        .setConsentDebugSettings(debugSettings)
                        .build();
            }else{
                params = new ConsentRequestParameters
                        .Builder()
                        .setTagForUnderAgeOfConsent(false)
                        .build();
            }
            consentInformation = UserMessagingPlatform.getConsentInformation(mActivity);
            //consentInformation.reset();//debug使用，重置表单显示
            consentInformation.requestConsentInfoUpdate(
                    mActivity, params,
                    () -> {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                mActivity, loadAndShowError -> {
                                    hasrequestGdpr = true;

                                    if(!BaseSdk.getBoolForKey("gdpr_EU_showed", false)){
                                        Log.d(TAG, "启动CMP弹窗, canRequest is " + consentInformation.canRequestAds());
                                        if (loadAndShowError != null) {
                                            // Consent gathering failed.
                                            Log.w(TAG, String.format("%s: %s",
                                                    loadAndShowError.getErrorCode(),
                                                    loadAndShowError.getMessage()));
                                            mIsShowedThisTime = false;
                                        } else{
                                            Log.i(TAG, "set gdpr true");
                                            BaseSdk.setBoolForKey("gdpr_EU_showed", true);
                                            mIsShowedThisTime = true;
                                        }
                                        setAdLevels();
                                        Bundle bundle = new Bundle();
                                        bundle.putInt("Ad_Level", getAdLevels());
                                        FirebaseManager.logParamsEvent("gdpr_start_form_finish",bundle);
                                        onGDPRCompleted(consentInformation.canRequestAds());
                                    } else{
                                        Log.d(TAG, "requestConsentInfoUpdate: has shown CMP before");

                                        onGDPRCompleted(true);
                                    }
                                }
                        );

                    },
                    requestConsentError -> {
                        Log.d(TAG, "未启动CMP弹窗, canRequest is " + consentInformation.canRequestAds());
                        FirebaseManager.logNullParamEvent("gdpr_start_form_exception");
                        // Consent gathering failed.
                        Log.w(TAG, String.format("%s: %s",
                                requestConsentError.getErrorCode(),
                                requestConsentError.getMessage()));
                        mIsShowedThisTime = false;
                        onGDPRCompleted(consentInformation.canRequestAds());
                    });
        });
    }

    /**
     * 在调用updateGDPR前的操作使用，以提前加载GDPR表单，并不进行展示
     */
    protected void updateGDPR(){
        if(mActivity == null || !isGDPRCountry()) {
            return;
        }
        if(mIsFromStartGDPR) {
            //打start点
            FirebaseManager.logNullParamEvent("gdpr_start_form");
        }else{
            //打update点
            FirebaseManager.logNullParamEvent("gdpr_update_form");
        }

        if(!hasrequestGdpr){
            mActivity.runOnUiThread(()-> {
                Log.d(TAG, "requestBeforeUpdate: has not launched CMP");
                ConsentRequestParameters params = null;
                if (mIsDebug) {
                    ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(mActivity.getApplicationContext())
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            //.addTestDeviceHashedId("583A93B2E92FF74F416F09C52E0643F0")
                            .addTestDeviceHashedId(mTestDeviceHashedId)
                            .build();
                    params = new ConsentRequestParameters
                            .Builder()
                            .setTagForUnderAgeOfConsent(false)
                            .setConsentDebugSettings(debugSettings)
                            .build();
                } else {
                    params = new ConsentRequestParameters
                            .Builder()
                            .setTagForUnderAgeOfConsent(false)
                            .build();
                }


                consentInformation = UserMessagingPlatform.getConsentInformation(mActivity);
                consentInformation.requestConsentInfoUpdate(
                        mActivity, params,
                        () -> {
                            hasrequestGdpr = true;
                            loadGdprForm();

                            //initAdmob(consentInformation.canRequestAds());
                        },
                        requestConsentError -> {
                            Log.d(TAG, "Request failed, canRequest is " + consentInformation.canRequestAds());
                            // Consent gathering failed.
                            Log.w(TAG, "loadAndShowConsentFormIfRequired" + String.format("%s: %s",
                                    requestConsentError.getErrorCode(),
                                    requestConsentError.getMessage()));
                            mIsShowedThisTime = false;
                            onGDPRCompleted(consentInformation.canRequestAds());
                        }
                );
            });
        }else{
            Log.d(TAG, "requestBeforeUpdate: has launched CMP, skip request" );
            loadGdprForm();
        }

    }

    /**
     * 加载GDPR表单
     */
    private void loadGdprForm(){
        if(mActivity == null) {
            return;
        }
        mActivity.runOnUiThread(() -> UserMessagingPlatform.loadConsentForm(
                mActivity.getApplicationContext(),
                new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                    @Override
                    public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                        Log.d(TAG, "loadGdprForm: Load ConsentForm Success!");
                        consentForm.show(mActivity, new ConsentForm.OnConsentFormDismissedListener() {
                            @Override
                            public void onConsentFormDismissed(@Nullable FormError formError) {
                                Log.d(TAG, "loadGdprForm: Show ConsentForm Success!");
                                setAdLevels();
                                if(mIsFromStartGDPR){
                                    //说明是游戏开始时进行的调用，打start点
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("Ad_Level", getAdLevels());
                                    FirebaseManager.logParamsEvent("gdpr_start_form_finish",bundle);
                                    mIsFromStartGDPR = false;
                                }else{
                                    //说明是游戏中主动进行的调用，打update点
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("Ad_Level", getAdLevels());
                                    FirebaseManager.logParamsEvent("gdpr_update_form_finish",bundle);
                                }
                                mIsShowedThisTime = true;
                                onGDPRCompleted(consentInformation.canRequestAds());
                            }
                        });
                    }
                },
                new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                    @Override
                    public void onConsentFormLoadFailure(@NonNull FormError formError) {
                        Log.d(TAG, "loadGdprForm: Load ConsentForm Fails!");
                        if(mIsFromStartGDPR){
                            //打start点
                            FirebaseManager.logNullParamEvent("gdpr_start_form_exception");
                            mIsFromStartGDPR = false;
                        }else{
                            //打update点
                            FirebaseManager.logNullParamEvent("gdpr_update_form_exception");
                        }
                        mIsShowedThisTime = false;
                        onGDPRCompleted(consentInformation.canRequestAds());
                    }
                }
        ));

    }

    //GDPR操作完成回调
    private static void onGDPRCompleted(boolean canSet){
        Log.d(TAG, "onGDPRComplete: 完成GDPR操作");
        GdprSdk.onGDPRShowed(canSet);
    }
}
