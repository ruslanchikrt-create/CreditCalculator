package com.example.creditcalculator;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 2001;

    private enum CalculatorMode {
        CREDIT,
        MORTGAGE,
        AUTO,
        INSTALLMENT,
        DEPOSIT
    }

    private TextInputLayout amountLayout;
    private TextInputLayout monthsLayout;
    private TextInputLayout rateLayout;
    private TextInputLayout extraLayout;

    private TextInputEditText amountInput;
    private TextInputEditText monthsInput;
    private TextInputEditText rateInput;
    private TextInputEditText extraInput;

    private TextView calculatorTitle;
    private TextView calculatorSubtitle;
    private TextView resultLabel1;
    private TextView resultLabel2;
    private TextView resultLabel3;
    private TextView monthlyPaymentText;
    private TextView totalPaymentText;
    private TextView overpaymentText;

    private MaterialCardView formCard;
    private MaterialCardView resultCard;
    private SwitchMaterial capitalizationSwitch;
    private DrawerLayout drawerLayout;
    private ScrollView mainScroll;

    private MaterialButton creditButton;
    private MaterialButton mortgageButton;
    private MaterialButton autoButton;
    private MaterialButton installmentButton;
    private MaterialButton depositButton;

    private CalculatorMode currentMode;
    private NumberFormat moneyFormat;
    private MoneyTextWatcher amountMoneyWatcher;
    private MoneyTextWatcher monthsMoneyWatcher;

    private double lastSuggestedPayment = 0.0;
    private int lastSuggestedMonths = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupMoneyFormat();
        setupMoneyInputs();
        setupModeButtons();
        setupDrawer();
        setupKeyboardScrolling();

        formCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        mainScroll = findViewById(R.id.mainScroll);

        amountLayout = findViewById(R.id.amountLayout);
        monthsLayout = findViewById(R.id.monthsLayout);
        rateLayout = findViewById(R.id.rateLayout);
        extraLayout = findViewById(R.id.extraLayout);

        amountInput = findViewById(R.id.amountInput);
        monthsInput = findViewById(R.id.monthsInput);
        rateInput = findViewById(R.id.rateInput);
        extraInput = findViewById(R.id.extraInput);

        calculatorTitle = findViewById(R.id.calculatorTitle);
        calculatorSubtitle = findViewById(R.id.calculatorSubtitle);
        resultLabel1 = findViewById(R.id.resultLabel1);
        resultLabel2 = findViewById(R.id.resultLabel2);
        resultLabel3 = findViewById(R.id.resultLabel3);
        monthlyPaymentText = findViewById(R.id.monthlyPaymentText);
        totalPaymentText = findViewById(R.id.totalPaymentText);
        overpaymentText = findViewById(R.id.overpaymentText);

        formCard = findViewById(R.id.formCard);
        resultCard = findViewById(R.id.resultCard);
        capitalizationSwitch = findViewById(R.id.capitalizationSwitch);

        creditButton = findViewById(R.id.creditButton);
        mortgageButton = findViewById(R.id.mortgageButton);
        autoButton = findViewById(R.id.autoButton);
        installmentButton = findViewById(R.id.installmentButton);
        depositButton = findViewById(R.id.depositButton);

        MaterialButton calcButton = findViewById(R.id.calcButton);
        MaterialButton addReminderButton = findViewById(R.id.addReminderButton);
        calcButton.setOnClickListener(v -> calculate());
        addReminderButton.setOnClickListener(v -> showReminderDialog());
    }

    private void setupMoneyFormat() {
        moneyFormat = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        moneyFormat.setMaximumFractionDigits(2);
        moneyFormat.setMinimumFractionDigits(2);
    }

    private void setupMoneyInputs() {
        amountMoneyWatcher = new MoneyTextWatcher(amountInput);
        monthsMoneyWatcher = new MoneyTextWatcher(monthsInput);
        amountMoneyWatcher.setEnabled(true);
        monthsMoneyWatcher.setEnabled(false);
        amountInput.addTextChangedListener(amountMoneyWatcher);
        monthsInput.addTextChangedListener(monthsMoneyWatcher);
    }

    private void setupModeButtons() {
        creditButton.setOnClickListener(v -> selectMode(CalculatorMode.CREDIT));
        mortgageButton.setOnClickListener(v -> selectMode(CalculatorMode.MORTGAGE));
        autoButton.setOnClickListener(v -> selectMode(CalculatorMode.AUTO));
        installmentButton.setOnClickListener(v -> selectMode(CalculatorMode.INSTALLMENT));
        depositButton.setOnClickListener(v -> selectMode(CalculatorMode.DEPOSIT));
    }

    private void setupDrawer() {
        findViewById(R.id.menuButton).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.drawerCalculators).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            mainScroll.smoothScrollTo(0, 0);
        });

        findViewById(R.id.drawerPayments).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showPaymentsDialog();
        });

        findViewById(R.id.drawerAddReminder).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showReminderDialog();
        });

        findViewById(R.id.drawerAbout).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            new AlertDialog.Builder(this)
                    .setTitle("Финансовый калькулятор")
                    .setMessage("Кредит, ипотека, автокредит, рассрочка и вклад. Можно сохранить график платежей и получать напоминания заранее.")
                    .setPositiveButton("ОК", null)
                    .show();
        });
    }

    private void setupKeyboardScrolling() {
        setupAutoScroll(amountInput);
        setupAutoScroll(monthsInput);
        setupAutoScroll(rateInput);
        setupAutoScroll(extraInput);
    }

    private void setupAutoScroll(View field) {
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                return;
            }
            mainScroll.postDelayed(() -> {
                Rect rect = new Rect();
                v.getDrawingRect(rect);
                mainScroll.offsetDescendantRectToMyCoords(v, rect);
                mainScroll.smoothScrollTo(0, Math.max(0, rect.top - dp(100)));
            }, 300);
        });
    }

    private void selectMode(CalculatorMode mode) {
        currentMode = mode;
        clearInputs();
        resultCard.setVisibility(View.GONE);
        extraLayout.setVisibility(View.GONE);
        capitalizationSwitch.setVisibility(View.GONE);
        capitalizationSwitch.setChecked(false);
        lastSuggestedPayment = 0.0;
        lastSuggestedMonths = 0;

        monthsMoneyWatcher.setEnabled(
                mode == CalculatorMode.MORTGAGE
                        || mode == CalculatorMode.AUTO
                        || mode == CalculatorMode.INSTALLMENT
        );

        switch (mode) {
            case CREDIT:
                calculatorTitle.setText("Кредит");
                calculatorSubtitle.setText("Рассчитайте ежемесячный платёж и переплату");
                amountLayout.setHint("Сумма кредита, ₽");
                monthsLayout.setHint("Срок, месяцев");
                rateLayout.setHint("Процентная ставка, % годовых");
                break;

            case MORTGAGE:
                calculatorTitle.setText("Ипотека");
                calculatorSubtitle.setText("Укажите стоимость жилья, первый взнос, срок и ставку");
                amountLayout.setHint("Стоимость жилья, ₽");
                monthsLayout.setHint("Первоначальный взнос, ₽");
                rateLayout.setHint("Срок ипотеки, лет");
                extraLayout.setHint("Ставка, % годовых");
                extraLayout.setVisibility(View.VISIBLE);
                break;

            case AUTO:
                calculatorTitle.setText("Автокредит");
                calculatorSubtitle.setText("Рассчитайте платёж по кредиту на автомобиль");
                amountLayout.setHint("Стоимость автомобиля, ₽");
                monthsLayout.setHint("Первоначальный взнос, ₽");
                rateLayout.setHint("Срок кредита, месяцев");
                extraLayout.setHint("Ставка, % годовых");
                extraLayout.setVisibility(View.VISIBLE);
                break;

            case INSTALLMENT:
                calculatorTitle.setText("Рассрочка");
                calculatorSubtitle.setText("Рассчитайте платёж без процентов");
                amountLayout.setHint("Стоимость покупки, ₽");
                monthsLayout.setHint("Первоначальный взнос, ₽");
                rateLayout.setHint("Срок рассрочки, месяцев");
                break;

            case DEPOSIT:
                calculatorTitle.setText("Вклад");
                calculatorSubtitle.setText("Рассчитайте доход и итоговую сумму вклада");
                amountLayout.setHint("Сумма вклада, ₽");
                monthsLayout.setHint("Срок вклада, месяцев");
                rateLayout.setHint("Ставка, % годовых");
                capitalizationSwitch.setVisibility(View.VISIBLE);
                break;
        }

        updateButtonStyles();
        formCard.setVisibility(View.VISIBLE);
        mainScroll.post(() -> mainScroll.smoothScrollTo(0, formCard.getTop()));
    }

    private void calculate() {
        if (currentMode == null) {
            Toast.makeText(this, "Выберите раздел", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasRequiredValues()) {
            resultCard.setVisibility(View.GONE);
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            switch (currentMode) {
                case CREDIT:
                    calculateCredit();
                    break;
                case MORTGAGE:
                    calculateMortgage();
                    break;
                case AUTO:
                    calculateAutoLoan();
                    break;
                case INSTALLMENT:
                    calculateInstallment();
                    break;
                case DEPOSIT:
                    calculateDeposit();
                    break;
            }
        } catch (Exception e) {
            resultCard.setVisibility(View.GONE);
            Toast.makeText(this, "Проверьте введённые данные", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateCredit() {
        double principal = positiveDouble(amountInput);
        int months = positiveInt(monthsInput);
        double annualRate = nonNegativeDouble(rateInput);

        double[] result = annuity(principal, months, annualRate);
        lastSuggestedPayment = result[0];
        lastSuggestedMonths = months;
        showMoneyResult(
                "Ежемесячный платёж", result[0],
                "Общая сумма выплат", result[1],
                "Переплата", result[2]
        );
    }

    private void calculateMortgage() {
        double propertyPrice = positiveDouble(amountInput);
        double downPayment = nonNegativeDouble(monthsInput);
        int years = positiveInt(rateInput);
        double annualRate = nonNegativeDouble(extraInput);

        if (downPayment >= propertyPrice) {
            throw new IllegalArgumentException();
        }

        long monthsLong = years * 12L;
        if (monthsLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }

        int months = (int) monthsLong;
        double principal = propertyPrice - downPayment;
        double[] result = annuity(principal, months, annualRate);
        lastSuggestedPayment = result[0];
        lastSuggestedMonths = months;

        showMoneyResult(
                "Ежемесячный платёж", result[0],
                "Всего выплат банку", result[1],
                "Переплата по процентам", result[2]
        );
    }

    private void calculateAutoLoan() {
        double carPrice = positiveDouble(amountInput);
        double downPayment = nonNegativeDouble(monthsInput);
        int months = positiveInt(rateInput);
        double annualRate = nonNegativeDouble(extraInput);

        if (downPayment >= carPrice) {
            throw new IllegalArgumentException();
        }

        double principal = carPrice - downPayment;
        double[] result = annuity(principal, months, annualRate);
        lastSuggestedPayment = result[0];
        lastSuggestedMonths = months;

        showMoneyResult(
                "Ежемесячный платёж", result[0],
                "Всего выплат банку", result[1],
                "Переплата по процентам", result[2]
        );
    }

    private void calculateInstallment() {
        double purchasePrice = positiveDouble(amountInput);
        double downPayment = nonNegativeDouble(monthsInput);
        int months = positiveInt(rateInput);

        if (downPayment >= purchasePrice) {
            throw new IllegalArgumentException();
        }

        double installmentAmount = purchasePrice - downPayment;
        double monthlyPayment = installmentAmount / months;
        lastSuggestedPayment = monthlyPayment;
        lastSuggestedMonths = months;

        showMoneyResult(
                "Ежемесячный платёж", monthlyPayment,
                "Сумма в рассрочку", installmentAmount,
                "Общая стоимость", purchasePrice
        );
    }

    private void calculateDeposit() {
        double principal = positiveDouble(amountInput);
        int months = positiveInt(monthsInput);
        double annualRate = nonNegativeDouble(rateInput);

        double finalAmount;
        if (capitalizationSwitch.isChecked()) {
            double monthlyRate = annualRate / 100.0 / 12.0;
            finalAmount = principal * Math.pow(1.0 + monthlyRate, months);
        } else {
            double years = months / 12.0;
            finalAmount = principal + principal * (annualRate / 100.0) * years;
        }

        double income = finalAmount - principal;
        lastSuggestedPayment = 0.0;
        lastSuggestedMonths = 0;

        resultLabel1.setText("Доход по вкладу");
        monthlyPaymentText.setText(formatMoney(income));
        resultLabel2.setText("Итоговая сумма");
        totalPaymentText.setText(formatMoney(finalAmount));
        resultLabel3.setText("Капитализация");
        overpaymentText.setText(capitalizationSwitch.isChecked() ? "Ежемесячная" : "Без капитализации");
        resultCard.setVisibility(View.VISIBLE);
        scrollResultIntoView();
    }

    private double[] annuity(double principal, int months, double annualRate) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        double monthlyPayment;

        if (monthlyRate == 0.0) {
            monthlyPayment = principal / months;
        } else {
            double factor = Math.pow(1.0 + monthlyRate, months);
            monthlyPayment = principal * monthlyRate * factor / (factor - 1.0);
        }

        double totalPayment = monthlyPayment * months;
        double overpayment = totalPayment - principal;
        return new double[]{monthlyPayment, totalPayment, overpayment};
    }

    private void showMoneyResult(
            String label1, double value1,
            String label2, double value2,
            String label3, double value3
    ) {
        resultLabel1.setText(label1);
        monthlyPaymentText.setText(formatMoney(value1));
        resultLabel2.setText(label2);
        totalPaymentText.setText(formatMoney(value2));
        resultLabel3.setText(label3);
        overpaymentText.setText(formatMoney(value3));
        resultCard.setVisibility(View.VISIBLE);
        scrollResultIntoView();
    }

    private void scrollResultIntoView() {
        resultCard.postDelayed(() -> {
            Rect rect = new Rect();
            resultCard.getDrawingRect(rect);
            mainScroll.offsetDescendantRectToMyCoords(resultCard, rect);
            mainScroll.smoothScrollTo(0, Math.max(0, rect.top - dp(90)));
        }, 120);
    }

    private void showReminderDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        TextInputEditText titleInput = view.findViewById(R.id.reminderTitleInput);
        TextInputEditText paymentInput = view.findViewById(R.id.reminderAmountInput);
        TextInputEditText monthsInputDialog = view.findViewById(R.id.reminderMonthsInput);
        TextInputEditText dateInput = view.findViewById(R.id.reminderDateInput);
        Spinner daysSpinner = view.findViewById(R.id.reminderDaysSpinner);

        MoneyTextWatcher paymentWatcher = new MoneyTextWatcher(paymentInput);
        paymentWatcher.setEnabled(true);
        paymentInput.addTextChangedListener(paymentWatcher);

        if (currentMode != null) {
            titleInput.setText(modeTitle(currentMode));
        } else {
            titleInput.setText("Кредит");
        }

        if (lastSuggestedPayment > 0.0) {
            paymentInput.setText(formatInputAmount(lastSuggestedPayment));
        }
        if (lastSuggestedMonths > 0) {
            monthsInputDialog.setText(String.valueOf(lastSuggestedMonths));
        }

        String[] daysOptions = {
                "За 1 день",
                "За 2 дня",
                "За 3 дня",
                "За 4 дня",
                "За 5 дней",
                "За 6 дней",
                "За 7 дней"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                daysOptions
        );
        daysSpinner.setAdapter(adapter);
        daysSpinner.setSelection(2);

        final Calendar[] selectedDate = new Calendar[1];
        dateInput.setOnClickListener(v -> {
            Calendar base = selectedDate[0] == null ? Calendar.getInstance() : selectedDate[0];
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (dialog, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.clear();
                        selected.set(year, month, dayOfMonth, 9, 0, 0);
                        selectedDate[0] = selected;
                        dateInput.setText(new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU"))
                                .format(selected.getTime()));
                    },
                    base.get(Calendar.YEAR),
                    base.get(Calendar.MONTH),
                    base.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Новое напоминание")
                .setView(view)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        String title = text(titleInput).trim();
                        if (title.isEmpty()) {
                            title = "Платёж по кредиту";
                        }

                        double payment = parsePositiveDouble(paymentInput);
                        int months = parsePositiveInt(monthsInputDialog);
                        if (selectedDate[0] == null) {
                            Toast.makeText(this, "Выберите дату первого платежа", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Calendar lastPayment = ReminderScheduler.buildDueDate(
                                selectedDate[0].getTimeInMillis(),
                                months - 1
                        );
                        if (lastPayment.getTimeInMillis() < System.currentTimeMillis()) {
                            Toast.makeText(this, "Срок платежей уже закончился", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int daysBefore = daysSpinner.getSelectedItemPosition() + 1;
                        ReminderScheduler.PaymentReminder reminder = new ReminderScheduler.PaymentReminder(
                                System.currentTimeMillis(),
                                title,
                                payment,
                                selectedDate[0].getTimeInMillis(),
                                months,
                                daysBefore
                        );

                        ReminderScheduler.add(this, reminder);
                        requestNotificationPermissionIfNeeded();
                        Toast.makeText(this, "Напоминание сохранено", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(this, "Проверьте сумму и срок", Toast.LENGTH_SHORT).show();
                    }
                }));

        dialog.show();
    }

    private void showPaymentsDialog() {
        List<ReminderScheduler.PaymentReminder> reminders = ReminderScheduler.load(this);
        if (reminders.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Мои платежи")
                    .setMessage("Сохранённых платежей пока нет. Нажмите +, чтобы добавить кредит и напоминания.")
                    .setPositiveButton("Добавить", (d, which) -> showReminderDialog())
                    .setNegativeButton("Закрыть", null)
                    .show();
            return;
        }

        String[] items = new String[reminders.size()];
        for (int i = 0; i < reminders.size(); i++) {
            ReminderScheduler.PaymentReminder reminder = reminders.get(i);
            Calendar next = nextPayment(reminder);
            String nextDate = next == null
                    ? "завершён"
                    : new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU")).format(next.getTime());
            items[i] = reminder.title + "\n" + formatMoneyNoCents(reminder.amount)
                    + " · следующий: " + nextDate;
        }

        new AlertDialog.Builder(this)
                .setTitle("Мои платежи")
                .setItems(items, (dialog, which) -> showPaymentDetails(reminders.get(which)))
                .setPositiveButton("Добавить", (dialog, which) -> showReminderDialog())
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showPaymentDetails(ReminderScheduler.PaymentReminder reminder) {
        Calendar next = nextPayment(reminder);
        String nextDate = next == null
                ? "Платежи завершены"
                : new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU")).format(next.getTime());

        String message = "Сумма: " + formatMoneyNoCents(reminder.amount)
                + "\nСледующий платёж: " + nextDate
                + "\nСрок: " + reminder.months + " мес."
                + "\nНапомнить за: " + reminder.daysBefore + " дн.";

        new AlertDialog.Builder(this)
                .setTitle(reminder.title)
                .setMessage(message)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    ReminderScheduler.delete(this, reminder.id);
                    Toast.makeText(this, "Напоминание удалено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private Calendar nextPayment(ReminderScheduler.PaymentReminder reminder) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < reminder.months; i++) {
            Calendar due = ReminderScheduler.buildDueDate(reminder.firstPaymentMillis, i);
            Calendar endOfDueDay = (Calendar) due.clone();
            endOfDueDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDueDay.set(Calendar.MINUTE, 59);
            if (endOfDueDay.getTimeInMillis() >= now) {
                return due;
            }
        }
        return null;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    private boolean hasRequiredValues() {
        boolean firstThree = !value(amountInput).isEmpty()
                && !value(monthsInput).isEmpty()
                && !value(rateInput).isEmpty();

        if (!firstThree) {
            return false;
        }

        return currentMode != CalculatorMode.MORTGAGE
                && currentMode != CalculatorMode.AUTO
                || !value(extraInput).isEmpty();
    }

    private double positiveDouble(TextInputEditText editText) {
        double number = Double.parseDouble(value(editText));
        if (Double.isNaN(number) || Double.isInfinite(number) || number <= 0.0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    private double nonNegativeDouble(TextInputEditText editText) {
        double number = Double.parseDouble(value(editText));
        if (Double.isNaN(number) || Double.isInfinite(number) || number < 0.0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    private int positiveInt(TextInputEditText editText) {
        double raw = Double.parseDouble(value(editText));
        if (Double.isNaN(raw)
                || Double.isInfinite(raw)
                || raw <= 0.0
                || raw != Math.floor(raw)
                || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        return (int) raw;
    }

    private double parsePositiveDouble(TextInputEditText editText) {
        String raw = text(editText)
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "")
                .replace(",", ".");
        double number = Double.parseDouble(raw);
        if (number <= 0 || Double.isNaN(number) || Double.isInfinite(number)) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    private int parsePositiveInt(TextInputEditText editText) {
        int number = Integer.parseInt(text(editText).trim());
        if (number <= 0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    private String value(TextInputEditText editText) {
        return text(editText)
                .trim()
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "")
                .replace(",", ".");
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value) + " ₽";
    }

    private String formatMoneyNoCents(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(value) + " ₽";
    }

    private String formatInputAmount(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(value).replace('\u00A0', ' ').replace('\u202F', ' ');
    }

    private String modeTitle(CalculatorMode mode) {
        switch (mode) {
            case MORTGAGE:
                return "Ипотека";
            case AUTO:
                return "Автокредит";
            case INSTALLMENT:
                return "Рассрочка";
            case DEPOSIT:
                return "Вклад";
            case CREDIT:
            default:
                return "Кредит";
        }
    }

    private void clearInputs() {
        amountInput.setText("");
        monthsInput.setText("");
        rateInput.setText("");
        extraInput.setText("");
        amountInput.clearFocus();
        monthsInput.clearFocus();
        rateInput.clearFocus();
        extraInput.clearFocus();
    }

    private void updateButtonStyles() {
        styleButton(creditButton, currentMode == CalculatorMode.CREDIT);
        styleButton(mortgageButton, currentMode == CalculatorMode.MORTGAGE);
        styleButton(autoButton, currentMode == CalculatorMode.AUTO);
        styleButton(installmentButton, currentMode == CalculatorMode.INSTALLMENT);
        styleButton(depositButton, currentMode == CalculatorMode.DEPOSIT);
    }

    private void styleButton(MaterialButton button, boolean selected) {
        int primary = ContextCompat.getColor(this, R.color.primary);
        int white = ContextCompat.getColor(this, R.color.white);

        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primary : white));
        button.setTextColor(selected ? white : primary);
        button.setStrokeColor(ColorStateList.valueOf(primary));
        button.setStrokeWidth(dp(1));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private static class MoneyTextWatcher implements TextWatcher {
        private final TextInputEditText editText;
        private boolean enabled;
        private boolean editing;

        MoneyTextWatcher(TextInputEditText editText) {
            this.editText = editText;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (enabled && editText.getText() != null && editText.getText().length() > 0) {
                afterTextChanged(editText.getText());
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (!enabled || editing) {
                return;
            }

            String formatted = groupNumber(editable.toString());
            if (formatted.equals(editable.toString())) {
                return;
            }

            editing = true;
            editText.setText(formatted);
            editText.setSelection(formatted.length());
            editing = false;
        }

        private String groupNumber(String source) {
            String clean = source
                    .replace(" ", "")
                    .replace("\u00A0", "")
                    .replace("\u202F", "");

            if (clean.isEmpty()) {
                return "";
            }

            int comma = clean.indexOf(',');
            int dot = clean.indexOf('.');
            int separator;
            if (comma >= 0 && dot >= 0) {
                separator = Math.min(comma, dot);
            } else {
                separator = Math.max(comma, dot);
            }

            String integerPart = separator >= 0 ? clean.substring(0, separator) : clean;
            String fractionPart = separator >= 0 ? clean.substring(separator + 1) : "";

            integerPart = integerPart.replaceAll("[^0-9]", "");
            fractionPart = fractionPart.replaceAll("[^0-9]", "");
            if (integerPart.isEmpty()) {
                integerPart = "0";
            }

            StringBuilder grouped = new StringBuilder();
            int length = integerPart.length();
            for (int i = 0; i < length; i++) {
                if (i > 0 && (length - i) % 3 == 0) {
                    grouped.append(' ');
                }
                grouped.append(integerPart.charAt(i));
            }

            if (separator >= 0) {
                grouped.append(',');
                grouped.append(fractionPart);
            }
            return grouped.toString();
        }
    }
}
