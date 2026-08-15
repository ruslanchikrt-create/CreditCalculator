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

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class PaymentsActivity extends AppCompatActivity {

    private LinearLayout listContainer;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPreferences.wrapLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderPayments();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(4), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView back = topButton("‹", 34);
        back.setContentDescription(AppPreferences.tr(this, "Назад", "Back"));
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView title = new TextView(this);
        title.setText(AppPreferences.tr(this, "Мои платежи", "My payments"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView add = topButton("+", 34);
        add.setContentDescription(AppPreferences.tr(this, "Добавить платёж", "Add payment"));
        add.setOnClickListener(v -> startActivity(new Intent(PaymentsActivity.this, AddReminderActivity.class)));
        bar.addView(add, new LinearLayout.LayoutParams(dp(64), dp(56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(listContainer, new ScrollView.LayoutParams(-1, -2));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0);
            scroll.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void renderPayments() {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        TextView heading = text(AppPreferences.tr(this, "Мои платежи", "My payments"), 28, R.color.text_main, true);
        listContainer.addView(heading);

        TextView subtitle = text(AppPreferences.tr(this,
                "Выберите кредит, чтобы посмотреть весь график платежей.",
                "Choose an item to view the full payment schedule."), 15, R.color.text_secondary, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.setMargins(0, dp(6), 0, dp(18));
        listContainer.addView(subtitle, subParams);

        List<ReminderScheduler.PaymentReminder> reminders = ReminderScheduler.load(this);
        if (reminders.isEmpty()) {
            TextView empty = text(AppPreferences.tr(this,
                    "Сохранённых платежей пока нет. Нажмите +, чтобы добавить первый.",
                    "No saved payments yet. Tap + to add the first one."), 17, R.color.text_secondary, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(70), dp(16), dp(70));
            listContainer.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        for (ReminderScheduler.PaymentReminder reminder : reminders) {
            listContainer.addView(createPaymentCard(reminder));
        }
    }

    private View createPaymentCard(ReminderScheduler.PaymentReminder reminder) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(18));
        card.setCardElevation(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentDetailsActivity.class);
            intent.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, reminder.id);
            startActivity(intent);
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(content);

        content.addView(text(FormatUtils.typeLabel(this, reminder.type), 13, R.color.primary, true));

        TextView title = text(reminder.title, 20, R.color.text_main, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(4), 0, dp(8));
        content.addView(title, titleParams);

        content.addView(text(AppPreferences.tr(this, "Сумма: ", "Amount: ")
                + FormatUtils.money(this, reminder.principal), 15, R.color.text_secondary, false));
        content.addView(text(AppPreferences.tr(this, "Ежемесячно: ", "Monthly: ")
                + FormatUtils.money(this, reminder.amount), 16, R.color.text_main, false));

        int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
        String nextText;
        if (nextIndex >= 0) {
            long nextDate = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, nextIndex).getTimeInMillis();
            nextText = AppPreferences.tr(this, "Следующий платёж: ", "Next payment: ") + FormatUtils.date(this, nextDate);
        } else {
            nextText = AppPreferences.tr(this, "Платежи завершены", "Payments completed");
        }
        TextView next = text(nextText, 14, R.color.text_secondary, false);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(-1, -2);
        nextParams.setMargins(0, dp(8), 0, 0);
        content.addView(next, nextParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        return card;
    }

    private TextView topButton(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        view.setBackgroundResource(android.R.drawable.list_selector_background);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, color));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
