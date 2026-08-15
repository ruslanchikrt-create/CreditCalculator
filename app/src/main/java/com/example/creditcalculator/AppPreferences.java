package com.example.creditcalculator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public final class AppPreferences {

    private static final String PREFS = "app_preferences";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_SOUND_URI = "sound_uri";

    private AppPreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, "ru");
    }

    public static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANGUAGE, "en".equals(language) ? "en" : "ru").apply();
    }

    public static boolean isEnglish(Context context) {
        return "en".equals(getLanguage(context));
    }

    public static boolean isSoundEnabled(Context context) {
        return prefs(context).getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public static boolean isVibrationEnabled(Context context) {
        return prefs(context).getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    public static String getSoundUri(Context context) {
        return prefs(context).getString(KEY_SOUND_URI, "");
    }

    public static void setSoundUri(Context context, String uri) {
        prefs(context).edit().putString(KEY_SOUND_URI, uri == null ? "" : uri).apply();
    }

    public static Context wrapLocale(Context base) {
        String language = getLanguage(base);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        return base.createConfigurationContext(configuration);
    }

    public static String tr(Context context, String ru, String en) {
        return isEnglish(context) ? en : ru;
    }
}
