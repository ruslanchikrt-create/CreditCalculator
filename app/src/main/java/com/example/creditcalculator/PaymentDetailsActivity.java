package com.example.creditcalculator;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
    private long renderedUpdatedAt;

    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        long id = getIntent().getLongExtra(EXTRA_REMINDER_ID, -1L);
        reminder = ReminderScheduler.findById(this, id);
        if (reminder == null) {
            Toast.makeText(this, AppPreferences.tr(this, "Запись не найдена", "Item not found"), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        renderedUpdatedAt = reminder.updatedAt;
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reminder == null) return;
        ReminderScheduler.PaymentReminder latest = ReminderScheduler.findById(this, reminder.id);
        if (latest == null) { finish(); return; }
        if (renderedUpdatedAt != latest.updatedAt) {
            reminder = latest;
            recreate();
        }
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        UiUtils.applyBackground(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(6), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView back = topText("‹", 34);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView barTitle = topText(reminder.title, 20);
        barTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        barTitle.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(barTitle, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView more = topText("⋮", 28);
        more.setOnClickListener(v -> showTopMenu(more));
        bar.addView(more, new LinearLayout.LayoutParams(dp(52), dp(56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        content.addView(text(FormatUtils.typeLabel(this, reminder.type), 14, R.color.primary, true));
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleRowParams = new LinearLayout.LayoutParams(-1, -2);
        titleRowParams.setMargins(0, dp(3), 0, dp(4));
        content.addView(titleRow, titleRowParams);

        TextView title = text(reminder.title, 28, R.color.text_main, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        if (!ReminderScheduler.STATUS_TRASH.equals(reminder.status)) {
            TextView edit = text("✎", 25, R.color.primary, false);
            edit.setGravity(Gravity.CENTER);
            edit.setClickable(true);
            edit.setFocusable(true);
            edit.setContentDescription(AppPreferences.tr(this, "Редактировать", "Edit"));
            edit.setOnClickListener(v -> editReminder());
            titleRow.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }

        TextView created = text(AppPreferences.tr(this, "Создано: ", "Created: ") + FormatUtils.date(this, reminder.createdAt), 12, R.color.text_secondary, false);
        LinearLayout.LayoutParams createdParams = new LinearLayout.LayoutParams(-1, -2);
        createdParams.setMargins(0, 0, 0, dp(16));
        content.addView(created, createdParams);

        content.addView(buildSummaryCard());

        boolean deposit = isDeposit();
        if (!deposit && ReminderScheduler.STATUS_ACTIVE.equals(reminder.status)) {
            content.addView(buildLoanActions(), sectionParams());
        }

        TextView scheduleTitle = text(deposit
                ? AppPreferences.tr(this, "Срок вклада", "Deposit term")
                : AppPreferences.tr(this, "График платежей", "Payment schedule"), 22, R.color.text_main, true);
        LinearLayout.LayoutParams scheduleTitleParams = new LinearLayout.LayoutParams(-1, -2);
        scheduleTitleParams.setMargins(0, dp(24), 0, dp(10));
        content.addView(scheduleTitle, scheduleTitleParams);

        if (deposit) content.addView(buildDepositPeriodCard());
        else {
            int nextIndex = ReminderScheduler.nextPaymentIndex(reminder);
            for (int i = 0; i < reminder.months; i++) content.addView(buildScheduleRow(i, nextIndex));
        }

        if (!ReminderScheduler.STATUS_TRASH.equals(reminder.status)) {
            MaterialButton delete = outlineButton(AppPreferences.tr(this, "Удалить в корзину", "Move to trash"), R.color.danger);
            delete.setOnClickListener(v -> confirmDelete());
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-1, dp(54));
            deleteParams.setMargins(0, dp(20), 0, 0);
            content.addView(delete, deleteParams);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0);
            scroll.setPadding(0, 0, 0, bars.bottom + dp(8));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void showTopMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String history = AppPreferences.tr(this, "История изменений", "Change history");
        menu.getMenu().add(history);
        menu.setOnMenuItemClickListener(item -> {
            openHistory();
            return true;
        });
        menu.show();
    }

    private void openHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra(HistoryActivity.EXTRA_REMINDER_ID, reminder.id);
        startActivity(intent);
    }

    private void editReminder() {
        Intent intent = new Intent(this, AddReminderActivity.class);
        intent.putExtra(AddReminderActivity.EXTRA_EDIT_ID, reminder.id);
        startActivity(intent);
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

        if (isDeposit()) {
            addSummaryLine(box, AppPreferences.tr(this, "Сумма вклада", "Deposit amount"), FormatUtils.money(this, reminder.principal));
            addSummaryLine(box, AppPreferences.tr(this, "Ставка", "Interest rate"), trimRate(reminder.annualRate) + "%");
            addSummaryLine(box, AppPreferences.tr(this, "Ожидаемый доход", "Expected income"), FormatUtils.money(this, ReminderScheduler.depositExpectedIncome(reminder)));
            addSummaryLine(box, AppPreferences.tr(this, "Итого к получению", "Expected total"), FormatUtils.money(this, ReminderScheduler.depositFinalAmount(reminder)));
            addSummaryLine(box, AppPreferences.tr(this, "Срок", "Term"), UiUtils.termText(this, reminder.months));
            addSummaryLine(box, AppPreferences.tr(this, "Дата открытия", "Start date"), FormatUtils.date(this, reminder.firstPaymentMillis));
        } else {
            addSummaryLine(box, AppPreferences.tr(this, "Исходная сумма", "Original amount"), FormatUtils.money(this, reminder.baseAmount));
            if (reminder.downPayment > 0) addSummaryLine(box, AppPreferences.tr(this, "Первоначальный взнос", "Down payment"), FormatUtils.money(this, reminder.downPayment));
            if (reminder.insurance > 0) addSummaryLine(box, AppPreferences.tr(this, "Страховка", "Insurance"), FormatUtils.money(this, reminder.insurance));
            addSummaryLine(box, AppPreferences.tr(this, "Текущая сумма кредита", "Current financed amount"), FormatUtils.money(this, reminder.principal));
            addSummaryLine(box, AppPreferences.tr(this, "Остаток долга", "Remaining debt"), FormatUtils.money(this, ReminderScheduler.remainingDebt(reminder)));
            addSummaryLine(box, AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), FormatUtils.money(this, reminder.amount));
            addSummaryLine(box, AppPreferences.tr(this, "Ставка", "Interest rate"), trimRate(reminder.annualRate) + "%");
            addSummaryLine(box, AppPreferences.tr(this, "Срок текущего графика", "Current schedule term"), UiUtils.termText(this, reminder.months));
            addSummaryLine(box, AppPreferences.tr(this, "Первый платёж текущего графика", "First payment of current schedule"), FormatUtils.date(this, reminder.firstPaymentMillis));

            addSmallResultLine(box, AppPreferences.tr(this, "Общая переплата по кредиту", "Total loan interest"), ReminderScheduler.totalInterest(reminder));
            addSmallResultLine(box, AppPreferences.tr(this, "Уже выплачено процентов", "Interest already paid"), ReminderScheduler.paidInterest(reminder));
            addSmallResultLine(box, AppPreferences.tr(this, "Осталось выплатить процентов", "Interest remaining"), ReminderScheduler.remainingInterest(reminder));
        }

        addSummaryLine(box, AppPreferences.tr(this, "Напоминание", "Reminder"),
                AppPreferences.isEnglish(this) ? reminder.daysBefore + " days before" : "за " + reminder.daysBefore + " дн.");
        if (!reminder.soundEnabled) addSummaryLine(box, AppPreferences.tr(this, "Звук", "Sound"), AppPreferences.tr(this, "Без звука", "Muted"));
        return card;
    }

    private void addSmallResultLine(LinearLayout parent, String label, double value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(row, rp);
        TextView left = text(label, 13, R.color.result_secondary, false);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView right = text(FormatUtils.money(this, value), 14, R.color.white, true);
        row.addView(right);
    }

    private View buildLoanActions() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        card.setRadius(dp(18));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(box);
        box.addView(text(AppPreferences.tr(this, "Изменить условия кредита", "Loan actions"), 18, R.color.text_main, true));

        MaterialButton early = outlineButton(AppPreferences.tr(this, "Досрочное погашение", "Early repayment"), R.color.primary);
        early.setOnClickListener(v -> showEarlyRepaymentInput());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(52)); ep.setMargins(0, dp(12), 0, 0); box.addView(early, ep);

        String type = ReminderScheduler.normalizeType(reminder.type);
        if (!ReminderScheduler.TYPE_INSTALLMENT.equals(type)) {
            MaterialButton refinance = outlineButton(AppPreferences.tr(this, "Рефинансирование", "Refinancing"), R.color.primary);
            refinance.setOnClickListener(v -> showRefinanceInput());
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(52)); rp.setMargins(0, dp(10), 0, 0); box.addView(refinance, rp);
        }
        return card;
    }

    private LinearLayout.LayoutParams sectionParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(18), 0, 0);
        return p;
    }

    private void showEarlyRepaymentInput() {
        final Calendar actionDate = Calendar.getInstance();
        LinearLayout box = dialogBox();
        EditText amount = dialogInput(AppPreferences.tr(this, "Сумма досрочного погашения, ₽", "Early repayment amount, ₽"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount, dialogFieldParams());
        EditText date = dialogInput(AppPreferences.tr(this, "Дата досрочного погашения", "Early repayment date"), InputType.TYPE_NULL);
        date.setFocusable(false); date.setClickable(true); date.setText(FormatUtils.date(this, actionDate.getTimeInMillis()));
        date.setOnClickListener(v -> pickDate(actionDate, date));
        box.addView(date, dialogFieldParams());

        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Досрочное погашение", "Early repayment"))
                .setMessage(AppPreferences.tr(this, "Сначала рассчитаем оба варианта. Данные кредита не изменятся до нажатия «Применить».", "Both options will be simulated first. Nothing changes until you tap Apply."))
                .setView(box)
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Рассчитать", "Calculate"), (d, w) -> {
                    try {
                        double value = parseDouble(amount.getText().toString());
                        ReminderScheduler.EarlyRepaymentSimulation s = ReminderScheduler.simulateEarlyRepayment(reminder, actionDate.getTimeInMillis(), value);
                        showEarlySimulation(s, actionDate.getTimeInMillis());
                    } catch (Exception e) {
                        Toast.makeText(this, AppPreferences.tr(this, "Проверьте сумму и дату", "Check amount and date"), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void showEarlySimulation(ReminderScheduler.EarlyRepaymentSimulation s, long date) {
        if (s.newBalance <= 0.01) {
            new AlertDialog.Builder(this)
                    .setTitle(AppPreferences.tr(this, "Полное погашение", "Full repayment"))
                    .setMessage(AppPreferences.tr(this, "После внесения этой суммы долг будет погашен полностью.", "This payment will pay off the loan in full."))
                    .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                    .setPositiveButton(AppPreferences.tr(this, "Применить", "Apply"), (d, w) -> {
                        ReminderScheduler.applyEarlyRepayment(this, reminder.id, date, s.prepayment, true);
                        refreshAfterAction(AppPreferences.tr(this, "Досрочное погашение применено", "Early repayment applied"));
                    }).show();
            return;
        }

        long endPayment = ReminderScheduler.buildDueDate(s.firstFuturePayment, Math.max(0, s.remainingMonths - 1)).getTimeInMillis();
        long endTerm = ReminderScheduler.buildDueDate(s.firstFuturePayment, Math.max(0, s.reducedMonths - 1)).getTimeInMillis();
        String optionPayment = AppPreferences.tr(this,
                "Уменьшить ежемесячный платёж\nНовый платёж: ",
                "Reduce monthly payment\nNew payment: ") + FormatUtils.money(this, s.reducedPayment)
                + AppPreferences.tr(this, "\nСрок до: ", "\nEnd date: ") + FormatUtils.date(this, endPayment)
                + AppPreferences.tr(this, "\nОстаток процентов: ", "\nRemaining interest: ") + FormatUtils.money(this, s.interestWithReducedPayment)
                + AppPreferences.tr(this, "\nЭкономия: ", "\nSavings: ") + FormatUtils.money(this, s.savingsWithReducedPayment);
        String optionTerm = AppPreferences.tr(this,
                "Уменьшить срок кредита\nПлатёж: ",
                "Reduce loan term\nPayment: ") + FormatUtils.money(this, s.keptPayment)
                + AppPreferences.tr(this, "\nНовый срок: ", "\nNew term: ") + UiUtils.termText(this, s.reducedMonths)
                + AppPreferences.tr(this, "\nСрок до: ", "\nEnd date: ") + FormatUtils.date(this, endTerm)
                + AppPreferences.tr(this, "\nОстаток процентов: ", "\nRemaining interest: ") + FormatUtils.money(this, s.interestWithReducedTerm)
                + AppPreferences.tr(this, "\nЭкономия: ", "\nSavings: ") + FormatUtils.money(this, s.savingsWithReducedTerm);
        boolean termBetter = s.interestWithReducedTerm + 0.01 < s.interestWithReducedPayment;
        String best = termBetter
                ? AppPreferences.tr(this, "Выгоднее по переплате: уменьшить срок.", "Lower interest: reduce the term.")
                : AppPreferences.tr(this, "Выгоднее по переплате: уменьшить платёж.", "Lower interest: reduce the payment.");
        final int[] selected = {termBetter ? 1 : 0};
        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Выберите вариант", "Choose an option"))
                .setMessage(best)
                .setSingleChoiceItems(new String[]{optionPayment, optionTerm}, selected[0], (dialog, which) -> selected[0] = which)
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Применить", "Apply"), (d, w) -> {
                    ReminderScheduler.applyEarlyRepayment(this, reminder.id, date, s.prepayment, selected[0] == 1);
                    refreshAfterAction(AppPreferences.tr(this, "Досрочное погашение применено", "Early repayment applied"));
                }).show();
    }

    private void showRefinanceInput() {
        final Calendar refinanceDate = Calendar.getInstance();
        LinearLayout box = dialogBox();
        EditText rate = dialogInput(AppPreferences.tr(this, "Новая ставка, % годовых", "New annual rate, %"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText months = dialogInput(AppPreferences.tr(this, "Новый срок, месяцев", "New term, months"), InputType.TYPE_CLASS_NUMBER);
        EditText commission = dialogInput(AppPreferences.tr(this, "Комиссия, ₽ (если есть)", "Commission, ₽ (if any)"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText insurance = dialogInput(AppPreferences.tr(this, "Новая страховка, ₽ (если есть)", "New insurance, ₽ (if any)"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText date = dialogInput(AppPreferences.tr(this, "Дата рефинансирования", "Refinancing date"), InputType.TYPE_NULL);
        date.setFocusable(false); date.setClickable(true); date.setText(FormatUtils.date(this, refinanceDate.getTimeInMillis())); date.setOnClickListener(v -> pickDate(refinanceDate, date));
        box.addView(rate, dialogFieldParams()); box.addView(months, dialogFieldParams()); box.addView(commission, dialogFieldParams()); box.addView(insurance, dialogFieldParams()); box.addView(date, dialogFieldParams());

        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Рефинансирование", "Refinancing"))
                .setMessage(AppPreferences.tr(this, "Введите условия нового кредита. Сначала покажем сравнение.", "Enter the new loan terms. A comparison will be shown first."))
                .setView(box)
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Сравнить", "Compare"), (d, w) -> {
                    try {
                        double newRate = parseNonNegative(rate.getText().toString());
                        int newMonths = Integer.parseInt(months.getText().toString().trim());
                        double fee = parseNonNegative(commission.getText().toString());
                        double ins = parseNonNegative(insurance.getText().toString());
                        ReminderScheduler.RefinanceSimulation s = ReminderScheduler.simulateRefinance(reminder, refinanceDate.getTimeInMillis(), newRate, newMonths, fee, ins);
                        showRefinanceSimulation(s, refinanceDate.getTimeInMillis());
                    } catch (Exception e) {
                        Toast.makeText(this, AppPreferences.tr(this, "Проверьте введённые условия", "Check the entered terms"), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void showRefinanceSimulation(ReminderScheduler.RefinanceSimulation s, long date) {
        long oldEnd = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, Math.max(0, reminder.months - 1)).getTimeInMillis();
        long newEnd = ReminderScheduler.buildDueDate(s.firstNewPayment, Math.max(0, s.newMonths - 1)).getTimeInMillis();
        String comparison = AppPreferences.tr(this, "ОСТАВИТЬ ТЕКУЩИЙ КРЕДИТ", "KEEP CURRENT LOAN")
                + AppPreferences.tr(this, "\nПлатёж: ", "\nPayment: ") + FormatUtils.money(this, reminder.amount)
                + AppPreferences.tr(this, "\nОстаток переплаты: ", "\nRemaining interest: ") + FormatUtils.money(this, s.oldRemainingOverpayment)
                + AppPreferences.tr(this, "\nОкончание: ", "\nEnd date: ") + FormatUtils.date(this, oldEnd)
                + "\n\n" + AppPreferences.tr(this, "РЕФИНАНСИРОВАТЬ", "REFINANCE")
                + AppPreferences.tr(this, "\nНовый платёж: ", "\nNew payment: ") + FormatUtils.money(this, s.newPayment)
                + AppPreferences.tr(this, "\nНовая переплата с расходами: ", "\nNew overpayment incl. costs: ") + FormatUtils.money(this, s.newOverpayment)
                + AppPreferences.tr(this, "\nОкончание: ", "\nEnd date: ") + FormatUtils.date(this, newEnd)
                + AppPreferences.tr(this, "\n\nРасчётная экономия: ", "\n\nEstimated savings: ") + FormatUtils.money(this, s.savings)
                + (s.savings > 0 ? AppPreferences.tr(this, "\nРефинансирование выгоднее по расчёту.", "\nRefinancing is cheaper by this estimate.") : AppPreferences.tr(this, "\nТекущий кредит выгоднее по расчёту.", "\nKeeping the current loan is cheaper by this estimate."));
        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Сравнение", "Comparison"))
                .setMessage(comparison)
                .setNegativeButton(AppPreferences.tr(this, "Оставить текущий кредит", "Keep current loan"), null)
                .setPositiveButton(AppPreferences.tr(this, "Применить рефинансирование", "Apply refinancing"), (d, w) -> {
                    ReminderScheduler.applyRefinance(this, reminder.id, date, s.newRate, s.newMonths, s.commission, s.insurance);
                    refreshAfterAction(AppPreferences.tr(this, "Рефинансирование применено", "Refinancing applied"));
                }).show();
    }

    private void refreshAfterAction(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        ReminderScheduler.PaymentReminder latest = ReminderScheduler.findById(this, reminder.id);
        if (latest != null) reminder = latest;
        recreate();
    }

    private void pickDate(Calendar value, EditText field) {
        new DatePickerDialog(this, (dialog, y, m, d) -> {
            value.set(Calendar.YEAR, y); value.set(Calendar.MONTH, m); value.set(Calendar.DAY_OF_MONTH, d);
            value.set(Calendar.HOUR_OF_DAY, 12); value.set(Calendar.MINUTE, 0); value.set(Calendar.SECOND, 0); value.set(Calendar.MILLISECOND, 0);
            field.setText(FormatUtils.date(this, value.getTimeInMillis()));
        }, value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DAY_OF_MONTH)).show();
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20); box.setPadding(p, dp(6), p, 0);
        return box;
    }

    private EditText dialogInput(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setInputType(inputType); e.setSingleLine(true); e.setTextColor(ContextCompat.getColor(this, R.color.text_main)); e.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        return e;
    }

    private LinearLayout.LayoutParams dialogFieldParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(58)); p.setMargins(0, dp(5), 0, dp(5)); return p;
    }

    private View buildDepositPeriodCard() {
        MaterialCardView card = basicCard();
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16), dp(14), dp(16), dp(14)); card.addView(box);
        Calendar maturity = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, reminder.months);
        box.addView(text(AppPreferences.tr(this, "Дата открытия: ", "Start date: ") + FormatUtils.date(this, reminder.firstPaymentMillis), 15, R.color.text_main, false));
        TextView end = text(AppPreferences.tr(this, "Ожидаемое окончание: ", "Expected maturity: ") + FormatUtils.date(this, maturity.getTimeInMillis()), 15, R.color.text_main, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(8), 0, 0); box.addView(end, p);
        return card;
    }

    private void addSummaryLine(LinearLayout parent, String label, String value) {
        parent.addView(text(label, 13, R.color.result_secondary, false));
        TextView valueView = text(value, 19, R.color.white, true);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2); valueParams.setMargins(0, dp(2), 0, dp(12)); parent.addView(valueView, valueParams);
    }

    private View buildScheduleRow(int index, int nextIndex) {
        Calendar due = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, index);
        boolean paid = nextIndex < 0 || index < nextIndex;
        boolean next = index == nextIndex;
        MaterialCardView card = basicCard();
        card.setStrokeColor(next ? ContextCompat.getColor(this, R.color.primary) : ContextCompat.getColor(this, R.color.border));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(14), dp(12), dp(14), dp(12)); card.addView(row);
        TextView number = text(String.valueOf(index + 1), 15, next ? R.color.white : R.color.primary, true); number.setGravity(Gravity.CENTER); number.setBackgroundResource(next ? R.drawable.circle_primary : R.drawable.circle_soft); row.addView(number, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout middle = new LinearLayout(this); middle.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f); mp.setMargins(dp(12), 0, dp(8), 0); row.addView(middle, mp);
        middle.addView(text(FormatUtils.date(this, due.getTimeInMillis()), 16, R.color.text_main, true));
        String status = next ? AppPreferences.tr(this, "Ближайший платёж", "Next payment") : paid ? AppPreferences.tr(this, "Дата прошла", "Date passed") : AppPreferences.tr(this, "Предстоящий", "Upcoming");
        middle.addView(text(status, 12, R.color.text_secondary, false));
        row.addView(text(FormatUtils.money(this, reminder.amount), 15, R.color.text_main, true));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, 0, 0, dp(8)); card.setLayoutParams(cp);
        return card;
    }

    private MaterialCardView basicCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(13)); card.setCardElevation(0); card.setStrokeWidth(dp(1)); card.setStrokeColor(ContextCompat.getColor(this, R.color.border)); card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        return card;
    }

    private MaterialButton outlineButton(String label, int colorRes) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(ContextCompat.getColor(this, colorRes));
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background)));
        b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))); b.setStrokeWidth(dp(1)); b.setCornerRadius(dp(14));
        return b;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this, "Переместить в корзину?", "Move to trash?"))
                .setMessage(AppPreferences.tr(this, "Запись можно будет восстановить в течение 30 дней.", "You can restore this item for 30 days."))
                .setNegativeButton(AppPreferences.tr(this, "Отмена", "Cancel"), null)
                .setPositiveButton(AppPreferences.tr(this, "Удалить", "Delete"), (dialog, which) -> { ReminderScheduler.moveToTrash(this, reminder.id); finish(); })
                .show();
    }

    private boolean isDeposit() { return ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(reminder.type)); }
    private double parseDouble(String value) { double d = Double.parseDouble(clean(value)); if (d <= 0) throw new IllegalArgumentException(); return d; }
    private double parseNonNegative(String value) { String c = clean(value); if (c.isEmpty()) return 0.0; double d = Double.parseDouble(c); if (d < 0) throw new IllegalArgumentException(); return d; }
    private String clean(String v) { return v == null ? "" : v.replace(" ", "").replace("\u00A0", "").replace("\u202F", "").replace(',', '.').trim(); }
    private TextView topText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setClickable(true); v.setFocusable(true); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(ContextCompat.getColor(this, color)); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private String trimRate(double value) { return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value).replace('.', ','); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
