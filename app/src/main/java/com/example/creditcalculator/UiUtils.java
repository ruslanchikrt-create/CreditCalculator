package com.example.creditcalculator;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.io.InputStream;

public final class UiUtils {

    private UiUtils() {
    }

    public static void applyBackground(Context context, View view) {
        String saved = AppPreferences.getBackgroundUri(context);
        if (saved != null && !saved.trim().isEmpty()) {
            InputStream stream = null;
            try {
                stream = context.getContentResolver().openInputStream(Uri.parse(saved));
                Drawable drawable = Drawable.createFromStream(stream, "user_background");
                if (drawable != null) {
                    view.setBackground(drawable);
                    return;
                }
            } catch (Exception ignored) {
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.background));
    }

    public static String termUnit(Context context, int value, boolean years) {
        if (AppPreferences.isEnglish(context)) {
            if (years) return value == 1 ? "year" : "years";
            return value == 1 ? "month" : "months";
        }
        if (years) return russianYears(value);
        return russianMonths(value);
    }

    public static String termText(Context context, int months) {
        if (months > 0 && months % 12 == 0) {
            int years = months / 12;
            return years + " " + termUnit(context, years, true);
        }
        return months + " " + termUnit(context, months, false);
    }

    private static String russianYears(int value) {
        int mod100 = value % 100;
        int mod10 = value % 10;
        if (mod100 >= 11 && mod100 <= 14) return "лет";
        if (mod10 == 1) return "год";
        if (mod10 >= 2 && mod10 <= 4) return "года";
        return "лет";
    }

    private static String russianMonths(int value) {
        int mod100 = value % 100;
        int mod10 = value % 10;
        if (mod100 >= 11 && mod100 <= 14) return "месяцев";
        if (mod10 == 1) return "месяц";
        if (mod10 >= 2 && mod10 <= 4) return "месяца";
        return "месяцев";
    }
}
