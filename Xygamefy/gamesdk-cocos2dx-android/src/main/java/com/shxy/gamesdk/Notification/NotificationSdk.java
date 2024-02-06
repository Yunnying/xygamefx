package com.shxy.gamesdk.Notification;

/**
 * @author: 翟宇翔
 * @date: 2023/11/8
 */


 import android.app.Activity;

public class NotificationSdk {

    public static void init(Activity activity){
        NotificationManager.init(activity);
    }
    /**
     * Send an notification regularly, and cancel it after the given days.
     * @param hours An integer array containing the time for sending reminders in 24-hour format, 2 time at most.
     * @param beginAfterDays An integer after which days the alarm will be sent.
     * @param cancelAfterDays An integer after which days the alarm will be canceled.
     */
    public static void sendAlarmRemind(int[] hours, int beginAfterDays, int cancelAfterDays){
        NotificationManager.sendAlarmRemind(hours, beginAfterDays, cancelAfterDays);
    }
}
