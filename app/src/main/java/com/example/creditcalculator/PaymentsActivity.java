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

import com.google.android.material.button.MaterialButton;
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
        bar.setPadding(dp(8), 0, dp(10), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(64)));

        MaterialButton back = topButton("‹");
        back.setTextSize(32);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView title = new TextView(this);
        title.setText(AppPreferences.tr(this, "Мои платежи", "My payments"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        MaterialButton add = topButton("+");
        add.setTextSize(28);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));
        bar.addView(add, new LinearLayout.LayoutParams(dp(54), dp(54)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(listContainer, new ScrollView.LayoutParams(-1, -2));

        return root;
    }

    private void renderPayments() {
        listContainer.removeAllViews();

        TextView heading = new TextView(this);
        heading.setText(AppPreferences.tr(this, "Мои платежи", "My payments"));
        heading.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        heading.setTextSize(28);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        listContainer.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText(AppPreferences.tr(this,
                "Выберите кредит, чтобы посмотреть весь график платежей.",
                "Choose an item to view the full payment schedule."));
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        subtitle.setTextSize(15);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.setMargins(0, dp(6), 0, dp(18));
        listContainer.addView(subtitle, subParams);

        List<ReminderScheduler.PaymentReminder> reminders = ReminderScheduler.load(this);
        if (reminders.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(AppPreferences.tr(this,
                    "Сохранённых платежей пока нет. Нажмите +, чтобы добавить первый.",
                    "No saved payments yet. Tap + to add the first one."));
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(17);
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

        TextView type = new TextView(this);
        type.setText(FormatUtils.typeLabel(this, reminder.type));
        type.setTextColor(ContextCompat.getColor(this, R.color.primary));
        type.setTextSize(13);
        type.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(type);

        TextView title = new TextView(this);
        title.setText(reminder.title);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(4), 0, dp(8));
        content.addView(title, titleParams);

        TextView principal = new TextView(this);
        principal.setText(AppPreferences.tr(this, "Сумма: ", "Amount: ") + FormatUtils.money(this, reminder.principal));
        principal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        principal.setTextSize(15);
        content.addView(principal);

        TextView payment = new TextView(this);
        payment.setText(AppPreferences.tr(this, "Ежемесячно: ", "Monthly: ") + FormatUtils.money(this, reminder.amount));
        payment.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        payment.setTextSize(16);
        content.addView(payment);

        int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
        TextView next = new TextView(this);
        if (nextIndex >= 0) {
            long nextDate = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, nextIndex).getTimeInMillis();
            next.setText(AppPreferences.tr(this, "Следующий платёж: ", "Next payment: ") + FormatUtils.date(this, nextDate));
        } else {
            next.setText(AppPreferences.tr(this, "Платежи завершены", "Payments completed"));
        }
        next.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        next.setTextSize(14);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(-1, -2);
        nextParams.setMargins(0, dp(8), 0, 0);
        content.addView(next, nextParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        return card;
    }

    private MaterialButton topButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
