package com.shxy.gamesdk.Notification;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * @author: 翟宇翔
 * @date: 2023/11/8
 */
public class NotificationManager {
    private static Activity mActivity = null;
    private static String TAG = "NotificationManager";
    protected static void init(Activity Activity){
        mActivity = Activity;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !checkPermission(mActivity.getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)) {
            ActivityCompat.requestPermissions(mActivity,new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
        }
    }


    protected static boolean checkPermission(Context context, String permission){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        }else{
            return true;
        }
    }
    /**
     * Send an notification regularly, and cancel it after the given days.
     * @param hours An integer array containing the time for sending reminders in 24-hour format, 2 time at most.
     * @param cancelAfterDays An integer after which days the alarm will be canceled.
     */
    public static void sendAlarmRemind(int[] hours, int beginAfterDays, int cancelAfterDays){
        Context context = mActivity.getApplicationContext();
        if(checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)){
            WakeupAlarmManager.setHours(hours);
            //有权限时发送通知
            WakeupAlarmManager.sendRemind(context,beginAfterDays, cancelAfterDays);
            Log.i(TAG, "Alarm Send Success!");
        }else{
            Log.e(TAG, "Lack Notification Permission.");
        }
    }
}
