/**
 * @author: 翟宇翔
 * @date: 2023/08/21
 */
package com.shxy.gamesdk.General;


import android.app.Activity;
import android.widget.FrameLayout;


public class GeneralSdk {
    //是否开启Debug模式
    //此处定义动态获取权限的requestCode
    /*----------------------------------------------- Basic Functions --------------------------------------------*/
    //初始化activity和frameLayout
    public static void init(Activity activity, FrameLayout frameLayout)
    {
        FunctionLibrary.init(activity,frameLayout);
    }

    /*----------------------------------------------- Other Functions --------------------------------------------*/
    //检查设备是否是ChromeBook
    public static boolean checkChromeBook(){
        return FunctionLibrary.checkChromeBook();
    }

    /**
     * 启动GooglePlay应用界面进行评分
     */
    public static void doRate(){
        FunctionLibrary.doRate();
    }

    /**
     * 唤醒分享界面，在这个界面点击分享方式后会发送分享语句到指定途径
     * @param shareText 分享语句
     */
    public static void doShareText(final String shareText){
        FunctionLibrary.doShareText(shareText);
    }

    /**
     * 将text内容复制到设备的剪贴板
     * @param text 要复制的内容
     */
    public static void copyToClipboard(final String text){
        FunctionLibrary.copyToClipboard(text);
    }

    /**
     * 信息弹窗，弹出默认样式的toast
     * @param message toast内容
     */
    public static void doAlert(final String message){
        FunctionLibrary.doAlert(message);
    }

    /**
     * 获取设备的总内存(MB)
     * @return 设备的总内存(MB)
     */
    public static int getTotalMemory(){
        return FunctionLibrary.getTotalMemory();
    }

    /**
     * 获取设备的可用内存（MB）
     * @return 设备当前的可用内存（MB）
     */
    public static int getAvailableMemory(){
        return FunctionLibrary.getAvailableMemory();
    }
}
