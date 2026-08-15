package com.example.creditcalculator;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class UiUtils {

    private UiUtils() {
    }

    public static void applyBackground(Context context, View view) {
        String saved = AppPreferences.getBackgroundUri(context);
        if (saved != null && !saved.trim().isEmpty()) {
            InputStream stream = null;
            try {
                stream = context.getContentResolver().openInputStream(Uri.parse(saved));
                Drawable image = Drawable.createFromStream(stream, "user_background");
                if (image != null) {
                    // User photos can be very bright or very dark. Keep them visible,
                    // but add a strong contrast layer so text remains readable.
                    int overlayColor = AppPreferences.isDarkMode(context)
                            ? 0xA60B1220   // dark translucent overlay
                            : 0xBDF4F7FB;  // light translucent overlay
                    LayerDrawable layered = new LayerDrawable(new Drawable[]{
                            image,
                            new ColorDrawable(overlayColor)
                    });
                    view.setBackground(layered);
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
        view.setBackgroundResource(R.drawable.app_background);
    }

    /**
     * Spinner adapter with explicit text/background colors. Android's stock spinner
     * layout can become white-on-white in dark mode on some devices.
     */
    public static ArrayAdapter<String> spinnerAdapter(Context context, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_item,
                new ArrayList<>(values)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                styleSpinnerText(context, view, false);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                styleSpinnerText(context, view, true);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    public static ArrayAdapter<String> spinnerAdapter(Context context, String[] values) {
        List<String> list = new ArrayList<>();
        if (values != null) {
            for (String value : values) list.add(value);
        }
        return spinnerAdapter(context, list);
    }

    public static void styleSpinner(Context context, Spinner spinner) {
        if (spinner == null) return;
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(context, R.color.card_background));
        background.setCornerRadius(dp(context, 12));
        background.setStroke(dp(context, 1), ContextCompat.getColor(context, R.color.border));
        spinner.setBackground(background);
        spinner.setPadding(dp(context, 12), 0, dp(context, 12), 0);
    }

    private static void styleSpinnerText(Context context, View view, boolean dropdown) {
        if (!(view instanceof TextView)) return;
        TextView text = (TextView) view;
        text.setTextColor(ContextCompat.getColor(context, R.color.text_main));
        text.setTextSize(16);
        text.setBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
        text.setPadding(dp(context, 12), dropdown ? dp(context, 10) : 0,
                dp(context, 12), dropdown ? dp(context, 10) : 0);
        if (dropdown) text.setMinHeight(dp(context, 48));
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

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
