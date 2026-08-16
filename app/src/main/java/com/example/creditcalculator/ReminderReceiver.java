package com.example.creditcalculator;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        long id = intent.getLongExtra("reminder_id", -1L);
        int index = intent.getIntExtra("payment_index", -1);
        String kind = intent.getStringExtra("reminder_kind");
        ReminderScheduler.PaymentReminder r = ReminderScheduler.findById(context, id);
        if (r == null || !ReminderScheduler.STATUS_ACTIVE.equals(r.status) || index < 0 || index >= r.months || ReminderScheduler.isPaid(r, index)) return;

        double amount = ReminderScheduler.paymentAmount(r, index);
        long dueDate = ReminderScheduler.buildDueDate(r, index).getTimeInMillis();
        boolean today = ReminderScheduler.isToday(r,index);
        boolean overdue = ReminderScheduler.isOverdue(r,index);
        boolean snoozed = "snooze".equals(kind);
        boolean sound = AppPreferences.isSoundEnabled(context) && r.soundEnabled;
        boolean vibration = AppPreferences.isVibrationEnabled(context);
        Uri soundUri = resolveSound(context, sound);
        String channel = createChannel(context, sound, vibration, soundUri);
        int notificationId = PaymentNotificationHelper.notificationId(id,index);

        Intent open = new Intent(context, PaymentDetailsActivity.class);
        open.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snooze = new Intent(context, SnoozeActivity.class);
        snooze.putExtra("reminder_id", id); snooze.putExtra("payment_index", index); snooze.putExtra("notification_id", notificationId);
        snooze.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent snoozeIntent = PendingIntent.getActivity(context, notificationId + 200000, snooze, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title;
        String content;
        if (overdue) {
            title = AppPreferences.tr(context, "Платёж просрочен", "Payment overdue");
            content = r.title + " — " + FormatUtils.money(context, amount) + " · " + FormatUtils.date(context, dueDate);
        } else if (today) {
            title = AppPreferences.tr(context, "Платёж сегодня", "Payment due today");
            content = r.title + " — " + FormatUtils.money(context, amount) + "\n" + AppPreferences.tr(context, "Срок оплаты: сегодня", "Due: today");
        } else if(snoozed){
            title = AppPreferences.tr(context,"Напоминание о платеже","Payment reminder");
            content = r.title + " — " + FormatUtils.money(context, amount) + " · " + FormatUtils.date(context, dueDate);
        } else {
            title = r.title;
            content = AppPreferences.tr(context, "Скоро платёж", "Payment coming up") + ": " + FormatUtils.money(context, amount) + " · " + FormatUtils.date(context, dueDate);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content.replace("\n", " · "))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(!today)
                .setOngoing(today)
                .setContentIntent(contentIntent);

        // "Already paid" is deliberately shown only on the actual due date.
        if(today){
            PendingIntent paidIntent;
            if (AppPreferences.hasConfiguredSecurity(context)) {
                Intent paidOpen = new Intent(context, PaymentDetailsActivity.class);
                paidOpen.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, id);
                paidOpen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                paidIntent = PendingIntent.getActivity(context, notificationId + 100000, paidOpen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                Intent paid = new Intent(context, PaymentActionReceiver.class);
                paid.setAction(PaymentActionReceiver.ACTION_MARK_PAID);
                paid.putExtra("reminder_id", id); paid.putExtra("payment_index", index); paid.putExtra("notification_id", notificationId);
                paidIntent = PendingIntent.getBroadcast(context, notificationId + 100000, paid, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }
            b.addAction(0, AppPreferences.tr(context, "✓ Уже оплачено", "✓ Already paid"), paidIntent);
            PaymentNotificationHelper.scheduleEndOfDayClear(context,id,index);
        }
        b.addAction(0, AppPreferences.tr(context, "Напомнить позже", "Remind later"), snoozeIntent);

        if (today || overdue) b.setColor(ContextCompat.getColor(context, R.color.danger));
        else b.setColor(ContextCompat.getColor(context, R.color.primary));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (sound && soundUri != null) b.setSound(soundUri);
            if (vibration) b.setVibrate(new long[]{0,350,180,350,180,500});
        }
        NotificationManagerCompat.from(context).notify(notificationId, b.build());
    }

    private Uri resolveSound(Context c,boolean enabled){if(!enabled)return null;String saved=AppPreferences.getSoundUri(c);if(saved==null||saved.isEmpty())return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);try{return Uri.parse(saved);}catch(Exception e){return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);}}
    private String createChannel(Context c,boolean sound,boolean vibration,Uri uri){String key=String.valueOf(uri)+"_"+sound+"_"+vibration;String id="payment_alarm_"+Integer.toHexString(key.hashCode());if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(id,AppPreferences.tr(c,"Напоминания о платежах","Payment reminders"),NotificationManager.IMPORTANCE_HIGH);ch.setDescription(AppPreferences.tr(c,"Напоминания до платежа и в день оплаты","Reminders before and on payment day"));ch.enableVibration(vibration);if(vibration)ch.setVibrationPattern(new long[]{0,350,180,350});if(sound&&uri!=null){AudioAttributes a=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();ch.setSound(uri,a);}else ch.setSound(null,null);NotificationManager nm=c.getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(ch);}return id;}
}
