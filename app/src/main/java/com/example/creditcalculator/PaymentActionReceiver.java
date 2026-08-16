package com.example.creditcalculator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class PaymentActionReceiver extends BroadcastReceiver {
    public static final String ACTION_MARK_PAID = "com.example.creditcalculator.ACTION_MARK_PAID";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_MARK_PAID.equals(intent.getAction())) return;
        long id = intent.getLongExtra("reminder_id", -1L);
        int index = intent.getIntExtra("payment_index", -1);
        int notificationId = intent.getIntExtra("notification_id", -1);
        ReminderScheduler.PaymentReminder r = ReminderScheduler.findById(context, id);
        if (r == null || index < 0 || index >= r.months) return;
        ReminderScheduler.markPaid(context, id, index, System.currentTimeMillis(), ReminderScheduler.paymentAmount(r, index), 0);
        if (notificationId >= 0) NotificationManagerCompat.from(context).cancel(notificationId);
    }
}
