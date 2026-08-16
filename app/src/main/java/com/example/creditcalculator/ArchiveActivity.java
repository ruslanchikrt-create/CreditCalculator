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

import androidx.appcompat.app.AlertDialog;
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
        if (!items.isEmpty()) listContainer.addView(archiveSummary(items));
        if (items.isEmpty()) {
            TextView empty = text(AppPreferences.tr(this, "Архив пока пуст.", "Archive is empty."), 17, R.color.text_secondary, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(70), 0, dp(70));
            listContainer.addView(empty);
            return;
        }
        for (ReminderScheduler.PaymentReminder item : items) listContainer.addView(card(item));
    }

    private View archiveSummary(List<ReminderScheduler.PaymentReminder> items){
        double original=0,benefit=0,overpayment=0,insurance=0,commissions=0;for(ReminderScheduler.PaymentReminder r:items){original+=r.baseAmount>0?r.baseAmount:ReminderScheduler.progressOriginalPrincipal(r);benefit+=ReminderScheduler.totalBenefit(r);if(!ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(r.type))){overpayment+=ReminderScheduler.totalOverpayment(r);insurance+=ReminderScheduler.totalInsuranceCosts(r);commissions+=ReminderScheduler.totalCommissionCosts(r);}}
        MaterialCardView card=new MaterialCardView(this);card.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));card.setRadius(dp(18));card.setStrokeColor(ContextCompat.getColor(this,R.color.border));card.setStrokeWidth(dp(1));LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(16),dp(18),dp(16));card.addView(b);
        TextView h=text(AppPreferences.tr(this,"Архивная сводка","Archive summary")+"  ⓘ",19,R.color.text_main,true);h.setClickable(true);h.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Архивная сводка","Archive summary")).setMessage(AppPreferences.tr(this,"Здесь сохраняются данные закрытых и вручную архивированных записей. Они не участвуют в активной сводке и активной «Вашей выгоде», но история и накопленная выгода не удаляются.","Closed and manually archived items are preserved here. They are excluded from active summary and active savings, while their history and accumulated savings remain stored.")).setPositiveButton(AppPreferences.tr(this,"Понятно","OK"),null).show());b.addView(h);
        b.addView(text(AppPreferences.tr(this,"Записей в архиве: ","Archived items: ")+items.size(),14,R.color.text_secondary,false));
        b.addView(text(AppPreferences.tr(this,"Исходная сумма архивных записей: ","Original archived amount: ")+FormatUtils.money(this,original),15,R.color.text_main,true));
        b.addView(text(AppPreferences.tr(this,"Переплата архивных кредитов: ","Archived-loan overpayment: ")+FormatUtils.money(this,overpayment),15,R.color.text_main,true));
        if(insurance>0)b.addView(text(AppPreferences.tr(this,"В том числе страховка: ","Including insurance: ")+FormatUtils.money(this,insurance),14,R.color.text_secondary,false));
        if(commissions>0)b.addView(text(AppPreferences.tr(this,"В том числе комиссии: ","Including fees: ")+FormatUtils.money(this,commissions),14,R.color.text_secondary,false));
        b.addView(text(AppPreferences.tr(this,"Сохранённая выгода: ","Saved benefit: ")+FormatUtils.money(this,benefit),17,benefit>=0?R.color.success:R.color.danger,true));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(16));card.setLayoutParams(lp);return card;
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
        box.addView(text(AppPreferences.tr(this, "Исходная сумма: ", "Original amount: ") + FormatUtils.money(this, reminder.baseAmount>0?reminder.baseAmount:ReminderScheduler.progressOriginalPrincipal(reminder)), 15, R.color.text_secondary, false));
        if(!ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(reminder.type)))box.addView(text(AppPreferences.tr(this,"Переплата: ","Overpayment: ")+FormatUtils.money(this,ReminderScheduler.totalOverpayment(reminder)),15,R.color.text_secondary,false));
        box.addView(text(AppPreferences.tr(this, "Сохранённая выгода: ", "Saved benefit: ") + FormatUtils.money(this, ReminderScheduler.totalBenefit(reminder)), 15, ReminderScheduler.totalBenefit(reminder)>=0?R.color.success:R.color.danger, true));
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
