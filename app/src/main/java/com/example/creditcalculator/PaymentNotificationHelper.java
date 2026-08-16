package com.example.creditcalculator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public final class PaymentNotificationHelper {
    private PaymentNotificationHelper() {}

    public static int notificationId(long reminderId,int index){
        int base=(int)(reminderId^(reminderId>>>32));
        int value=31*base+index;
        return value==Integer.MIN_VALUE?0:Math.abs(value);
    }

    public static void cancel(Context context,long reminderId,int index){
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId,index));
        cancelEndOfDay(context,reminderId,index);
    }

    public static void scheduleEndOfDayClear(Context context,long reminderId,int index){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        Calendar clearAt=Calendar.getInstance();clearAt.add(Calendar.DAY_OF_MONTH,1);clearAt.set(Calendar.HOUR_OF_DAY,0);clearAt.set(Calendar.MINUTE,5);clearAt.set(Calendar.SECOND,0);clearAt.set(Calendar.MILLISECOND,0);
        PendingIntent pi=clearPendingIntent(context,reminderId,index,false);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,clearAt.getTimeInMillis(),pi);else am.set(AlarmManager.RTC_WAKEUP,clearAt.getTimeInMillis(),pi);
    }

    public static void cancelEndOfDay(Context context,long reminderId,int index){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);PendingIntent pi=clearPendingIntent(context,reminderId,index,true);if(pi!=null){if(am!=null)am.cancel(pi);pi.cancel();}
    }

    private static PendingIntent clearPendingIntent(Context context,long reminderId,int index,boolean noCreate){
        Intent i=new Intent(context,PaymentActionReceiver.class);i.setAction(PaymentActionReceiver.ACTION_CLEAR_TODAY_NOTIFICATION+"_"+reminderId+"_"+index);i.putExtra("reminder_id",reminderId);i.putExtra("payment_index",index);i.putExtra("notification_id",notificationId(reminderId,index));int flags=PendingIntent.FLAG_IMMUTABLE|(noCreate?PendingIntent.FLAG_NO_CREATE:PendingIntent.FLAG_UPDATE_CURRENT);return PendingIntent.getBroadcast(context,clearRequestCode(reminderId,index),i,flags);
    }

    private static int clearRequestCode(long id,int index){int n=notificationId(id,index)^0x5F3759DF;return n==Integer.MIN_VALUE?1:Math.abs(n);}
}
