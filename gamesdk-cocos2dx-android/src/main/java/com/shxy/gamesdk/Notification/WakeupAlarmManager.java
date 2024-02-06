package com.shxy.gamesdk.Notification;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by allen on 6/30/2017.
 */

public class WakeupAlarmManager {

    protected static String TAG = "WakeupAlarmManager";
    protected static int[] mHours = new int[]{8, 18};

    protected static void setHours(int[] hours){
        mHours = hours;
    }

    /**
     * 定期发送通知栏提醒，在一定时间后该提醒会不再发送
     * @param context
     * @param beginAfterDays beginAfterDays 天后开始发送通知
     * @param cancelAfterDays 通知发送 cancelAfterDays 天后停止发送
     */
    protected static void sendRemind(Context context, int beginAfterDays, int cancelAfterDays){
        //Remove the existed remind
        stopRemind(context);

        Arrays.sort(mHours);
        // 设置提醒时间
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.add(Calendar.DAY_OF_YEAR, beginAfterDays);
        int nextHour = -1;
        //设置当前的小时数
        int curHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        //判断当前的小时数与两个提醒时间的关系
        for (int mHour : mHours) {
            if (mHour > curHour) {
                nextHour = mHour;
                break;
            }
        }
        //当前时间已经过了今天的最晚提醒时间，并且开始时间为当天将天数设置为第二天
        if(nextHour < 0 && beginAfterDays == 0){
            nextHour = mHours[0];
            mCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        //设置提醒时间
        mCalendar.set(Calendar.HOUR_OF_DAY, nextHour);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        try {
            //AlarmReceiver.class为广播接受者
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("customHours",mHours);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            //得到AlarmManager实例
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(), pi);

        }catch (Exception ex){
            Log.d(TAG, "sendRemind: Error in sendRemind : " + ex.getMessage());
            ex.printStackTrace();
        }
        //cancelAfterDays不为0时，在若干时间后执行取消
        if(cancelAfterDays > 0){
            Calendar mCancelCalendar = Calendar.getInstance();
            mCancelCalendar.add(Calendar.DAY_OF_YEAR, cancelAfterDays);
            try {
                //CancelAlarmReceiver.class为广播接受者
                Intent cancelIntent = new Intent(context, CancelAlarmReceiver.class);
                PendingIntent cancelPi = PendingIntent.getBroadcast(context,0,cancelIntent,PendingIntent.FLAG_MUTABLE);
                AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, mCancelCalendar.getTimeInMillis(), cancelPi);
                Log.d(TAG,"Cancel Alarm");
            }catch (Exception ex){
                Log.d(TAG,"Error in cancel Alarm");
                ex.printStackTrace();
            }
        }
    }

    /**
     * 停止现有的闹钟
     * @param context 上下文内容
     */
    protected static void stopRemind(Context context){
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            //取消原有的广播
            if(pi != null){
                am.cancel(pi);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * 发送下一个提醒时刻的通知
     * @param context
     * @param hours
     */
    protected static void sendRemindByHours(Context context, int[] hours){
        Log.d(TAG, "Into sendRemindByHours");
        if(hours == null || hours.length == 0 || hours.length >= 3)
        {
            return;
        }
        // 移除已有的通知
        stopRemind(context);
        //对数组进行升序排序
        Arrays.sort(hours);
        // 设置calender。初始化为当前时间
        Calendar mCalendar = Calendar.getInstance();
        int nextHour = -1;
        //设置当前的小时数
        int curHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        //判断当前的小时数与两个提醒时间的关系
        for (int hour : hours) {
            if (hour > curHour) {
                nextHour = hour;
                break;
            }
        }
        //当前时间已经过了最晚提醒时间，将天数设置为第二天
        if(nextHour < 0){
            nextHour = hours[0];
            mCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        //设置提醒时间
        mCalendar.set(Calendar.HOUR_OF_DAY, nextHour);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        Log.d(TAG, "Remind Date " + mCalendar.getTime());
        //发送Alarm事件，并设立一个闹钟停止器
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("customHours",hours);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(), pi);
        }catch (Exception ex){
            Log.e(TAG, "alarm Error in sendRemind: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
