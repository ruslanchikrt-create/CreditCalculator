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
import java.util.Collections;
import java.util.List;

public final class ReminderScheduler {

    public static final String TYPE_CREDIT = "credit";
    public static final String TYPE_MORTGAGE = "mortgage";
    public static final String TYPE_AUTO = "auto";
    public static final String TYPE_INSTALLMENT = "installment";
    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_OTHER = "other";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVE = "archive";
    public static final String STATUS_TRASH = "trash";

    public static final String HISTORY_CREATED = "created";
    public static final String HISTORY_EDITED = "edited";
    public static final String HISTORY_EARLY = "early_repayment";
    public static final String HISTORY_REFINANCE = "refinance";
    public static final String HISTORY_STATUS = "status";
    public static final String HISTORY_REMINDER = "reminder";

    public static final long TRASH_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    public static final String PREFS_NAME = "payment_reminders";
    public static final String KEY_ITEMS = "items";

    private ReminderScheduler() {}

    public static class PaymentReminder {
        public long id;
        public String type;
        public String title;
        public double baseAmount;
        public double downPayment;
        public double insurance;
        /** Principal of the current schedule segment. */
        public double principal;
        public double annualRate;
        public double amount;
        public long firstPaymentMillis;
        public int months;
        public int daysBefore;
        public String status;
        public long deletedAt;
        public boolean soundEnabled;
        public long createdAt;
        public long updatedAt;
        /** Interest already paid in schedule segments that were closed by early repayment/refinancing. */
        public double interestPaidBefore;
        public String historyJson;

        public PaymentReminder(long id, String type, String title, double principal,
                               double annualRate, double amount, long firstPaymentMillis,
                               int months, int daysBefore) {
            this(id, type, title, principal, 0.0, 0.0, principal,
                    annualRate, amount, firstPaymentMillis, months, daysBefore,
                    STATUS_ACTIVE, 0L, true, id, id, 0.0, "");
        }

        public PaymentReminder(long id, String type, String title,
                               double baseAmount, double downPayment, double insurance,
                               double principal, double annualRate, double amount,
                               long firstPaymentMillis, int months, int daysBefore) {
            this(id, type, title, baseAmount, downPayment, insurance, principal,
                    annualRate, amount, firstPaymentMillis, months, daysBefore,
                    STATUS_ACTIVE, 0L, true, id, id, 0.0, "");
        }

        public PaymentReminder(long id, String type, String title,
                               double baseAmount, double downPayment, double insurance,
                               double principal, double annualRate, double amount,
                               long firstPaymentMillis, int months, int daysBefore,
                               String status, long deletedAt, boolean soundEnabled,
                               long createdAt, long updatedAt, double interestPaidBefore,
                               String historyJson) {
            this.id = id;
            this.type = normalizeType(type);
            this.title = title == null ? "" : title;
            this.baseAmount = Math.max(0.0, baseAmount);
            this.downPayment = Math.max(0.0, downPayment);
            this.insurance = Math.max(0.0, insurance);
            this.principal = Math.max(0.0, principal);
            this.annualRate = Math.max(0.0, annualRate);
            this.amount = Math.max(0.0, amount);
            this.firstPaymentMillis = firstPaymentMillis;
            this.months = Math.max(1, months);
            this.daysBefore = Math.max(1, Math.min(7, daysBefore));
            this.status = normalizeStatus(status);
            this.deletedAt = deletedAt;
            this.soundEnabled = soundEnabled;
            long fallback = id > 0 ? id : System.currentTimeMillis();
            this.createdAt = createdAt > 0 ? createdAt : fallback;
            this.updatedAt = updatedAt > 0 ? updatedAt : this.createdAt;
            this.interestPaidBefore = Math.max(0.0, interestPaidBefore);
            this.historyJson = historyJson == null ? "" : historyJson;
        }
    }

    public static class HistoryEvent {
        public long time;
        public String type;
        public String titleRu;
        public String titleEn;
        public String detailsRu;
        public String detailsEn;
    }

    public static class EarlyRepaymentSimulation {
        public double balance;
        public double prepayment;
        public double newBalance;
        public int remainingMonths;
        public long firstFuturePayment;
        public double oldRemainingInterest;
        public double reducedPayment;
        public double interestWithReducedPayment;
        public double savingsWithReducedPayment;
        public int reducedMonths;
        public double keptPayment;
        public double interestWithReducedTerm;
        public double savingsWithReducedTerm;
    }

