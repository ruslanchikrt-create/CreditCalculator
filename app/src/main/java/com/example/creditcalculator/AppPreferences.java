package com.example.creditcalculator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class AppPreferences {

    public static final String PREFS_NAME = "app_preferences";
    public static final String SECURITY_PIN = "pin";
    public static final String SECURITY_PASSWORD = "password";
    public static final String SECURITY_DEVICE = "device";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_LANGUAGE_CHOSEN = "language_chosen";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_SOUND_URI = "sound_uri";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_URI = "background_uri";
    private static final String KEY_PROFILE_NAME = "profile_name";
    private static final String KEY_AVATAR_URI = "avatar_uri";
    private static final String KEY_PAYMENTS_FILTER = "payments_filter";
    private static final String KEY_SECURITY_ENABLED = "security_enabled";
    private static final String KEY_SECURITY_KIND = "security_kind";
    private static final String KEY_SECURITY_HASH = "security_hash";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    private static final String KEY_LOCK_TIMEOUT = "lock_timeout_minutes";

    private AppPreferences() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getLanguage(Context context) {
        String value = prefs(context).getString(KEY_LANGUAGE, "ru");
        if ("en".equals(value) || "tr".equals(value) || "es".equals(value)) return value;
        return "ru";
    }

    public static void setLanguage(Context context, String language) {
        String value = "ru";
        if ("en".equals(language) || "tr".equals(language) || "es".equals(language)) value = language;
        prefs(context).edit().putString(KEY_LANGUAGE, value).putBoolean(KEY_LANGUAGE_CHOSEN, true).apply();
    }

    public static boolean isLanguageChosen(Context context) { return prefs(context).getBoolean(KEY_LANGUAGE_CHOSEN, false); }
    public static boolean isEnglish(Context context) { return "en".equals(getLanguage(context)); }
    public static boolean isTurkish(Context context) { return "tr".equals(getLanguage(context)); }
    public static boolean isSpanish(Context context) { return "es".equals(getLanguage(context)); }

    public static boolean isSoundEnabled(Context context) { return prefs(context).getBoolean(KEY_SOUND_ENABLED, true); }
    public static void setSoundEnabled(Context context, boolean enabled) { prefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply(); }
    public static boolean isVibrationEnabled(Context context) { return prefs(context).getBoolean(KEY_VIBRATION_ENABLED, true); }
    public static void setVibrationEnabled(Context context, boolean enabled) { prefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply(); }
    public static String getSoundUri(Context context) { return prefs(context).getString(KEY_SOUND_URI, ""); }
    public static void setSoundUri(Context context, String uri) { prefs(context).edit().putString(KEY_SOUND_URI, uri == null ? "" : uri).apply(); }

    public static boolean isDarkMode(Context context) { return prefs(context).getBoolean(KEY_DARK_MODE, false); }
    public static void setDarkMode(Context context, boolean enabled) { prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply(); }
    public static void applyNightMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(isDarkMode(context) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static String getBackgroundUri(Context context) { return prefs(context).getString(KEY_BACKGROUND_URI, ""); }
    public static void setBackgroundUri(Context context, String uri) { prefs(context).edit().putString(KEY_BACKGROUND_URI, uri == null ? "" : uri).apply(); }
    public static String getProfileName(Context context) { return prefs(context).getString(KEY_PROFILE_NAME, ""); }
    public static void setProfileName(Context context, String name) { prefs(context).edit().putString(KEY_PROFILE_NAME, name == null ? "" : name.trim()).apply(); }
    public static String getAvatarUri(Context context) { return prefs(context).getString(KEY_AVATAR_URI, ""); }
    public static void setAvatarUri(Context context, String uri) { prefs(context).edit().putString(KEY_AVATAR_URI, uri == null ? "" : uri).apply(); }

    public static String getPaymentsFilter(Context context) { return prefs(context).getString(KEY_PAYMENTS_FILTER, "all"); }
    public static void setPaymentsFilter(Context context, String value) { prefs(context).edit().putString(KEY_PAYMENTS_FILTER, value == null ? "all" : value).apply(); }
    /** Kept for compatibility. Sorting is session-only from v1.9. */
    public static String getPaymentsSort(Context context) { return "nearest"; }
    public static void setPaymentsSort(Context context, String value) { /* intentionally not persisted */ }

    public static boolean isSecurityEnabled(Context context) { return prefs(context).getBoolean(KEY_SECURITY_ENABLED, false); }
    public static void setSecurityEnabled(Context context, boolean enabled) { prefs(context).edit().putBoolean(KEY_SECURITY_ENABLED, enabled).apply(); }
    public static String getSecurityKind(Context context) {
        String kind=prefs(context).getString(KEY_SECURITY_KIND, SECURITY_PIN);
        if(SECURITY_PASSWORD.equals(kind)||SECURITY_DEVICE.equals(kind))return kind;
        return SECURITY_PIN;
    }
    public static boolean isDeviceCredentialSecurity(Context context){return SECURITY_DEVICE.equals(getSecurityKind(context));}
    public static boolean isBiometricEnabled(Context context) { return prefs(context).getBoolean(KEY_BIOMETRIC, false); }
    public static void setBiometricEnabled(Context context, boolean enabled) { prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply(); }
    public static int getLockTimeoutMinutes(Context context) { return prefs(context).getInt(KEY_LOCK_TIMEOUT, 0); }
    public static void setLockTimeoutMinutes(Context context, int minutes) { prefs(context).edit().putInt(KEY_LOCK_TIMEOUT, Math.max(0, minutes)).apply(); }

    public static void setAppSecret(Context context, String kind, String secret) {
        String normalizedKind = SECURITY_PASSWORD.equals(kind) ? SECURITY_PASSWORD : SECURITY_PIN;
        prefs(context).edit()
                .putString(KEY_SECURITY_KIND, normalizedKind)
                .putString(KEY_SECURITY_HASH, hash(secret == null ? "" : secret))
                .putBoolean(KEY_SECURITY_ENABLED, true)
                .apply();
    }

    public static void setDeviceCredentialSecurity(Context context){
        prefs(context).edit()
                .putString(KEY_SECURITY_KIND,SECURITY_DEVICE)
                .remove(KEY_SECURITY_HASH)
                .putBoolean(KEY_SECURITY_ENABLED,true)
                .apply();
    }

    public static boolean verifyAppSecret(Context context, String secret) {
        if(isDeviceCredentialSecurity(context))return false;
        String saved = prefs(context).getString(KEY_SECURITY_HASH, "");
        return !saved.isEmpty() && saved.equals(hash(secret == null ? "" : secret));
    }

    public static boolean hasAppSecret(Context context) {
        return !prefs(context).getString(KEY_SECURITY_HASH, "").isEmpty();
    }

    public static boolean hasConfiguredSecurity(Context context){
        return isSecurityEnabled(context) && (isDeviceCredentialSecurity(context) || hasAppSecret(context));
    }

    public static void clearSecurity(Context context) {
        prefs(context).edit()
                .remove(KEY_SECURITY_HASH)
                .remove(KEY_SECURITY_KIND)
                .putBoolean(KEY_SECURITY_ENABLED, false)
                .putBoolean(KEY_BIOMETRIC, false)
                .apply();
    }

    public static void clearAll(Context context) {
        prefs(context).edit().clear().commit();
        context.getSharedPreferences(ReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();
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
        String lang = getLanguage(context);
        if ("ru".equals(lang)) return ru;
        if ("en".equals(lang)) return en;
        return Translations.translate(lang, ru, en);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b));
            return out.toString();
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }
}
