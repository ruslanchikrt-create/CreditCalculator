package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class LanguageSelectionActivity extends AppCompatActivity {
    private String selected = "ru";
    private MaterialButton ru, en, tr, es;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        selected = AppPreferences.getLanguage(this);
        setContentView(build());
        updateButtons();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        UiUtils.applyBackground(this, root);

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(22));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.addView(box);
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text("Выберите язык\nChoose language\nDil seçin\nElige idioma", 25, R.color.text_main, true);
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        TextView sub = text("Язык можно изменить позже в настройках.", 14, R.color.text_secondary, false);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.setMargins(0, dp(8), 0, dp(18)); box.addView(sub, sp);

        ru = languageButton("🇷🇺  Русский", "ru");
        en = languageButton("🇬🇧  English", "en");
        tr = languageButton("🇹🇷  Türkçe", "tr");
        es = languageButton("🇪🇸  Español", "es");
        box.addView(ru, buttonParams()); box.addView(en, buttonParams()); box.addView(tr, buttonParams()); box.addView(es, buttonParams());

        MaterialButton cont = new MaterialButton(this);
        cont.setText("Продолжить / Continue");
        cont.setAllCaps(false); cont.setTextSize(17); cont.setTextColor(Color.WHITE);
        cont.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        cont.setCornerRadius(dp(14));
        cont.setOnClickListener(v -> {
            AppPreferences.setLanguage(this, selected);
            if (getApplication() instanceof CreditApplication) ((CreditApplication) getApplication()).markUnlocked();
            Intent i = new Intent(this, MainActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(i); finish();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(56)); cp.setMargins(0, dp(12), 0, 0); box.addView(cont, cp);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(dp(22), bars.top + dp(20), dp(22), bars.bottom + dp(20)); return insets;
        });
        return root;
    }

    private MaterialButton languageButton(String label, String code) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label); b.setAllCaps(false); b.setTextSize(17); b.setCornerRadius(dp(14));
        b.setOnClickListener(v -> { selected = code; updateButtons(); });
        return b;
    }

    private void updateButtons() {
        style(ru, "ru".equals(selected)); style(en, "en".equals(selected)); style(tr, "tr".equals(selected)); style(es, "es".equals(selected));
    }

    private void style(MaterialButton b, boolean chosen) {
        int primary = ContextCompat.getColor(this, R.color.primary);
        b.setBackgroundTintList(ColorStateList.valueOf(chosen ? primary : ContextCompat.getColor(this, R.color.card_background)));
        b.setTextColor(chosen ? Color.WHITE : primary);
        b.setStrokeColor(ColorStateList.valueOf(primary)); b.setStrokeWidth(dp(1));
    }

    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54)); p.setMargins(0, 0, 0, dp(10)); return p; }
    private TextView text(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ContextCompat.getColor(this, color)); if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD); return t; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
