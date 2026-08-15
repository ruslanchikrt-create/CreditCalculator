package com.example.creditcalculator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class ReminderScheduler {

    public static final String TYPE_CREDIT = "credit";
    public static final String TYPE_MORTGAGE = "mortgage";
    public static final String TYPE_AUTO = "auto";
    public static final String TYPE_INSTALLMENT = "installment";
    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_OTHER = "other";

    private static final String PREFS = "payment_reminders";
    private static final String KEY_ITEMS = "items";

    private ReminderScheduler() {
    }

    public static class PaymentReminder {
        public long id;
        public String type;
        public String title;
        public double principal;
        public double annualRate;
        public double amount;
        public long firstPaymentMillis;
        public int months;
        public int daysBefore;

        public PaymentReminder(long id, String type, String title, double principal,
                               double annualRate, double amount, long firstPaymentMillis,
                               int months, int daysBefore) {
            this.id = id;
            this.type = normalizeType(type);
            this.title = title;
            this.principal = principal;
            this.annualRate = annualRate;
            this.amount = amount;
            this.firstPaymentMillis = firstPaymentMillis;
            this.months = months;
            this.daysBefore = daysBefore;
        }
    }

    public static List<PaymentReminder> load(Context context) {
        List<PaymentReminder> result = new ArrayList<>();
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ITEMS, "[]");

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                double amount = object.optDouble("amount", 0.0);
                int months = object.optInt("months", 1);
                double principal = object.has("principal")
                        ? object.optDouble("principal", 0.0)
                        : amount * Math.max(1, months);
                result.add(new PaymentReminder(
                        object.getLong("id"),
                        object.optString("type", TYPE_CREDIT),
                        object.optString("title", "Кредит"),
                        principal,
                        object.optDouble("annualRate", 0.0),
                        amount,
                        object.getLong("firstPaymentMillis"),
                        Math.max(1, months),
                        Math.max(1, Math.min(7, object.optInt("daysBefore", 3)))
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static PaymentReminder findById(Context context, long id) {
        for (PaymentReminder reminder : load(context)) {
            if (reminder.id == id) {
                return reminder;
            }
        }
        return null;
    }

    private static void save(Context context, List<PaymentReminder> reminders) {
        JSONArray array = new JSONArray();
        try {
            for (PaymentReminder reminder : reminders) {
                JSONObject object = new JSONObject();
                object.put("id", reminder.id);
                object.put("type", normalizeType(reminder.type));
                object.put("title", reminder.title);
                object.put("principal", reminder.principal);
                object.put("annualRate", reminder.annualRate);
                object.put("amount", reminder.amount);
                object.put("firstPaymentMillis", reminder.firstPaymentMillis);
                object.put("months", reminder.months);
                object.put("daysBefore", reminder.daysBefore);
                array.put(object);
            }
        } catch (Exception ignored) {
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    public static void add(Context context, PaymentReminder reminder) {
        List<PaymentReminder> reminders = load(context);
        reminders.add(reminder);
        save(context, reminders);
        schedule(context, reminder);
    }

    public static void delete(Context context, long id) {
        List<PaymentReminder> reminders = load(context);
        List<PaymentReminder> updated = new ArrayList<>();
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                cancel(context, reminder);
            } else {
                updated.add(reminder);
            }
        }
        save(context, updated);
    }

    public static void rescheduleAll(Context context) {
        for (PaymentReminder reminder : load(context)) {
            schedule(context, reminder);
        }
    }

    public static void schedule(Context context, PaymentReminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (int index = 0; index < reminder.months; index++) {
            Calendar dueDate = buildDueDate(reminder.firstPaymentMillis, index);
            Calendar notificationDate = (Calendar) dueDate.clone();
            notificationDate.add(Calendar.DAY_OF_MONTH, -reminder.daysBefore);

            long triggerAt = notificationDate.getTimeInMillis();
            if (triggerAt <= now) {
                continue;
            }

            PendingIntent pendingIntent = buildPendingIntent(context, reminder, index, dueDate, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }
    }

    private static void cancel(Context context, PaymentReminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        for (int index = 0; index < reminder.months; index++) {
            Calendar dueDate = buildDueDate(reminder.firstPaymentMillis, index);
            PendingIntent pendingIntent = buildPendingIntent(context, reminder, index, dueDate, true);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static PendingIntent buildPendingIntent(Context context, PaymentReminder reminder,
                                                     int index, Calendar dueDate, boolean noCreate) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.creditcalculator.PAYMENT_" + reminder.id + "_" + index);
        intent.putExtra("reminder_id", reminder.id);
        intent.putExtra("title", reminder.title);
        intent.putExtra("type", normalizeType(reminder.type));
        intent.putExtra("amount", reminder.amount);
        intent.putExtra("due_date", dueDate.getTimeInMillis());
        intent.putExtra("days_before", reminder.daysBefore);

        int flags = PendingIntent.FLAG_IMMUTABLE;
        flags |= noCreate ? PendingIntent.FLAG_NO_CREATE : PendingIntent.FLAG_UPDATE_CURRENT;

        return PendingIntent.getBroadcast(
                context,
                requestCode(reminder.id, index),
                intent,
                flags
        );
    }

    public static Calendar buildDueDate(long firstPaymentMillis, int monthIndex) {
        Calendar first = Calendar.getInstance();
        first.setTimeInMillis(firstPaymentMillis);
        int preferredDay = first.get(Calendar.DAY_OF_MONTH);

        Calendar due = Calendar.getInstance();
        due.clear();
        due.set(Calendar.YEAR, first.get(Calendar.YEAR));
        due.set(Calendar.MONTH, first.get(Calendar.MONTH));
        due.set(Calendar.DAY_OF_MONTH, 1);
        due.set(Calendar.HOUR_OF_DAY, 9);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        due.add(Calendar.MONTH, monthIndex);
        due.set(Calendar.DAY_OF_MONTH, Math.min(preferredDay, due.getActualMaximum(Calendar.DAY_OF_MONTH)));
        return due;
    }

    public static int nextPaymentIndex(PaymentReminder reminder) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < reminder.months; i++) {
            Calendar due = buildDueDate(reminder.firstPaymentMillis, i);
            Calendar endOfDay = (Calendar) due.clone();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            if (endOfDay.getTimeInMillis() >= now) {
                return i;
            }
        }
        return -1;
    }

    public static String normalizeType(String type) {
        if (type == null) {
            return TYPE_CREDIT;
        }
        String value = type.trim().toLowerCase();
        if (value.equals(TYPE_CREDIT) || value.contains("кредит") && !value.contains("авто")) {
            return TYPE_CREDIT;
        }
        if (value.equals(TYPE_MORTGAGE) || value.contains("ипот")) {
            return TYPE_MORTGAGE;
        }
        if (value.equals(TYPE_AUTO) || value.contains("авто")) {
            return TYPE_AUTO;
        }
        if (value.equals(TYPE_INSTALLMENT) || value.contains("расср")) {
            return TYPE_INSTALLMENT;
        }
        if (value.equals(TYPE_DEPOSIT) || value.contains("вклад")) {
            return TYPE_DEPOSIT;
        }
        if (value.equals(TYPE_OTHER)) {
            return TYPE_OTHER;
        }
        return TYPE_CREDIT;
    }

    private static int requestCode(long id, int index) {
        int base = (int) (id ^ (id >>> 32));
        return 31 * base + index;
    }
}
