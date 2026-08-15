package com.example.creditcalculator;

import android.content.Context;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static Locale locale(Context context) {
        return AppPreferences.isEnglish(context) ? Locale.US : new Locale("ru", "RU");
    }

    public static String money(Context context, double value) {
        NumberFormat format = NumberFormat.getNumberInstance(locale(context));
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(value).replace('\u00A0', ' ').replace('\u202F', ' ') + " ₽";
    }

    public static String date(Context context, long millis) {
        return new SimpleDateFormat("dd.MM.yyyy", locale(context)).format(new Date(millis));
    }

    public static String typeLabel(Context context, String type) {
        String normalized = ReminderScheduler.normalizeType(type);
        boolean en = AppPreferences.isEnglish(context);
        switch (normalized) {
            case ReminderScheduler.TYPE_MORTGAGE:
                return en ? "Mortgage" : "Ипотека";
            case ReminderScheduler.TYPE_AUTO:
                return en ? "Auto loan" : "Автокредит";
            case ReminderScheduler.TYPE_INSTALLMENT:
                return en ? "Installment" : "Рассрочка";
            case ReminderScheduler.TYPE_DEPOSIT:
                return en ? "Deposit" : "Вклад";
            case ReminderScheduler.TYPE_CREDIT:
            default:
                return en ? "Loan" : "Кредит";
        }
    }

    public static String typeCodeByPosition(int position) {
        switch (position) {
            case 1:
                return ReminderScheduler.TYPE_MORTGAGE;
            case 2:
                return ReminderScheduler.TYPE_AUTO;
            case 3:
                return ReminderScheduler.TYPE_INSTALLMENT;
            case 4:
                return ReminderScheduler.TYPE_DEPOSIT;
            case 0:
            default:
                return ReminderScheduler.TYPE_CREDIT;
        }
    }

    public static int typePosition(String type) {
        switch (ReminderScheduler.normalizeType(type)) {
            case ReminderScheduler.TYPE_MORTGAGE:
                return 1;
            case ReminderScheduler.TYPE_AUTO:
                return 2;
            case ReminderScheduler.TYPE_INSTALLMENT:
                return 3;
            case ReminderScheduler.TYPE_DEPOSIT:
                return 4;
            default:
                return 0;
        }
    }
}
