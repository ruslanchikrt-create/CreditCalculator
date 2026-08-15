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

public class ArchiveActivity extends AppCompatActivity {

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
        render();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        UiUtils.applyBackground(this, root);

        LinearLayout bar = topBar(root, AppPreferences.tr(this, "Архив", "Archive"));
        TextView back = topText("‹", 34);
        back.setOnClickListener(v -> finish());
        bar.addView(back, 0, new LinearLayout.LayoutParams(dp(54), dp(54)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(listContainer, new ScrollView.LayoutParams(-1, -2));

        applyInsets(root);
        return root;
    }

    private void render() {
        listContainer.removeAllViews();
        listContainer.addView(text(AppPreferences.tr(this, "Архив", "Archive"), 28, R.color.text_main, true));
        TextView subtitle = text(AppPreferences.tr(this,
                "Здесь хранятся завершённые или перенесённые в архив кредиты.",
                "Completed or archived payment plans are stored here."), 15, R.color.text_secondary, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(6), 0, dp(18));
        listContainer.addView(subtitle, sp);

        List<ReminderScheduler.PaymentReminder> items = ReminderScheduler.listByStatus(this, ReminderScheduler.STATUS_ARCHIVE);
        if (items.isEmpty()) {
            TextView empty = text(AppPreferences.tr(this, "Архив пока пуст.", "Archive is empty."), 17, R.color.text_secondary, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(70), 0, dp(70));
            listContainer.addView(empty);
            return;
        }
        for (ReminderScheduler.PaymentReminder item : items) listContainer.addView(card(item));
    }

    private View card(ReminderScheduler.PaymentReminder reminder) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setCardElevation(dp(1));
        card.setOnClickListener(v -> openDetails(reminder));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(14), dp(10), dp(16));
        card.addView(box);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top, new LinearLayout.LayoutParams(-1, -2));
        top.addView(text(FormatUtils.typeLabel(this, reminder.type), 13, R.color.primary, true), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView more = text("⋮", 28, R.color.text_main, true);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> actions(more, reminder));
        top.addView(more, new LinearLayout.LayoutParams(dp(52), dp(46)));

        box.addView(text(reminder.title, 20, R.color.text_main, true));
        box.addView(text(AppPreferences.tr(this, "Сумма: ", "Amount: ") + FormatUtils.money(this, reminder.principal), 15, R.color.text_secondary, false));
        box.addView(text(AppPreferences.tr(this, "Срок: ", "Term: ") + UiUtils.termText(this, reminder.months), 15, R.color.text_secondary, false));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private void actions(View anchor, ReminderScheduler.PaymentReminder reminder) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String restore = AppPreferences.tr(this, "Вернуть в мои платежи", "Restore to payments");
        String delete = AppPreferences.tr(this, "Удалить", "Delete");
        menu.getMenu().add(restore);
        menu.getMenu().add(delete);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals(restore)) ReminderScheduler.restoreFromArchive(this, reminder.id);
            else ReminderScheduler.moveToTrash(this, reminder.id);
            render();
            return true;
        });
        menu.show();
    }

    private void openDetails(ReminderScheduler.PaymentReminder reminder) {
        Intent intent = new Intent(this, PaymentDetailsActivity.class);
        intent.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, reminder.id);
        startActivity(intent);
    }

    private LinearLayout topBar(LinearLayout root, String title) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(10), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView titleView = topText(title, 20);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, -1, 1f));
        return bar;
    }

    private TextView topText(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(ContextCompat.getColor(this, color));
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
