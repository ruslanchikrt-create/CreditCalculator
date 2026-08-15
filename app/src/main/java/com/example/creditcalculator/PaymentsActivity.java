package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
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
        AppPreferences.applyNightMode(this);
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
        UiUtils.applyBackground(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(10), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView back = topText("‹", 34);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView title = topText(AppPreferences.tr(this, "Мои платежи", "My payments"), 20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView add = topText("+", 32);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));
        bar.addView(add, new LinearLayout.LayoutParams(dp(64), dp(58)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(listContainer, new ScrollView.LayoutParams(-1, -2));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void renderPayments() {
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
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18));
        card.setCardElevation(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openDetails(reminder));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(10), dp(16));
        card.addView(content);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(top, new LinearLayout.LayoutParams(-1, -2));

        TextView type = text(FormatUtils.typeLabel(this, reminder.type), 13, R.color.primary, true);
        top.addView(type, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView more = text("⋮", 28, R.color.text_main, true);
        more.setGravity(Gravity.CENTER);
        more.setClickable(true);
        more.setFocusable(true);
        more.setContentDescription(AppPreferences.tr(this, "Действия", "Actions"));
        more.setOnClickListener(v -> showActions(more, reminder));
        top.addView(more, new LinearLayout.LayoutParams(dp(52), dp(46)));

        TextView title = text(reminder.title, 20, R.color.text_main, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(2), 0, dp(8));
        content.addView(title, titleParams);

        content.addView(text(AppPreferences.tr(this, "Сумма: ", "Amount: ")
                + FormatUtils.money(this, reminder.principal), 15, R.color.text_secondary, false));

        content.addView(text(AppPreferences.tr(this, "Ежемесячно: ", "Monthly: ")
                + FormatUtils.money(this, reminder.amount), 16, R.color.text_main, false));

        if (!reminder.soundEnabled) {
            TextView muted = text(AppPreferences.tr(this, "🔇 Без звука", "🔇 Muted"), 13, R.color.text_secondary, false);
            LinearLayout.LayoutParams mutedParams = new LinearLayout.LayoutParams(-1, -2);
            mutedParams.setMargins(0, dp(5), 0, 0);
            content.addView(muted, mutedParams);
        }

        int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
        TextView next = text("", 14, R.color.text_secondary, false);
        if (nextIndex >= 0) {
            long nextDate = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, nextIndex).getTimeInMillis();
            next.setText(AppPreferences.tr(this, "Следующий платёж: ", "Next payment: ") + FormatUtils.date(this, nextDate));
        } else {
            next.setText(AppPreferences.tr(this, "Платежи завершены", "Payments completed"));
        }
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(-1, -2);
        nextParams.setMargins(0, dp(8), 0, 0);
        content.addView(next, nextParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void showActions(View anchor, ReminderScheduler.PaymentReminder reminder) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(reminder.soundEnabled
                ? AppPreferences.tr(this, "Без звука", "Mute")
                : AppPreferences.tr(this, "Включить звук", "Enable sound"));
        menu.getMenu().add(AppPreferences.tr(this, "В архив", "Archive"));
        menu.getMenu().add(AppPreferences.tr(this, "Удалить", "Delete"));
        menu.setOnMenuItemClickListener(item -> {
            int position = item.getItemId();
            String label = item.getTitle().toString();
            if (label.equals(AppPreferences.tr(this, "Без звука", "Mute"))
                    || label.equals(AppPreferences.tr(this, "Включить звук", "Enable sound"))) {
                ReminderScheduler.setMuted(this, reminder.id, reminder.soundEnabled);
            } else if (label.equals(AppPreferences.tr(this, "В архив", "Archive"))) {
                ReminderScheduler.archive(this, reminder.id);
            } else {
                ReminderScheduler.moveToTrash(this, reminder.id);
            }
            renderPayments();
            return true;
        });
        menu.show();
    }

    private void openDetails(ReminderScheduler.PaymentReminder reminder) {
        Intent intent = new Intent(this, PaymentDetailsActivity.class);
        intent.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, reminder.id);
        startActivity(intent);
    }

    private TextView topText(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
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
