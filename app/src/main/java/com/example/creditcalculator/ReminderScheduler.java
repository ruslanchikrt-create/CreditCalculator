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
        for (PaymentReminder r : loadAll(context)) if (normalized.equals(r.status)) result.add(r);
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
                // v1.8 and older did not store per-installment payment status. Preserve the old
                // behaviour on upgrade by marking already elapsed installments as paid once.
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
        return result;
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
                array.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    public static String exportPaymentsJson(Context context) { return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]"); }
    public static void importPaymentsJson(Context context, String json) throws Exception {
        new JSONArray(json == null ? "[]" : json);
        for (PaymentReminder old : loadAll(context)) cancel(context, old);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, json).commit();
        rescheduleAll(context);
    }

    public static void add(Context context, PaymentReminder r) {
        List<PaymentReminder> items = loadAll(context); long now = System.currentTimeMillis();
        r.status = STATUS_ACTIVE; r.deletedAt = 0; r.createdAt = r.createdAt > 0 ? r.createdAt : now; r.updatedAt = now;
        if (r.historyJson == null || r.historyJson.trim().isEmpty()) appendHistory(r, r.createdAt, HISTORY_CREATED, "Создана запись", "Item created", "Создана запись «" + r.title + "».", "Created “" + r.title + "”.");
        items.add(r); saveRaw(context, items); schedule(context, r);
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
    public static int historyCount(PaymentReminder r,String type){int n=0;for(HistoryEvent e:history(r))if(type.equals(e.type))n++;return n;}
    public static void addHistory(Context c,long id,String type,String titleRu,String titleEn,String detailsRu,String detailsEn){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,type,titleRu,titleEn,detailsRu,detailsEn);break;}saveRaw(c,items);}
    private static void appendHistory(PaymentReminder r,long time,String type,String tr,String te,String dr,String de){try{JSONArray a=new JSONArray(r.historyJson==null||r.historyJson.trim().isEmpty()?"[]":r.historyJson);JSONObject o=new JSONObject();o.put("time",time);o.put("type",type);o.put("titleRu",tr);o.put("titleEn",te);o.put("detailsRu",dr);o.put("detailsEn",de);a.put(o);r.historyJson=a.toString();}catch(Exception ignored){}}

    // -------- Ledger / schedule --------
    public static List<InstallmentEntry> ledger(PaymentReminder r){List<InstallmentEntry> out=new ArrayList<>();if(r==null)return out;try{JSONArray a=new JSONArray(r.ledgerJson==null||r.ledgerJson.trim().isEmpty()?"[]":r.ledgerJson);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;InstallmentEntry e=new InstallmentEntry();e.index=o.optInt("index",-1);if(e.index<0)continue;e.plannedOverride=o.optDouble("plannedOverride",0);e.paidAt=o.optLong("paidAt",0);e.paidAmount=o.optDouble("paidAmount",0);e.penalty=o.optDouble("penalty",0);e.snoozeUntil=o.optLong("snoozeUntil",0);out.add(e);}}catch(Exception ignored){}return out;}
    private static InstallmentEntry entry(PaymentReminder r,int index){for(InstallmentEntry e:ledger(r))if(e.index==index)return e;InstallmentEntry e=new InstallmentEntry();e.index=index;return e;}
    private static void writeLedger(PaymentReminder r,List<InstallmentEntry> list){JSONArray a=new JSONArray();try{for(InstallmentEntry e:list){if(e.index<0)continue;if(e.plannedOverride<=0&&e.paidAt<=0&&e.paidAmount<=0&&e.penalty<=0&&e.snoozeUntil<=0)continue;JSONObject o=new JSONObject();o.put("index",e.index);o.put("plannedOverride",e.plannedOverride);o.put("paidAt",e.paidAt);o.put("paidAmount",e.paidAmount);o.put("penalty",e.penalty);o.put("snoozeUntil",e.snoozeUntil);a.put(o);}}catch(Exception ignored){}r.ledgerJson=a.toString();}
    private static InstallmentEntry mutableEntry(List<InstallmentEntry> list,int index){for(InstallmentEntry e:list)if(e.index==index)return e;InstallmentEntry e=new InstallmentEntry();e.index=index;list.add(e);return e;}

    public static PaymentParts paymentParts(PaymentReminder r,int wanted){PaymentParts last=new PaymentParts();if(r==null||wanted<0)return last;double balance=r.principal;double monthly=r.annualRate/100d/12d;List<InstallmentEntry> l=ledger(r);for(int i=0;i<=wanted&&i<r.months;i++){InstallmentEntry e=null;for(InstallmentEntry x:l)if(x.index==i){e=x;break;}double interest=Math.max(0,balance*monthly);double amount;double principalPart;if(PAYMENT_DIFFERENTIAL.equals(normalizePaymentType(r.paymentType))&&!TYPE_INSTALLMENT.equals(normalizeType(r.type))){double fixed=r.months<=0?balance:r.principal/r.months;principalPart=Math.min(balance,fixed);amount=principalPart+interest;if(e!=null&&e.plannedOverride>0){amount=e.plannedOverride;principalPart=Math.max(0,Math.min(balance,amount-interest));}}else{amount=e!=null&&e.plannedOverride>0?e.plannedOverride:r.amount;if(monthly<=0)principalPart=Math.min(balance,amount);else principalPart=Math.max(0,Math.min(balance,amount-interest));if(balance>0&&principalPart>=balance-.005)amount=balance+interest;}PaymentParts p=new PaymentParts();p.index=i;p.dueDate=buildDueDate(r,i).getTimeInMillis();p.balanceBefore=balance;p.amount=Math.max(0,amount);p.interestPart=Math.min(p.amount,interest);p.principalPart=Math.max(0,Math.min(balance,principalPart));p.balanceAfter=Math.max(0,balance-p.principalPart);balance=p.balanceAfter;last=p;}return last;}
    public static double paymentAmount(PaymentReminder r,int index){return paymentParts(r,index).amount;}
    public static boolean isPaid(PaymentReminder r,int index){InstallmentEntry e=entry(r,index);double planned=paymentAmount(r,index);return e.paidAt>0&&e.paidAmount>=Math.max(0,planned-.01);}
    public static double paidAmount(PaymentReminder r,int index){return Math.max(0,entry(r,index).paidAmount);}
    public static long paidAt(PaymentReminder r,int index){return entry(r,index).paidAt;}
    public static double penalty(PaymentReminder r,int index){return Math.max(0,entry(r,index).penalty);}
    public static boolean isOverdue(PaymentReminder r,int index){if(isPaid(r,index))return false;Calendar d=buildDueDate(r,index);d.set(Calendar.HOUR_OF_DAY,23);d.set(Calendar.MINUTE,59);d.set(Calendar.SECOND,59);return d.getTimeInMillis()<System.currentTimeMillis();}
    public static boolean isToday(PaymentReminder r,int index){Calendar a=Calendar.getInstance(),b=buildDueDate(r,index);return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);}
    public static String paymentStatus(PaymentReminder r,int index){if(isPaid(r,index))return "paid";if(isOverdue(r,index))return "overdue";if(isToday(r,index))return "today";return "upcoming";}

    public static void markPaid(Context c,long id,int index,long paidAt,double paidAmount,double penalty){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id&&index>=0&&index<r.months){List<InstallmentEntry> l=ledger(r);InstallmentEntry e=mutableEntry(l,index);PaymentParts p=paymentParts(r,index);e.paidAt=paidAt>0?paidAt:System.currentTimeMillis();e.paidAmount=paidAmount>0?paidAmount:p.amount;e.penalty=Math.max(0,penalty);e.snoozeUntil=0;writeLedger(r,l);r.updatedAt=System.currentTimeMillis();long delayDays=Math.max(0,(e.paidAt-p.dueDate)/(24L*60L*60L*1000L));String late=delayDays>0?"\nПросрочка: "+delayDays+" дн.":"";appendHistory(r,r.updatedAt,HISTORY_PAYMENT,"Платёж оплачен","Payment paid","Платёж от "+dateText(p.dueDate)+" оплачен "+dateText(e.paidAt)+". Сумма: "+round2(e.paidAmount)+" ₽"+late+(e.penalty>0?"\nШтраф / пеня: "+round2(e.penalty)+" ₽":""),"Payment due "+dateText(p.dueDate)+" paid "+dateText(e.paidAt)+". Amount: "+round2(e.paidAmount)+" ₽"+(delayDays>0?"\nLate by "+delayDays+" days.":"")+(e.penalty>0?"\nPenalty: "+round2(e.penalty)+" ₽":""));cancelInstallment(c,r,index);break;}saveRaw(c,items);}

    public static void unmarkPaid(Context c,long id,int index){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id&&index>=0&&index<r.months){List<InstallmentEntry> l=ledger(r);InstallmentEntry e=mutableEntry(l,index);e.paidAt=0;e.paidAmount=0;e.penalty=0;e.snoozeUntil=0;writeLedger(r,l);r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_PAYMENT,"Снята отметка об оплате","Payment marked unpaid","С платежа "+dateText(buildDueDate(r,index).getTimeInMillis())+" снята отметка «Оплачен».","The paid mark was removed from the payment due "+dateText(buildDueDate(r,index).getTimeInMillis())+".");saveRaw(c,items);schedule(c,r);return;}}

    public static void markPastPaid(Context c,long id){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){List<InstallmentEntry> l=ledger(r);long now=System.currentTimeMillis();for(int i=0;i<r.months;i++){Calendar d=buildDueDate(r,i);d.set(Calendar.HOUR_OF_DAY,23);d.set(Calendar.MINUTE,59);d.set(Calendar.SECOND,59);if(d.getTimeInMillis()>=now)break;InstallmentEntry e=mutableEntry(l,i);if(e.paidAt<=0){e.paidAt=buildDueDate(r,i).getTimeInMillis();e.paidAmount=paymentAmount(r,i);}}writeLedger(r,l);r.updatedAt=now;appendHistory(r,now,HISTORY_PAYMENT,"Прошедшие платежи отмечены оплаченными","Past payments marked paid","Все прошедшие платежи отмечены как оплаченные по графику.","All past payments were marked paid according to schedule.");break;}saveRaw(c,items);}

    public static void changePlannedAmount(Context c,long id,int index,double amount,boolean following,boolean includeArchived){if(amount<=0)return;List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){List<InstallmentEntry> l=ledger(r);for(int i=0;i<r.months;i++){boolean past=buildDueDate(r,i).getTimeInMillis()<System.currentTimeMillis()||isPaid(r,i);boolean apply=(i==index)||(following&&i>=index)||(includeArchived&&past);if(apply)mutableEntry(l,i).plannedOverride=amount;}writeLedger(r,l);r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_SCHEDULE,"Изменена сумма графика","Schedule amount changed","Плановый платёж изменён на "+round2(amount)+" ₽. "+(following?"Применено к выбранному и следующим платежам.":"Применено к одному платежу.")+(includeArchived?" Изменение также применено к архивным платежам.":""),"Planned payment changed to "+round2(amount)+" ₽. "+(following?"Applied to the selected and following payments.":"Applied to one payment.")+(includeArchived?" Archived payments were included.":""));cancel(c,r);saveRaw(c,items);schedule(c,r);return;} }

    public static int nextPaymentIndex(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return -1;for(int i=0;i<r.months;i++)if(!isPaid(r,i))return i;return -1;}
    public static long nextPaymentMillis(PaymentReminder r){int i=nextPaymentIndex(r);return i<0?Long.MAX_VALUE:buildDueDate(r,i).getTimeInMillis();}
    public static int paidPaymentCount(PaymentReminder r){int n=0;if(r!=null)for(int i=0;i<r.months;i++)if(isPaid(r,i))n++;return n;}
    /** Legacy name now intentionally reflects actual paid status, not elapsed dates. */
    public static int elapsedPayments(PaymentReminder r){return paidPaymentCount(r);}

    private static double actualInterestPaid(PaymentReminder r,int index){PaymentParts p=paymentParts(r,index);return Math.max(0,Math.min(p.interestPart,paidAmount(r,index)));}
    private static double actualPrincipalPaid(PaymentReminder r,int index){PaymentParts p=paymentParts(r,index);double principal=Math.max(0,paidAmount(r,index)-actualInterestPaid(r,index));return Math.max(0,Math.min(p.principalPart,principal));}
    public static double remainingDebt(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double paidPrincipal=0;for(int i=0;i<r.months;i++)if(paidAmount(r,i)>0)paidPrincipal+=actualPrincipalPaid(r,i);return Math.max(0,r.principal-paidPrincipal);}
    public static double balanceAtDate(PaymentReminder r,long date){if(r==null)return 0;double b=r.principal;long now=System.currentTimeMillis();for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(p.dueDate>=date)break;if(p.dueDate>now)b=Math.max(0,b-p.principalPart);else if(paidAmount(r,i)>0)b=Math.max(0,b-actualPrincipalPaid(r,i));}return Math.min(remainingDebt(r),b);}
    public static int remainingPaymentsAfterDate(PaymentReminder r,long date){int n=0;for(int i=0;i<r.months;i++)if(buildDueDate(r,i).getTimeInMillis()>=date&&!isPaid(r,i))n++;return n;}
    private static long firstDueOnOrAfter(PaymentReminder r,long date){for(int i=0;i<r.months;i++){long d=buildDueDate(r,i).getTimeInMillis();if(d>=date&&!isPaid(r,i))return d;}Calendar c=Calendar.getInstance();c.setTimeInMillis(date);c.add(Calendar.MONTH,1);return c.getTimeInMillis();}
    public static double segmentPaidInterest(PaymentReminder r){double t=0;for(int i=0;i<r.months;i++)if(paidAmount(r,i)>0)t+=actualInterestPaid(r,i);return t;}
    public static double paidInterest(PaymentReminder r){return r==null?0:Math.max(0,r.interestPaidBefore+segmentPaidInterest(r));}
    public static double remainingInterest(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double t=0;for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);t+=Math.max(0,p.interestPart-actualInterestPaid(r,i));}return Math.max(0,t);}
    public static double totalInterest(PaymentReminder r){return paidInterest(r)+remainingInterest(r);}
    public static double totalPenalties(PaymentReminder r){double t=0;if(r!=null)for(InstallmentEntry e:ledger(r))t+=Math.max(0,e.penalty);return t;}
    public static double dueThisMonth(PaymentReminder r){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;Calendar n=Calendar.getInstance();double t=0;for(int i=0;i<r.months;i++){Calendar d=buildDueDate(r,i);if(d.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==n.get(Calendar.MONTH))t+=paymentAmount(r,i);}return t;}
    public static double paidThisMonth(PaymentReminder r){if(r==null)return 0;Calendar n=Calendar.getInstance();double t=0;for(int i=0;i<r.months;i++){Calendar d=buildDueDate(r,i);if(d.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==n.get(Calendar.MONTH))t+=Math.min(paymentAmount(r,i),paidAmount(r,i));}return t;}
    public static double remainingThisMonth(PaymentReminder r){return Math.max(0,dueThisMonth(r)-paidThisMonth(r));}
    public static int overdueCount(PaymentReminder r){int n=0;if(r!=null)for(int i=0;i<r.months;i++)if(isOverdue(r,i))n++;return n;}
    public static double overdueAmount(PaymentReminder r){double t=0;if(r!=null)for(int i=0;i<r.months;i++)if(isOverdue(r,i))t+=Math.max(0,paymentAmount(r,i)-paidAmount(r,i));return t;}
    public static double paidPrincipal(PaymentReminder r){return Math.max(0,r==null?0:r.principal-remainingDebt(r));}
    public static double progress(PaymentReminder r){return r==null||r.principal<=0?0:Math.min(1,paidPrincipal(r)/r.principal);}

    public static double depositExpectedIncome(PaymentReminder r){if(r==null||!TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;return Math.max(0,r.principal*r.annualRate/100d*(r.months/12d));}
    public static double depositFinalAmount(PaymentReminder r){return r==null?0:Math.max(0,r.principal+depositExpectedIncome(r));}

    // -------- Notifications --------
    public static void rescheduleAll(Context c){for(PaymentReminder r:load(c))schedule(c,r);}
    public static void schedule(Context c,PaymentReminder r){if(r==null||!STATUS_ACTIVE.equals(r.status)||TYPE_DEPOSIT.equals(normalizeType(r.type)))return;AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;long now=System.currentTimeMillis();for(int i=0;i<r.months;i++){if(isPaid(r,i))continue;Calendar due=buildDueDate(r,i);Calendar pre=(Calendar)due.clone();pre.add(Calendar.DAY_OF_MONTH,-r.daysBefore);if(pre.getTimeInMillis()>now)scheduleAlarm(c,am,r,i,due,pre.getTimeInMillis(),"pre",false);if(due.getTimeInMillis()>now)scheduleAlarm(c,am,r,i,due,due.getTimeInMillis(),"due",false);InstallmentEntry e=entry(r,i);if(e.snoozeUntil>now)scheduleAlarm(c,am,r,i,due,e.snoozeUntil,"snooze",false);}}
    public static void snoozePayment(Context c,long id,int index,long when){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id&&index>=0&&index<r.months){List<InstallmentEntry> l=ledger(r);mutableEntry(l,index).snoozeUntil=when;writeLedger(r,l);r.updatedAt=System.currentTimeMillis();saveRaw(c,items);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)scheduleAlarm(c,am,r,index,buildDueDate(r,index),when,"snooze",false);return;}}
    private static void scheduleAlarm(Context c,AlarmManager am,PaymentReminder r,int index,Calendar due,long trigger,String kind,boolean noCreate){PendingIntent pi=buildPendingIntent(c,r,index,due,kind,noCreate);if(pi==null)return;if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);else am.set(AlarmManager.RTC_WAKEUP,trigger,pi);}
    private static void cancel(Context c,PaymentReminder r){if(r==null)return;for(int i=0;i<r.months;i++)cancelInstallment(c,r,i);}
    private static void cancelInstallment(Context c,PaymentReminder r,int index){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;for(String kind:new String[]{"pre","due","snooze"}){PendingIntent pi=buildPendingIntent(c,r,index,buildDueDate(r,index),kind,true);if(pi!=null){am.cancel(pi);pi.cancel();}}}
    private static PendingIntent buildPendingIntent(Context c,PaymentReminder r,int index,Calendar due,String kind,boolean noCreate){Intent i=new Intent(c,ReminderReceiver.class);i.setAction("com.example.creditcalculator.PAYMENT_"+r.id+"_"+index+"_"+kind);i.putExtra("reminder_id",r.id);i.putExtra("payment_index",index);i.putExtra("title",r.title);i.putExtra("type",normalizeType(r.type));i.putExtra("amount",paymentAmount(r,index));i.putExtra("due_date",due.getTimeInMillis());i.putExtra("days_before",r.daysBefore);i.putExtra("reminder_kind",kind);i.putExtra("item_sound_enabled",r.soundEnabled);int flags=PendingIntent.FLAG_IMMUTABLE|(noCreate?PendingIntent.FLAG_NO_CREATE:PendingIntent.FLAG_UPDATE_CURRENT);return PendingIntent.getBroadcast(c,requestCode(r.id,index,kind),i,flags);}
    public static Calendar buildDueDate(PaymentReminder r,int index){Calendar d=buildDueDate(r.firstPaymentMillis,index);d.set(Calendar.HOUR_OF_DAY,r.reminderHour);d.set(Calendar.MINUTE,r.reminderMinute);return d;}
    public static Calendar buildDueDate(long first,int index){Calendar f=Calendar.getInstance();f.setTimeInMillis(first);int preferred=f.get(Calendar.DAY_OF_MONTH);Calendar d=Calendar.getInstance();d.clear();d.set(f.get(Calendar.YEAR),f.get(Calendar.MONTH),1,9,0,0);d.add(Calendar.MONTH,index);d.set(Calendar.DAY_OF_MONTH,Math.min(preferred,d.getActualMaximum(Calendar.DAY_OF_MONTH)));d.set(Calendar.MILLISECOND,0);return d;}

    // -------- Early repayment / refinance --------
    public static EarlyRepaymentSimulation simulateEarlyRepayment(PaymentReminder r,long date,double prepayment){if(r==null||prepayment<=0)throw new IllegalArgumentException();double balance=balanceAtDate(r,date);if(balance<=0||prepayment>balance+.01)throw new IllegalArgumentException();int remaining=Math.max(1,remainingPaymentsAfterDate(r,date));EarlyRepaymentSimulation s=new EarlyRepaymentSimulation();s.balance=balance;s.prepayment=Math.min(prepayment,balance);s.newBalance=Math.max(0,balance-s.prepayment);s.remainingMonths=remaining;s.firstFuturePayment=firstDueOnOrAfter(r,date);s.oldRemainingInterest=remainingInterest(r);if(s.newBalance<=.01){s.reducedPayment=0;s.interestWithReducedPayment=0;s.savingsWithReducedPayment=s.oldRemainingInterest;s.reducedMonths=0;s.keptPayment=0;s.interestWithReducedTerm=0;s.savingsWithReducedTerm=s.oldRemainingInterest;return s;}s.reducedPayment=annuity(s.newBalance,remaining,r.annualRate);s.interestWithReducedPayment=futureInterest(s.newBalance,r.annualRate,s.reducedPayment,remaining);s.savingsWithReducedPayment=Math.max(0,s.oldRemainingInterest-s.interestWithReducedPayment);s.keptPayment=Math.max(.01,paymentAmount(r,Math.max(0,nextPaymentIndex(r))));s.reducedMonths=monthsForPayment(s.newBalance,r.annualRate,s.keptPayment,remaining);s.interestWithReducedTerm=futureInterest(s.newBalance,r.annualRate,s.keptPayment,s.reducedMonths);s.savingsWithReducedTerm=Math.max(0,s.oldRemainingInterest-s.interestWithReducedTerm);return s;}
    public static void applyEarlyRepayment(Context c,long id,long date,double prepayment,boolean reduceTerm){applyEarlyRepayment(c,id,date,prepayment,reduceTerm,0,0);}
    public static void applyEarlyRepayment(Context c,long id,long date,double prepayment,boolean reduceTerm,double customPayment,int customMonths){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){EarlyRepaymentSimulation s=simulateEarlyRepayment(r,date,prepayment);cancel(c,r);r.interestPaidBefore+=segmentPaidInterest(r);r.updatedAt=System.currentTimeMillis();String before="Остаток до погашения: "+round2(s.balance)+" ₽\nДосрочно внесено: "+round2(s.prepayment)+" ₽";if(s.newBalance<=.01){r.principal=0;r.amount=0;r.firstPaymentMillis=date;r.months=1;r.ledgerJson="";r.status=STATUS_ARCHIVE;appendHistory(r,r.updatedAt,HISTORY_EARLY,"Полное досрочное погашение","Full early repayment",before+"\nКредит полностью погашен.","Loan fully repaid.");}else{r.principal=s.newBalance;r.firstPaymentMillis=s.firstFuturePayment;r.ledgerJson="";if(reduceTerm){r.months=customMonths>0?customMonths:Math.max(1,s.reducedMonths);r.amount=customPayment>0?customPayment:s.keptPayment;appendHistory(r,r.updatedAt,HISTORY_EARLY,"Досрочное погашение","Early repayment",before+"\nВыбрано: сократить срок.\nНовый срок: "+r.months+" мес.\nНовый платёж: "+round2(r.amount)+" ₽\nЭкономия на процентах: "+round2(s.savingsWithReducedTerm)+" ₽","Selected: reduce term.");}else{r.months=customMonths>0?customMonths:Math.max(1,s.remainingMonths);r.amount=customPayment>0?customPayment:s.reducedPayment;appendHistory(r,r.updatedAt,HISTORY_EARLY,"Досрочное погашение","Early repayment",before+"\nВыбрано: уменьшить ежемесячный платёж.\nНовый платёж: "+round2(r.amount)+" ₽\nЭкономия на процентах: "+round2(s.savingsWithReducedPayment)+" ₽","Selected: reduce payment.");}}saveRaw(c,items);if(STATUS_ACTIVE.equals(r.status))schedule(c,r);return;}}

    public static RefinanceSimulation simulateRefinance(PaymentReminder r,long date,double newRate,int newMonths,double commission,double insurance){return simulateRefinance(r,date,newRate,newMonths,commission,insurance,0);}
    public static RefinanceSimulation simulateRefinance(PaymentReminder r,long date,double newRate,int newMonths,double commission,double insurance,double requestedPrincipal){if(r==null||newRate<0||newMonths<=0||commission<0||insurance<0)throw new IllegalArgumentException();RefinanceSimulation s=new RefinanceSimulation();s.balance=balanceAtDate(r,date);if(s.balance<=0)throw new IllegalArgumentException();s.oldRemainingMonths=Math.max(1,remainingPaymentsAfterDate(r,date));s.oldRemainingOverpayment=remainingInterest(r);s.commission=commission;s.insurance=insurance;s.newPrincipal=requestedPrincipal>0?requestedPrincipal:s.balance+commission+insurance;s.newRate=newRate;s.newMonths=newMonths;s.newPayment=annuity(s.newPrincipal,newMonths,newRate);double total=s.newPayment*newMonths;s.newOverpayment=Math.max(0,total-s.balance);s.savings=s.oldRemainingOverpayment-s.newOverpayment;Calendar cal=Calendar.getInstance();cal.setTimeInMillis(date);cal.add(Calendar.MONTH,1);s.firstNewPayment=cal.getTimeInMillis();return s;}
    public static void applyRefinance(Context c,long id,long date,double newRate,int newMonths,double commission,double insurance){applyRefinance(c,id,date,newRate,newMonths,commission,insurance,0);}
    public static void applyRefinance(Context c,long id,long date,double newRate,int newMonths,double commission,double insurance,double requestedPrincipal){List<PaymentReminder> items=loadAll(c);for(PaymentReminder r:items)if(r.id==id){RefinanceSimulation s=simulateRefinance(r,date,newRate,newMonths,commission,insurance,requestedPrincipal);cancel(c,r);double oldRate=r.annualRate,oldPayment=paymentAmount(r,Math.max(0,nextPaymentIndex(r)));int oldMonths=s.oldRemainingMonths;r.interestPaidBefore+=segmentPaidInterest(r);r.principal=s.newPrincipal;r.annualRate=newRate;r.months=newMonths;r.amount=s.newPayment;r.paymentType=PAYMENT_ANNUITY;r.firstPaymentMillis=s.firstNewPayment;r.ledgerJson="";r.updatedAt=System.currentTimeMillis();appendHistory(r,r.updatedAt,HISTORY_REFINANCE,"Рефинансирование","Refinancing","Остаток: "+round2(s.balance)+" ₽\nСтавка: "+round2(oldRate)+"% → "+round2(newRate)+"%\nПлатёж: "+round2(oldPayment)+" ₽ → "+round2(s.newPayment)+" ₽\nСрок: "+oldMonths+" → "+newMonths+" мес.\nКомиссия: "+round2(commission)+" ₽\nСтраховка: "+round2(insurance)+" ₽\nРасчётная экономия: "+round2(s.savings)+" ₽","Refinancing applied. Estimated savings: "+round2(s.savings)+" ₽");saveRaw(c,items);schedule(c,r);return;}}

    public static double annuity(double principal,int months,double rate){if(months<=0||principal<=0)return 0;double m=rate/100d/12d;if(m<=0)return principal/months;double f=Math.pow(1+m,months);return principal*m*f/(f-1);}
    public static double differentialFirstPayment(double principal,int months,double rate){if(months<=0)return 0;return principal/months+principal*rate/100d/12d;}
    public static double differentialLastPayment(double principal,int months,double rate){if(months<=0)return 0;double part=principal/months;double balance=part;return part+balance*rate/100d/12d;}
    public static double differentialTotalInterest(double principal,int months,double rate){if(months<=0)return 0;double part=principal/months,b=principal,m=rate/100d/12d,t=0;for(int i=0;i<months;i++){t+=b*m;b=Math.max(0,b-part);}return t;}
    private static int monthsForPayment(double b,double rate,double payment,int fallback){if(b<=0)return 0;if(payment<=0)return Math.max(1,fallback);double m=rate/100d/12d;for(int n=1;n<=1200;n++){double interest=b*m;double part=m<=0?payment:payment-interest;if(part<=0)return Math.max(1,fallback);b-=part;if(b<=.01)return n;}return Math.max(1,fallback);}
    private static double futureInterest(double b,double rate,double payment,int max){if(b<=0||payment<=0||max<=0)return 0;double m=rate/100d/12d,t=0;for(int i=0;i<max&&b>.01;i++){double interest=b*m;t+=Math.max(0,interest);double part=m<=0?payment:payment-interest;if(part<=0)break;b=Math.max(0,b-part);}return Math.max(0,t);}

    public static String normalizeType(String type){if(type==null)return TYPE_CREDIT;String v=type.trim().toLowerCase();if(v.equals(TYPE_MORTGAGE)||v.contains("ипот"))return TYPE_MORTGAGE;if(v.equals(TYPE_AUTO)||v.contains("авто"))return TYPE_AUTO;if(v.equals(TYPE_INSTALLMENT)||v.contains("расср"))return TYPE_INSTALLMENT;if(v.equals(TYPE_DEPOSIT)||v.contains("вклад"))return TYPE_DEPOSIT;return TYPE_CREDIT;}
    public static String normalizePaymentType(String type){return PAYMENT_DIFFERENTIAL.equals(type)?PAYMENT_DIFFERENTIAL:PAYMENT_ANNUITY;}
    private static String normalizeStatus(String s){if(STATUS_ARCHIVE.equals(s))return STATUS_ARCHIVE;if(STATUS_TRASH.equals(s))return STATUS_TRASH;return STATUS_ACTIVE;}
    private static int requestCode(long id,int index,String kind){int base=(int)(id^(id>>>32));int k="due".equals(kind)?1:"snooze".equals(kind)?2:0;return 31*(31*base+index)+k;}
    private static String round2(double v){return String.format(java.util.Locale.US,"%.2f",v);}
    private static String dateText(long millis){return new java.text.SimpleDateFormat("dd.MM.yyyy",java.util.Locale.getDefault()).format(new java.util.Date(millis));}
}
