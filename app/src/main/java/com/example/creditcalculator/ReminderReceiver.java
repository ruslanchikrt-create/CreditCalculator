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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String title = intent.getStringExtra("title");
        double amount = intent.getDoubleExtra("amount", 0.0);
        long dueDate = intent.getLongExtra("due_date", 0L);
        int daysBefore = intent.getIntExtra("days_before", 1);
        long reminderId = intent.getLongExtra("reminder_id", -1L);
        boolean itemSoundEnabled = intent.getBooleanExtra("item_sound_enabled", true);

        boolean soundEnabled = AppPreferences.isSoundEnabled(context) && itemSoundEnabled;
        boolean vibrationEnabled = AppPreferences.isVibrationEnabled(context);
        Uri soundUri = resolveSoundUri(context, soundEnabled);
        String channelId = createChannel(context, soundEnabled, vibrationEnabled, soundUri);

        String content = AppPreferences.isEnglish(context)
                ? "Payment " + FormatUtils.money(context, amount) + " is due on " + FormatUtils.date(context, dueDate)
                    + ". Reminder " + daysBefore + (daysBefore == 1 ? " day before." : " days before.")
                : "Платёж " + FormatUtils.money(context, amount) + " — " + FormatUtils.date(context, dueDate)
                    + ". Напоминание за " + daysBefore + " дн.";

        Intent open = reminderId > 0
                ? new Intent(context, PaymentDetailsActivity.class)
                : new Intent(context, MainActivity.class);
        if (reminderId > 0) open.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, reminderId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title == null || title.trim().isEmpty()
                        ? AppPreferences.tr(context, "Платёж", "Payment")
                        : title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (soundEnabled && soundUri != null) builder.setSound(soundUri);
            if (vibrationEnabled) builder.setVibrate(new long[]{0, 250, 150, 250});
            else builder.setVibrate(new long[]{0L});
        }

        int notificationId = (int) (System.currentTimeMillis() & 0x7fffffff);
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private Uri resolveSoundUri(Context context, boolean enabled) {
        if (!enabled) return null;
        String saved = AppPreferences.getSoundUri(context);
        if (saved == null || saved.trim().isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        try {
            return Uri.parse(saved);
        } catch (Exception e) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }

    private String createChannel(Context context, boolean soundEnabled, boolean vibrationEnabled, Uri soundUri) {
        String key = String.valueOf(soundUri) + "_" + soundEnabled + "_" + vibrationEnabled;
        String channelId = "payment_reminders_" + Integer.toHexString(key.hashCode());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    AppPreferences.tr(context, "Напоминания о платежах", "Payment reminders"),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(AppPreferences.tr(context,
                    "Напоминания о предстоящих платежах",
                    "Upcoming payment reminders"));
            channel.enableVibration(vibrationEnabled);
            if (vibrationEnabled) channel.setVibrationPattern(new long[]{0, 250, 150, 250});
            if (soundEnabled && soundUri != null) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, attributes);
            } else {
                channel.setSound(null, null);
            }
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        return channelId;
    }
}
