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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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
        bar.setPadding(dp(4), 0, dp(10), 0);
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
        barTitle.setText(reminder.title);
        barTitle.setSingleLine(true);
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        barTitle.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(barTitle, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView type = text(FormatUtils.typeLabel(this, reminder.type), 14, R.color.primary, true);
        content.addView(type);

        TextView title = text(reminder.title, 28, R.color.text_main, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(3), 0, dp(16));
        content.addView(title, titleParams);

        content.addView(buildSummaryCard());

        TextView scheduleTitle = text(AppPreferences.tr(this, "График платежей", "Payment schedule"), 22, R.color.text_main, true);
        LinearLayout.LayoutParams scheduleTitleParams = new LinearLayout.LayoutParams(-1, -2);
        scheduleTitleParams.setMargins(0, dp(24), 0, dp(10));
        content.addView(scheduleTitle, scheduleTitleParams);

        int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
        for (int i = 0; i < reminder.months; i++) {
            content.addView(buildScheduleRow(i, nextIndex));
        }

        MaterialButton delete = new MaterialButton(this);
        delete.setText(AppPreferences.tr(this, "Удалить из моих платежей", "Delete payment plan"));
        delete.setAllCaps(false);
        delete.setTextColor(ContextCompat.getColor(this, R.color.danger));
        delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        delete.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.danger)));
        delete.setStrokeWidth(dp(1));
        delete.setCornerRadius(dp(14));
        delete.setOnClickListener(v -> confirmDelete());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-1, dp(54));
        deleteParams.setMargins(0, dp(20), 0, 0);
        content.addView(delete, deleteParams);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0);
            scroll.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
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
        addSummaryLine(box, AppPreferences.tr(this, "Срок", "Term"), formatTerm(reminder.months));
        addSummaryLine(box, AppPreferences.tr(this, "Первый платёж", "First payment"), FormatUtils.date(this, reminder.firstPaymentMillis));
        addSummaryLine(box, AppPreferences.tr(this, "Напоминание", "Reminder"),
                AppPreferences.isEnglish(this) ? reminder.daysBefore + " days before" : "за " + reminder.daysBefore + " дн.");
        return card;
    }

    private String formatTerm(int months) {
        if (months >= 12 && months % 12 == 0) {
            int years = months / 12;
            if (AppPreferences.isEnglish(this)) return years + (years == 1 ? " year" : " years");
            return years + " " + russianYears(years);
        }
        if (AppPreferences.isEnglish(this)) return months + (months == 1 ? " month" : " months");
        return months + " " + russianMonths(months);
    }

    private void addSummaryLine(LinearLayout parent, String label, String value) {
        parent.addView(text(label, 13, R.color.result_secondary, false));
        TextView valueView = text(value, 19, R.color.white, true);
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
        card.setStrokeColor(next ? ContextCompat.getColor(this, R.color.primary) : ContextCompat.getColor(this, R.color.border));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(row);

        TextView number = text(String.valueOf(index + 1), 15, next ? R.color.white : R.color.primary, true);
        number.setGravity(Gravity.CENTER);
        number.setBackgroundResource(next ? R.drawable.circle_primary : R.drawable.circle_soft);
        row.addView(number, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(0, -2, 1f);
        middleParams.setMargins(dp(12), 0, dp(8), 0);
        row.addView(middle, middleParams);

        middle.addView(text(FormatUtils.date(this, due.getTimeInMillis()), 16, R.color.text_main, true));
        String status = next
                ? AppPreferences.tr(this, "Ближайший платёж", "Next payment")
                : paid
                ? AppPreferences.tr(this, "Дата прошла", "Date passed")
                : AppPreferences.tr(this, "Предстоящий", "Upcoming");
        middle.addView(text(status, 12, R.color.text_secondary, false));
        row.addView(text(FormatUtils.money(this, reminder.amount), 15, R.color.text_main, true));

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

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, color));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
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

    private String russianMonths(int value) {
        int mod100 = value % 100;
        int mod10 = value % 10;
        if (mod100 >= 11 && mod100 <= 14) return "месяцев";
        if (mod10 == 1) return "месяц";
        if (mod10 >= 2 && mod10 <= 4) return "месяца";
        return "месяцев";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
