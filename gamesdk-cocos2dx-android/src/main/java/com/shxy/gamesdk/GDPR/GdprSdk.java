package com.shxy.gamesdk.GDPR;

/**
 * @author: 翟宇翔
 * @date: 2023/10/8
 */
import android.app.Activity;
import com.shxy.gamesdk.BaseSdk.BaseSdk;
enum MediationName
{
    m_Admob,
    m_AppLovin,
}

public class GdprSdk {

    private static BaseGdprManager mGdprManager = null;
    private static MediationName mMediationName = MediationName.m_Admob;
    /**
     * CMP结束后的回调，用于回调C++代码，如加载主场景
     */
    public static void onGDPRShowed(boolean canSet){
        BaseSdk.runOnGLThread(()->{
            onGDPRShowedNative(canSet);
        });
    }
    private static native void onGDPRShowedNative(boolean canSet);

    /**
     * 初始化Gdprsdk
     * @param activity 初始化使用的activity
     * @param mediationName 广告聚合平台的类型
     * @param isDebug 是否是调试模式
     */
    public static void init(Activity activity, String mediationName, boolean isDebug){
        if(mediationName.equals("Admob")){
            mGdprManager = new AdmobGdprManager();
            mMediationName = MediationName.m_Admob;
        }else {
            mGdprManager = new MaxGdprManager();
            mMediationName = MediationName.m_AppLovin;
        }
        mGdprManager.init(activity, isDebug);
    }

    public static void setURL(String privacyPolicyURL, String termsOfServiceURL){
        if(mMediationName == MediationName.m_AppLovin){
            mGdprManager.setURL(privacyPolicyURL, termsOfServiceURL);
        }
    }

    /**
     * 判断用户的国家/地区是否适用GDPR（欧盟、英国），请在isNecessary后使用，否则总为false
     * @return 为true时适用；为false时不适用
     */
    public static boolean isGDPRCountry(){
        return mGdprManager.isGDPRCountry();
    }

    /**
     * 游戏启动时进行的逻辑判断
     * 如果不是欧盟/英国用户，直接返回
     * 如果是欧盟/英国用户：
     *  1. 首次启动，没有弹出过CMP窗口，弹出CMP
     *  2. 非首次启动，但是未同意广告选项，弹出CMP
     *  3. 非首次启动，且同意广告选项，初始化广告
     */
    public static void startGDPR(){
        mGdprManager.startGDPR();
    }

    /**
     * 用户手动调用CMP表单
     */
    public static void updateGDPR(){
        mGdprManager.updateGDPR();
    }

    /**
     * 获取当前的广告等级：
     * 0：不能加载任何广告
     * 1：可以加载非个性化广告
     * 2：可以加载个性化广告
     * @return 广告等级
     */
    public static int getAdLevels(){
        return mGdprManager.getAdLevels();
    }

    /**
     * 获取当前是否已经弹出过CMP窗口
     * @return true：弹出过CMP窗口；false：没有弹出过CMP窗口
     */
    public static boolean hasShowedThisTime(){return mGdprManager.hasShowedThisTime();}
}
