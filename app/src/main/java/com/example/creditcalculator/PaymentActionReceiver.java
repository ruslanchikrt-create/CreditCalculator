package com.example.creditcalculator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class PaymentActionReceiver extends BroadcastReceiver {
    public static final String ACTION_MARK_PAID = "com.example.creditcalculator.ACTION_MARK_PAID";
    public static final String ACTION_CLEAR_TODAY_NOTIFICATION = "com.example.creditcalculator.ACTION_CLEAR_TODAY_NOTIFICATION";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction()==null) return;
        String action=intent.getAction();
        long id = intent.getLongExtra("reminder_id", -1L);
        int index = intent.getIntExtra("payment_index", -1);
        int notificationId = intent.getIntExtra("notification_id", -1);

        if(action.startsWith(ACTION_CLEAR_TODAY_NOTIFICATION)){
            // Payment-day notices are intentionally persistent and are cleared only after the payment is marked paid.
            return;
        }
        if (!ACTION_MARK_PAID.equals(action)) return;
        ReminderScheduler.PaymentReminder r = ReminderScheduler.findById(context, id);
        if (r == null || index < 0 || index >= r.months) return;
        // With app protection enabled the notification opens the protected details screen instead,
        // so this receiver is only reached when it is safe to record the payment immediately.
        if (AppPreferences.hasConfiguredSecurity(context)) return;
        ReminderScheduler.markPaid(context, id, index, System.currentTimeMillis(), ReminderScheduler.paymentAmount(r, index), 0);
        PaymentNotificationHelper.cancel(context,id,index);
    }
}
