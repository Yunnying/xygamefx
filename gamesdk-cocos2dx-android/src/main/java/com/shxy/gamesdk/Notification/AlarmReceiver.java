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


import com.shxy.gamesdk.R;

import java.util.Calendar;

/**
 * Created by allen on 6/30/2017.
 * modified by Yuxiang_Zhai on 9/15/2023.
 */


public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int[] hours = new int[]{8, 18};
        if(intent != null && intent.hasExtra("customHours")){
            hours = intent.getIntArrayExtra("customHours");
        }

        final int NOTIFY_ID = 1001;
        final String CHANNEL_ID = context.getString(R.string.notify_channel_id);
        final String CHANNEL_NAME = context.getString(R.string.notify_channel_name);
        final String NOTIFY_TITLE = context.getString(R.string.notify_title);
        String NOTIFY_DESC = context.getString(R.string.notify_desc_1);
        final String TAG = "Alarm";

        //判断当前处于哪个时间点
        Calendar mCalendar = Calendar.getInstance();
        int curHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        //根据当前时间点确定加载通知的样式
        int id = (curHour==hours[0]) ? 0 : 1;
        int[] notification_desc_ids = new int[]{
                R.string.notify_desc_1,
                R.string.notify_desc_2
        };

        CharSequence notification_desc = context.getText(notification_desc_ids[id]);
        Log.d(TAG, "onReceive: Id is " + id);
        Intent appIntent = new Intent("Activity.ENTER");
        appIntent.putExtra("isFromNotification", "yes");
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.notify_layout);
        Log.d(TAG, "onReceive: Package Name is "+context.getPackageName());
        notificationLayout.setTextViewText(R.id.notification_title, notification_desc);
        notification_desc.length();

        final DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();

        TextView textView = new TextView(context);
        textView.setTextSize(18);
        textView.setWidth(dm.widthPixels);

        TextPaint textPaint = textView.getPaint();
        float textPaintWidth = textPaint.measureText(notification_desc.toString());
        if (textPaintWidth > dm.widthPixels * 1.5f){
            notificationLayout.setTextViewTextSize(R.id.notification_title, COMPLEX_UNIT_SP, 14);
            Log.d("AlarmReceiver", "COMPLEX_UNIT_SP 14");
        }
        else{
            notificationLayout.setTextViewTextSize(R.id.notification_title, COMPLEX_UNIT_SP, 18);
            Log.d("AlarmReceiver", "COMPLEX_UNIT_SP 18");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notify_small_icon) //设置小图标
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_HIGH) //设置优先级
                .setContentIntent(pendingIntent)//点击跳转到应用
                .setAutoCancel(true);//点击后通知自动消失

        Notification notify = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            builder.setChannelId((String) context.getText(R.string.notify_channel_id));
        }
        notify = builder.build();

        notify.defaults |= Notification.DEFAULT_SOUND;
        notify.flags |= Notification.FLAG_AUTO_CANCEL; // FLAG_AUTO_CANCEL表明当通知被用户点击时，通知将被清除。
        // 在Android进行通知处理，首先需要重系统哪里获得通知管理器NotificationManager，它是一个系统Service。
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(new NotificationChannel(
                    (String)context.getText(R.string.notify_channel_id),
                    context.getText(R.string.notify_channel_name), NotificationManager.IMPORTANCE_HIGH));
        }
        manager.notify(1001, notify);
        WakeupAlarmManager.sendRemindByHours(context,hours);
    }
}