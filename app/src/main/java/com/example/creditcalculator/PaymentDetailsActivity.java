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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;

public class PaymentDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "reminder_id";
    private ReminderScheduler.PaymentReminder reminder;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPreferences.wrapLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_REMINDER_ID, -1L);
        reminder = ReminderScheduler.findById(this, id);
        if (reminder == null) {
            Toast.makeText(this, AppPreferences.tr(this, "Платёж не найден", "Payment not found"), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(buildContent());
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

        TextView barTitle = new TextView(this);
        barTitle.setText(reminder.title);
        barTitle.setSingleLine(true);
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        bar.addView(barTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView type = new TextView(this);
        type.setText(FormatUtils.typeLabel(this, reminder.type));
        type.setTextColor(ContextCompat.getColor(this, R.color.primary));
        type.setTextSize(14);
        type.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(type);

        TextView title = new TextView(this);
        title.setText(reminder.title);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(3), 0, dp(16));
        content.addView(title, titleParams);

        content.addView(buildSummaryCard());

        TextView scheduleTitle = new TextView(this);
        scheduleTitle.setText(AppPreferences.tr(this, "График платежей", "Payment schedule"));
        scheduleTitle.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        scheduleTitle.setTextSize(22);
        scheduleTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams scheduleTitleParams = new LinearLayout.LayoutParams(-1, -2);
        scheduleTitleParams.setMargins(0, dp(24), 0, dp(10));
        content.addView(scheduleTitle, scheduleTitleParams);

        int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
        for (int i = 0; i < reminder.months; i++) {
            content.addView(buildScheduleRow(i, nextIndex));
        }

        MaterialButton delete = new MaterialButton(this);
        delete.setText(AppPreferences.tr(this, "Удалить из моих платежей", "Delete payment plan"));
        delete.setTextColor(ContextCompat.getColor(this, R.color.danger));
        delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        delete.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.danger)));
        delete.setStrokeWidth(dp(1));
        delete.setCornerRadius(dp(14));
        delete.setOnClickListener(v -> confirmDelete());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-1, dp(54));
        deleteParams.setMargins(0, dp(20), 0, 0);
        content.addView(delete, deleteParams);

        return root;
    }

    private View buildSummaryCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.result_card));
        card.setRadius(dp(18));
        card.setCardElevation(dp(1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(box);

        addSummaryLine(box, AppPreferences.tr(this, "Сумма, которую взяли", "Amount borrowed"), FormatUtils.money(this, reminder.principal));
        addSummaryLine(box, AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), FormatUtils.money(this, reminder.amount));
        if (reminder.annualRate > 0) {
            addSummaryLine(box, AppPreferences.tr(this, "Ставка", "Interest rate"), trimRate(reminder.annualRate) + "%");
        }
        int years = reminder.months / 12;
        addSummaryLine(box, AppPreferences.tr(this, "Срок", "Term"),
                AppPreferences.isEnglish(this) ? years + (years == 1 ? " year" : " years") : years + " " + russianYears(years));
        addSummaryLine(box, AppPreferences.tr(this, "Первый платёж", "First payment"), FormatUtils.date(this, reminder.firstPaymentMillis));
        addSummaryLine(box, AppPreferences.tr(this, "Напоминание", "Reminder"),
                AppPreferences.isEnglish(this) ? reminder.daysBefore + " days before" : "за " + reminder.daysBefore + " дн.");

        return card;
    }

    private void addSummaryLine(LinearLayout parent, String label, String value) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.result_secondary));
        labelView.setTextSize(13);
        parent.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(19);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
        valueParams.setMargins(0, dp(2), 0, dp(12));
        parent.addView(valueView, valueParams);
    }

    private View buildScheduleRow(int index, int nextIndex) {
        Calendar due = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, index);
        boolean paid = nextIndex < 0 || index < nextIndex;
        boolean next = index == nextIndex;

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(13));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(next
                ? ContextCompat.getColor(this, R.color.primary)
                : ContextCompat.getColor(this, R.color.border));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(row);

        TextView number = new TextView(this);
        number.setText(String.valueOf(index + 1));
        number.setGravity(Gravity.CENTER);
        number.setTextColor(next ? Color.WHITE : ContextCompat.getColor(this, R.color.primary));
        number.setTextSize(15);
        number.setTypeface(null, android.graphics.Typeface.BOLD);
        number.setBackgroundResource(next ? R.drawable.circle_primary : R.drawable.circle_soft);
        row.addView(number, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(0, -2, 1f);
        middleParams.setMargins(dp(12), 0, dp(8), 0);
        row.addView(middle, middleParams);

        TextView date = new TextView(this);
        date.setText(FormatUtils.date(this, due.getTimeInMillis()));
        date.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        date.setTextSize(16);
        date.setTypeface(null, android.graphics.Typeface.BOLD);
        middle.addView(date);

        TextView status = new TextView(this);
        status.setText(next
                ? AppPreferences.tr(this, "Ближайший платёж", "Next payment")
                : paid
                ? AppPreferences.tr(this, "Дата прошла", "Date passed")
                : AppPreferences.tr(this, "Предстоящий", "Upcoming"));
        status.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        status.setTextSize(12);
        middle.addView(status);

        TextView amount = new TextView(this);
        amount.setText(FormatUtils.money(this, reminder.amount));
        amount.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        amount.setTextSize(15);
        amount.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(amount);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Удалить платёж?", "Delete payment plan?"))
                .setMessage(AppPreferences.tr(this,
                        "Будут удалены график и все будущие напоминания.",
                        "The schedule and all future reminders will be removed."))
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Удалить", "Delete"), (dialog, which) -> {
                    ReminderScheduler.delete(this, reminder.id);
                    finish();
                })
                .show();
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

    private String trimRate(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value).replace('.', ',');
    }

    private String russianYears(int value) {
        int mod100 = value % 100;
        int mod10 = value % 10;
        if (mod100 >= 11 && mod100 <= 14) return "лет";
        if (mod10 == 1) return "год";
        if (mod10 >= 2 && mod10 <= 4) return "года";
        return "лет";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
