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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PaymentsActivity extends AppCompatActivity {

    private LinearLayout listContainer;
    private String filterType;
    private String sortMode;

    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        filterType = AppPreferences.getPaymentsFilter(this);
        sortMode = AppPreferences.getPaymentsSort(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() { super.onResume(); renderPayments(); }

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

        listContainer.addView(text(AppPreferences.tr(this, "Мои платежи", "My payments"), 28, R.color.text_main, true));
        TextView subtitle = text(AppPreferences.tr(this,
                "Сначала показываются записи с ближайшим платежом.",
                "Items with the nearest payment are shown first."), 15, R.color.text_secondary, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.setMargins(0, dp(6), 0, dp(18));
        listContainer.addView(subtitle, subParams);

        List<ReminderScheduler.PaymentReminder> all = ReminderScheduler.load(this);
        listContainer.addView(createSummaryCard(all));
        listContainer.addView(buildControls(), controlsParams());

        List<ReminderScheduler.PaymentReminder> shown = filterAndSort(all);
        TextView listTitle = text(AppPreferences.tr(this, "Активные записи — ", "Active items — ") + shown.size(), 19, R.color.text_main, true);
        LinearLayout.LayoutParams listTitleParams = new LinearLayout.LayoutParams(-1, -2);
        listTitleParams.setMargins(0, dp(18), 0, dp(10));
        listContainer.addView(listTitle, listTitleParams);

        if (shown.isEmpty()) {
            TextView empty = text(AppPreferences.tr(this,
                    all.isEmpty() ? "Активных записей пока нет. Нажмите +, чтобы добавить первую." : "По выбранному фильтру записей нет.",
                    all.isEmpty() ? "No active items yet. Tap + to add the first one." : "No items match the selected filter."),
                    17, R.color.text_secondary, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(46), dp(16), dp(70));
            listContainer.addView(empty);
            return;
        }

        for (ReminderScheduler.PaymentReminder reminder : shown) listContainer.addView(createPaymentCard(reminder));
    }

    private View buildControls() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        MaterialButton filter = smallButton(filterLabel());
        filter.setOnClickListener(v -> showFilter(filter));
        row.addView(filter, new LinearLayout.LayoutParams(0, dp(48), 1f));

        MaterialButton sort = smallButton(sortLabel());
        LinearLayout.LayoutParams sortParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        sortParams.setMargins(dp(10), 0, 0, 0);
        row.addView(sort, sortParams);
        sort.setOnClickListener(v -> showSort(sort));
        return row;
    }

    private LinearLayout.LayoutParams controlsParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(16), 0, 0);
        return p;
    }

    private MaterialButton smallButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(ContextCompat.getColor(this, R.color.primary));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background)));
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.border)));
        b.setStrokeWidth(dp(1));
        b.setCornerRadius(dp(12));
        return b;
    }

    private String filterLabel() {
        String value;
        if (ReminderScheduler.TYPE_CREDIT.equals(filterType)) value = FormatUtils.typeLabel(this, ReminderScheduler.TYPE_CREDIT);
        else if (ReminderScheduler.TYPE_MORTGAGE.equals(filterType)) value = FormatUtils.typeLabel(this, ReminderScheduler.TYPE_MORTGAGE);
        else if (ReminderScheduler.TYPE_AUTO.equals(filterType)) value = FormatUtils.typeLabel(this, ReminderScheduler.TYPE_AUTO);
        else if (ReminderScheduler.TYPE_INSTALLMENT.equals(filterType)) value = FormatUtils.typeLabel(this, ReminderScheduler.TYPE_INSTALLMENT);
        else if (ReminderScheduler.TYPE_DEPOSIT.equals(filterType)) value = FormatUtils.typeLabel(this, ReminderScheduler.TYPE_DEPOSIT);
        else value = AppPreferences.tr(this, "Все", "All");
        return AppPreferences.tr(this, "Фильтр: ", "Filter: ") + value;
    }

    private String sortLabel() {
        return AppPreferences.tr(this, "Сортировка: ", "Sort: ") + sortName(sortMode);
    }

    private String sortName(String mode) {
        if ("created_new".equals(mode)) return AppPreferences.tr(this, "новые", "newest");
        if ("created_old".equals(mode)) return AppPreferences.tr(this, "старые", "oldest");
        if ("name".equals(mode)) return AppPreferences.tr(this, "название", "name");
        if ("debt".equals(mode)) return AppPreferences.tr(this, "остаток", "balance");
        if ("payment".equals(mode)) return AppPreferences.tr(this, "платёж", "payment");
        if ("rate".equals(mode)) return AppPreferences.tr(this, "ставка", "rate");
        if ("type".equals(mode)) return AppPreferences.tr(this, "тип", "type");
        return AppPreferences.tr(this, "ближайший платёж", "nearest payment");
    }

    private void showFilter(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        addFilterItem(menu, AppPreferences.tr(this, "Все", "All"), "all");
        addFilterItem(menu, FormatUtils.typeLabel(this, ReminderScheduler.TYPE_CREDIT), ReminderScheduler.TYPE_CREDIT);
        addFilterItem(menu, FormatUtils.typeLabel(this, ReminderScheduler.TYPE_MORTGAGE), ReminderScheduler.TYPE_MORTGAGE);
        addFilterItem(menu, FormatUtils.typeLabel(this, ReminderScheduler.TYPE_AUTO), ReminderScheduler.TYPE_AUTO);
        addFilterItem(menu, FormatUtils.typeLabel(this, ReminderScheduler.TYPE_INSTALLMENT), ReminderScheduler.TYPE_INSTALLMENT);
        addFilterItem(menu, FormatUtils.typeLabel(this, ReminderScheduler.TYPE_DEPOSIT), ReminderScheduler.TYPE_DEPOSIT);
        menu.show();
    }

    private void addFilterItem(PopupMenu menu, String label, String value) {
        android.view.MenuItem item = menu.getMenu().add(label);
        item.setOnMenuItemClickListener(clicked -> {
            filterType = value;
            AppPreferences.setPaymentsFilter(this, value);
            renderPayments();
            return true;
        });
    }

    private void showSort(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        addSortItem(menu, AppPreferences.tr(this, "Ближайший платёж", "Nearest payment"), "nearest");
        addSortItem(menu, AppPreferences.tr(this, "Дата создания — новые сначала", "Created — newest first"), "created_new");
        addSortItem(menu, AppPreferences.tr(this, "Дата создания — старые сначала", "Created — oldest first"), "created_old");
        addSortItem(menu, AppPreferences.tr(this, "Название А–Я", "Name A–Z"), "name");
        addSortItem(menu, AppPreferences.tr(this, "Остаток долга — больше сначала", "Balance — highest first"), "debt");
        addSortItem(menu, AppPreferences.tr(this, "Ежемесячный платёж — больше сначала", "Monthly payment — highest first"), "payment");
        addSortItem(menu, AppPreferences.tr(this, "Процентная ставка — больше сначала", "Rate — highest first"), "rate");
        addSortItem(menu, AppPreferences.tr(this, "По типу", "By type"), "type");
        menu.show();
    }

    private void addSortItem(PopupMenu menu, String label, String value) {
        android.view.MenuItem item = menu.getMenu().add(label);
        item.setOnMenuItemClickListener(clicked -> {
            sortMode = value;
            AppPreferences.setPaymentsSort(this, value);
            renderPayments();
            return true;
        });
    }

    private List<ReminderScheduler.PaymentReminder> filterAndSort(List<ReminderScheduler.PaymentReminder> source) {
        List<ReminderScheduler.PaymentReminder> result = new ArrayList<>();
        for (ReminderScheduler.PaymentReminder r : source) {
            if ("all".equals(filterType) || filterType == null || filterType.equals(ReminderScheduler.normalizeType(r.type))) result.add(r);
        }
        Comparator<ReminderScheduler.PaymentReminder> comparator;
        if ("created_new".equals(sortMode)) comparator = (a, b) -> Long.compare(b.createdAt, a.createdAt);
        else if ("created_old".equals(sortMode)) comparator = (a, b) -> Long.compare(a.createdAt, b.createdAt);
        else if ("name".equals(sortMode)) comparator = (a, b) -> a.title.compareToIgnoreCase(b.title);
        else if ("debt".equals(sortMode)) comparator = (a, b) -> Double.compare(ReminderScheduler.remainingDebt(b), ReminderScheduler.remainingDebt(a));
        else if ("payment".equals(sortMode)) comparator = (a, b) -> Double.compare(b.amount, a.amount);
        else if ("rate".equals(sortMode)) comparator = (a, b) -> Double.compare(b.annualRate, a.annualRate);
        else if ("type".equals(sortMode)) comparator = (a, b) -> {
            int byType = Integer.compare(typeOrder(a.type), typeOrder(b.type));
            return byType != 0 ? byType : Long.compare(ReminderScheduler.nextPaymentMillis(a), ReminderScheduler.nextPaymentMillis(b));
        };
        else comparator = (a, b) -> {
            boolean ad = ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(a.type));
            boolean bd = ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(b.type));
            if (ad != bd) return ad ? 1 : -1;
            int date = Long.compare(ReminderScheduler.nextPaymentMillis(a), ReminderScheduler.nextPaymentMillis(b));
            return date != 0 ? date : Long.compare(a.createdAt, b.createdAt);
        };
        Collections.sort(result, comparator);
        return result;
    }

    private int typeOrder(String type) {
        String t = ReminderScheduler.normalizeType(type);
        if (ReminderScheduler.TYPE_CREDIT.equals(t)) return 0;
        if (ReminderScheduler.TYPE_MORTGAGE.equals(t)) return 1;
        if (ReminderScheduler.TYPE_AUTO.equals(t)) return 2;
        if (ReminderScheduler.TYPE_INSTALLMENT.equals(t)) return 3;
        return 4;
    }

    private View createSummaryCard(List<ReminderScheduler.PaymentReminder> reminders) {
        double totalDebt = 0.0, dueThisMonth = 0.0;
        double totalInterest = 0.0, paidInterest = 0.0, remainingInterest = 0.0;
        double depositPrincipal = 0.0, depositIncome = 0.0, depositFinal = 0.0;
        boolean hasDebt = false, hasDeposit = false;

        for (ReminderScheduler.PaymentReminder r : reminders) {
            if (ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(r.type))) {
                hasDeposit = true;
                depositPrincipal += r.principal;
                depositIncome += ReminderScheduler.depositExpectedIncome(r);
                depositFinal += ReminderScheduler.depositFinalAmount(r);
            } else {
                hasDebt = true;
                totalDebt += ReminderScheduler.remainingDebt(r);
                dueThisMonth += ReminderScheduler.dueThisMonth(r);
                totalInterest += ReminderScheduler.totalInterest(r);
                paidInterest += ReminderScheduler.paidInterest(r);
                remainingInterest += ReminderScheduler.remainingInterest(r);
            }
        }

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18)); card.setStrokeColor(ContextCompat.getColor(this, R.color.border)); card.setStrokeWidth(dp(1)); card.setCardElevation(dp(1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18), dp(16), dp(18), dp(18)); card.addView(box);
        box.addView(text(AppPreferences.tr(this, "Общая сводка", "Summary"), 19, R.color.text_main, true));

        TextView debtLabel = text(AppPreferences.tr(this, "Общий остаток долга", "Total remaining debt"), 13, R.color.text_secondary, false);
        LinearLayout.LayoutParams debtLabelParams = new LinearLayout.LayoutParams(-1, -2); debtLabelParams.setMargins(0, dp(14), 0, dp(2)); box.addView(debtLabel, debtLabelParams);
        box.addView(text(FormatUtils.money(this, totalDebt), 25, R.color.text_main, true));
        TextView monthLabel = text(AppPreferences.tr(this, "К оплате в этом месяце", "Due this month"), 13, R.color.text_secondary, false);
        LinearLayout.LayoutParams monthLabelParams = new LinearLayout.LayoutParams(-1, -2); monthLabelParams.setMargins(0, dp(12), 0, dp(2)); box.addView(monthLabel, monthLabelParams);
        box.addView(text(FormatUtils.money(this, dueThisMonth), 21, R.color.primary, true));

        if (hasDebt) {
            addDivider(box);
            box.addView(text(AppPreferences.tr(this, "Переплата по активным кредитам", "Active loan interest"), 17, R.color.text_main, true));
            addSummaryLine(box, AppPreferences.tr(this, "Общая переплата", "Total interest"), totalInterest);
            addSummaryLine(box, AppPreferences.tr(this, "Уже выплачено процентов", "Interest already paid"), paidInterest);
            addSummaryLine(box, AppPreferences.tr(this, "Осталось выплатить процентов", "Interest remaining"), remainingInterest);
        }
        if (hasDeposit) {
            addDivider(box);
            box.addView(text(AppPreferences.tr(this, "Активные вклады", "Active deposits"), 17, R.color.text_main, true));
            addSummaryLine(box, AppPreferences.tr(this, "Во вкладах", "Deposited"), depositPrincipal);
            addSummaryLine(box, AppPreferences.tr(this, "Ожидаемый доход", "Expected income"), depositIncome);
            addSummaryLine(box, AppPreferences.tr(this, "Итого к получению", "Expected total"), depositFinal);
        }
        return card;
    }

    private void addDivider(LinearLayout box) {
        View divider = new View(this); divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1)); p.setMargins(0, dp(16), 0, dp(14)); box.addView(divider, p);
    }

    private void addSummaryLine(LinearLayout parent, String label, double value) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.setMargins(0, dp(9), 0, 0); parent.addView(row, rp);
        row.addView(text(label, 14, R.color.text_secondary, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(FormatUtils.money(this, value), 15, R.color.text_main, true));
    }

    private View createPaymentCard(ReminderScheduler.PaymentReminder reminder) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background)); card.setRadius(dp(18)); card.setCardElevation(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border)); card.setStrokeWidth(dp(1)); card.setClickable(true); card.setFocusable(true);
        card.setOnClickListener(v -> openDetails(reminder));

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(18), dp(14), dp(10), dp(16)); card.addView(content);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); content.addView(top);
        top.addView(text(FormatUtils.typeLabel(this, reminder.type), 13, R.color.primary, true), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView more = text("⋮", 28, R.color.text_main, true); more.setGravity(Gravity.CENTER); more.setOnClickListener(v -> showActions(more, reminder));
        top.addView(more, new LinearLayout.LayoutParams(dp(52), dp(46)));

        LinearLayout titleRow = new LinearLayout(this); titleRow.setOrientation(LinearLayout.HORIZONTAL); titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trp = new LinearLayout.LayoutParams(-1, -2); trp.setMargins(0, dp(2), 0, dp(8)); content.addView(titleRow, trp);
        TextView title = text(reminder.title, 20, R.color.text_main, true); titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView created = text(AppPreferences.tr(this, "Создано: ", "Created: ") + FormatUtils.date(this, reminder.createdAt), 11, R.color.text_secondary, false);
        created.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); titleRow.addView(created, new LinearLayout.LayoutParams(-2, -2));

        boolean deposit = ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(reminder.type));
        if (deposit) {
            content.addView(text(AppPreferences.tr(this, "Сумма вклада: ", "Deposit: ") + FormatUtils.money(this, reminder.principal), 15, R.color.text_secondary, false));
            content.addView(text(AppPreferences.tr(this, "Ожидаемый доход: ", "Expected income: ") + FormatUtils.money(this, ReminderScheduler.depositExpectedIncome(reminder)), 16, R.color.text_main, false));
            content.addView(text(AppPreferences.tr(this, "Итого: ", "Total: ") + FormatUtils.money(this, ReminderScheduler.depositFinalAmount(reminder)), 15, R.color.text_secondary, false));
        } else {
            content.addView(text(AppPreferences.tr(this, "Исходная сумма: ", "Original amount: ") + FormatUtils.money(this, reminder.baseAmount), 15, R.color.text_secondary, false));
            content.addView(text(AppPreferences.tr(this, "Остаток долга: ", "Remaining debt: ") + FormatUtils.money(this, ReminderScheduler.remainingDebt(reminder)), 16, R.color.text_main, true));
            content.addView(text(AppPreferences.tr(this, "Ежемесячно: ", "Monthly: ") + FormatUtils.money(this, reminder.amount), 16, R.color.text_main, false));
            content.addView(text(AppPreferences.tr(this, "Переплата: ", "Interest: ") + FormatUtils.money(this, ReminderScheduler.totalInterest(reminder)), 13, R.color.text_secondary, false));
        }
        if (!reminder.soundEnabled) content.addView(text(AppPreferences.tr(this, "🔇 Без звука", "🔇 Muted"), 13, R.color.text_secondary, false));

        int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
        TextView next = text("", 14, R.color.text_secondary, false);
        if (nextIndex >= 0) {
            long nextDate = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, nextIndex).getTimeInMillis();
            next.setText((deposit ? AppPreferences.tr(this, "Ближайшая дата: ", "Next date: ") : AppPreferences.tr(this, "Следующий платёж: ", "Next payment: ")) + FormatUtils.date(this, nextDate));
        } else next.setText(AppPreferences.tr(this, "Срок завершён", "Term completed"));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2); np.setMargins(0, dp(8), 0, 0); content.addView(next, np);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, 0, 0, dp(12)); card.setLayoutParams(cp);
        return card;
    }

    private void showActions(View anchor, ReminderScheduler.PaymentReminder reminder) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String mute = reminder.soundEnabled ? AppPreferences.tr(this, "Без звука", "Mute") : AppPreferences.tr(this, "Включить звук", "Enable sound");
        String history = AppPreferences.tr(this, "История изменений", "Change history");
        String archive = AppPreferences.tr(this, "В архив", "Archive");
        String delete = AppPreferences.tr(this, "Удалить", "Delete");
        menu.getMenu().add(mute); menu.getMenu().add(history); menu.getMenu().add(archive); menu.getMenu().add(delete);
        menu.setOnMenuItemClickListener(item -> {
            String label = item.getTitle().toString();
            if (label.equals(mute)) ReminderScheduler.setMuted(this, reminder.id, reminder.soundEnabled);
            else if (label.equals(history)) {
                Intent intent = new Intent(this, HistoryActivity.class); intent.putExtra(HistoryActivity.EXTRA_REMINDER_ID, reminder.id); startActivity(intent); return true;
            } else if (label.equals(archive)) ReminderScheduler.archive(this, reminder.id);
            else ReminderScheduler.moveToTrash(this, reminder.id);
            renderPayments(); return true;
        });
        menu.show();
    }

    private void openDetails(ReminderScheduler.PaymentReminder reminder) {
        Intent intent = new Intent(this, PaymentDetailsActivity.class); intent.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID, reminder.id); startActivity(intent);
    }

    private TextView topText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setClickable(true); v.setFocusable(true); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(ContextCompat.getColor(this, color)); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
