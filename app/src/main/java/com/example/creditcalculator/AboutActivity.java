package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); UiUtils.applyBackground(this, root);
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary)); root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView back = topText("‹", 34); back.setOnClickListener(v -> finish()); bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView title = topText(AppPreferences.tr(this, "О приложении", "About"), 20); title.setTypeface(null, android.graphics.Typeface.BOLD); title.setGravity(Gravity.CENTER_VERTICAL); bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.TRANSPARENT); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(24), dp(20), dp(32)); scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        content.addView(text(AppPreferences.tr(this, "Финансовый калькулятор", "Financial calculator"), 28, R.color.text_main, true));
        TextView desc = text(AppPreferences.tr(this,
                "Расчёт кредитов, ипотеки, автокредитов, рассрочек и вкладов. Контроль платежей, напоминания, история изменений, досрочное погашение и рефинансирование.",
                "Loan, mortgage, auto loan, installment and deposit calculations with payment tracking, reminders, history, early repayment and refinancing."), 15, R.color.text_secondary, false);
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(-1, -2); dp1.setMargins(0, dp(8), 0, dp(18)); content.addView(desc, dp1);

        MaterialCardView info = card();
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18), dp(16), dp(18), dp(16)); info.addView(box);
        addLine(box, AppPreferences.tr(this, "Версия приложения", "App version"), versionName());
        addLine(box, AppPreferences.tr(this, "Разработчик", "Developer"), "SKRYTON");
        content.addView(info);

        MaterialButton guide = new MaterialButton(this); guide.setText(AppPreferences.tr(this, "Инструкция по использованию", "User guide")); guide.setAllCaps(false); guide.setTextSize(16); guide.setTextColor(ContextCompat.getColor(this, R.color.primary)); guide.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background))); guide.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); guide.setStrokeWidth(dp(1)); guide.setCornerRadius(dp(14)); guide.setOnClickListener(v -> startActivity(new Intent(this, InstructionActivity.class)));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(54)); gp.setMargins(0, dp(18), 0, 0); content.addView(guide, gp);

        TextView disclaimerTitle = text(AppPreferences.tr(this, "Важно", "Important"), 18, R.color.text_main, true);
        LinearLayout.LayoutParams dtp = new LinearLayout.LayoutParams(-1, -2); dtp.setMargins(0, dp(24), 0, dp(6)); content.addView(disclaimerTitle, dtp);
        content.addView(text(AppPreferences.tr(this,
                "Расчёты носят информационный характер и могут отличаться от расчётов банка или другой финансовой организации.",
                "Calculations are for information only and may differ from a bank or other financial institution."), 14, R.color.text_secondary, false));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> { Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(0, bars.top, 0, 0); scroll.setPadding(0, 0, 0, bars.bottom + dp(8)); return insets; });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private String versionName() {
        try {
            String value = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return value == null || value.trim().isEmpty() ? "1.8" : value;
        } catch (Exception ignored) {
            return "1.8";
        }
    }

    private MaterialCardView card() { MaterialCardView c = new MaterialCardView(this); c.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background)); c.setRadius(dp(18)); c.setStrokeColor(ContextCompat.getColor(this, R.color.border)); c.setStrokeWidth(dp(1)); return c; }
    private void addLine(LinearLayout parent, String label, String value) { LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.setMargins(0, dp(8), 0, dp(8)); parent.addView(row, rp); row.addView(text(label, 14, R.color.text_secondary, false), new LinearLayout.LayoutParams(0, -2, 1f)); row.addView(text(value, 15, R.color.text_main, true)); }
    private TextView topText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setClickable(true); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(ContextCompat.getColor(this, color)); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
