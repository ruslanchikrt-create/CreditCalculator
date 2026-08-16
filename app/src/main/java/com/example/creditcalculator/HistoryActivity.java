package com.example.creditcalculator;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "history_reminder_id";
    private ReminderScheduler.PaymentReminder reminder;
    private LinearLayout content;
    private boolean newestFirst = true;

    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        reminder = ReminderScheduler.findById(this, getIntent().getLongExtra(EXTRA_REMINDER_ID, -1L));
        if (reminder == null) {
            Toast.makeText(this, AppPreferences.tr(this, "Запись не найдена", "Item not found"), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(buildContent());
        render();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        UiUtils.applyBackground(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView back = topText("‹", 34);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView title = topText(AppPreferences.tr(this, "История изменений", "Change history"), 20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0);
            scroll.setPadding(0, 0, 0, bars.bottom + dp(8));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void render() {
        content.removeAllViews();
        content.addView(text(reminder.title, 28, R.color.text_main, true));
        TextView sub = text(FormatUtils.typeLabel(this, reminder.type), 14, R.color.primary, true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(4), 0, dp(16));
        content.addView(sub, sp);
        content.addView(summaryCard());

        LinearLayout sortRow = new LinearLayout(this);
        sortRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(-1, -2);
        srp.setMargins(0, dp(18), 0, dp(10));
        content.addView(sortRow, srp);
        TextView heading = text(AppPreferences.tr(this, "Все события", "All events"), 20, R.color.text_main, true);
        sortRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialButton sort = new MaterialButton(this);
        sort.setAllCaps(false);
        sort.setTextSize(12);
        sort.setText(newestFirst ? AppPreferences.tr(this, "Сначала новые", "Newest first") : AppPreferences.tr(this, "Сначала старые", "Oldest first"));
        sort.setTextColor(ContextCompat.getColor(this, R.color.primary));
        sort.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background)));
        sort.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.border)));
        sort.setStrokeWidth(dp(1));
        sort.setOnClickListener(v -> { newestFirst = !newestFirst; render(); });
        sortRow.addView(sort, new LinearLayout.LayoutParams(-2, dp(44)));

        List<ReminderScheduler.HistoryEvent> events = ReminderScheduler.history(reminder);
        if (newestFirst) Collections.reverse(events);
        if (events.isEmpty()) {
            content.addView(text(AppPreferences.tr(this, "История пока пуста.", "History is empty."), 15, R.color.text_secondary, false));
            return;
        }
        for (ReminderScheduler.HistoryEvent event : events) content.addView(eventCard(event));
    }

    private View summaryCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(box);
        box.addView(text(AppPreferences.tr(this, "Сводка", "Summary"), 19, R.color.text_main, true));
        addLine(box, AppPreferences.tr(this, "Создано", "Created"), FormatUtils.date(this, reminder.createdAt));
        addLine(box, AppPreferences.tr(this, "Последнее изменение", "Last change"), FormatUtils.date(this, reminder.updatedAt));
        addLine(box, AppPreferences.tr(this, "Досрочных погашений", "Early repayments"), String.valueOf(ReminderScheduler.historyCount(reminder, ReminderScheduler.HISTORY_EARLY)));
        addLine(box, AppPreferences.tr(this, "Рефинансирований", "Refinancings"), String.valueOf(ReminderScheduler.historyCount(reminder, ReminderScheduler.HISTORY_REFINANCE)));
        return card;
    }

    private void addLine(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, dp(10), 0, 0);
        parent.addView(row, rp);
        row.addView(text(label, 14, R.color.text_secondary, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(value, 14, R.color.text_main, true));
    }

    private View eventCard(ReminderScheduler.HistoryEvent event) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(14));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(box);
        String title = AppPreferences.tr(this, event.titleRu, event.titleEn);
        String details = AppPreferences.tr(this, event.detailsRu, event.detailsEn);
        box.addView(text(title, 17, R.color.text_main, true));
        TextView date = text(FormatUtils.date(this, event.time), 12, R.color.text_secondary, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.setMargins(0, dp(3), 0, details.isEmpty() ? 0 : dp(8));
        box.addView(date, dp);
        if (!details.isEmpty()) box.addView(text(details, 14, R.color.text_secondary, false));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cp);
        return card;
    }

    private TextView topText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setClickable(true); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(ContextCompat.getColor(this, color)); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
