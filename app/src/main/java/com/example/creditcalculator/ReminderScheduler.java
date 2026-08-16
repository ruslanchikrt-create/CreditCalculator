package com.example.creditcalculator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ReminderScheduler {
    public static final String TYPE_CREDIT = "credit";
    public static final String TYPE_MORTGAGE = "mortgage";
    public static final String TYPE_AUTO = "auto";
    public static final String TYPE_INSTALLMENT = "installment";
    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_OTHER = "other";

    public static final String PAYMENT_ANNUITY = "annuity";
    public static final String PAYMENT_DIFFERENTIAL = "differential";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVE = "archive";
    public static final String STATUS_TRASH = "trash";

    public static final String HISTORY_CREATED = "created";
    public static final String HISTORY_EDITED = "edited";
    public static final String HISTORY_EARLY = "early_repayment";
    public static final String HISTORY_REFINANCE = "refinance";
    public static final String HISTORY_STATUS = "status";
    public static final String HISTORY_REMINDER = "reminder";
    public static final String HISTORY_PAYMENT = "payment";
    public static final String HISTORY_SCHEDULE = "schedule";

    public static final long TRASH_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L;
    public static final String PREFS_NAME = "payment_reminders";
    public static final String KEY_ITEMS = "items";
    private static final Object STORE_LOCK = new Object();
    /** Keep alarm count comfortably below OEM per-app limits even for long mortgages and many records. */
    private static final int SCHEDULE_WINDOW_INSTALLMENTS = 4;
    private static final ExecutorService ALARM_EXECUTOR = Executors.newSingleThreadExecutor();
    private static String cachedJson;
    private static List<PaymentReminder> cachedItems;

    private ReminderScheduler() {}

    public static class PaymentReminder {
        public long id;
        public String type;
        public String title;
        public double baseAmount;
        public double downPayment;
        public double insurance;
        public boolean insuranceFinanced;
        public double principal;
        public double annualRate;
        /** Default payment. For differential loans this stores the first scheduled payment. */
        public double amount;
        public String paymentType;
        public long firstPaymentMillis;
        public int months;
        public int daysBefore;
        public int reminderHour;
        public int reminderMinute;
        public String status;
        public long deletedAt;
        public boolean soundEnabled;
        public long createdAt;
        public long updatedAt;
        public double interestPaidBefore;
        public String historyJson;
        /** Sparse per-installment overrides/statuses. */
        public String ledgerJson;
        /** Original principal used only for lifetime progress; it does not reset after restructuring. */
        public double progressOriginalPrincipal;
        /** Principal already repaid before the current schedule segment. */
        public double progressRepaidBefore;
        /** Portion of the current segment principal that belongs to the original debt. */
        public double progressTrackPrincipal;
        /** Structured savings history for early repayment/refinancing. */
        public String benefitJson;
        /** One-time insurance costs added by applied refinancings. Initial insurance remains in insurance. */
        public double refinanceInsuranceCosts;
        /** One-time commissions added by applied refinancings. */
        public double refinanceCommissionCosts;
        transient List<InstallmentEntry> ledgerCache;
        transient PaymentParts[] partsCache;

        public PaymentReminder(long id, String type, String title, double principal,
                               double annualRate, double amount, long firstPaymentMillis,
                               int months, int daysBefore) {
            this(id, type, title, principal, 0, 0, true, principal, annualRate, amount,
                    PAYMENT_ANNUITY, firstPaymentMillis, months, daysBefore, 9, 0,
                    STATUS_ACTIVE, 0, true, id, id, 0, "", "");
        }

        public PaymentReminder(long id, String type, String title,
                               double baseAmount, double downPayment, double insurance,
                               double principal, double annualRate, double amount,
                               long firstPaymentMillis, int months, int daysBefore) {
            this(id, type, title, baseAmount, downPayment, insurance, true, principal, annualRate, amount,
                    PAYMENT_ANNUITY, firstPaymentMillis, months, daysBefore, 9, 0,
                    STATUS_ACTIVE, 0, true, id, id, 0, "", "");
        }

        public PaymentReminder(long id, String type, String title,
                               double baseAmount, double downPayment, double insurance,
                               boolean insuranceFinanced, double principal, double annualRate, double amount,
                               String paymentType, long firstPaymentMillis, int months, int daysBefore,
                               int reminderHour, int reminderMinute) {
            this(id, type, title, baseAmount, downPayment, insurance, insuranceFinanced, principal, annualRate,
                    amount, paymentType, firstPaymentMillis, months, daysBefore, reminderHour, reminderMinute,
                    STATUS_ACTIVE, 0, true, id, id, 0, "", "");
        }

        public PaymentReminder(long id, String type, String title,
                               double baseAmount, double downPayment, double insurance,
                               boolean insuranceFinanced, double principal, double annualRate, double amount,
                               String paymentType, long firstPaymentMillis, int months, int daysBefore,
                               int reminderHour, int reminderMinute, String status, long deletedAt,
                               boolean soundEnabled, long createdAt, long updatedAt,
                               double interestPaidBefore, String historyJson, String ledgerJson) {
            this.id = id;
            this.type = normalizeType(type);
            this.title = title == null ? "" : title;
            this.baseAmount = Math.max(0, baseAmount);
            this.downPayment = Math.max(0, downPayment);
            this.insurance = Math.max(0, insurance);
            this.insuranceFinanced = insuranceFinanced;
            this.principal = Math.max(0, principal);
            this.annualRate = Math.max(0, annualRate);
            this.amount = Math.max(0, amount);
            this.paymentType = normalizePaymentType(paymentType);
            this.firstPaymentMillis = firstPaymentMillis;
            this.months = Math.max(1, months);
            this.daysBefore = Math.max(1, Math.min(30, daysBefore));
            this.reminderHour = Math.max(0, Math.min(23, reminderHour));
            this.reminderMinute = Math.max(0, Math.min(59, reminderMinute));
            this.status = normalizeStatus(status);
            this.deletedAt = deletedAt;
            this.soundEnabled = soundEnabled;
            long fallback = id > 0 ? id : System.currentTimeMillis();
            this.createdAt = createdAt > 0 ? createdAt : fallback;
            this.updatedAt = updatedAt > 0 ? updatedAt : this.createdAt;
            this.interestPaidBefore = Math.max(0, interestPaidBefore);
            this.historyJson = historyJson == null ? "" : historyJson;
            this.ledgerJson = ledgerJson == null ? "" : ledgerJson;
            this.progressOriginalPrincipal = this.principal;
            this.progressRepaidBefore = 0;
            this.progressTrackPrincipal = this.principal;
            this.benefitJson = "";
            this.refinanceInsuranceCosts = 0;
            this.refinanceCommissionCosts = 0;
        }
    }

    public static class InstallmentEntry {
        public int index;
        public double plannedOverride;
        public long paidAt;
        public double paidAmount;
        public double penalty;
        public long snoozeUntil;
    }

    public static class PaymentParts {
        public int index;
        public long dueDate;
        public double amount;
        public double interestPart;
        public double principalPart;
        public double balanceBefore;
        public double balanceAfter;
    }

    public static class HistoryEvent {
        public long time;
        public String type;
        public String titleRu;
        public String titleEn;
        public String detailsRu;
        public String detailsEn;
    }

    public static class BenefitEvent {
        public long time;
        public String type;
        public String reminderTitle;
        public double savings;
        public double actionAmount;
        public double paymentBefore;
        public double paymentAfter;
        public int monthsBefore;
        public int monthsAfter;
        public double rateBefore;
        public double rateAfter;
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
        public double newTotalPayments;
        public double outOfPocketCosts;
        public double cashOut;
        public long firstNewPayment;
        public String paymentType;
    }

    public static List<PaymentReminder> load(Context context) { return listByStatus(context, STATUS_ACTIVE); }

    /** Re-read all persisted data and rebuild derived reminder state. Used by pull-to-refresh. */
    public static void refreshAll(Context context) {
        synchronized (STORE_LOCK) { cachedJson = null; cachedItems = null; }
        List<PaymentReminder> all = loadAll(context);
        for (PaymentReminder r : all) {
            if (TYPE_DEPOSIT.equals(normalizeType(r.type))) continue;
            for (int i = 0; i < r.months; i++) if (isPaid(r, i)) PaymentNotificationHelper.cancel(context, r.id, i);
        }
        rescheduleAll(context);
    }

    public static List<PaymentReminder> listByStatus(Context context, String status) {
        List<PaymentReminder> result = new ArrayList<>();
        String normalized = normalizeStatus(status);
        for (PaymentReminder r : loadAll(context)) if (normalized.equals(r.status)) result.add(r);
        return result;
    }

    public static List<PaymentReminder> loadAll(Context context) {
        synchronized (STORE_LOCK) {
            List<PaymentReminder> result = new ArrayList<>();
            String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]");
            if (cachedItems != null && json.equals(cachedJson)) return new ArrayList<>(cachedItems);
            boolean changed = false;
            long now = System.currentTimeMillis();
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    double amount = o.optDouble("amount", 0);
                    int months = Math.max(1, o.optInt("months", 1));
                    double principal = o.has("principal") ? o.optDouble("principal", 0) : amount * months;
                    double baseAmount = o.has("baseAmount") ? o.optDouble("baseAmount", principal) : principal;
                    String status = normalizeStatus(o.optString("status", STATUS_ACTIVE));
                    long deletedAt = o.optLong("deletedAt", 0);
                    if (STATUS_TRASH.equals(status) && deletedAt > 0 && now - deletedAt >= TRASH_RETENTION_MILLIS) { changed = true; continue; }
                    long id = o.optLong("id", now + i);
                    long createdAt = o.optLong("createdAt", id > 0 ? id : now);
                    PaymentReminder r = new PaymentReminder(
                            id, o.optString("type", TYPE_CREDIT), o.optString("title", "Кредит"),
                            baseAmount, o.optDouble("downPayment", 0), o.optDouble("insurance", 0),
                            o.optBoolean("insuranceFinanced", true), principal,
                            o.optDouble("annualRate", 0), amount, o.optString("paymentType", PAYMENT_ANNUITY),
                            o.optLong("firstPaymentMillis", now), months, o.optInt("daysBefore", 3),
                            o.optInt("reminderHour", 9), o.optInt("reminderMinute", 0), status, deletedAt,
                            o.optBoolean("soundEnabled", true), createdAt, o.optLong("updatedAt", createdAt),
                            o.optDouble("interestPaidBefore", 0), o.optString("history", ""), o.optString("ledger", "")
                    );
                    double inferredOriginal = Math.max(r.principal, Math.max(0, r.baseAmount - r.downPayment + (r.insuranceFinanced ? r.insurance : 0)));
                    boolean hadProgress = o.has("progressOriginalPrincipal") && o.has("progressRepaidBefore") && o.has("progressTrackPrincipal");
                    r.progressOriginalPrincipal = Math.max(.01, o.optDouble("progressOriginalPrincipal", inferredOriginal));
                    double legacyRepaid = Math.max(0, r.progressOriginalPrincipal - r.principal);
                    r.progressRepaidBefore = Math.max(0, o.optDouble("progressRepaidBefore", legacyRepaid));
                    double defaultTrack = Math.max(0, Math.min(r.principal, r.progressOriginalPrincipal - r.progressRepaidBefore));
                    r.progressTrackPrincipal = Math.max(0, o.optDouble("progressTrackPrincipal", defaultTrack));
                    r.benefitJson = o.optString("benefits", "");
                    boolean hadRefinanceCosts=o.has("refinanceInsuranceCosts")&&o.has("refinanceCommissionCosts");
                    r.refinanceInsuranceCosts = Math.max(0, o.optDouble("refinanceInsuranceCosts", 0));
                    r.refinanceCommissionCosts = Math.max(0, o.optDouble("refinanceCommissionCosts", 0));
                    if(!hadRefinanceCosts){double[] legacyCosts=legacyRefinanceCosts(r);r.refinanceCommissionCosts=legacyCosts[0];r.refinanceInsuranceCosts=legacyCosts[1];changed=true;}
                    if (!hadProgress) changed = true;
                    if (!o.has("ledger") && !TYPE_DEPOSIT.equals(normalizeType(r.type))) {
                        migrateLegacyPastPaid(r, now);
                        changed = true;
                    }
                    if (r.historyJson.trim().isEmpty()) {
                        appendHistory(r, r.createdAt, HISTORY_CREATED, "Создана запись", "Item created",
                                "Создана запись «" + r.title + "».", "Created “" + r.title + "”.");
                        changed = true;
                    }
                    result.add(r);
                }
            } catch (Exception ignored) {}
            if (changed) saveRaw(context, result);
            cachedJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]");
            cachedItems = new ArrayList<>(result);
            return new ArrayList<>(result);
        }
    }

    private static void migrateLegacyPastPaid(PaymentReminder r, long now) {
        List<InstallmentEntry> migrated = new ArrayList<>();
        for (int i = 0; i < r.months; i++) {
            Calendar due = buildDueDate(r, i);
            due.set(Calendar.HOUR_OF_DAY, 23);
            due.set(Calendar.MINUTE, 59);
            due.set(Calendar.SECOND, 59);
            if (due.getTimeInMillis() >= now) break;
            PaymentParts parts = paymentParts(r, i);
            InstallmentEntry e = new InstallmentEntry();
            e.index = i;
            e.paidAt = buildDueDate(r, i).getTimeInMillis();
            e.paidAmount = parts.amount;
            migrated.add(e);
        }
        writeLedger(r, migrated);
    }

    public static PaymentReminder findById(Context context, long id) {
        for (PaymentReminder r : loadAll(context)) if (r.id == id) return r;
        return null;
    }

    private static void saveRaw(Context context, List<PaymentReminder> reminders) {
        synchronized (STORE_LOCK) {
            JSONArray array = new JSONArray();
            try {
                for (PaymentReminder r : reminders) {
                    JSONObject o = new JSONObject();
                    o.put("id", r.id); o.put("type", normalizeType(r.type)); o.put("title", r.title);
                    o.put("baseAmount", r.baseAmount); o.put("downPayment", r.downPayment); o.put("insurance", r.insurance);
                    o.put("insuranceFinanced", r.insuranceFinanced); o.put("principal", r.principal);
                    o.put("annualRate", r.annualRate); o.put("amount", r.amount); o.put("paymentType", normalizePaymentType(r.paymentType));
                    o.put("firstPaymentMillis", r.firstPaymentMillis); o.put("months", r.months); o.put("daysBefore", r.daysBefore);
                    o.put("reminderHour", r.reminderHour); o.put("reminderMinute", r.reminderMinute);
                    o.put("status", normalizeStatus(r.status)); o.put("deletedAt", r.deletedAt); o.put("soundEnabled", r.soundEnabled);
                    o.put("createdAt", r.createdAt); o.put("updatedAt", r.updatedAt); o.put("interestPaidBefore", r.interestPaidBefore);
                    o.put("history", r.historyJson == null ? "" : r.historyJson); o.put("ledger", r.ledgerJson == null ? "" : r.ledgerJson);
                    o.put("progressOriginalPrincipal", progressOriginalPrincipal(r)); o.put("progressRepaidBefore", Math.max(0,r.progressRepaidBefore)); o.put("progressTrackPrincipal", Math.max(0,r.progressTrackPrincipal));
                    o.put("benefits", r.benefitJson == null ? "" : r.benefitJson);
                    o.put("refinanceInsuranceCosts", Math.max(0,r.refinanceInsuranceCosts)); o.put("refinanceCommissionCosts", Math.max(0,r.refinanceCommissionCosts));
                    array.put(o);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize payment reminders", e);
            }
            String raw=array.toString();
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, raw).apply();
            cachedJson=raw;cachedItems=new ArrayList<>(reminders);
        }
    }

    public static String exportPaymentsJson(Context context) { return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]"); }
    public static void importPaymentsJson(Context context, String json) throws Exception {
        new JSONArray(json == null ? "[]" : json);
        for (PaymentReminder old : loadAll(context)) cancel(context, old);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, json).commit();
        rescheduleAll(context);
    }

    public static void add(Context context, PaymentReminder r) {
        synchronized (STORE_LOCK) {
            List<PaymentReminder> items = loadAll(context); long now = System.currentTimeMillis();
            boolean duplicate=true;
            while(duplicate){duplicate=false;for(PaymentReminder x:items)if(x.id==r.id){r.id=Math.max(now,r.id+1);duplicate=true;break;}}
            r.status = STATUS_ACTIVE; r.deletedAt = 0; r.createdAt = r.createdAt > 0 ? r.createdAt : now; r.updatedAt = now;
            if(r.progressOriginalPrincipal<=0)r.progressOriginalPrincipal=Math.max(.01,r.principal);
            if(r.progressTrackPrincipal<=0&&r.progressRepaidBefore<=0)r.progressTrackPrincipal=r.principal;
            if (r.historyJson == null || r.historyJson.trim().isEmpty()) appendHistory(r, r.createdAt, HISTORY_CREATED, "Создана запись", "Item created", "Создана запись «" + r.title + "».", "Created “" + r.title + "”.");
            items.add(r); saveRaw(context, items);
        }
        schedule(context, r);
    }

    public static void updateEdited(Context context, PaymentReminder edited) {
        List<PaymentReminder> items = loadAll(context);
        for (int i = 0; i < items.size(); i++) {
            PaymentReminder old = items.get(i); if (old.id != edited.id) continue;
            cancel(context, old);
            String ru = editDetails(old, edited, false), en = editDetails(old, edited, true);
            edited.status = old.status; edited.deletedAt = old.deletedAt; edited.soundEnabled = old.soundEnabled;
            edited.createdAt = old.createdAt; edited.updatedAt = System.currentTimeMillis(); edited.historyJson = old.historyJson;
            edited.interestPaidBefore = old.interestPaidBefore; edited.ledgerJson = old.ledgerJson;
            edited.progressOriginalPrincipal=old.progressOriginalPrincipal;edited.progressRepaidBefore=old.progressRepaidBefore;edited.progressTrackPrincipal=old.progressTrackPrincipal;edited.benefitJson=old.benefitJson;edited.refinanceInsuranceCosts=old.refinanceInsuranceCosts;edited.refinanceCommissionCosts=old.refinanceCommissionCosts;
            appendHistory(edited, edited.updatedAt, HISTORY_EDITED, "Редактирование", "Edited", ru, en);
            items.set(i, edited); saveRaw(context, items); if (STATUS_ACTIVE.equals(edited.status)) schedule(context, edited); return;
        }
    }

    private static String editDetails(PaymentReminder a, PaymentReminder b, boolean en) {
        StringBuilder s = new StringBuilder();
        addDiff(s, en?"Name":"Название", a.title,b.title); addDiff(s,en?"Amount":"Сумма",a.baseAmount,b.baseAmount);
        addDiff(s,en?"Down payment":"Первоначальный взнос",a.downPayment,b.downPayment); addDiff(s,en?"Insurance":"Страховка",a.insurance,b.insurance);
        addDiff(s,en?"Rate":"Ставка",a.annualRate,b.annualRate); addDiff(s,en?"Default payment":"Плановый платёж",a.amount,b.amount);
        if (!normalizePaymentType(a.paymentType).equals(normalizePaymentType(b.paymentType))) addLine(s,(en?"Payment type: ":"Тип платежей: ")+a.paymentType+" → "+b.paymentType);
        if (a.insuranceFinanced != b.insuranceFinanced) addLine(s,en?"Insurance financing changed.":"Изменён способ учёта страховки.");
        if (a.months!=b.months) addLine(s,(en?"Term: ":"Срок: ")+a.months+" → "+b.months+(en?" months":" мес."));
        if (a.firstPaymentMillis!=b.firstPaymentMillis) addLine(s,en?"First payment date changed.":"Изменена дата первого платежа.");
        if (a.daysBefore!=b.daysBefore || a.reminderHour!=b.reminderHour || a.reminderMinute!=b.reminderMinute) addLine(s,en?"Reminder settings changed.":"Изменены параметры напоминания.");
        return s.length()==0 ? (en?"Saved without visible changes.":"Сохранено без видимых изменений.") : s.toString();
    }
    private static void addDiff(StringBuilder s,String l,String a,String b){if(a==null)a="";if(b==null)b="";if(!a.equals(b))addLine(s,l+": "+a+" → "+b);}
    private static void addDiff(StringBuilder s,String l,double a,double b){if(Math.abs(a-b)>.005)addLine(s,l+": "+round2(a)+" → "+round2(b));}
    private static void addLine(StringBuilder s,String v){if(s.length()>0)s.append('\n');s.append(v);}

    public static void setMuted(Context context,long id,boolean muted){List<PaymentReminder> items=loadAll(context);for(PaymentReminder r:items)if(r.id==id){r.soundEnabled=!muted;r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_REMINDER,muted?"Звук выключен":"Звук включён",muted?"Sound muted":"Sound enabled",muted?"Уведомления переведены в беззвучный режим.":"Звук уведомлений включён.",muted?"Notifications muted.":"Notification sound enabled.");break;}saveRaw(context,items);}
    public static void archive(Context c,long id){changeStatus(c,id,STATUS_ARCHIVE);} public static void restoreFromArchive(Context c,long id){changeStatus(c,id,STATUS_ACTIVE);} public static void delete(Context c,long id){moveToTrash(c,id);}
    public static void moveToTrash(Context c,long id){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){cancel(c,r);r.status=STATUS_TRASH;r.deletedAt=System.currentTimeMillis();r.updatedAt=r.deletedAt;appendHistory(r,r.updatedAt,HISTORY_STATUS,"Перемещено в корзину","Moved to trash","Запись перемещена в корзину.","The item was moved to trash.");break;}saveRaw(c,items);}
    public static void restoreFromTrash(Context c,long id){List<PaymentReminder> items=loadAll(c);PaymentReminder target=null;for(PaymentReminder r:items)if(r.id==id){r.status=STATUS_ACTIVE;r.deletedAt=0;r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_STATUS,"Восстановлено из корзины","Restored from trash","Запись восстановлена в активные платежи.","The item was restored.");target=r;break;}saveRaw(c,items);if(target!=null)schedule(c,target);}
    public static void deleteForever(Context c,long id){List<PaymentReminder> items=loadAll(c),out=new ArrayList<>();for(PaymentReminder r:items){if(r.id==id)cancel(c,r);else out.add(r);}saveRaw(c,out);}
    private static void changeStatus(Context c,long id,String st){List<PaymentReminder> items=loadAll(c);PaymentReminder target=null;for(PaymentReminder r:items)if(r.id==id){target=r;cancel(c,r);r.status=normalizeStatus(st);r.deletedAt=0;r.updatedAt=System.currentTimeMillis();boolean active=STATUS_ACTIVE.equals(r.status);appendHistory(r,r.updatedAt,HISTORY_STATUS,active?"Возвращено в активные":"Перемещено в архив",active?"Restored to active":"Archived",active?"Запись возвращена в «Мои платежи».":"Запись перемещена в архив.",active?"The item was restored.":"The item was archived.");break;}saveRaw(c,items);if(target!=null&&STATUS_ACTIVE.equals(target.status))schedule(c,target);}
    public static int trashDaysRemaining(PaymentReminder r){if(r==null||r.deletedAt<=0)return 30;long x=TRASH_RETENTION_MILLIS-(System.currentTimeMillis()-r.deletedAt);return x<=0?0:(int)Math.ceil(x/(24d*60d*60d*1000d));}

    public static List<HistoryEvent> history(PaymentReminder r){List<HistoryEvent> out=new ArrayList<>();if(r==null)return out;try{JSONArray a=new JSONArray(r.historyJson==null||r.historyJson.trim().isEmpty()?"[]":r.historyJson);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;HistoryEvent e=new HistoryEvent();e.time=o.optLong("time",r.createdAt);e.type=o.optString("type","event");e.titleRu=o.optString("titleRu","Изменение");e.titleEn=o.optString("titleEn","Change");e.detailsRu=o.optString("detailsRu","");e.detailsEn=o.optString("detailsEn","");out.add(e);}}catch(Exception ignored){}return out;}
    private static double[] legacyRefinanceCosts(PaymentReminder r){double commission=0,insurance=0;for(HistoryEvent e:history(r))if(HISTORY_REFINANCE.equals(e.type)){commission+=historyMoney(e.detailsRu,"Комиссия:");insurance+=historyMoney(e.detailsRu,"Страховка:");}return new double[]{Math.max(0,commission),Math.max(0,insurance)};}
    private static double historyMoney(String details,String label){if(details==null)return 0;int start=details.indexOf(label);if(start<0)return 0;start+=label.length();int end=details.indexOf('\n',start);String raw=(end<0?details.substring(start):details.substring(start,end)).replace("₽","").trim().replace(',','.');try{return Math.max(0,Double.parseDouble(raw));}catch(Exception ignored){return 0;}}
    public static int historyCount(PaymentReminder r,String type){int n=0;for(HistoryEvent e:history(r))if(type.equals(e.type))n++;return n;}
    public static void addHistory(Context c,long id,String type,String titleRu,String titleEn,String detailsRu,String detailsEn){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,type,titleRu,titleEn,detailsRu,detailsEn);break;}saveRaw(c,items);}
    private static void appendHistory(PaymentReminder r,long time,String type,String tr,String te,String dr,String de){try{JSONArray a=new JSONArray(r.historyJson==null||r.historyJson.trim().isEmpty()?"[]":r.historyJson);JSONObject o=new JSONObject();o.put("time",time);o.put("type",type);o.put("titleRu",tr);o.put("titleEn",te);o.put("detailsRu",dr);o.put("detailsEn",de);a.put(o);r.historyJson=a.toString();}catch(Exception ignored){}}


    public static List<BenefitEvent> benefits(PaymentReminder r){List<BenefitEvent> out=new ArrayList<>();if(r==null)return out;try{JSONArray a=new JSONArray(r.benefitJson==null||r.benefitJson.trim().isEmpty()?"[]":r.benefitJson);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;BenefitEvent e=new BenefitEvent();e.time=o.optLong("time",r.updatedAt);e.type=o.optString("type",HISTORY_EARLY);e.reminderTitle=o.optString("reminderTitle",r.title);e.savings=o.optDouble("savings",0);e.actionAmount=o.optDouble("actionAmount",0);e.paymentBefore=o.optDouble("paymentBefore",0);e.paymentAfter=o.optDouble("paymentAfter",0);e.monthsBefore=o.optInt("monthsBefore",0);e.monthsAfter=o.optInt("monthsAfter",0);e.rateBefore=o.optDouble("rateBefore",0);e.rateAfter=o.optDouble("rateAfter",0);out.add(e);}}catch(Exception ignored){}return out;}
    public static List<BenefitEvent> allBenefits(Context c){List<BenefitEvent> out=new ArrayList<>();for(PaymentReminder r:loadAll(c))out.addAll(benefits(r));return out;}
    public static List<BenefitEvent> activeBenefits(Context c){List<BenefitEvent> out=new ArrayList<>();for(PaymentReminder r:load(c))out.addAll(benefits(r));return out;}
    public static List<BenefitEvent> archivedBenefits(Context c){List<BenefitEvent> out=new ArrayList<>();for(PaymentReminder r:listByStatus(c,STATUS_ARCHIVE))out.addAll(benefits(r));return out;}
    public static double totalBenefit(PaymentReminder r){double t=0;for(BenefitEvent e:benefits(r))t+=e.savings;return t;}
    /** Active savings only. Archived savings remain in archive/history but do not affect My payments. */
    public static double totalBenefit(Context c){double t=0;for(PaymentReminder r:load(c))t+=totalBenefit(r);return t;}
    public static double totalBenefitAll(Context c){double t=0;for(BenefitEvent e:allBenefits(c))t+=e.savings;return t;}
    public static double archivedBenefit(Context c){double t=0;for(BenefitEvent e:archivedBenefits(c))t+=e.savings;return t;}
    private static void appendBenefit(PaymentReminder r,long time,String type,double savings,double actionAmount,double paymentBefore,double paymentAfter,int monthsBefore,int monthsAfter,double rateBefore,double rateAfter){try{JSONArray a=new JSONArray(r.benefitJson==null||r.benefitJson.trim().isEmpty()?"[]":r.benefitJson);JSONObject o=new JSONObject();o.put("time",time);o.put("type",type);o.put("reminderTitle",r.title);o.put("savings",savings);o.put("actionAmount",actionAmount);o.put("paymentBefore",paymentBefore);o.put("paymentAfter",paymentAfter);o.put("monthsBefore",monthsBefore);o.put("monthsAfter",monthsAfter);o.put("rateBefore",rateBefore);o.put("rateAfter",rateAfter);a.put(o);r.benefitJson=a.toString();}catch(Exception ignored){}}

    // -------- Ledger / schedule --------
    public static List<InstallmentEntry> ledger(PaymentReminder r){if(r==null)return new ArrayList<>();if(r.ledgerCache!=null)return r.ledgerCache;List<InstallmentEntry> out=new ArrayList<>();try{JSONArray a=new JSONArray(r.ledgerJson==null||r.ledgerJson.trim().isEmpty()?"[]":r.ledgerJson);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;InstallmentEntry e=new InstallmentEntry();e.index=o.optInt("index",-1);if(e.index<0)continue;e.plannedOverride=o.optDouble("plannedOverride",0);e.paidAt=o.optLong("paidAt",0);e.paidAmount=o.optDouble("paidAmount",0);e.penalty=o.optDouble("penalty",0);e.snoozeUntil=o.optLong("snoozeUntil",0);out.add(e);}}catch(Exception ignored){}r.ledgerCache=out;return out;}
    private static InstallmentEntry entry(PaymentReminder r,int index){for(InstallmentEntry e:ledger(r))if(e.index==index)return e;InstallmentEntry e=new InstallmentEntry();e.index=index;return e;}
    private static void writeLedger(PaymentReminder r,List<InstallmentEntry> list){JSONArray a=new JSONArray();try{for(InstallmentEntry e:list){if(e.index<0)continue;if(e.plannedOverride<=0&&e.paidAt<=0&&e.paidAmount<=0&&e.penalty<=0&&e.snoozeUntil<=0)continue;JSONObject o=new JSONObject();o.put("index",e.index);o.put("plannedOverride",e.plannedOverride);o.put("paidAt",e.paidAt);o.put("paidAmount",e.paidAmount);o.put("penalty",e.penalty);o.put("snoozeUntil",e.snoozeUntil);a.put(o);}}catch(Exception ignored){}r.ledgerJson=a.toString();r.ledgerCache=list;r.partsCache=null;}
    private static InstallmentEntry mutableEntry(List<InstallmentEntry> list,int index){for(InstallmentEntry e:list)if(e.index==index)return e;InstallmentEntry e=new InstallmentEntry();e.index=index;list.add(e);return e;}

    public static PaymentParts paymentParts(PaymentReminder r,int wanted){
        PaymentParts empty=new PaymentParts();if(r==null||wanted<0||wanted>=r.months)return empty;
        if(r.partsCache!=null&&r.partsCache.length==r.months&&r.partsCache[wanted]!=null)return r.partsCache[wanted];
        PaymentParts[] parts=new PaymentParts[r.months];double balance=r.principal;double monthly=r.annualRate/100d/12d;List<InstallmentEntry> l=ledger(r);
        for(int i=0;i<r.months;i++){
  InstallmentEntry e=null;for(InstallmentEntry x:l)if(x.index==i){e=x;break;}
  double interest=balance>0?Math.max(0,balance*monthly):0;double amount=0;double principalPart=0;
  if(balance>.005){
      if(PAYMENT_DIFFERENTIAL.equals(normalizePaymentType(r.paymentType))&&!TYPE_INSTALLMENT.equals(normalizeType(r.type))){
          double theoretical=r.months<=0?balance:r.principal/r.months;double firstInterest=Math.max(0,r.principal*monthly);double fixed=r.amount>firstInterest+.005?r.amount-firstInterest:theoretical;if(fixed<=0)fixed=theoretical;
          principalPart=Math.min(balance,fixed);amount=principalPart+interest;
          if(e!=null&&e.plannedOverride>0){amount=e.plannedOverride;principalPart=Math.max(0,Math.min(balance,amount-interest));}
          if(i==r.months-1&&balance>principalPart+.005){principalPart=balance;amount=balance+interest;}
      }else{
          amount=e!=null&&e.plannedOverride>0?e.plannedOverride:r.amount;
          if(i==r.months-1){principalPart=balance;amount=balance+interest;}
          else{
              principalPart=monthly<=0?Math.min(balance,amount):Math.max(0,Math.min(balance,amount-interest));
              if(principalPart>=balance-.005){principalPart=balance;amount=balance+interest;}
          }
      }
  }
  PaymentParts part=new PaymentParts();part.index=i;part.dueDate=buildDueDate(r,i).getTimeInMillis();part.balanceBefore=balance;part.amount=Math.max(0,amount);part.interestPart=Math.min(part.amount,interest);part.principalPart=Math.max(0,Math.min(balance,principalPart));part.balanceAfter=Math.max(0,balance-part.principalPart);balance=part.balanceAfter;parts[i]=part;
        }
        r.partsCache=parts;return parts[wanted];
    }
    public static double paymentAmount(PaymentReminder r,int index){return paymentParts(r,index).amount;}
    public static boolean isPaid(PaymentReminder r,int index){InstallmentEntry e=entry(r,index);double planned=paymentAmount(r,index);return e.paidAt>0&&e.paidAmount>=Math.max(0,planned-.01);}
    public static double paidAmount(PaymentReminder r,int index){return Math.max(0,entry(r,index).paidAmount);}
    public static long paidAt(PaymentReminder r,int index){return entry(r,index).paidAt;}
    public static double penalty(PaymentReminder r,int index){return Math.max(0,entry(r,index).penalty);}
    public static boolean isOverdue(PaymentReminder r,int index){if(isPaid(r,index))return false;Calendar d=buildDueDate(r,index);d.set(Calendar.HOUR_OF_DAY,23);d.set(Calendar.MINUTE,59);d.set(Calendar.SECOND,59);return d.getTimeInMillis()<System.currentTimeMillis();}
    public static boolean isToday(PaymentReminder r,int index){Calendar a=Calendar.getInstance(),b=buildDueDate(r,index);return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);}
    public static String paymentStatus(PaymentReminder r,int index){if(isPaid(r,index))return "paid";if(isOverdue(r,index))return "overdue";if(isToday(r,index))return "today";return "upcoming";}

    public static void markPaid(Context c,long id,int index,long paidAt,double paidAmount,double penalty){List<PaymentReminder> items=loadAll(c);PaymentReminder target=null;for(PaymentReminder r:items)if(r.id==id&&index>=0&&index<r.months){List<InstallmentEntry> l=ledger(r);InstallmentEntry e=mutableEntry(l,index);PaymentParts p=paymentParts(r,index);e.paidAt=paidAt>0?paidAt:System.currentTimeMillis();e.paidAmount=paidAmount>0?paidAmount:p.amount;e.penalty=Math.max(0,penalty);e.snoozeUntil=0;writeLedger(r,l);r.updatedAt=System.currentTimeMillis();long delayDays=Math.max(0,(e.paidAt-p.dueDate)/(24L*60L*60L*1000L));String late=delayDays>0?"\nПросрочка: "+delayDays+" дн.":"";appendHistory(r,r.updatedAt,HISTORY_PAYMENT,"Платёж оплачен","Payment paid","Платёж от "+dateText(p.dueDate)+" оплачен "+dateText(e.paidAt)+". Сумма: "+round2(e.paidAmount)+" ₽"+late+(e.penalty>0?"\nШтраф / пеня: "+round2(e.penalty)+" ₽":""),"Payment due "+dateText(p.dueDate)+" paid "+dateText(e.paidAt)+". Amount: "+round2(e.paidAmount)+" ₽"+(delayDays>0?"\nLate by "+delayDays+" days.":"")+(e.penalty>0?"\nPenalty: "+round2(e.penalty)+" ₽":""));cancelInstallment(c,r,index);target=r;break;}saveRaw(c,items);if(target!=null)schedule(c,target);}

    public static void unmarkPaid(Context c,long id,int index){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id&&index>=0&&index<r.months){List<InstallmentEntry> l=ledger(r);InstallmentEntry e=mutableEntry(l,index);e.paidAt=0;e.paidAmount=0;e.penalty=0;e.snoozeUntil=0;writeLedger(r,l);r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_PAYMENT,"Снята отметка об оплате","Payment marked unpaid","С платежа "+dateText(buildDueDate(r,index).getTimeInMillis())+" снята отметка «Оплачен».","The paid mark was removed from the payment due "+dateText(buildDueDate(r,index).getTimeInMillis())+".");saveRaw(c,items);schedule(c,r);return;}}

    public static void markPastPaid(Context c,long id){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){List<InstallmentEntry> l=ledger(r);long now=System.currentTimeMillis();for(int i=0;i<r.months;i++){Calendar d=buildDueDate(r,i);d.set(Calendar.HOUR_OF_DAY,23);d.set(Calendar.MINUTE,59);d.set(Calendar.SECOND,59);if(d.getTimeInMillis()>=now)break;InstallmentEntry e=mutableEntry(l,i);if(e.paidAt<=0){long dueMillis=buildDueDate(r,i).getTimeInMillis();e.paidAt=dueMillis;e.paidAmount=paymentAmount(r,i);appendHistory(r,now,HISTORY_PAYMENT,"Платёж оплачен","Payment paid","Платёж от "+dateText(dueMillis)+" оплачен "+dateText(e.paidAt)+". Сумма: "+round2(e.paidAmount)+" ₽","Payment due "+dateText(dueMillis)+" marked paid. Amount: "+round2(e.paidAmount)+" ₽");}}writeLedger(r,l);r.updatedAt=now;appendHistory(r,now,HISTORY_PAYMENT,"Прошедшие платежи отмечены оплаченными","Past payments marked paid","Все прошедшие платежи отмечены как оплаченные по графику.","All past payments were marked paid according to schedule.");break;}saveRaw(c,items);}

    public static void changePlannedAmount(Context c,long id,int index,double amount,boolean following,boolean includeArchived){if(amount<=0)return;List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){List<InstallmentEntry> l=ledger(r);for(int i=0;i<r.months;i++){boolean past=buildDueDate(r,i).getTimeInMillis()<System.currentTimeMillis()||isPaid(r,i);boolean apply=(i==index)||(following&&i>=index)||(includeArchived&&past);if(apply)mutableEntry(l,i).plannedOverride=amount;}writeLedger(r,l);r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_SCHEDULE,"Изменена сумма графика","Schedule amount changed","Плановый платёж изменён на "+round2(amount)+" ₽. "+(following?"Применено к выбранному и следующим платежам.":"Применено к одному платежу.")+(includeArchived?" Изменение также применено к архивным платежам.":""),"Planned payment changed to "+round2(amount)+" ₽. "+(following?"Applied to the selected and following payments.":"Applied to one payment.")+(includeArchived?" Archived payments were included.":""));cancel(c,r);saveRaw(c,items);schedule(c,r);return;} }

    public static int nextPaymentIndex(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return -1;for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&!isPaid(r,i))return i;return -1;}
    public static int lastPlannedPaymentIndex(PaymentReminder r){if(r==null)return -1;for(int i=r.months-1;i>=0;i--)if(paymentAmount(r,i)>.005)return i;return -1;}
    public static int remainingPaymentCount(PaymentReminder r){if(r==null)return 0;int n=0;for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&!isPaid(r,i))n++;return n;}
    public static long nextPaymentMillis(PaymentReminder r){int i=nextPaymentIndex(r);return i<0?Long.MAX_VALUE:buildDueDate(r,i).getTimeInMillis();}
    public static int paidPaymentCount(PaymentReminder r){int n=0;if(r!=null)for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&isPaid(r,i))n++;return n;}
    /** Legacy name now intentionally reflects actual paid status, not elapsed dates. */
    public static int elapsedPayments(PaymentReminder r){return paidPaymentCount(r);}

    private static double actualInterestPaid(PaymentReminder r,int index){PaymentParts p=paymentParts(r,index);return Math.max(0,Math.min(p.interestPart,paidAmount(r,index)));}
    private static double actualPrincipalPaid(PaymentReminder r,int index){PaymentParts p=paymentParts(r,index);double principal=Math.max(0,paidAmount(r,index)-actualInterestPaid(r,index));return Math.max(0,Math.min(p.principalPart,principal));}
    public static double remainingDebt(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double paidPrincipal=0;for(int i=0;i<r.months;i++)if(paidAmount(r,i)>0)paidPrincipal+=actualPrincipalPaid(r,i);return Math.max(0,r.principal-paidPrincipal);}
    public static double balanceAtDate(PaymentReminder r,long date){if(r==null)return 0;double b=r.principal;long now=System.currentTimeMillis();long actionDay=PaymentDateMath.startOfDay(date);for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(PaymentDateMath.startOfDay(p.dueDate)>=actionDay)break;if(p.dueDate>now)b=Math.max(0,b-p.principalPart);else if(paidAmount(r,i)>0)b=Math.max(0,b-actualPrincipalPaid(r,i));}return Math.min(remainingDebt(r),b);}
    public static int remainingPaymentsAfterDate(PaymentReminder r,long date){int n=0;for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&PaymentDateMath.isOnOrAfterDay(buildDueDate(r,i).getTimeInMillis(),date)&&!isPaid(r,i))n++;return n;}
    private static long firstDueOnOrAfter(PaymentReminder r,long date){for(int i=0;i<r.months;i++){long d=buildDueDate(r,i).getTimeInMillis();if(paymentAmount(r,i)>.005&&PaymentDateMath.isOnOrAfterDay(d,date)&&!isPaid(r,i))return d;}Calendar c=Calendar.getInstance();c.setTimeInMillis(PaymentDateMath.startOfDay(date));c.add(Calendar.MONTH,1);return c.getTimeInMillis();}
    private static int firstUnpaidIndexOnOrAfterDate(PaymentReminder r,long date){for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&PaymentDateMath.isOnOrAfterDay(buildDueDate(r,i).getTimeInMillis(),date)&&!isPaid(r,i))return i;return Math.max(0,r.months-1);}
    public static double segmentPaidInterest(PaymentReminder r){double t=0;for(int i=0;i<r.months;i++)if(paidAmount(r,i)>0)t+=actualInterestPaid(r,i);return t;}
    public static double paidInterest(PaymentReminder r){return r==null?0:Math.max(0,r.interestPaidBefore+segmentPaidInterest(r));}
    public static double remainingInterest(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double t=0;for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(p.amount<=.005)continue;t+=Math.max(0,p.interestPart-actualInterestPaid(r,i));}return Math.max(0,t);}
    public static double remainingInterestFromDate(PaymentReminder r,long date){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double t=0;for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(p.amount<=.005||PaymentDateMath.isBeforeDay(p.dueDate,date))continue;t+=Math.max(0,p.interestPart-actualInterestPaid(r,i));}return Math.max(0,t);}
    public static double totalInterest(PaymentReminder r){return paidInterest(r)+remainingInterest(r);}
    public static double totalInsuranceCosts(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;return Math.max(0,r.insurance)+Math.max(0,r.refinanceInsuranceCosts);}
    public static double totalCommissionCosts(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;return Math.max(0,r.refinanceCommissionCosts);}
    public static double totalOverpayment(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;return FinanceMath.totalOverpayment(totalInterest(r),totalInsuranceCosts(r),totalCommissionCosts(r));}
    public static double totalPenalties(PaymentReminder r){double t=0;if(r!=null)for(InstallmentEntry e:ledger(r))t+=Math.max(0,e.penalty);return t;}
    public static double dueThisMonth(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;Calendar n=Calendar.getInstance();double t=0;for(int i=0;i<r.months;i++){Calendar d=buildDueDate(r,i);if(d.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==n.get(Calendar.MONTH))t+=paymentAmount(r,i);}return t;}
    private static class HistoricalPaid{long paidAt;double amount;HistoricalPaid(long paidAt,double amount){this.paidAt=paidAt;this.amount=amount;}}
    private static long parseHistoryDate(String value){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("dd.MM.yyyy",java.util.Locale.US);f.setLenient(false);return f.parse(value).getTime();}catch(Exception e){return 0;}}
    public static double paidThisMonth(PaymentReminder r){
        if(r==null)return 0;java.util.LinkedHashMap<String,HistoricalPaid> state=new java.util.LinkedHashMap<>();
        try{JSONArray a=new JSONArray(r.historyJson==null||r.historyJson.trim().isEmpty()?"[]":r.historyJson);java.util.regex.Pattern paidPattern=java.util.regex.Pattern.compile("Платёж от (\\d{2}\\.\\d{2}\\.\\d{4}) оплачен (\\d{2}\\.\\d{2}\\.\\d{4})\\. Сумма: ([0-9]+(?:\\.[0-9]+)?) ₽");java.util.regex.Pattern undoPattern=java.util.regex.Pattern.compile("С платежа (\\d{2}\\.\\d{2}\\.\\d{4}) снята отметка");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!HISTORY_PAYMENT.equals(o.optString("type","")))continue;String details=o.optString("detailsRu","");java.util.regex.Matcher paid=paidPattern.matcher(details);if(paid.find()){long at=parseHistoryDate(paid.group(2));double amount=Double.parseDouble(paid.group(3));if(at>0)state.put(paid.group(1),new HistoricalPaid(at,amount));continue;}java.util.regex.Matcher undo=undoPattern.matcher(details);if(undo.find())state.remove(undo.group(1));}}catch(Exception ignored){}
        for(int i=0;i<r.months;i++){long at=paidAt(r,i);if(at<=0)continue;state.put(dateText(buildDueDate(r,i).getTimeInMillis()),new HistoricalPaid(at,paidAmount(r,i)));}
        Calendar now=Calendar.getInstance();double total=0;for(HistoricalPaid x:state.values()){Calendar d=Calendar.getInstance();d.setTimeInMillis(x.paidAt);if(d.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==now.get(Calendar.MONTH))total+=Math.max(0,x.amount);}return Math.max(0,total);
    }
    public static double paidDueThisMonth(PaymentReminder r){if(r==null)return 0;Calendar n=Calendar.getInstance();double t=0;for(int i=0;i<r.months;i++){Calendar d=buildDueDate(r,i);if(d.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==n.get(Calendar.MONTH))t+=Math.min(paymentAmount(r,i),paidAmount(r,i));}return Math.max(0,t);}
    public static double earlyRepaymentThisMonth(PaymentReminder r){if(r==null)return 0;Calendar n=Calendar.getInstance();double t=0;for(BenefitEvent e:benefits(r)){if(!HISTORY_EARLY.equals(e.type)||e.actionAmount<=0)continue;Calendar d=Calendar.getInstance();d.setTimeInMillis(e.time);if(d.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==n.get(Calendar.MONTH))t+=e.actionAmount;}return Math.max(0,t);}
    public static double remainingThisMonth(PaymentReminder r){return Math.max(0,dueThisMonth(r)-paidDueThisMonth(r));}
    public static int overdueCount(PaymentReminder r){int n=0;if(r!=null)for(int i=0;i<r.months;i++)if(isOverdue(r,i))n++;return n;}
    public static double overdueAmount(PaymentReminder r){double t=0;if(r!=null)for(int i=0;i<r.months;i++)if(isOverdue(r,i))t+=Math.max(0,paymentAmount(r,i)-paidAmount(r,i));return t;}
    private static double segmentPrincipalPaid(PaymentReminder r){if(r==null)return 0;double t=0;for(int i=0;i<r.months;i++)if(paidAmount(r,i)>0)t+=actualPrincipalPaid(r,i);return Math.max(0,t);}
    private static double trackedSegmentPaid(PaymentReminder r){return r==null?0:Math.min(Math.max(0,r.progressTrackPrincipal),segmentPrincipalPaid(r));}
    public static double progressOriginalPrincipal(PaymentReminder r){if(r==null)return 0;if(r.progressOriginalPrincipal>0)return r.progressOriginalPrincipal;return Math.max(.01,Math.max(r.principal,r.baseAmount-r.downPayment+(r.insuranceFinanced?r.insurance:0)));}
    public static double paidPrincipal(PaymentReminder r){if(r==null)return 0;return Math.max(0,Math.min(progressOriginalPrincipal(r),r.progressRepaidBefore+trackedSegmentPaid(r)));}
    public static double progress(PaymentReminder r){double original=progressOriginalPrincipal(r);return original<=0?0:Math.max(0,Math.min(1,paidPrincipal(r)/original));}


    // -------- Notifications --------
    public static void rescheduleAll(Context c){for(PaymentReminder r:load(c)){cancel(c,r);schedule(c,r);}}
    public static void schedule(Context c,PaymentReminder r){if(r==null)return;Context app=c.getApplicationContext();PaymentReminder snap=alarmSnapshot(r);ALARM_EXECUTOR.execute(()->scheduleWindow(app,snap,0,true));}
    public static void scheduleFollowing(Context c,PaymentReminder r,int afterIndex){if(r==null)return;Context app=c.getApplicationContext();PaymentReminder snap=alarmSnapshot(r);ALARM_EXECUTOR.execute(()->scheduleWindow(app,snap,Math.max(0,afterIndex+1),false));}
    private static void scheduleWindow(Context c,PaymentReminder r,int startIndex,boolean allowImmediateToday){
        if(r==null||!STATUS_ACTIVE.equals(r.status)||TYPE_DEPOSIT.equals(normalizeType(r.type)))return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        long now=System.currentTimeMillis();int scheduledInstallments=0;Calendar nowCal=Calendar.getInstance();
        for(int i=Math.max(0,startIndex);i<r.months&&scheduledInstallments<SCHEDULE_WINDOW_INSTALLMENTS;i++){
            if(isPaid(r,i))continue;
            Calendar due=buildDueDate(r,i);Calendar end=(Calendar)due.clone();end.set(Calendar.HOUR_OF_DAY,23);end.set(Calendar.MINUTE,59);end.set(Calendar.SECOND,59);
            if(end.getTimeInMillis()<now)continue;
            Calendar pre=(Calendar)due.clone();pre.add(Calendar.DAY_OF_MONTH,-r.daysBefore);
            if(pre.getTimeInMillis()>now)scheduleAlarmNow(c,am,r,i,due,pre.getTimeInMillis(),"pre",false);
            Calendar day=(Calendar)due.clone();day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,1);day.set(Calendar.MILLISECOND,0);
            if(day.getTimeInMillis()>now)scheduleAlarmNow(c,am,r,i,due,day.getTimeInMillis(),"day",false);
            else if(allowImmediateToday&&isSameDay(due,nowCal))scheduleAlarmNow(c,am,r,i,due,now+350L,"day",false);
            long alarmTrigger=due.getTimeInMillis();
            if(alarmTrigger>now)scheduleAlarmNow(c,am,r,i,due,alarmTrigger,"alarm",false);
            // If today's configured sound time has already passed, do not ring immediately. The silent day notice stays visible.
            InstallmentEntry e=entry(r,i);if(e.snoozeUntil>now)scheduleAlarmNow(c,am,r,i,due,e.snoozeUntil,"snooze",false);
            scheduledInstallments++;
        }
    }
    private static boolean isSameDay(Calendar a,Calendar b){return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);}
    public static void snoozePayment(Context c,long id,int index,long when){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id&&index>=0&&index<r.months){List<InstallmentEntry> l=ledger(r);mutableEntry(l,index).snoozeUntil=when;writeLedger(r,l);r.updatedAt=System.currentTimeMillis();saveRaw(c,items);cancelInstallment(c,r,index);Context app=c.getApplicationContext();PaymentReminder snap=alarmSnapshot(r);ALARM_EXECUTOR.execute(()->{AlarmManager am=(AlarmManager)app.getSystemService(Context.ALARM_SERVICE);if(am!=null)scheduleAlarmNow(app,am,snap,index,buildDueDate(snap,index),when,"snooze",false);});return;}}
    private static void scheduleAlarmNow(Context c,AlarmManager am,PaymentReminder r,int index,Calendar due,long trigger,String kind,boolean noCreate){PendingIntent pi=buildPendingIntent(c,r,index,due,kind,noCreate);if(pi==null)return;try{if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);else am.set(AlarmManager.RTC_WAKEUP,trigger,pi);}catch(RuntimeException ignored){}}
    private static void cancel(Context c,PaymentReminder r){if(r==null)return;Context app=c.getApplicationContext();PaymentReminder snap=alarmSnapshot(r);ALARM_EXECUTOR.execute(()->cancelNow(app,snap));}
    private static void cancelNow(Context c,PaymentReminder r){if(r==null)return;for(int i=0;i<r.months;i++)cancelInstallmentNow(c,r,i);}
    private static void cancelInstallment(Context c,PaymentReminder r,int index){if(r==null)return;Context app=c.getApplicationContext();PaymentReminder snap=alarmSnapshot(r);ALARM_EXECUTOR.execute(()->cancelInstallmentNow(app,snap,index));}
    private static void cancelInstallmentNow(Context c,PaymentReminder r,int index){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;for(String kind:new String[]{"pre","day","alarm","snooze","due"}){PendingIntent pi=buildPendingIntent(c,r,index,buildDueDate(r,index),kind,true);if(pi!=null){am.cancel(pi);pi.cancel();}}}
    private static PaymentReminder alarmSnapshot(PaymentReminder r){PaymentReminder x=new PaymentReminder(r.id,r.type,r.title,r.baseAmount,r.downPayment,r.insurance,r.insuranceFinanced,r.principal,r.annualRate,r.amount,r.paymentType,r.firstPaymentMillis,r.months,r.daysBefore,r.reminderHour,r.reminderMinute,r.status,r.deletedAt,r.soundEnabled,r.createdAt,r.updatedAt,r.interestPaidBefore,r.historyJson,r.ledgerJson);x.ledgerJson=r.ledgerJson;x.progressOriginalPrincipal=r.progressOriginalPrincipal;x.progressRepaidBefore=r.progressRepaidBefore;x.progressTrackPrincipal=r.progressTrackPrincipal;x.benefitJson=r.benefitJson;x.refinanceInsuranceCosts=r.refinanceInsuranceCosts;x.refinanceCommissionCosts=r.refinanceCommissionCosts;return x;}
    private static PendingIntent buildPendingIntent(Context c,PaymentReminder r,int index,Calendar due,String kind,boolean noCreate){Intent i=new Intent(c,ReminderReceiver.class);i.setAction("com.example.creditcalculator.PAYMENT_"+r.id+"_"+index+"_"+kind);i.putExtra("reminder_id",r.id);i.putExtra("payment_index",index);i.putExtra("title",r.title);i.putExtra("type",normalizeType(r.type));i.putExtra("amount",paymentAmount(r,index));i.putExtra("due_date",due.getTimeInMillis());i.putExtra("days_before",r.daysBefore);i.putExtra("reminder_kind",kind);i.putExtra("item_sound_enabled",r.soundEnabled);int flags=PendingIntent.FLAG_IMMUTABLE|(noCreate?PendingIntent.FLAG_NO_CREATE:PendingIntent.FLAG_UPDATE_CURRENT);return PendingIntent.getBroadcast(c,requestCode(r.id,index,kind),i,flags);}
    public static Calendar buildDueDate(PaymentReminder r,int index){Calendar d=buildDueDate(r.firstPaymentMillis,index);d.set(Calendar.HOUR_OF_DAY,r.reminderHour);d.set(Calendar.MINUTE,r.reminderMinute);return d;}
    public static Calendar buildDueDate(long first,int index){Calendar f=Calendar.getInstance();f.setTimeInMillis(first);int preferred=f.get(Calendar.DAY_OF_MONTH);Calendar d=Calendar.getInstance();d.clear();d.set(f.get(Calendar.YEAR),f.get(Calendar.MONTH),1,9,0,0);d.add(Calendar.MONTH,index);d.set(Calendar.DAY_OF_MONTH,Math.min(preferred,d.getActualMaximum(Calendar.DAY_OF_MONTH)));d.set(Calendar.MILLISECOND,0);return d;}

    // -------- Early repayment / refinance --------
    public static EarlyRepaymentSimulation simulateEarlyRepayment(PaymentReminder r,long date,double prepayment){if(r==null||prepayment<=0)throw new IllegalArgumentException();double balance=balanceAtDate(r,date);if(balance<=0||prepayment>balance+.01)throw new IllegalArgumentException();int remaining=Math.max(1,remainingPaymentsAfterDate(r,date));EarlyRepaymentSimulation s=new EarlyRepaymentSimulation();s.balance=balance;s.prepayment=Math.min(prepayment,balance);s.newBalance=Math.max(0,balance-s.prepayment);s.remainingMonths=remaining;s.firstFuturePayment=firstDueOnOrAfter(r,date);s.oldRemainingInterest=remainingInterestFromDate(r,date);if(s.newBalance<=.01){s.reducedPayment=0;s.interestWithReducedPayment=0;s.savingsWithReducedPayment=s.oldRemainingInterest;s.reducedMonths=0;s.keptPayment=0;s.interestWithReducedTerm=0;s.savingsWithReducedTerm=s.oldRemainingInterest;return s;}boolean diff=PAYMENT_DIFFERENTIAL.equals(normalizePaymentType(r.paymentType));if(diff){s.reducedPayment=differentialFirstPayment(s.newBalance,remaining,r.annualRate);s.interestWithReducedPayment=differentialTotalInterest(s.newBalance,remaining,r.annualRate);s.savingsWithReducedPayment=Math.max(0,s.oldRemainingInterest-s.interestWithReducedPayment);double monthly=r.annualRate/100d/12d;double firstInterest=r.principal*monthly;double principalPart=r.amount>firstInterest+.005?r.amount-firstInterest:(r.months>0?r.principal/r.months:r.principal);if(principalPart<=0)principalPart=s.newBalance/remaining;s.reducedMonths=Math.max(1,(int)Math.ceil(s.newBalance/principalPart));s.keptPayment=Math.min(principalPart,s.newBalance)+s.newBalance*monthly;s.interestWithReducedTerm=differentialInterestWithFirstPayment(s.newBalance,s.reducedMonths,r.annualRate,s.keptPayment);s.savingsWithReducedTerm=Math.max(0,s.oldRemainingInterest-s.interestWithReducedTerm);}else{s.reducedPayment=annuity(s.newBalance,remaining,r.annualRate);s.interestWithReducedPayment=futureInterest(s.newBalance,r.annualRate,s.reducedPayment,remaining);s.savingsWithReducedPayment=Math.max(0,s.oldRemainingInterest-s.interestWithReducedPayment);s.keptPayment=Math.max(.01,paymentAmount(r,firstUnpaidIndexOnOrAfterDate(r,date)));s.reducedMonths=monthsForPayment(s.newBalance,r.annualRate,s.keptPayment,remaining);s.interestWithReducedTerm=futureInterest(s.newBalance,r.annualRate,s.keptPayment,s.reducedMonths);s.savingsWithReducedTerm=Math.max(0,s.oldRemainingInterest-s.interestWithReducedTerm);}return s;}
    public static void applyEarlyRepayment(Context c,long id,long date,double prepayment,boolean reduceTerm){applyEarlyRepayment(c,id,date,prepayment,reduceTerm,0,0);}
    public static void applyEarlyRepayment(Context c,long id,long date,double prepayment,boolean reduceTerm,double customPayment,int customMonths){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){EarlyRepaymentSimulation sim=simulateEarlyRepayment(r,date,prepayment);cancel(c,r);double oldPayment=paymentAmount(r,Math.max(0,nextPaymentIndex(r)));int oldMonths=Math.max(1,remainingPaymentsAfterDate(r,date));double oldRate=r.annualRate;double trackedPaid=trackedSegmentPaid(r);double trackedRemaining=Math.max(0,r.progressTrackPrincipal-trackedPaid);r.progressRepaidBefore=Math.min(progressOriginalPrincipal(r),r.progressRepaidBefore+trackedPaid);double trackedPrepay=Math.min(sim.prepayment,trackedRemaining);r.progressRepaidBefore=Math.min(progressOriginalPrincipal(r),r.progressRepaidBefore+trackedPrepay);r.progressTrackPrincipal=Math.max(0,trackedRemaining-trackedPrepay);r.interestPaidBefore+=segmentPaidInterest(r);r.updatedAt=System.currentTimeMillis();String before="Остаток до погашения: "+round2(sim.balance)+" ₽\nДосрочно внесено: "+round2(sim.prepayment)+" ₽";double saving;int afterMonths;double afterPayment;if(sim.newBalance<=.01){saving=sim.oldRemainingInterest;r.progressRepaidBefore=progressOriginalPrincipal(r);r.progressTrackPrincipal=0;r.principal=0;r.amount=0;r.firstPaymentMillis=date;r.months=1;r.ledgerJson="";r.ledgerCache=new ArrayList<>();r.partsCache=null;r.status=STATUS_ARCHIVE;afterMonths=0;afterPayment=0;appendHistory(r,r.updatedAt,HISTORY_EARLY,"Полное досрочное погашение","Full early repayment",before+"\nКредит полностью погашен.\nЭкономия на процентах: "+round2(saving)+" ₽","Loan fully repaid. Estimated interest saving: "+round2(saving)+" ₽");}else{r.principal=sim.newBalance;r.firstPaymentMillis=sim.firstFuturePayment;r.ledgerJson="";r.ledgerCache=new ArrayList<>();r.partsCache=null;if(reduceTerm){r.months=customMonths>0?customMonths:Math.max(1,sim.reducedMonths);r.amount=customPayment>0?customPayment:sim.keptPayment;saving=sim.oldRemainingInterest-interestForPaymentPlan(r,sim.newBalance,r.months,r.amount);appendHistory(r,r.updatedAt,HISTORY_EARLY,"Досрочное погашение","Early repayment",before+"\nВыбрано: сократить срок.\nНовый срок: "+r.months+" мес.\nНовый платёж: "+round2(r.amount)+" ₽\nЭкономия на процентах: "+round2(saving)+" ₽","Selected: reduce term. Estimated saving: "+round2(saving)+" ₽");}else{r.months=customMonths>0?customMonths:Math.max(1,sim.remainingMonths);r.amount=customPayment>0?customPayment:sim.reducedPayment;saving=sim.oldRemainingInterest-interestForPaymentPlan(r,sim.newBalance,r.months,r.amount);appendHistory(r,r.updatedAt,HISTORY_EARLY,"Досрочное погашение","Early repayment",before+"\nВыбрано: уменьшить ежемесячный платёж.\nНовый платёж: "+round2(r.amount)+" ₽\nЭкономия на процентах: "+round2(saving)+" ₽","Selected: reduce payment. Estimated saving: "+round2(saving)+" ₽");}afterMonths=r.months;afterPayment=r.amount;}appendBenefit(r,r.updatedAt,HISTORY_EARLY,saving,sim.prepayment,oldPayment,afterPayment,oldMonths,afterMonths,oldRate,oldRate);saveRaw(c,items);if(STATUS_ACTIVE.equals(r.status))schedule(c,r);return;}}

    public static RefinanceSimulation simulateRefinance(PaymentReminder r,long date,double newRate,int newMonths,double commission,double insurance){return simulateRefinance(r,date,newRate,newMonths,commission,insurance,0,PAYMENT_ANNUITY);}
    public static RefinanceSimulation simulateRefinance(PaymentReminder r,long date,double newRate,int newMonths,double commission,double insurance,double requestedPrincipal){return simulateRefinance(r,date,newRate,newMonths,commission,insurance,requestedPrincipal,PAYMENT_ANNUITY);}
    public static RefinanceSimulation simulateRefinance(PaymentReminder r,long date,double newRate,int newMonths,double commission,double insurance,double requestedPrincipal,String paymentType){if(r==null||newRate<0||newMonths<=0||commission<0||insurance<0)throw new IllegalArgumentException();RefinanceSimulation s=new RefinanceSimulation();s.balance=balanceAtDate(r,date);if(s.balance<=0)throw new IllegalArgumentException();s.oldRemainingMonths=Math.max(1,remainingPaymentsAfterDate(r,date));s.oldRemainingOverpayment=remainingInterestFromDate(r,date);s.commission=commission;s.insurance=insurance;s.newPrincipal=requestedPrincipal>0?requestedPrincipal:s.balance+commission+insurance;s.newRate=newRate;s.newMonths=newMonths;s.paymentType=normalizePaymentType(paymentType);if(PAYMENT_DIFFERENTIAL.equals(s.paymentType)){s.newPayment=differentialFirstPayment(s.newPrincipal,newMonths,newRate);s.newTotalPayments=s.newPrincipal+differentialTotalInterest(s.newPrincipal,newMonths,newRate);}else{s.newPayment=annuity(s.newPrincipal,newMonths,newRate);s.newTotalPayments=s.newPayment*newMonths;}double fees=commission+insurance;s.outOfPocketCosts=FinanceMath.outOfPocketCosts(s.balance,s.newPrincipal,fees);s.cashOut=FinanceMath.cashOut(s.balance,s.newPrincipal,fees);double comparable=FinanceMath.comparableRefinanceFutureCost(s.balance,s.newPrincipal,fees,s.newTotalPayments);s.newOverpayment=comparable-s.balance;s.savings=(s.balance+s.oldRemainingOverpayment)-comparable;Calendar cal=Calendar.getInstance();cal.setTimeInMillis(date);cal.add(Calendar.MONTH,1);s.firstNewPayment=cal.getTimeInMillis();return s;}
    public static void applyRefinance(Context c,long id,long date,double newRate,int newMonths,double commission,double insurance){applyRefinance(c,id,date,newRate,newMonths,commission,insurance,0,PAYMENT_ANNUITY);}
    public static void applyRefinance(Context c,long id,long date,double newRate,int newMonths,double commission,double insurance,double requestedPrincipal){applyRefinance(c,id,date,newRate,newMonths,commission,insurance,requestedPrincipal,PAYMENT_ANNUITY);}
    public static void applyRefinance(Context c,long id,long date,double newRate,int newMonths,double commission,double insurance,double requestedPrincipal,String paymentType){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){RefinanceSimulation sim=simulateRefinance(r,date,newRate,newMonths,commission,insurance,requestedPrincipal,paymentType);cancel(c,r);double oldRate=r.annualRate,oldPayment=paymentAmount(r,Math.max(0,nextPaymentIndex(r)));int oldMonths=sim.oldRemainingMonths;double trackedPaid=trackedSegmentPaid(r);double trackedRemaining=Math.max(0,r.progressTrackPrincipal-trackedPaid);r.progressRepaidBefore=Math.min(progressOriginalPrincipal(r),r.progressRepaidBefore+trackedPaid);if(sim.newPrincipal<trackedRemaining){r.progressRepaidBefore=Math.min(progressOriginalPrincipal(r),r.progressRepaidBefore+(trackedRemaining-sim.newPrincipal));r.progressTrackPrincipal=sim.newPrincipal;}else r.progressTrackPrincipal=trackedRemaining;r.interestPaidBefore+=segmentPaidInterest(r);r.refinanceInsuranceCosts+=Math.max(0,insurance);r.refinanceCommissionCosts+=Math.max(0,commission);r.principal=sim.newPrincipal;r.annualRate=newRate;r.months=newMonths;r.amount=sim.newPayment;r.paymentType=sim.paymentType;r.firstPaymentMillis=sim.firstNewPayment;r.ledgerJson="";r.ledgerCache=new ArrayList<>();r.partsCache=null;r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_REFINANCE,"Рефинансирование","Refinancing","Остаток: "+round2(sim.balance)+" ₽\nСтавка: "+round2(oldRate)+"% → "+round2(newRate)+"%\nТип платежей: "+sim.paymentType+"\nПлатёж: "+round2(oldPayment)+" ₽ → "+round2(sim.newPayment)+" ₽\nСрок: "+oldMonths+" → "+newMonths+" мес.\nКомиссия: "+round2(commission)+" ₽\nСтраховка: "+round2(insurance)+" ₽\nРасчётная экономия: "+round2(sim.savings)+" ₽","Refinancing applied. Payment type: "+sim.paymentType+". Estimated savings: "+round2(sim.savings)+" ₽");appendBenefit(r,r.updatedAt,HISTORY_REFINANCE,sim.savings,0,oldPayment,sim.newPayment,oldMonths,newMonths,oldRate,newRate);saveRaw(c,items);schedule(c,r);return;}}

    public static double annuity(double principal,int months,double rate){return principal<=0||months<=0?0:FinanceMath.annuityPayment(principal,months,rate);}
    public static double differentialFirstPayment(double principal,int months,double rate){return principal<=0||months<=0?0:FinanceMath.differentialFirstPayment(principal,months,rate);}
    public static double differentialLastPayment(double principal,int months,double rate){return principal<=0||months<=0?0:FinanceMath.differentialLastPayment(principal,months,rate);}
    public static double differentialTotalInterest(double principal,int months,double rate){return principal<=0||months<=0?0:FinanceMath.differentialTotalInterest(principal,months,rate);}
    public static double differentialInterestWithFirstPayment(double principal,int months,double rate,double firstPayment){return principal<=0||months<=0?0:FinanceMath.differentialInterestWithFirstPayment(principal,months,rate,firstPayment);}
    public static double interestForPaymentPlan(PaymentReminder r,double principal,int months,double payment){if(r==null||principal<=0||months<=0||payment<=0)return 0;return PAYMENT_DIFFERENTIAL.equals(normalizePaymentType(r.paymentType))?FinanceMath.differentialInterestWithFirstPayment(principal,months,r.annualRate,payment):FinanceMath.fixedPaymentInterest(principal,months,r.annualRate,payment);}
    private static int monthsForPayment(double balance,double rate,double payment,int fallback){int n=FinanceMath.monthsToPayoff(balance,rate,payment,1200);return n>0?n:Math.max(1,fallback);}
    private static double futureInterest(double balance,double rate,double payment,int months){if(balance<=0||payment<=0||months<=0)return 0;try{return FinanceMath.fixedPaymentInterest(balance,months,rate,payment);}catch(IllegalArgumentException e){return Double.POSITIVE_INFINITY;}}

    public static double depositExpectedIncome(PaymentReminder r){if(r==null||!TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;return Math.max(0,FinanceMath.simpleDepositInterest(r.principal,r.months,r.annualRate));}
    public static double depositFinalAmount(PaymentReminder r){return r==null?0:Math.max(0,r.principal+depositExpectedIncome(r));}

    public static String normalizeType(String type){if(type==null)return TYPE_CREDIT;String v=type.trim().toLowerCase();if(v.equals(TYPE_MORTGAGE)||v.contains("ипот"))return TYPE_MORTGAGE;if(v.equals(TYPE_AUTO)||v.contains("авто"))return TYPE_AUTO;if(v.equals(TYPE_INSTALLMENT)||v.contains("расср"))return TYPE_INSTALLMENT;if(v.equals(TYPE_DEPOSIT)||v.contains("вклад"))return TYPE_DEPOSIT;return TYPE_CREDIT;}
    public static String normalizePaymentType(String type){return PAYMENT_DIFFERENTIAL.equals(type)?PAYMENT_DIFFERENTIAL:PAYMENT_ANNUITY;}
    private static String normalizeStatus(String s){if(STATUS_ARCHIVE.equals(s))return STATUS_ARCHIVE;if(STATUS_TRASH.equals(s))return STATUS_TRASH;return STATUS_ACTIVE;}
    private static int requestCode(long id,int index,String kind){int base=(int)(id^(id>>>32));int k="day".equals(kind)?1:"alarm".equals(kind)?2:"snooze".equals(kind)?3:0;return 31*(31*base+index)+k;}
    private static String round2(double v){return String.format(java.util.Locale.US,"%.2f",v);}
    private static String dateText(long millis){return new java.text.SimpleDateFormat("dd.MM.yyyy",java.util.Locale.getDefault()).format(new java.util.Date(millis));}
}
