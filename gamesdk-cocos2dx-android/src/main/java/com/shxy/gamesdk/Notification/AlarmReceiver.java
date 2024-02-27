package com.shxy.gamesdk.Notification;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

/**
 * Created by allen on 6/30/2017.
 * modified by Yuxiang_Zhai on 9/15/2023.
 * modified by zhaiyx on 2/21/2024.
 */


public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String pkName = context.getPackageName();
        int[] hours = new int[]{8, 18};
        int resIDNotifyTime1 = context.getResources().getIdentifier("notify_time_1", "integer",pkName);
        int resIDNotifyTime2 = context.getResources().getIdentifier("notify_time_2", "integer",pkName);
        if(resIDNotifyTime1 != 0){
            hours[0] = context.getResources().getInteger(resIDNotifyTime1);
            Log.i("AlarmReceiver", "onReceive: The hours_0 is "+ hours[0]);
        }
        if(resIDNotifyTime2 != 0){
            hours[1] = context.getResources().getInteger(resIDNotifyTime2);
            Log.i("AlarmReceiver", "onReceive: The hours_1 is "+ hours[1]);
        }
        final String TAG = "AlarmReceiver";
        if(intent != null && intent.hasExtra("customHours")){
            hours = intent.getIntArrayExtra("customHours");
        }

        //从主模块获取通知的相关信息
        //获取包名
        Log.i("AlarmReceiver", "onReceive: The packageName is "+ pkName);
        //获取CHANNEL_ID
        int resIDChannelId = context.getResources().getIdentifier("notify_channel_id", "string",pkName);
        String CHANNEL_ID = "";
        if(resIDChannelId != 0){
            CHANNEL_ID = context.getString(resIDChannelId);
            Log.i("AlarmReceiver", "onReceive: The CHANNEL_ID is "+ CHANNEL_ID);
        }else{
            Log.e("AlarmReceiver", "onReceive: The CHANNEL_ID is null");
        }
        //获取CHANNEL_NAME
        int resIDChannelName = context.getResources().getIdentifier("notify_channel_name", "string",pkName);
        String CHANNEL_NAME = "";
        if(resIDChannelName != 0){
            CHANNEL_NAME = context.getString(resIDChannelName);
            Log.i("AlarmReceiver", "onReceive: The CHANNEL_NAME is "+ CHANNEL_NAME);
        }else{
            Log.e("AlarmReceiver", "onReceive: The CHANNEL_NAME is null");
        }
        //获取NOTIFY_TITLE
        int resIDNotifyTitle = context.getResources().getIdentifier("notify_title", "string",pkName);
        String NOTIFY_TITLE = "";
        if(resIDNotifyTitle != 0){
            NOTIFY_TITLE = context.getString(resIDNotifyTitle);
            Log.i("AlarmReceiver", "onReceive: The NOTIFY_TITLE is "+ NOTIFY_TITLE);
        }else{
            Log.e("AlarmReceiver", "onReceive: The NOTIFY_TITLE is null");
        }
        //获取NOTIFY_DESC
        int resIDNotifyDesc1 = context.getResources().getIdentifier("notify_desc_1", "string",pkName);
        int resIDNotifyDesc2 = context.getResources().getIdentifier("notify_desc_2", "string",pkName);
        String NOTIFY_DESC = "";
        if(resIDNotifyDesc1 != 0){
            NOTIFY_DESC = context.getString(resIDNotifyDesc1);
            Log.i("AlarmReceiver", "onReceive: The NOTIFY_DESC is "+ NOTIFY_DESC);
        }else{
            Log.e("AlarmReceiver", "onReceive: The NOTIFY_DESC is null");
        }

        //获取通知布局
        int resIDNotifyLayout = context.getResources().getIdentifier("notify_layout", "layout",pkName);
        if(resIDNotifyLayout == 0){
            Log.e("AlarmReceiver", "onReceive: The NotifyLayout is null");
        }
        //获取通知标题
        int resIDNotificationTitle = context.getResources().getIdentifier("notification_title", "id",pkName);
        if(resIDNotificationTitle == 0){
            Log.e("AlarmReceiver", "onReceive: The NotificationTitle is null");
        }
        //获取通知的小图标
        int resIDSmallIcon = context.getResources().getIdentifier("notify_small_icon", "drawable",pkName);
        if(resIDSmallIcon == 0){
            Log.e("AlarmReceiver", "onReceive: The SmallIcon is null");
        }



        //判断当前处于哪个时间点
        Calendar mCalendar = Calendar.getInstance();
        int curHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        //根据当前时间点确定加载通知的样式
        int id = (curHour==hours[0]) ? 0 : 1;
        int[] notification_desc_ids = new int[]{
                resIDNotifyDesc1,
                resIDNotifyDesc2
        };

        CharSequence notification_desc = context.getText(notification_desc_ids[id]);
        Log.d(TAG, "onReceive: Id is " + id);
        Intent appIntent = new Intent("Activity.ENTER");
        appIntent.putExtra("isFromNotification", true);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews notificationLayout = new RemoteViews(pkName, resIDNotifyLayout);
        notificationLayout.setTextViewText(resIDNotificationTitle, notification_desc);
        notification_desc.length();

        final DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();

        TextView textView = new TextView(context);
        textView.setTextSize(18);
        textView.setWidth(dm.widthPixels);

        TextPaint textPaint = textView.getPaint();
        float textPaintWidth = textPaint.measureText(notification_desc.toString());
        if (textPaintWidth > dm.widthPixels * 1.5f){
            notificationLayout.setTextViewTextSize(resIDNotificationTitle, COMPLEX_UNIT_SP, 14);
            Log.d("AlarmReceiver", "COMPLEX_UNIT_SP 14");
        }
        else{
            notificationLayout.setTextViewTextSize(resIDNotificationTitle, COMPLEX_UNIT_SP, 18);
            Log.d("AlarmReceiver", "COMPLEX_UNIT_SP 18");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(resIDSmallIcon) //设置小图标
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_HIGH) //设置优先级
                .setContentIntent(pendingIntent)//点击跳转到应用
                .setAutoCancel(true);//点击后通知自动消失

        Notification notify = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            builder.setChannelId((String) context.getText(resIDChannelId));
        }
        notify = builder.build();

        notify.defaults |= Notification.DEFAULT_SOUND;
        notify.flags |= Notification.FLAG_AUTO_CANCEL; // FLAG_AUTO_CANCEL表明当通知被用户点击时，通知将被清除。
        // 在Android进行通知处理，首先需要重系统哪里获得通知管理器NotificationManager，它是一个系统Service。
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(new NotificationChannel(
                    (String)context.getText(resIDChannelId),
                    context.getText(resIDChannelName), NotificationManager.IMPORTANCE_HIGH));
        }
        manager.notify(1001, notify);
        WakeupAlarmManager.sendRemindByHours(context,hours);
    }
}