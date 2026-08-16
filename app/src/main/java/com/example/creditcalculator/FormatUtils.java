package com.example.creditcalculator;

import android.content.Context;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {
    private FormatUtils() {}

    public static Locale locale(Context context) {
        String l = AppPreferences.getLanguage(context);
        if ("en".equals(l)) return Locale.US;
        if ("tr".equals(l)) return new Locale("tr", "TR");
        if ("es".equals(l)) return new Locale("es", "ES");
        return new Locale("ru", "RU");
    }

    public static String money(Context context, double value) {
        NumberFormat format = NumberFormat.getNumberInstance(locale(context));
        format.setMaximumFractionDigits(2); format.setMinimumFractionDigits(0);
        return format.format(value).replace('\u00A0', ' ').replace('\u202F', ' ') + " ₽";
    }

    public static String date(Context context, long millis) { return new SimpleDateFormat("dd.MM.yyyy", locale(context)).format(new Date(millis)); }

    public static String typeLabel(Context context, String type) {
        String normalized = ReminderScheduler.normalizeType(type);
        if (ReminderScheduler.TYPE_MORTGAGE.equals(normalized)) return AppPreferences.tr(context, "Ипотека", "Mortgage");
        if (ReminderScheduler.TYPE_AUTO.equals(normalized)) return AppPreferences.tr(context, "Автокредит", "Auto loan");
        if (ReminderScheduler.TYPE_INSTALLMENT.equals(normalized)) return AppPreferences.tr(context, "Рассрочка", "Installment");
        if (ReminderScheduler.TYPE_DEPOSIT.equals(normalized)) return AppPreferences.tr(context, "Вклад", "Deposit");
        return AppPreferences.tr(context, "Кредит", "Loan");
    }

    public static String paymentTypeLabel(Context context, String paymentType) {
        return ReminderScheduler.PAYMENT_DIFFERENTIAL.equals(ReminderScheduler.normalizePaymentType(paymentType))
                ? AppPreferences.tr(context, "Дифференцированный", "Differential")
                : AppPreferences.tr(context, "Аннуитетный", "Annuity");
    }

    public static String typeCodeByPosition(int position) {
        switch (position) { case 1:return ReminderScheduler.TYPE_MORTGAGE; case 2:return ReminderScheduler.TYPE_AUTO; case 3:return ReminderScheduler.TYPE_INSTALLMENT; case 4:return ReminderScheduler.TYPE_DEPOSIT; default:return ReminderScheduler.TYPE_CREDIT; }
    }

    public static int typePosition(String type) {
        switch (ReminderScheduler.normalizeType(type)) { case ReminderScheduler.TYPE_MORTGAGE:return 1; case ReminderScheduler.TYPE_AUTO:return 2; case ReminderScheduler.TYPE_INSTALLMENT:return 3; case ReminderScheduler.TYPE_DEPOSIT:return 4; default:return 0; }
    }
}
