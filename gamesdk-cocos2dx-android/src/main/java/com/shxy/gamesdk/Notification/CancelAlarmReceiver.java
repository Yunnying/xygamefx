package com.shxy.gamesdk.Notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 取消已经设定的Alarm
 * @author: 翟宇翔
 * @date: 2023/9/12
 */
public class CancelAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        try {
            // 创建一个与设置提醒时相同的 Intent
            Intent remindIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent remindPendingIntent = PendingIntent.getBroadcast(context, 0, remindIntent, PendingIntent.FLAG_IMMUTABLE);
            // 获取 AlarmManager 的实例
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // 取消提醒，即取消之前设置的提醒
            if(remindPendingIntent != null){
                alarmManager.cancel(remindPendingIntent);
                Log.d("CancelAlarmReceiver","Cancel Alarm Success!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
