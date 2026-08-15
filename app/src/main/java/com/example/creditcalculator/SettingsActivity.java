package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_SYSTEM_SOUND = 4101;
    private static final int REQUEST_CUSTOM_SOUND = 4102;
    private static final int REQUEST_BACKGROUND = 4103;
    private static final int REQUEST_CREATE_BACKUP = 4104;
    private static final int REQUEST_OPEN_BACKUP = 4105;

    private Spinner languageSpinner;
    private SwitchMaterial soundSwitch;
    private SwitchMaterial vibrationSwitch;
    private SwitchMaterial darkModeSwitch;
    private TextView selectedSoundText;
    private TextView backgroundStatusText;
    private MaterialButton systemSoundButton;
    private MaterialButton customSoundButton;
    private boolean binding;
    private String pendingBackupPassword = "";

    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(buildContent());
        bindValues();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        UiUtils.applyBackground(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(12), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView back = topText("‹", 34); back.setOnClickListener(v -> finish()); bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView barTitle = topText(AppPreferences.tr(this, "Настройки", "Settings"), 20); barTitle.setTypeface(null, android.graphics.Typeface.BOLD); barTitle.setGravity(Gravity.CENTER_VERTICAL); bar.addView(barTitle, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        content.addView(heading(AppPreferences.tr(this, "Настройки", "Settings"), 28));
        TextView languageTitle = heading(AppPreferences.tr(this, "Язык приложения", "App language"), 18);
        LinearLayout.LayoutParams ltp = new LinearLayout.LayoutParams(-1, -2); ltp.setMargins(0, dp(22), 0, dp(8)); content.addView(languageTitle, ltp);
        languageSpinner = new Spinner(this); UiUtils.styleSpinner(this, languageSpinner); languageSpinner.setAdapter(UiUtils.spinnerAdapter(this, new String[]{"Русский", "English"})); content.addView(languageSpinner, new LinearLayout.LayoutParams(-1, dp(56)));

        content.addView(buildAppearanceCard(), cardParams());
        content.addView(buildNotificationsCard(), cardParams());
        content.addView(buildBackupCard(), cardParams());

        TextView note = normalText(AppPreferences.tr(this,
                "Резервная копия позволяет перенести записи и настройки на другой телефон.",
                "A backup lets you move records and settings to another phone."), 13);
        note.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2); np.setMargins(0, dp(18), 0, 0); content.addView(note, np);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0); scroll.setPadding(0, 0, 0, bars.bottom + dp(8)); return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private View buildAppearanceCard() {
        MaterialCardView card = card();
        LinearLayout box = cardBox(card);
        box.addView(heading(AppPreferences.tr(this, "Оформление", "Appearance"), 20));
        LinearLayout modeRow = new LinearLayout(this); modeRow.setOrientation(LinearLayout.HORIZONTAL); modeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, dp(60)); mp.setMargins(0, dp(8), 0, dp(6)); box.addView(modeRow, mp);
        modeRow.addView(normalText("☀  " + AppPreferences.tr(this, "День", "Day"), 16), new LinearLayout.LayoutParams(0, -2, 1f));
        darkModeSwitch = new SwitchMaterial(this); modeRow.addView(darkModeSwitch);
        TextView moon = normalText(AppPreferences.tr(this, "Ночь", "Night") + "  ☾", 16); moon.setGravity(Gravity.END); modeRow.addView(moon, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView bgTitle = heading(AppPreferences.tr(this, "Фон приложения", "App background"), 17);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.setMargins(0, dp(8), 0, dp(6)); box.addView(bgTitle, bp);
        backgroundStatusText = normalText("", 14); backgroundStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); box.addView(backgroundStatusText);
        MaterialButton choose = outlineButton(AppPreferences.tr(this, "Выбрать изображение", "Choose image")); choose.setOnClickListener(v -> chooseBackground()); box.addView(choose, buttonParams());
        MaterialButton reset = outlineButton(AppPreferences.tr(this, "Сбросить фон", "Reset background")); reset.setOnClickListener(v -> { AppPreferences.setBackgroundUri(this, ""); recreate(); }); box.addView(reset, buttonParams());
        return card;
    }

    private View buildNotificationsCard() {
        MaterialCardView card = card();
        LinearLayout box = cardBox(card);
        box.addView(heading(AppPreferences.tr(this, "Оповещения", "Notifications"), 20));
        soundSwitch = new SwitchMaterial(this); soundSwitch.setText(AppPreferences.tr(this, "Звук уведомления", "Notification sound")); soundSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_main)); soundSwitch.setTextSize(16);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(56)); sp.setMargins(0, dp(8), 0, 0); box.addView(soundSwitch, sp);
        selectedSoundText = normalText("", 14); selectedSoundText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); box.addView(selectedSoundText);
        systemSoundButton = outlineButton(AppPreferences.tr(this, "Выбрать стандартный звук телефона", "Choose phone notification sound")); systemSoundButton.setOnClickListener(v -> chooseSystemSound()); box.addView(systemSoundButton, buttonParams());
        customSoundButton = outlineButton(AppPreferences.tr(this, "Выбрать свой звуковой файл", "Choose custom audio file")); customSoundButton.setOnClickListener(v -> chooseCustomSound()); box.addView(customSoundButton, buttonParams());
        vibrationSwitch = new SwitchMaterial(this); vibrationSwitch.setText(AppPreferences.tr(this, "Вибрация", "Vibration")); vibrationSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_main)); vibrationSwitch.setTextSize(16); box.addView(vibrationSwitch, new LinearLayout.LayoutParams(-1, dp(56)));
        return card;
    }

    private View buildBackupCard() {
        MaterialCardView card = card();
        LinearLayout box = cardBox(card);
        box.addView(heading(AppPreferences.tr(this, "Резервное копирование", "Backup"), 20));
        TextView info = normalText(AppPreferences.tr(this,
                "Сохраните данные в файл и восстановите их на новом телефоне. Можно создать обычную или защищённую паролем копию.",
                "Save your data to a file and restore it on another phone. Backups can be plain or password-protected."), 14);
        info.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2); ip.setMargins(0, dp(8), 0, dp(6)); box.addView(info, ip);
        MaterialButton save = outlineButton(AppPreferences.tr(this, "Сохранить резервную копию", "Save backup")); save.setOnClickListener(v -> chooseBackupProtection()); box.addView(save, buttonParams());
        MaterialButton restore = outlineButton(AppPreferences.tr(this, "Восстановить из файла", "Restore from file")); restore.setOnClickListener(v -> chooseBackupFile()); box.addView(restore, buttonParams());
        return card;
    }

    private void chooseBackupProtection() {
        String[] options = {AppPreferences.tr(this, "Без пароля", "Without password"), AppPreferences.tr(this, "С паролем", "With password")};
        new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this, "Защита резервной копии", "Backup protection"))
                .setItems(options, (dialog, which) -> { if (which == 0) { pendingBackupPassword = ""; createBackupFile(); } else askNewBackupPassword(); }).show();
    }

    private void askNewBackupPassword() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(4), dp(22), 0);
        EditText first = passwordField(AppPreferences.tr(this, "Пароль", "Password"));
        EditText second = passwordField(AppPreferences.tr(this, "Повторите пароль", "Repeat password"));
        box.addView(first, new LinearLayout.LayoutParams(-1, dp(58))); box.addView(second, new LinearLayout.LayoutParams(-1, dp(58)));
        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Защитить паролем", "Protect with password"))
                .setMessage(AppPreferences.tr(this, "Если вы забудете пароль, восстановить резервную копию будет невозможно.", "If you forget the password, the backup cannot be restored."))
                .setView(box)
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Продолжить", "Continue"), (d, w) -> {
                    String a = first.getText().toString(); String b = second.getText().toString();
                    if (a.length() < 4) { Toast.makeText(this, AppPreferences.tr(this, "Пароль должен содержать минимум 4 символа", "Password must have at least 4 characters"), Toast.LENGTH_LONG).show(); return; }
                    if (!a.equals(b)) { Toast.makeText(this, AppPreferences.tr(this, "Пароли не совпадают", "Passwords do not match"), Toast.LENGTH_LONG).show(); return; }
                    pendingBackupPassword = a; createBackupFile();
                }).show();
    }

    private EditText passwordField(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); e.setTextColor(ContextCompat.getColor(this, R.color.text_main)); e.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary)); return e;
    }

    private void createBackupFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "FinanceBackup-" + new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date()) + ".fcalc");
        startActivityForResult(intent, REQUEST_CREATE_BACKUP);
    }
    private void chooseBackupFile() { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*"); startActivityForResult(intent, REQUEST_OPEN_BACKUP); }

    private void askRestorePassword(Uri uri) {
        EditText password = passwordField(AppPreferences.tr(this, "Пароль резервной копии", "Backup password")); int p = dp(22); password.setPadding(p, 0, p, 0);
        new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this, "Введите пароль", "Enter password")).setView(password)
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Восстановить", "Restore"), (d, w) -> restoreBackup(uri, password.getText().toString())).show();
    }

    private void restoreBackup(Uri uri, String password) {
        try { BackupManager.restoreBackup(this, uri, password); Toast.makeText(this, AppPreferences.tr(this, "Данные восстановлены", "Data restored"), Toast.LENGTH_LONG).show(); recreate(); }
        catch (Exception e) { Toast.makeText(this, AppPreferences.tr(this, "Не удалось восстановить. Проверьте файл и пароль.", "Restore failed. Check the file and password."), Toast.LENGTH_LONG).show(); }
    }

    private void bindValues() {
        binding = true; languageSpinner.setSelection(AppPreferences.isEnglish(this) ? 1 : 0); soundSwitch.setChecked(AppPreferences.isSoundEnabled(this)); vibrationSwitch.setChecked(AppPreferences.isVibrationEnabled(this)); darkModeSwitch.setChecked(AppPreferences.isDarkMode(this)); updateSelectedSoundLabel(); updateSoundControls(); updateBackgroundLabel(); binding = false;
        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { if (binding) return; String language = position == 1 ? "en" : "ru"; if (!language.equals(AppPreferences.getLanguage(SettingsActivity.this))) { AppPreferences.setLanguage(SettingsActivity.this, language); recreate(); } }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        darkModeSwitch.setOnCheckedChangeListener((buttonView, checked) -> { if (binding) return; AppPreferences.setDarkMode(this, checked); AppPreferences.applyNightMode(this); recreate(); });
        soundSwitch.setOnCheckedChangeListener((buttonView, checked) -> { AppPreferences.setSoundEnabled(this, checked); updateSoundControls(); });
        vibrationSwitch.setOnCheckedChangeListener((buttonView, checked) -> AppPreferences.setVibrationEnabled(this, checked));
    }

    private void chooseBackground() { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*"); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(intent, REQUEST_BACKGROUND); }
    private void chooseCustomSound() { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("audio/*"); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(intent, REQUEST_CUSTOM_SOUND); }
    private void chooseSystemSound() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER); intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION); intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true); intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        String saved = AppPreferences.getSoundUri(this); Uri existing = saved.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(saved); intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing); startActivityForResult(intent, REQUEST_SYSTEM_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQUEST_CREATE_BACKUP) {
            Uri uri = data.getData(); if (uri == null) return;
            try { BackupManager.writeBackup(this, uri, pendingBackupPassword); Toast.makeText(this, AppPreferences.tr(this, "Резервная копия сохранена", "Backup saved"), Toast.LENGTH_LONG).show(); }
            catch (Exception e) { Toast.makeText(this, AppPreferences.tr(this, "Не удалось сохранить резервную копию", "Could not save backup"), Toast.LENGTH_LONG).show(); }
            pendingBackupPassword = ""; return;
        }
        if (requestCode == REQUEST_OPEN_BACKUP) {
            Uri uri = data.getData(); if (uri == null) return;
            try { if (BackupManager.isProtected(this, uri)) askRestorePassword(uri); else restoreBackup(uri, ""); }
            catch (Exception e) { Toast.makeText(this, AppPreferences.tr(this, "Файл резервной копии повреждён или не поддерживается", "Backup file is invalid or unsupported"), Toast.LENGTH_LONG).show(); }
            return;
        }
        if (requestCode == REQUEST_SYSTEM_SOUND) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI); if (uri != null) { AppPreferences.setSoundUri(this, uri.toString()); AppPreferences.setSoundEnabled(this, true); soundSwitch.setChecked(true); updateSelectedSoundLabel(); } return;
        }
        Uri uri = data.getData(); if (uri == null) return;
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        if (requestCode == REQUEST_CUSTOM_SOUND) { AppPreferences.setSoundUri(this, uri.toString()); AppPreferences.setSoundEnabled(this, true); soundSwitch.setChecked(true); updateSelectedSoundLabel(); }
        else if (requestCode == REQUEST_BACKGROUND) { AppPreferences.setBackgroundUri(this, uri.toString()); recreate(); }
    }

    private void updateSelectedSoundLabel() {
        String saved = AppPreferences.getSoundUri(this); Uri uri = saved.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(saved); String title = AppPreferences.tr(this, "Звук по умолчанию", "Default notification sound");
        try { Ringtone ringtone = RingtoneManager.getRingtone(this, uri); if (ringtone != null) title = ringtone.getTitle(this); } catch (Exception ignored) {}
        selectedSoundText.setText(AppPreferences.tr(this, "Выбрано: ", "Selected: ") + title);
    }
    private void updateBackgroundLabel() { backgroundStatusText.setText(AppPreferences.getBackgroundUri(this).trim().isEmpty() ? AppPreferences.tr(this, "Стандартный фон", "Default background") : AppPreferences.tr(this, "Используется своё изображение", "Custom image selected")); }
    private void updateSoundControls() { boolean enabled = soundSwitch.isChecked(); systemSoundButton.setEnabled(enabled); customSoundButton.setEnabled(enabled); selectedSoundText.setAlpha(enabled ? 1f : 0.5f); }

    private MaterialCardView card() { MaterialCardView c = new MaterialCardView(this); c.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background)); c.setRadius(dp(18)); c.setStrokeColor(ContextCompat.getColor(this, R.color.border)); c.setStrokeWidth(dp(1)); return c; }
    private LinearLayout cardBox(MaterialCardView card) { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18), dp(16), dp(18), dp(18)); card.addView(box); return box; }
    private LinearLayout.LayoutParams cardParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(22), 0, 0); return p; }
    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.setMargins(0, dp(10), 0, 0); return p; }
    private MaterialButton outlineButton(String value) { MaterialButton b = new MaterialButton(this); b.setText(value); b.setAllCaps(false); b.setTextColor(ContextCompat.getColor(this, R.color.primary)); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background))); b.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); b.setStrokeWidth(dp(1)); b.setCornerRadius(dp(14)); return b; }
    private TextView heading(String value, int size) { TextView v = normalText(value, size); v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private TextView normalText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextColor(ContextCompat.getColor(this, R.color.text_main)); v.setTextSize(size); return v; }
    private TextView topText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setClickable(true); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
