package com.example.creditcalculator;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "payment_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        createChannel(context);

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String title = intent.getStringExtra("title");
        double amount = intent.getDoubleExtra("amount", 0.0);
        long dueDate = intent.getLongExtra("due_date", 0L);
        int daysBefore = intent.getIntExtra("days_before", 1);

        NumberFormat money = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        money.setMaximumFractionDigits(2);
        money.setMinimumFractionDigits(0);
        String date = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU"))
                .format(new Date(dueDate));

        String content = "Платёж " + money.format(amount) + " ₽ — " + date
                + ". Напоминание за " + daysBefore + " дн.";

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                1001,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null || title.trim().isEmpty() ? "Платёж по кредиту" : title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        int notificationId = (int) (System.currentTimeMillis() & 0x7fffffff);
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о платежах",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Напоминания о предстоящих платежах по кредитам и рассрочкам");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
