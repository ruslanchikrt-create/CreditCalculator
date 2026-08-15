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
    public static final String TYPE_OTHER = "other"; // legacy only

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVE = "archive";
    public static final String STATUS_TRASH = "trash";

    public static final long TRASH_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L;

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
        public String status;
        public long deletedAt;
        public boolean soundEnabled;

        public PaymentReminder(long id, String type, String title, double principal,
                               double annualRate, double amount, long firstPaymentMillis,
                               int months, int daysBefore) {
            this(id, type, title, principal, annualRate, amount, firstPaymentMillis,
                    months, daysBefore, STATUS_ACTIVE, 0L, true);
        }

        public PaymentReminder(long id, String type, String title, double principal,
                               double annualRate, double amount, long firstPaymentMillis,
                               int months, int daysBefore, String status, long deletedAt,
                               boolean soundEnabled) {
            this.id = id;
            this.type = normalizeType(type);
            this.title = title;
            this.principal = principal;
            this.annualRate = annualRate;
            this.amount = amount;
            this.firstPaymentMillis = firstPaymentMillis;
            this.months = Math.max(1, months);
            this.daysBefore = Math.max(1, Math.min(7, daysBefore));
            this.status = normalizeStatus(status);
            this.deletedAt = deletedAt;
            this.soundEnabled = soundEnabled;
        }
    }

    /** Active payment plans, kept for compatibility with existing screens. */
    public static List<PaymentReminder> load(Context context) {
        return listByStatus(context, STATUS_ACTIVE);
    }

    public static List<PaymentReminder> listByStatus(Context context, String status) {
        List<PaymentReminder> result = new ArrayList<>();
        String normalized = normalizeStatus(status);
        for (PaymentReminder reminder : loadAll(context)) {
            if (normalized.equals(reminder.status)) result.add(reminder);
        }
        return result;
    }

    public static List<PaymentReminder> loadAll(Context context) {
        List<PaymentReminder> result = new ArrayList<>();
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ITEMS, "[]");
        boolean changed = false;
        long now = System.currentTimeMillis();

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                double amount = object.optDouble("amount", 0.0);
                int months = Math.max(1, object.optInt("months", 1));
                double principal = object.has("principal")
                        ? object.optDouble("principal", 0.0)
                        : amount * months;
                String status = normalizeStatus(object.optString("status", STATUS_ACTIVE));
                long deletedAt = object.optLong("deletedAt", 0L);

                if (STATUS_TRASH.equals(status)
                        && deletedAt > 0
                        && now - deletedAt >= TRASH_RETENTION_MILLIS) {
                    changed = true;
                    continue;
                }

                result.add(new PaymentReminder(
                        object.getLong("id"),
                        object.optString("type", TYPE_CREDIT),
                        object.optString("title", "Кредит"),
                        principal,
                        object.optDouble("annualRate", 0.0),
                        amount,
                        object.getLong("firstPaymentMillis"),
                        months,
                        object.optInt("daysBefore", 3),
                        status,
                        deletedAt,
                        object.optBoolean("soundEnabled", true)
                ));
            }
        } catch (Exception ignored) {
        }

        if (changed) saveRaw(context, result);
        return result;
    }

    public static PaymentReminder findById(Context context, long id) {
        for (PaymentReminder reminder : loadAll(context)) {
            if (reminder.id == id) return reminder;
        }
        return null;
    }

    private static void saveRaw(Context context, List<PaymentReminder> reminders) {
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
                object.put("status", normalizeStatus(reminder.status));
                object.put("deletedAt", reminder.deletedAt);
                object.put("soundEnabled", reminder.soundEnabled);
                array.put(object);
            }
        } catch (Exception ignored) {
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    public static void add(Context context, PaymentReminder reminder) {
        List<PaymentReminder> reminders = loadAll(context);
        reminder.status = STATUS_ACTIVE;
        reminder.deletedAt = 0L;
        reminders.add(reminder);
        saveRaw(context, reminders);
        schedule(context, reminder);
    }

    public static void setMuted(Context context, long id, boolean muted) {
        List<PaymentReminder> reminders = loadAll(context);
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                reminder.soundEnabled = !muted;
                break;
            }
        }
        saveRaw(context, reminders);
    }

    public static void archive(Context context, long id) {
        changeStatus(context, id, STATUS_ARCHIVE);
    }

    public static void restoreFromArchive(Context context, long id) {
        changeStatus(context, id, STATUS_ACTIVE);
    }

    /** Soft delete: moves the plan to Trash for 30 days. */
    public static void delete(Context context, long id) {
        moveToTrash(context, id);
    }

    public static void moveToTrash(Context context, long id) {
        List<PaymentReminder> reminders = loadAll(context);
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                cancel(context, reminder);
                reminder.status = STATUS_TRASH;
                reminder.deletedAt = System.currentTimeMillis();
                break;
            }
        }
        saveRaw(context, reminders);
    }

    public static void restoreFromTrash(Context context, long id) {
        List<PaymentReminder> reminders = loadAll(context);
        PaymentReminder restored = null;
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                reminder.status = STATUS_ACTIVE;
                reminder.deletedAt = 0L;
                restored = reminder;
                break;
            }
        }
        saveRaw(context, reminders);
        if (restored != null) schedule(context, restored);
    }

    public static void deleteForever(Context context, long id) {
        List<PaymentReminder> reminders = loadAll(context);
        List<PaymentReminder> updated = new ArrayList<>();
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) cancel(context, reminder);
            else updated.add(reminder);
        }
        saveRaw(context, updated);
    }

    private static void changeStatus(Context context, long id, String newStatus) {
        List<PaymentReminder> reminders = loadAll(context);
        PaymentReminder target = null;
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                target = reminder;
                if (STATUS_ACTIVE.equals(newStatus)) {
                    reminder.status = STATUS_ACTIVE;
                    reminder.deletedAt = 0L;
                } else {
                    cancel(context, reminder);
                    reminder.status = normalizeStatus(newStatus);
                    reminder.deletedAt = 0L;
                }
                break;
            }
        }
        saveRaw(context, reminders);
        if (target != null && STATUS_ACTIVE.equals(target.status)) schedule(context, target);
    }

    public static int trashDaysRemaining(PaymentReminder reminder) {
        if (reminder == null || reminder.deletedAt <= 0) return 30;
        long remaining = TRASH_RETENTION_MILLIS - (System.currentTimeMillis() - reminder.deletedAt);
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / (24d * 60d * 60d * 1000d));
    }

    public static void rescheduleAll(Context context) {
        for (PaymentReminder reminder : load(context)) schedule(context, reminder);
    }

    public static void schedule(Context context, PaymentReminder reminder) {
        if (reminder == null || !STATUS_ACTIVE.equals(reminder.status)) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long now = System.currentTimeMillis();
        for (int index = 0; index < reminder.months; index++) {
            Calendar dueDate = buildDueDate(reminder.firstPaymentMillis, index);
            Calendar notificationDate = (Calendar) dueDate.clone();
            notificationDate.add(Calendar.DAY_OF_MONTH, -reminder.daysBefore);
            long triggerAt = notificationDate.getTimeInMillis();
            if (triggerAt <= now) continue;

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
        if (alarmManager == null || reminder == null) return;
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
        intent.putExtra("item_sound_enabled", reminder.soundEnabled);

        int flags = PendingIntent.FLAG_IMMUTABLE;
        flags |= noCreate ? PendingIntent.FLAG_NO_CREATE : PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getBroadcast(context, requestCode(reminder.id, index), intent, flags);
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
            if (endOfDay.getTimeInMillis() >= now) return i;
        }
        return -1;
    }

    public static String normalizeType(String type) {
        if (type == null) return TYPE_CREDIT;
        String value = type.trim().toLowerCase();
        if (value.equals(TYPE_MORTGAGE) || value.contains("ипот")) return TYPE_MORTGAGE;
        if (value.equals(TYPE_AUTO) || value.contains("авто")) return TYPE_AUTO;
        if (value.equals(TYPE_INSTALLMENT) || value.contains("расср")) return TYPE_INSTALLMENT;
        if (value.equals(TYPE_DEPOSIT) || value.contains("вклад")) return TYPE_DEPOSIT;
        if (value.equals(TYPE_CREDIT) || value.contains("кредит")) return TYPE_CREDIT;
        return TYPE_CREDIT;
    }

    private static String normalizeStatus(String status) {
        if (STATUS_ARCHIVE.equals(status)) return STATUS_ARCHIVE;
        if (STATUS_TRASH.equals(status)) return STATUS_TRASH;
        return STATUS_ACTIVE;
    }

    private static int requestCode(long id, int index) {
        int base = (int) (id ^ (id >>> 32));
        return 31 * base + index;
    }
}