    public static class RefinanceSimulation {
        public double balance;
        public int oldRemainingMonths;
        public double oldRemainingOverpayment;
        public double newPrincipal;
        public double newRate;
        public int newMonths;
        public double commission;
        public double insurance;
        public double newPayment;
        public double newOverpayment;
        public double savings;
        public long firstNewPayment;
    }

    public static List<PaymentReminder> load(Context context) { return listByStatus(context, STATUS_ACTIVE); }

    public static List<PaymentReminder> listByStatus(Context context, String status) {
        List<PaymentReminder> result = new ArrayList<>();
        String normalized = normalizeStatus(status);
        for (PaymentReminder reminder : loadAll(context)) if (normalized.equals(reminder.status)) result.add(reminder);
        return result;
    }

    public static List<PaymentReminder> loadAll(Context context) {
        List<PaymentReminder> result = new ArrayList<>();
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]");
        boolean changed = false;
        long now = System.currentTimeMillis();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                double amount = object.optDouble("amount", 0.0);
                int months = Math.max(1, object.optInt("months", 1));
                double principal = object.has("principal") ? object.optDouble("principal", 0.0) : amount * months;
                double baseAmount = object.has("baseAmount") ? object.optDouble("baseAmount", principal) : principal;
                String status = normalizeStatus(object.optString("status", STATUS_ACTIVE));
                long deletedAt = object.optLong("deletedAt", 0L);
                if (STATUS_TRASH.equals(status) && deletedAt > 0 && now - deletedAt >= TRASH_RETENTION_MILLIS) {
                    changed = true;
                    continue;
                }
                long id = object.optLong("id", now + i);
                long createdAt = object.optLong("createdAt", id > 0 ? id : now);
                long updatedAt = object.optLong("updatedAt", createdAt);
                String history = object.optString("history", "");
                PaymentReminder reminder = new PaymentReminder(
                        id,
                        object.optString("type", TYPE_CREDIT),
                        object.optString("title", "Кредит"),
                        baseAmount,
                        object.optDouble("downPayment", 0.0),
                        object.optDouble("insurance", 0.0),
                        principal,
                        object.optDouble("annualRate", 0.0),
                        amount,
                        object.optLong("firstPaymentMillis", now),
                        months,
                        object.optInt("daysBefore", 3),
                        status,
                        deletedAt,
                        object.optBoolean("soundEnabled", true),
                        createdAt,
                        updatedAt,
                        object.optDouble("interestPaidBefore", 0.0),
                        history
                );
                if (reminder.historyJson.trim().isEmpty()) {
                    appendHistory(reminder, reminder.createdAt, HISTORY_CREATED,
                            "Создана запись", "Item created",
                            "Создана запись «" + reminder.title + "».", "Created “" + reminder.title + "”.");
                    changed = true;
                }
                result.add(reminder);
            }
        } catch (Exception ignored) {}
        if (changed) saveRaw(context, result);
        return result;
    }

    public static PaymentReminder findById(Context context, long id) {
        for (PaymentReminder reminder : loadAll(context)) if (reminder.id == id) return reminder;
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
                object.put("baseAmount", reminder.baseAmount);
                object.put("downPayment", reminder.downPayment);
                object.put("insurance", reminder.insurance);
                object.put("principal", reminder.principal);
                object.put("annualRate", reminder.annualRate);
                object.put("amount", reminder.amount);
                object.put("firstPaymentMillis", reminder.firstPaymentMillis);
                object.put("months", reminder.months);
                object.put("daysBefore", reminder.daysBefore);
                object.put("status", normalizeStatus(reminder.status));
                object.put("deletedAt", reminder.deletedAt);
                object.put("soundEnabled", reminder.soundEnabled);
                object.put("createdAt", reminder.createdAt);
                object.put("updatedAt", reminder.updatedAt);
                object.put("interestPaidBefore", reminder.interestPaidBefore);
                object.put("history", reminder.historyJson == null ? "" : reminder.historyJson);
                array.put(object);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    public static String exportPaymentsJson(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]");
    }

    public static void importPaymentsJson(Context context, String json) throws Exception {
        new JSONArray(json == null ? "[]" : json);
        for (PaymentReminder old : loadAll(context)) cancel(context, old);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, json).commit();
        rescheduleAll(context);
    }

    public static void add(Context context, PaymentReminder reminder) {
        List<PaymentReminder> reminders = loadAll(context);
        long now = System.currentTimeMillis();
        reminder.status = STATUS_ACTIVE;
        reminder.deletedAt = 0L;
        reminder.createdAt = reminder.createdAt > 0 ? reminder.createdAt : now;
        reminder.updatedAt = now;
        if (reminder.historyJson == null || reminder.historyJson.trim().isEmpty()) {
            appendHistory(reminder, reminder.createdAt, HISTORY_CREATED,
                    "Создана запись", "Item created",
                    "Создана запись «" + reminder.title + "».", "Created “" + reminder.title + "”.");
        }
        reminders.add(reminder);
        saveRaw(context, reminders);
        schedule(context, reminder);
    }

    public static void updateEdited(Context context, PaymentReminder edited) {
        List<PaymentReminder> reminders = loadAll(context);
        for (int i = 0; i < reminders.size(); i++) {
            PaymentReminder old = reminders.get(i);
            if (old.id != edited.id) continue;
            cancel(context, old);
            String detailsRu = editDetails(old, edited, false);
            String detailsEn = editDetails(old, edited, true);
            edited.status = old.status;
            edited.deletedAt = old.deletedAt;
            edited.soundEnabled = old.soundEnabled;
            edited.createdAt = old.createdAt;
            edited.updatedAt = System.currentTimeMillis();
            edited.historyJson = old.historyJson;
            edited.interestPaidBefore = old.interestPaidBefore;
            appendHistory(edited, edited.updatedAt, HISTORY_EDITED,
                    "Редактирование", "Edited", detailsRu, detailsEn);
            reminders.set(i, edited);
            saveRaw(context, reminders);
            if (STATUS_ACTIVE.equals(edited.status)) schedule(context, edited);
            return;
        }
    }

    private static String editDetails(PaymentReminder old, PaymentReminder edited, boolean en) {
        StringBuilder sb = new StringBuilder();
        addDiff(sb, en ? "Name" : "Название", old.title, edited.title);
        addDiff(sb, en ? "Amount" : "Сумма", old.baseAmount, edited.baseAmount);
        addDiff(sb, en ? "Down payment" : "Первоначальный взнос", old.downPayment, edited.downPayment);
        addDiff(sb, en ? "Insurance" : "Страховка", old.insurance, edited.insurance);
        addDiff(sb, en ? "Rate" : "Ставка", old.annualRate, edited.annualRate);
        addDiff(sb, en ? "Monthly payment" : "Ежемесячный платёж", old.amount, edited.amount);
        if (old.months != edited.months) addLine(sb, (en ? "Term: " : "Срок: ") + old.months + " → " + edited.months + (en ? " months" : " мес."));
        if (old.firstPaymentMillis != edited.firstPaymentMillis) addLine(sb, en ? "First payment date changed." : "Изменена дата первого платежа.");
        if (old.daysBefore != edited.daysBefore) addLine(sb, (en ? "Reminder: " : "Напоминание: ") + old.daysBefore + " → " + edited.daysBefore + (en ? " days" : " дн."));
        if (sb.length() == 0) return en ? "Saved without visible field changes." : "Сохранено без видимых изменений полей.";
        return sb.toString();
    }

    private static void addDiff(StringBuilder sb, String label, String oldValue, String newValue) {
        if (oldValue == null) oldValue = "";
        if (newValue == null) newValue = "";
        if (!oldValue.equals(newValue)) addLine(sb, label + ": " + oldValue + " → " + newValue);
    }

    private static void addDiff(StringBuilder sb, String label, double oldValue, double newValue) {
        if (Math.abs(oldValue - newValue) > 0.005) addLine(sb, label + ": " + round2(oldValue) + " → " + round2(newValue));
    }

    private static void addLine(StringBuilder sb, String value) {
        if (sb.length() > 0) sb.append('\n');
        sb.append(value);
    }

    public static void setMuted(Context context, long id, boolean muted) {
        List<PaymentReminder> reminders = loadAll(context);
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                reminder.soundEnabled = !muted;
                reminder.updatedAt = System.currentTimeMillis();
                appendHistory(reminder, reminder.updatedAt, HISTORY_REMINDER,
                        muted ? "Звук выключен" : "Звук включён",
                        muted ? "Sound muted" : "Sound enabled",
                        muted ? "Уведомления для записи переведены в беззвучный режим." : "Звук уведомлений для записи включён.",
                        muted ? "Notifications for this item were muted." : "Notification sound for this item was enabled.");
                break;
            }
        }
        saveRaw(context, reminders);
    }

    public static void archive(Context context, long id) { changeStatus(context, id, STATUS_ARCHIVE); }
    public static void restoreFromArchive(Context context, long id) { changeStatus(context, id, STATUS_ACTIVE); }
    public static void delete(Context context, long id) { moveToTrash(context, id); }

    public static void moveToTrash(Context context, long id) {
        List<PaymentReminder> reminders = loadAll(context);
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                cancel(context, reminder);
                reminder.status = STATUS_TRASH;
                reminder.deletedAt = System.currentTimeMillis();
                reminder.updatedAt = reminder.deletedAt;
                appendHistory(reminder, reminder.updatedAt, HISTORY_STATUS,
                        "Перемещено в корзину", "Moved to trash",
                        "Запись перемещена в корзину.", "The item was moved to trash.");
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
                reminder.updatedAt = System.currentTimeMillis();
                appendHistory(reminder, reminder.updatedAt, HISTORY_STATUS,
                        "Восстановлено из корзины", "Restored from trash",
                        "Запись восстановлена в активные платежи.", "The item was restored to active payments.");
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
            if (reminder.id == id) cancel(context, reminder); else updated.add(reminder);
        }
        saveRaw(context, updated);
    }

    private static void changeStatus(Context context, long id, String newStatus) {
        List<PaymentReminder> reminders = loadAll(context);
        PaymentReminder target = null;
        for (PaymentReminder reminder : reminders) {
            if (reminder.id == id) {
                target = reminder;
                cancel(context, reminder);
                reminder.status = normalizeStatus(newStatus);
                reminder.deletedAt = 0L;
                reminder.updatedAt = System.currentTimeMillis();
                boolean active = STATUS_ACTIVE.equals(reminder.status);
                appendHistory(reminder, reminder.updatedAt, HISTORY_STATUS,
                        active ? "Возвращено в активные" : "Перемещено в архив",
                        active ? "Restored to active" : "Archived",
                        active ? "Запись возвращена в «Мои платежи»." : "Запись перемещена в архив.",
                        active ? "The item was restored to My payments." : "The item was moved to archive.");
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

    public static List<HistoryEvent> history(PaymentReminder reminder) {
        List<HistoryEvent> result = new ArrayList<>();
        if (reminder == null) return result;
        try {
            JSONArray array = new JSONArray(reminder.historyJson == null || reminder.historyJson.trim().isEmpty() ? "[]" : reminder.historyJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                HistoryEvent e = new HistoryEvent();
                e.time = o.optLong("time", reminder.createdAt);
                e.type = o.optString("type", "event");
                e.titleRu = o.optString("titleRu", "Изменение");
                e.titleEn = o.optString("titleEn", "Change");
                e.detailsRu = o.optString("detailsRu", "");
                e.detailsEn = o.optString("detailsEn", "");
                result.add(e);
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static int historyCount(PaymentReminder reminder, String type) {
        int count = 0;
        for (HistoryEvent e : history(reminder)) if (type.equals(e.type)) count++;
        return count;
    }

    private static void appendHistory(PaymentReminder reminder, long time, String type,
                                      String titleRu, String titleEn, String detailsRu, String detailsEn) {
        try {
            JSONArray array = new JSONArray(reminder.historyJson == null || reminder.historyJson.trim().isEmpty() ? "[]" : reminder.historyJson);
            JSONObject o = new JSONObject();
            o.put("time", time);
            o.put("type", type);
            o.put("titleRu", titleRu);
            o.put("titleEn", titleEn);
            o.put("detailsRu", detailsRu);
            o.put("detailsEn", detailsEn);
            array.put(o);
            reminder.historyJson = array.toString();
        } catch (Exception ignored) {}
    }

    public static void rescheduleAll(Context context) { for (PaymentReminder reminder : load(context)) schedule(context, reminder); }

    public static void schedule(Context context, PaymentReminder reminder) {
        if (reminder == null || !STATUS_ACTIVE.equals(reminder.status) || reminder.amount <= 0.0) return;
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            else alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static void cancel(Context context, PaymentReminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || reminder == null) return;
        for (int index = 0; index < reminder.months; index++) {
            Calendar dueDate = buildDueDate(reminder.firstPaymentMillis, index);
            PendingIntent pendingIntent = buildPendingIntent(context, reminder, index, dueDate, true);
            if (pendingIntent != null) { alarmManager.cancel(pendingIntent); pendingIntent.cancel(); }
        }
    }

    private static PendingIntent buildPendingIntent(Context context, PaymentReminder reminder, int index, Calendar dueDate, boolean noCreate) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.creditcalculator.PAYMENT_" + reminder.id + "_" + index);
        intent.putExtra("reminder_id", reminder.id);
        intent.putExtra("title", reminder.title);
        intent.putExtra("type", normalizeType(reminder.type));
        intent.putExtra("amount", reminder.amount);
        intent.putExtra("due_date", dueDate.getTimeInMillis());
        intent.putExtra("days_before", reminder.daysBefore);
        intent.putExtra("item_sound_enabled", reminder.soundEnabled);
        int flags = PendingIntent.FLAG_IMMUTABLE | (noCreate ? PendingIntent.FLAG_NO_CREATE : PendingIntent.FLAG_UPDATE_CURRENT);
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
        if (reminder == null || reminder.amount <= 0.0) return -1;
        long now = System.currentTimeMillis();
        for (int i = 0; i < reminder.months; i++) {
            Calendar due = buildDueDate(reminder.firstPaymentMillis, i);
            Calendar endOfDay = (Calendar) due.clone();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23); endOfDay.set(Calendar.MINUTE, 59); endOfDay.set(Calendar.SECOND, 59);
            if (endOfDay.getTimeInMillis() >= now) return i;
        }
        return -1;
    }

    public static long nextPaymentMillis(PaymentReminder reminder) {
        int index = nextPaymentIndex(reminder);
        return index < 0 ? Long.MAX_VALUE : buildDueDate(reminder.firstPaymentMillis, index).getTimeInMillis();
    }

    public static int elapsedPayments(PaymentReminder reminder) {
        if (reminder == null) return 0;
        long now = System.currentTimeMillis();
        int elapsed = 0;
        for (int i = 0; i < reminder.months; i++) {
            Calendar due = buildDueDate(reminder.firstPaymentMillis, i);
            due.set(Calendar.HOUR_OF_DAY, 23); due.set(Calendar.MINUTE, 59); due.set(Calendar.SECOND, 59);
            if (due.getTimeInMillis() < now) elapsed++; else break;
        }
        return elapsed;
    }

    public static double remainingDebt(PaymentReminder reminder) {
        if (reminder == null || TYPE_DEPOSIT.equals(normalizeType(reminder.type))) return 0.0;
        return balanceAfterPayments(reminder.principal, reminder.annualRate, reminder.amount,
                Math.min(reminder.months, elapsedPayments(reminder)));
    }

    public static double balanceAtDate(PaymentReminder reminder, long dateMillis) {
        if (reminder == null || TYPE_DEPOSIT.equals(normalizeType(reminder.type))) return 0.0;
        int count = paymentsBeforeDate(reminder, dateMillis);
        return balanceAfterPayments(reminder.principal, reminder.annualRate, reminder.amount, count);
    }

    private static int paymentsBeforeDate(PaymentReminder reminder, long dateMillis) {
        int count = 0;
        for (int i = 0; i < reminder.months; i++) {
            long due = buildDueDate(reminder.firstPaymentMillis, i).getTimeInMillis();
            if (due < dateMillis) count++; else break;
        }
        return count;
    }

    public static int remainingPaymentsAfterDate(PaymentReminder reminder, long dateMillis) {
        return Math.max(0, reminder.months - paymentsBeforeDate(reminder, dateMillis));
    }

    private static long firstDueOnOrAfter(PaymentReminder reminder, long dateMillis) {
        for (int i = 0; i < reminder.months; i++) {
            long due = buildDueDate(reminder.firstPaymentMillis, i).getTimeInMillis();
            if (due >= dateMillis) return due;
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateMillis); c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis();
    }

    private static double balanceAfterPayments(double principal, double annualRate, double payment, int paid) {
        if (principal <= 0.0) return 0.0;
        if (paid <= 0) return principal;
        double rate = annualRate / 100.0 / 12.0;
        double balance = principal;
        for (int i = 0; i < paid && balance > 0.005; i++) {
            double interest = balance * rate;
            double principalPart = Math.max(0.0, payment - interest);
            if (rate <= 0.0) principalPart = payment;
            balance = Math.max(0.0, balance - principalPart);
        }
        return balance;
    }

    public static double paidInterest(PaymentReminder reminder) {
        if (reminder == null || TYPE_DEPOSIT.equals(normalizeType(reminder.type))) return 0.0;
        return Math.max(0.0, reminder.interestPaidBefore + interestForFirstPayments(reminder, elapsedPayments(reminder)));
    }

    public static double remainingInterest(PaymentReminder reminder) {
        if (reminder == null || TYPE_DEPOSIT.equals(normalizeType(reminder.type))) return 0.0;
        int remaining = Math.max(0, reminder.months - elapsedPayments(reminder));
        return futureInterest(remainingDebt(reminder), reminder.annualRate, reminder.amount, remaining);
    }

    public static double totalInterest(PaymentReminder reminder) { return paidInterest(reminder) + remainingInterest(reminder); }

    private static double interestForFirstPayments(PaymentReminder reminder, int count) {
        double balance = reminder.principal;
        double rate = reminder.annualRate / 100.0 / 12.0;
        double total = 0.0;
        for (int i = 0; i < count && balance > 0.005; i++) {
            double interest = balance * rate;
            total += interest;
            double principalPart = rate <= 0.0 ? reminder.amount : Math.max(0.0, reminder.amount - interest);
            balance = Math.max(0.0, balance - principalPart);
        }
        return total;
    }

    private static double interestBeforeDate(PaymentReminder reminder, long dateMillis) {
        return interestForFirstPayments(reminder, paymentsBeforeDate(reminder, dateMillis));
    }

    public static double dueThisMonth(PaymentReminder reminder) {
        if (reminder == null || TYPE_DEPOSIT.equals(normalizeType(reminder.type))) return 0.0;
        Calendar now = Calendar.getInstance();
        for (int i = 0; i < reminder.months; i++) {
            Calendar due = buildDueDate(reminder.firstPaymentMillis, i);
            if (due.get(Calendar.YEAR) == now.get(Calendar.YEAR) && due.get(Calendar.MONTH) == now.get(Calendar.MONTH)) return Math.max(0.0, reminder.amount);
        }
        return 0.0;
    }

    public static double depositExpectedIncome(PaymentReminder reminder) {
        if (reminder == null || !TYPE_DEPOSIT.equals(normalizeType(reminder.type))) return 0.0;
        return Math.max(0.0, reminder.principal * reminder.annualRate / 100.0 * (reminder.months / 12.0));
    }

    public static double depositFinalAmount(PaymentReminder reminder) {
        return reminder == null ? 0.0 : Math.max(0.0, reminder.principal + depositExpectedIncome(reminder));
    }

    public static EarlyRepaymentSimulation simulateEarlyRepayment(PaymentReminder reminder, long effectiveDate, double prepayment) {
        if (reminder == null || prepayment <= 0.0) throw new IllegalArgumentException();
        double balance = balanceAtDate(reminder, effectiveDate);
        if (balance <= 0.0 || prepayment > balance + 0.01) throw new IllegalArgumentException();
        int remaining = remainingPaymentsAfterDate(reminder, effectiveDate);
        if (remaining <= 0) throw new IllegalArgumentException();
        EarlyRepaymentSimulation s = new EarlyRepaymentSimulation();
        s.balance = balance;
        s.prepayment = Math.min(prepayment, balance);
        s.newBalance = Math.max(0.0, balance - s.prepayment);
        s.remainingMonths = remaining;
        s.firstFuturePayment = firstDueOnOrAfter(reminder, effectiveDate);
        s.oldRemainingInterest = futureInterest(balance, reminder.annualRate, reminder.amount, remaining);
        if (s.newBalance <= 0.01) {
            s.reducedPayment = 0.0; s.interestWithReducedPayment = 0.0; s.savingsWithReducedPayment = s.oldRemainingInterest;
            s.reducedMonths = 0; s.keptPayment = 0.0; s.interestWithReducedTerm = 0.0; s.savingsWithReducedTerm = s.oldRemainingInterest;
            return s;
        }
        s.reducedPayment = annuity(s.newBalance, remaining, reminder.annualRate);
        s.interestWithReducedPayment = futureInterest(s.newBalance, reminder.annualRate, s.reducedPayment, remaining);
        s.savingsWithReducedPayment = Math.max(0.0, s.oldRemainingInterest - s.interestWithReducedPayment);
        s.keptPayment = reminder.amount;
        s.reducedMonths = monthsForPayment(s.newBalance, reminder.annualRate, reminder.amount, remaining);
        s.interestWithReducedTerm = futureInterest(s.newBalance, reminder.annualRate, reminder.amount, s.reducedMonths);
        s.savingsWithReducedTerm = Math.max(0.0, s.oldRemainingInterest - s.interestWithReducedTerm);
        return s;
    }

    public static void applyEarlyRepayment(Context context, long id, long effectiveDate, double prepayment, boolean reduceTerm) {
        List<PaymentReminder> reminders = loadAll(context);
        for (PaymentReminder r : reminders) {
            if (r.id != id) continue;
            EarlyRepaymentSimulation s = simulateEarlyRepayment(r, effectiveDate, prepayment);
            cancel(context, r);
            double paidSegmentInterest = interestBeforeDate(r, effectiveDate);
            r.interestPaidBefore += paidSegmentInterest;
            r.updatedAt = System.currentTimeMillis();
            String beforeRu = "Остаток до погашения: " + round2(s.balance) + " ₽\nДосрочно внесено: " + round2(s.prepayment) + " ₽";
            String beforeEn = "Balance before repayment: " + round2(s.balance) + " ₽\nEarly repayment: " + round2(s.prepayment) + " ₽";
            if (s.newBalance <= 0.01) {
                r.principal = 0.0; r.amount = 0.0; r.firstPaymentMillis = effectiveDate; r.months = 1; r.status = STATUS_ARCHIVE;
                appendHistory(r, r.updatedAt, HISTORY_EARLY,
                        "Полное досрочное погашение", "Full early repayment",
                        beforeRu + "\nКредит полностью погашен.", beforeEn + "\nThe loan was paid off in full.");
            } else {
                r.principal = s.newBalance;
                r.firstPaymentMillis = s.firstFuturePayment;
                if (reduceTerm) {
                    r.months = Math.max(1, s.reducedMonths);
                    r.amount = s.keptPayment;
                    appendHistory(r, r.updatedAt, HISTORY_EARLY,
                            "Досрочное погашение", "Early repayment",
                            beforeRu + "\nВыбрано: уменьшить срок.\nНовый срок: " + r.months + " мес.\nЭкономия процентов: " + round2(s.savingsWithReducedTerm) + " ₽",
                            beforeEn + "\nSelected: reduce term.\nNew term: " + r.months + " months.\nInterest saved: " + round2(s.savingsWithReducedTerm) + " ₽");
                } else {
                    r.months = Math.max(1, s.remainingMonths);
                    r.amount = s.reducedPayment;
                    appendHistory(r, r.updatedAt, HISTORY_EARLY,
                            "Досрочное погашение", "Early repayment",
                            beforeRu + "\nВыбрано: уменьшить платёж.\nНовый платёж: " + round2(r.amount) + " ₽\nЭкономия процентов: " + round2(s.savingsWithReducedPayment) + " ₽",
                            beforeEn + "\nSelected: reduce payment.\nNew payment: " + round2(r.amount) + " ₽\nInterest saved: " + round2(s.savingsWithReducedPayment) + " ₽");
                }
            }
            saveRaw(context, reminders);
            if (STATUS_ACTIVE.equals(r.status)) schedule(context, r);
            return;
        }
    }

    public static RefinanceSimulation simulateRefinance(PaymentReminder reminder, long effectiveDate,
                                                         double newRate, int newMonths, double commission, double insurance) {
        if (reminder == null || newRate < 0 || newMonths <= 0 || commission < 0 || insurance < 0) throw new IllegalArgumentException();
        RefinanceSimulation s = new RefinanceSimulation();
        s.balance = balanceAtDate(reminder, effectiveDate);
        if (s.balance <= 0.0) throw new IllegalArgumentException();
        s.oldRemainingMonths = remainingPaymentsAfterDate(reminder, effectiveDate);
        s.oldRemainingOverpayment = futureInterest(s.balance, reminder.annualRate, reminder.amount, s.oldRemainingMonths);
        s.commission = commission;
        s.insurance = insurance;
        s.newPrincipal = s.balance + commission + insurance;
        s.newRate = newRate;
        s.newMonths = newMonths;
        s.newPayment = annuity(s.newPrincipal, newMonths, newRate);
        double totalNewPayments = s.newPayment * newMonths;
        s.newOverpayment = Math.max(0.0, totalNewPayments - s.balance);
        s.savings = s.oldRemainingOverpayment - s.newOverpayment;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(effectiveDate); c.add(Calendar.MONTH, 1);
        s.firstNewPayment = c.getTimeInMillis();
        return s;
    }

    public static void applyRefinance(Context context, long id, long effectiveDate,
                                      double newRate, int newMonths, double commission, double insurance) {
        List<PaymentReminder> reminders = loadAll(context);
        for (PaymentReminder r : reminders) {
            if (r.id != id) continue;
            RefinanceSimulation s = simulateRefinance(r, effectiveDate, newRate, newMonths, commission, insurance);
            cancel(context, r);
            double oldRate = r.annualRate;
            double oldPayment = r.amount;
            int oldMonths = s.oldRemainingMonths;
            r.interestPaidBefore += interestBeforeDate(r, effectiveDate);
            r.principal = s.newPrincipal;
            r.annualRate = newRate;
            r.months = newMonths;
            r.amount = s.newPayment;
            r.firstPaymentMillis = s.firstNewPayment;
            r.updatedAt = System.currentTimeMillis();
            appendHistory(r, r.updatedAt, HISTORY_REFINANCE,
                    "Рефинансирование", "Refinancing",
                    "Остаток: " + round2(s.balance) + " ₽\nСтавка: " + round2(oldRate) + "% → " + round2(newRate) + "%\nПлатёж: " + round2(oldPayment) + " ₽ → " + round2(s.newPayment) + " ₽\nСрок: " + oldMonths + " → " + newMonths + " мес.\nКомиссия: " + round2(commission) + " ₽\nСтраховка: " + round2(insurance) + " ₽\nРасчётная экономия: " + round2(s.savings) + " ₽",
                    "Balance: " + round2(s.balance) + " ₽\nRate: " + round2(oldRate) + "% → " + round2(newRate) + "%\nPayment: " + round2(oldPayment) + " ₽ → " + round2(s.newPayment) + " ₽\nTerm: " + oldMonths + " → " + newMonths + " months.\nCommission: " + round2(commission) + " ₽\nInsurance: " + round2(insurance) + " ₽\nEstimated savings: " + round2(s.savings) + " ₽");
            saveRaw(context, reminders);
            schedule(context, r);
            return;
        }
    }

    public static double annuity(double principal, int months, double annualRate) {
        if (months <= 0 || principal <= 0.0) return 0.0;
        double monthlyRate = annualRate / 100.0 / 12.0;
        if (monthlyRate <= 0.0) return principal / months;
        double factor = Math.pow(1.0 + monthlyRate, months);
        return principal * monthlyRate * factor / (factor - 1.0);
    }

    private static int monthsForPayment(double balance, double annualRate, double payment, int fallbackMax) {
        if (balance <= 0) return 0;
        if (payment <= 0) return Math.max(1, fallbackMax);
        double r = annualRate / 100.0 / 12.0;
        double b = balance;
        for (int m = 1; m <= 1200; m++) {
            double interest = b * r;
            double principalPart = r <= 0 ? payment : payment - interest;
            if (principalPart <= 0.0) return Math.max(1, fallbackMax);
            b -= principalPart;
            if (b <= 0.01) return m;
        }
        return Math.max(1, fallbackMax);
    }

    private static double futureInterest(double balance, double annualRate, double payment, int maxMonths) {
        if (balance <= 0 || payment <= 0 || maxMonths <= 0) return 0.0;
        double r = annualRate / 100.0 / 12.0;
        double b = balance;
        double total = 0.0;
        for (int i = 0; i < maxMonths && b > 0.01; i++) {
            double interest = b * r;
            total += Math.max(0.0, interest);
            double principalPart = r <= 0 ? payment : payment - interest;
            if (principalPart <= 0) break;
            b = Math.max(0.0, b - principalPart);
        }
        return Math.max(0.0, total);
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

    private static String round2(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
