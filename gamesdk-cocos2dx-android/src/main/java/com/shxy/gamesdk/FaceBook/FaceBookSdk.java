package com.shxy.gamesdk.FaceBook;

import android.app.Activity;
import android.os.Bundle;

import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;

/**
 * @author: 翟宇翔
 * @date: 2023/12/20
 */
public class FaceBookSdk {
    private static Activity mActivity;

    /**
     * 初始化facebook
     * @param activity 主activity
     * @param isDebug 是否是debug模式
     */
    public static void init(Activity activity, boolean isDebug){
        mActivity = activity;
        if(isDebug){
            FacebookSdk.setIsDebugEnabled(true);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        }
    }

    /**
     * 记录无参数事件到facebook
     * @param eventName 事件名称
     */
    public static void logEvent(String eventName){
        if(mActivity == null){
            return;
        }
        AppEventsLogger logger = AppEventsLogger.newLogger(mActivity);
        logger.logEvent(eventName);
    }

    /**
     * 记录有参数事件到facebook
     * @param eventName 事件名称
     * @param parameter 事件参数，bundle中的类型只能是string和int
     */
    public static void logEvent(String eventName, Bundle parameter){
        if(mActivity == null){
            return;
        }
        AppEventsLogger logger = AppEventsLogger.newLogger(mActivity);
        logger.logEvent(eventName, parameter);
    }

}
