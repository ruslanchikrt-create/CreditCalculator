package com.example.creditcalculator;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class BackupManager {

    private static final String FORMAT = "FCALC";
    private static final int VERSION = 2;
    private static final int MAX_MEDIA_BYTES = 20 * 1024 * 1024;

    private BackupManager() {}

    public static void writeBackup(Context context, Uri target, String password) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("payments", ReminderScheduler.exportPaymentsJson(context));
        payload.put("preferences", serializePreferences(context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)));
        payload.put("media", captureMedia(context));
        payload.put("createdAt", System.currentTimeMillis());

        byte[] raw = payload.toString().getBytes(StandardCharsets.UTF_8);
        JSONObject envelope = new JSONObject();
        envelope.put("format", FORMAT);
        envelope.put("version", VERSION);

        boolean encrypted = password != null && !password.isEmpty();
        envelope.put("encrypted", encrypted);
        if (encrypted) {
            byte[] salt = new byte[16];
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);
            SecretKeySpec key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encryptedData = cipher.doFinal(raw);
            envelope.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
            envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
            envelope.put("data", Base64.encodeToString(encryptedData, Base64.NO_WRAP));
        } else {
            envelope.put("data", Base64.encodeToString(raw, Base64.NO_WRAP));
        }

        OutputStream out = context.getContentResolver().openOutputStream(target, "w");
        if (out == null) throw new IllegalStateException("Cannot open target");
        try {
            out.write(envelope.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } finally {
            out.close();
        }
    }

    public static boolean isProtected(Context context, Uri source) throws Exception {
        JSONObject envelope = readEnvelope(context, source);
        validateEnvelope(envelope);
        return envelope.optBoolean("encrypted", false);
    }

    public static void restoreBackup(Context context, Uri source, String password) throws Exception {
        JSONObject envelope = readEnvelope(context, source);
        validateEnvelope(envelope);
        byte[] payloadBytes;
        if (envelope.optBoolean("encrypted", false)) {
            if (password == null || password.isEmpty()) throw new SecurityException("Password required");
            byte[] salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP);
            byte[] iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(envelope.getString("data"), Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
            payloadBytes = cipher.doFinal(encrypted);
        } else {
            payloadBytes = Base64.decode(envelope.getString("data"), Base64.NO_WRAP);
        }

        JSONObject payload = new JSONObject(new String(payloadBytes, StandardCharsets.UTF_8));
        String payments = payload.optString("payments", "[]");
        JSONObject preferences = payload.optJSONObject("preferences");
        if (preferences == null) preferences = new JSONObject();

        restorePreferences(context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE), preferences);
        JSONObject media = payload.optJSONObject("media");
        if (media != null) restoreMedia(context, media);
        ReminderScheduler.importPaymentsJson(context, payments);
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        spec.clearPassword();
        return new SecretKeySpec(key, "AES");
    }

    private static JSONObject readEnvelope(Context context, Uri source) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(source);
        if (in == null) throw new IllegalStateException("Cannot open backup");
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > 80 * 1024 * 1024) throw new IllegalArgumentException("Backup too large");
            }
            return new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            in.close();
        }
    }

    private static void validateEnvelope(JSONObject envelope) throws Exception {
        if (!FORMAT.equals(envelope.optString("format"))) throw new IllegalArgumentException("Invalid backup format");
        if (!envelope.has("data")) throw new IllegalArgumentException("Invalid backup data");
    }

    private static JSONObject serializePreferences(SharedPreferences prefs) throws Exception {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Object value = entry.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof Boolean) { item.put("type", "b"); item.put("value", value); }
            else if (value instanceof Integer) { item.put("type", "i"); item.put("value", value); }
            else if (value instanceof Long) { item.put("type", "l"); item.put("value", value); }
            else if (value instanceof Float) { item.put("type", "f"); item.put("value", value); }
            else { item.put("type", "s"); item.put("value", String.valueOf(value)); }
            object.put(entry.getKey(), item);
        }
        return object;
    }

    private static void restorePreferences(SharedPreferences prefs, JSONObject object) throws Exception {
        SharedPreferences.Editor editor = prefs.edit().clear();
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = object.optJSONObject(key);
            if (item == null) continue;
            String type = item.optString("type", "s");
            if ("b".equals(type)) editor.putBoolean(key, item.optBoolean("value"));
            else if ("i".equals(type)) editor.putInt(key, item.optInt("value"));
            else if ("l".equals(type)) editor.putLong(key, item.optLong("value"));
            else if ("f".equals(type)) editor.putFloat(key, (float) item.optDouble("value"));
            else editor.putString(key, item.optString("value", ""));
        }
        editor.commit();
    }

    private static JSONObject captureMedia(Context context) {
        JSONObject media = new JSONObject();
        SharedPreferences prefs = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE);
        captureOne(context, media, "background", prefs.getString("background_uri", ""));
        captureOne(context, media, "avatar", prefs.getString("avatar_uri", ""));
        captureOne(context, media, "sound", prefs.getString("sound_uri", ""));
        return media;
    }

    private static void captureOne(Context context, JSONObject media, String key, String uriValue) {
        if (uriValue == null || uriValue.trim().isEmpty()) return;
        try {
            InputStream in = context.getContentResolver().openInputStream(Uri.parse(uriValue));
            if (in == null) return;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) >= 0) {
                out.write(chunk, 0, read);
                if (out.size() > MAX_MEDIA_BYTES) { in.close(); return; }
            }
            in.close();
            media.put(key, Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
        } catch (Exception ignored) {}
    }

    private static void restoreMedia(Context context, JSONObject media) {
        File dir = new File(context.getFilesDir(), "restored_backup_media");
        if (!dir.exists()) dir.mkdirs();
        restoreOne(context, media, dir, "background", "background_uri");
        restoreOne(context, media, dir, "avatar", "avatar_uri");
        restoreOne(context, media, dir, "sound", "sound_uri");
    }

    private static void restoreOne(Context context, JSONObject media, File dir, String key, String prefKey) {
        String encoded = media.optString(key, "");
        if (encoded.isEmpty()) return;
        try {
            byte[] data = Base64.decode(encoded, Base64.NO_WRAP);
            File file = new File(dir, key + ".dat");
            FileOutputStream out = new FileOutputStream(file, false);
            out.write(data);
            out.close();
            context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(prefKey, Uri.fromFile(file).toString()).commit();
        } catch (Exception ignored) {}
    }
}
