package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
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

    private Spinner languageSpinner;
    private SwitchMaterial soundSwitch;
    private SwitchMaterial vibrationSwitch;
    private SwitchMaterial darkModeSwitch;
    private TextView selectedSoundText;
    private TextView backgroundStatusText;
    private MaterialButton systemSoundButton;
    private MaterialButton customSoundButton;
    private boolean binding;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPreferences.wrapLocale(newBase));
    }

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

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setBackgroundResource(android.R.drawable.list_selector_background);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView barTitle = new TextView(this);
        barTitle.setText(AppPreferences.tr(this, "Настройки", "Settings"));
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        barTitle.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(barTitle, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        content.addView(heading(AppPreferences.tr(this, "Настройки", "Settings"), 28));

        TextView languageTitle = heading(AppPreferences.tr(this, "Язык приложения", "App language"), 18);
        LinearLayout.LayoutParams languageTitleParams = new LinearLayout.LayoutParams(-1, -2);
        languageTitleParams.setMargins(0, dp(22), 0, dp(8));
        content.addView(languageTitle, languageTitleParams);

        languageSpinner = new Spinner(this);
        languageSpinner.setBackgroundResource(android.R.drawable.editbox_background);
        languageSpinner.setPadding(dp(12), 0, dp(12), 0);
        languageSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Русский", "English"}));
        content.addView(languageSpinner, new LinearLayout.LayoutParams(-1, dp(56)));

        content.addView(buildAppearanceCard(), cardParams());
        content.addView(buildNotificationsCard(), cardParams());

        TextView note = new TextView(this);
        note.setText(AppPreferences.tr(this,
                "Настройки сохраняются на этом устройстве.",
                "Settings are saved on this device."));
        note.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        note.setTextSize(13);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.setMargins(0, dp(18), 0, 0);
        content.addView(note, noteParams);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0);
            scroll.setPadding(0, 0, 0, bars.bottom + dp(8));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private View buildAppearanceCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(18));
        card.addView(box);
        box.addView(heading(AppPreferences.tr(this, "Оформление", "Appearance"), 20));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(-1, dp(60));
        modeParams.setMargins(0, dp(8), 0, dp(6));
        box.addView(modeRow, modeParams);

        TextView sun = normalText("☀  " + AppPreferences.tr(this, "День", "Day"), 16);
        modeRow.addView(sun, new LinearLayout.LayoutParams(0, -2, 1f));
        darkModeSwitch = new SwitchMaterial(this);
        darkModeSwitch.setShowText(false);
        modeRow.addView(darkModeSwitch, new LinearLayout.LayoutParams(-2, -2));
        TextView moon = normalText(AppPreferences.tr(this, "Ночь", "Night") + "  ☾", 16);
        moon.setGravity(Gravity.END);
        modeRow.addView(moon, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView backgroundTitle = heading(AppPreferences.tr(this, "Фон приложения", "App background"), 17);
        LinearLayout.LayoutParams bt = new LinearLayout.LayoutParams(-1, -2);
        bt.setMargins(0, dp(8), 0, dp(6));
        box.addView(backgroundTitle, bt);

        backgroundStatusText = new TextView(this);
        backgroundStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        backgroundStatusText.setTextSize(14);
        LinearLayout.LayoutParams bs = new LinearLayout.LayoutParams(-1, -2);
        bs.setMargins(0, 0, 0, dp(10));
        box.addView(backgroundStatusText, bs);

        MaterialButton chooseBackground = outlineButton(AppPreferences.tr(this,
                "Выбрать изображение", "Choose image"));
        chooseBackground.setOnClickListener(v -> chooseBackground());
        box.addView(chooseBackground, buttonParams());

        MaterialButton resetBackground = outlineButton(AppPreferences.tr(this,
                "Сбросить фон", "Reset background"));
        resetBackground.setOnClickListener(v -> {
            AppPreferences.setBackgroundUri(this, "");
            recreate();
        });
        box.addView(resetBackground, buttonParams());
        return card;
    }

    private View buildNotificationsCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(18));
        card.addView(box);
        box.addView(heading(AppPreferences.tr(this, "Оповещения", "Notifications"), 20));

        soundSwitch = new SwitchMaterial(this);
        soundSwitch.setText(AppPreferences.tr(this, "Звук уведомления", "Notification sound"));
        soundSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        soundSwitch.setTextSize(16);
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-1, dp(56));
        switchParams.setMargins(0, dp(8), 0, 0);
        box.addView(soundSwitch, switchParams);

        selectedSoundText = new TextView(this);
        selectedSoundText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        selectedSoundText.setTextSize(14);
        LinearLayout.LayoutParams selectedParams = new LinearLayout.LayoutParams(-1, -2);
        selectedParams.setMargins(0, 0, 0, dp(12));
        box.addView(selectedSoundText, selectedParams);

        systemSoundButton = outlineButton(AppPreferences.tr(this,
                "Выбрать стандартный звук телефона", "Choose phone notification sound"));
        systemSoundButton.setOnClickListener(v -> chooseSystemSound());
        box.addView(systemSoundButton, buttonParams());

        customSoundButton = outlineButton(AppPreferences.tr(this,
                "Выбрать свой звуковой файл", "Choose custom audio file"));
        customSoundButton.setOnClickListener(v -> chooseCustomSound());
        box.addView(customSoundButton, buttonParams());

        vibrationSwitch = new SwitchMaterial(this);
        vibrationSwitch.setText(AppPreferences.tr(this, "Вибрация", "Vibration"));
        vibrationSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        vibrationSwitch.setTextSize(16);
        LinearLayout.LayoutParams vibrationParams = new LinearLayout.LayoutParams(-1, dp(56));
        vibrationParams.setMargins(0, dp(6), 0, 0);
        box.addView(vibrationSwitch, vibrationParams);
        return card;
    }

    private void bindValues() {
        binding = true;
        languageSpinner.setSelection(AppPreferences.isEnglish(this) ? 1 : 0);
        soundSwitch.setChecked(AppPreferences.isSoundEnabled(this));
        vibrationSwitch.setChecked(AppPreferences.isVibrationEnabled(this));
        darkModeSwitch.setChecked(AppPreferences.isDarkMode(this));
        updateSelectedSoundLabel();
        updateSoundControls();
        updateBackgroundLabel();
        binding = false;

        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (binding) return;
                String language = position == 1 ? "en" : "ru";
                if (!language.equals(AppPreferences.getLanguage(SettingsActivity.this))) {
                    AppPreferences.setLanguage(SettingsActivity.this, language);
                    recreate();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (binding) return;
            AppPreferences.setDarkMode(this, checked);
            AppPreferences.applyNightMode(this);
            recreate();
        });
        soundSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            AppPreferences.setSoundEnabled(this, checked);
            updateSoundControls();
        });
        vibrationSwitch.setOnCheckedChangeListener((buttonView, checked) ->
                AppPreferences.setVibrationEnabled(this, checked));
    }

    private void chooseBackground() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_BACKGROUND);
    }

    private void chooseSystemSound() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        String saved = AppPreferences.getSoundUri(this);
        Uri existing = saved.isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                : Uri.parse(saved);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        startActivityForResult(intent, REQUEST_SYSTEM_SOUND);
    }

    private void chooseCustomSound() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CUSTOM_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_SYSTEM_SOUND) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                AppPreferences.setSoundUri(this, uri.toString());
                AppPreferences.setSoundEnabled(this, true);
                soundSwitch.setChecked(true);
                updateSelectedSoundLabel();
            }
            return;
        }

        Uri uri = data.getData();
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        if (requestCode == REQUEST_CUSTOM_SOUND) {
            AppPreferences.setSoundUri(this, uri.toString());
            AppPreferences.setSoundEnabled(this, true);
            soundSwitch.setChecked(true);
            updateSelectedSoundLabel();
        } else if (requestCode == REQUEST_BACKGROUND) {
            AppPreferences.setBackgroundUri(this, uri.toString());
            recreate();
        }
    }

    private void updateSelectedSoundLabel() {
        String saved = AppPreferences.getSoundUri(this);
        Uri uri = saved.isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                : Uri.parse(saved);
        String title = AppPreferences.tr(this, "Звук по умолчанию", "Default notification sound");
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) title = ringtone.getTitle(this);
        } catch (Exception ignored) {}
        selectedSoundText.setText(AppPreferences.tr(this, "Выбрано: ", "Selected: ") + title);
    }

    private void updateBackgroundLabel() {
        boolean custom = !AppPreferences.getBackgroundUri(this).trim().isEmpty();
        backgroundStatusText.setText(custom
                ? AppPreferences.tr(this, "Используется своё изображение", "Custom image selected")
                : AppPreferences.tr(this, "Стандартный фон", "Default background"));
    }

    private void updateSoundControls() {
        boolean enabled = soundSwitch.isChecked();
        systemSoundButton.setEnabled(enabled);
        customSoundButton.setEnabled(enabled);
        selectedSoundText.setAlpha(enabled ? 1f : 0.5f);
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(22), 0, 0);
        return lp;
    }

    private TextView heading(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        view.setTextSize(size);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView normalText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        view.setTextSize(size);
        return view;
    }

    private MaterialButton outlineButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ContextCompat.getColor(this, R.color.primary));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background)));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        button.setStrokeWidth(dp(1));
        button.setCornerRadius(dp(13));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.setMargins(0, 0, 0, dp(10));
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
