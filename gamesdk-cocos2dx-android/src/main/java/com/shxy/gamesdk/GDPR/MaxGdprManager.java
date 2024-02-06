package com.shxy.gamesdk.GDPR;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.applovin.sdk.AppLovinCmpError;
import com.applovin.sdk.AppLovinCmpService;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.shxy.gamesdk.BaseSdk.BaseSdk;
import com.shxy.gamesdk.Firebase.FirebaseManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * MAX的CMP管理类
 * @author: 翟宇翔
 * @date: 2023/12/18
 */
public class MaxGdprManager extends BaseGdprManager{
    private Activity mActivity = null;
    private static final String TAG = "GdprManager";
    private static boolean mIsGDPRCountry = false; //用户是否是欧盟/英国用户
    private static boolean mIsGdprPolicyValid = false;//CMP弹窗是否到达展示时间
    private static boolean mIsDebug = false;
    private static boolean mHasInitMAX = false;
    private static String mPrivacyPolicyURL = "";
    private static String mTermsOfServiceURL = "";
    private static boolean mIsFromStartGDPR = false;//本次进入updateGDPR是否为从startGDPR进入的

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
        setDebug(isDebug);
    }

    private static void setDebug(boolean isDebug){
        if(isDebug){
            mIsDebug = true;
        }
        setGdprPolicyValid();
    }

    /**
     * 设置URL
     * @param privacyPolicyURL 隐私政策URL
     * @param termsOfServiceURL 使用条款URL
     */
    protected void setURL(String privacyPolicyURL, String termsOfServiceURL){
        mPrivacyPolicyURL = privacyPolicyURL;
        mTermsOfServiceURL = termsOfServiceURL;
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
                Log.d(TAG,"has start CMP before, so skip request GDPR. Ad Level is" + getAdLevels());
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

        mActivity.runOnUiThread(()->{
            AppLovinSdkSettings settings = new AppLovinSdkSettings( mActivity );
            settings.getTermsAndPrivacyPolicyFlowSettings().setEnabled( true );

            if(!mPrivacyPolicyURL.isEmpty()){
                settings.getTermsAndPrivacyPolicyFlowSettings().setPrivacyPolicyUri(Uri.parse(mPrivacyPolicyURL));
            }else{
                Log.i(TAG, "requestConsentInfoUpdate: Empty PrivacyPolicyURL!");
            }
            if(!mTermsOfServiceURL.isEmpty()){
                settings.getTermsAndPrivacyPolicyFlowSettings().setTermsOfServiceUri(Uri.parse(mTermsOfServiceURL));
            }else{
                Log.i(TAG, "requestConsentInfoUpdate: Empty TermsOfServiceURL!");
            }

            //设置debug模式
            if(mIsDebug){
                settings.getTermsAndPrivacyPolicyFlowSettings().setDebugUserGeography( AppLovinSdkConfiguration.ConsentFlowUserGeography.GDPR );
            }

            AppLovinSdk sdk = AppLovinSdk.getInstance(settings, mActivity);
            sdk.setMediationProvider(AppLovinMediationProvider.MAX);
            sdk.initializeSdk(config->{
                Log.i(TAG, "requestConsentInfoUpdate: The config is " + config.toString());
                BaseSdk.setBoolForKey("gdpr_EU_showed", true);
                mHasInitMAX = true;
                setAdLevels();
                if(getAdLevels() == -1 && config.getConsentFlowUserGeography() == AppLovinSdkConfiguration.ConsentFlowUserGeography.GDPR){
                    FirebaseManager.logNullParamEvent("gdpr_start_form");
                }else{
                    Bundle bundle = new Bundle();
                    bundle.putInt("Ad_Level", getAdLevels());
                    FirebaseManager.logParamsEvent("gdpr_start_form_finish",bundle);
                }
                onGDPRCompleted(true);
            });
        });
    }

    /**
     * 用于设置是否已经初始化MAX SDK
     */
    public static void setHasInitMax(boolean hasInitMAX) {
        mHasInitMAX = hasInitMAX;
    }

    /**
     * 用于判断是否已经初始化MAX SDK
     */
    public static boolean getHasInitMax() {
        return mHasInitMAX;
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
        mActivity.runOnUiThread(()-> {
            AppLovinCmpService cmpService = AppLovinSdk.getInstance( mActivity ).getCmpService();
            cmpService.showCmpForExistingUser( mActivity, new AppLovinCmpService.OnCompletedListener()
            {
                @Override
                public void onCompleted(@Nullable AppLovinCmpError appLovinCmpError) {
                    setAdLevels();
                    if(appLovinCmpError == null){
                        //成功展示CMP弹窗
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
                        Log.i(TAG, "onCompleted: Update Success!");
                        onGDPRCompleted(true);
                    }else{
                        Log.i(TAG, "onCompleted: Update Fail :" + appLovinCmpError.getMessage());
                        if(mIsFromStartGDPR){
                            //打start点
                            FirebaseManager.logNullParamEvent("gdpr_start_form_exception");
                            mIsFromStartGDPR = false;
                        }else{
                            //打update点
                            FirebaseManager.logNullParamEvent("gdpr_update_form_exception");
                        }
                        onGDPRCompleted(true);
                    }
                }
            });
        });
    }

    //GDPR操作完成回调
    private void onGDPRCompleted(boolean canSet){
        Log.d(TAG, "onGDPRComplete: 完成GDPR操作");
        if(mIsDebug){
            AppLovinSdk.getInstance( mActivity ).showMediationDebugger();//Debug模式下启动AppLovinSdk的调试器
        }
        GdprSdk.onGDPRShowed(canSet);
    }
}
