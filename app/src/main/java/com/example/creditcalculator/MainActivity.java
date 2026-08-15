package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private enum CalculatorMode {
        CREDIT,
        MORTGAGE,
        AUTO,
        INSTALLMENT,
        DEPOSIT
    }

    private DrawerLayout drawerLayout;
    private ScrollView mainScroll;
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
    private TextView resultValue1;
    private TextView resultValue2;
    private TextView resultValue3;
    private MaterialCardView formCard;
    private MaterialCardView resultCard;
    private SwitchMaterial capitalizationSwitch;
    private MaterialButton creditButton;
    private MaterialButton mortgageButton;
    private MaterialButton autoButton;
    private MaterialButton installmentButton;
    private MaterialButton depositButton;
    private MoneyTextWatcher amountWatcher;
    private MoneyTextWatcher monthsWatcher;
    private CalculatorMode currentMode;
    private String loadedLanguage;

    private double lastPrincipal;
    private double lastRate;
    private double lastPayment;
    private int lastMonths;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPreferences.wrapLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadedLanguage = AppPreferences.getLanguage(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(buildContent());
        setupMoneyInputs();
        setupListeners();
        formCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loadedLanguage != null && !loadedLanguage.equals(AppPreferences.getLanguage(this))) {
            recreate();
        }
    }

    private View buildContent() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setFitsSystemWindows(true);

        FrameLayout mainFrame = new FrameLayout(this);
        drawerLayout.addView(mainFrame, new DrawerLayout.LayoutParams(-1, -1));

        LinearLayout mainColumn = new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        mainColumn.setBackgroundResource(R.drawable.app_background);
        mainFrame.addView(mainColumn, new FrameLayout.LayoutParams(-1, -1));

        mainColumn.addView(buildTopBar(), new LinearLayout.LayoutParams(-1, dp(64)));

        mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(true);
        mainScroll.setClipToPadding(false);
        mainScroll.setPadding(0, 0, 0, dp(84));
        mainColumn.addView(mainScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(28));
        mainScroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView heading = text(AppPreferences.tr(this, "Калькуляторы", "Calculators"), 28, R.color.text_main, true);
        content.addView(heading);

        TextView subtitle = text(AppPreferences.tr(this, "Выберите нужный раздел", "Choose a calculator"), 15, R.color.text_secondary, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.setMargins(0, dp(6), 0, dp(18));
        content.addView(subtitle, subtitleParams);

        creditButton = calculatorButton(AppPreferences.tr(this, "Кредит", "Loan"));
        mortgageButton = calculatorButton(AppPreferences.tr(this, "Ипотека", "Mortgage"));
        autoButton = calculatorButton(AppPreferences.tr(this, "Автокредит", "Auto loan"));
        installmentButton = calculatorButton(AppPreferences.tr(this, "Рассрочка", "Installment"));
        depositButton = calculatorButton(AppPreferences.tr(this, "Вклад", "Deposit"));
        content.addView(creditButton, calculatorButtonParams());
        content.addView(mortgageButton, calculatorButtonParams());
        content.addView(autoButton, calculatorButtonParams());
        content.addView(installmentButton, calculatorButtonParams());
        content.addView(depositButton, calculatorButtonParams());

        formCard = buildFormCard();
        LinearLayout.LayoutParams formParams = new LinearLayout.LayoutParams(-1, -2);
        formParams.setMargins(0, dp(10), 0, 0);
        content.addView(formCard, formParams);

        resultCard = buildResultCard();
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-1, -2);
        resultParams.setMargins(0, dp(16), 0, dp(20));
        content.addView(resultCard, resultParams);

        MaterialButton addReminder = new MaterialButton(this);
        addReminder.setText("+");
        addReminder.setTextSize(30);
        addReminder.setTextColor(Color.WHITE);
        addReminder.setMinWidth(0);
        addReminder.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        addReminder.setCornerRadius(dp(30));
        addReminder.setOnClickListener(v -> openAddReminder());
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM | Gravity.END);
        addParams.setMargins(0, 0, dp(20), dp(20));
        mainFrame.addView(addReminder, addParams);

        View drawer = buildDrawer();
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(dp(310), -1);
        drawerParams.gravity = GravityCompat.START;
        drawerLayout.addView(drawer, drawerParams);

        return drawerLayout;
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(16), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));

        MaterialButton menu = new MaterialButton(this);
        menu.setText("☰");
        menu.setTextSize(28);
        menu.setTextColor(Color.WHITE);
        menu.setMinWidth(0);
        menu.setPadding(0, 0, 0, 0);
        menu.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        menu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        bar.addView(menu, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView title = text(AppPreferences.tr(this, "Финансовый калькулятор", "Financial calculator"), 20, R.color.white, true);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        return bar;
    }

    private View buildDrawer() {
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(20), dp(20), dp(20));
        header.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        drawer.addView(header, new LinearLayout.LayoutParams(-1, dp(176)));

        TextView icon = text("%", 40, R.color.white, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark));
        header.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView title = text(AppPreferences.tr(this, "Финансовый\nкалькулятор", "Financial\ncalculator"), 22, R.color.white, false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.setMargins(dp(16), 0, 0, 0);
        header.addView(title, titleParams);

        TextView calculators = drawerItem("▶   " + AppPreferences.tr(this, "Калькуляторы", "Calculators"));
        calculators.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            mainScroll.smoothScrollTo(0, 0);
        });
        drawer.addView(calculators);

        TextView payments = drawerItem("☷   " + AppPreferences.tr(this, "Мои платежи", "My payments"));
        payments.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, PaymentsActivity.class));
        });
        drawer.addView(payments);

        TextView settings = drawerItem("⚙   " + AppPreferences.tr(this, "Настройки", "Settings"));
        settings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, SettingsActivity.class));
        });
        drawer.addView(settings);

        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
        drawer.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        TextView about = drawerItem("ⓘ   " + AppPreferences.tr(this, "О приложении", "About"));
        about.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            new AlertDialog.Builder(this)
                    .setTitle(AppPreferences.tr(this, "Финансовый калькулятор", "Financial calculator"))
                    .setMessage(AppPreferences.tr(this,
                            "Кредит, ипотека, автокредит, рассрочка и вклад. Сохраняйте платежи, смотрите полный график и получайте уведомления заранее.",
                            "Loan, mortgage, auto loan, installment and deposit calculators. Save payments, view full schedules and receive reminders in advance."))
                    .setPositiveButton("OK", null)
                    .show();
        });
        drawer.addView(about);

        TextView exit = drawerItem("↪   " + AppPreferences.tr(this, "Выход", "Exit"));
        exit.setOnClickListener(v -> finishAffinity());
        drawer.addView(exit);

        return drawer;
    }

    private MaterialCardView buildFormCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(20));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setCardElevation(dp(1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(box);

        calculatorTitle = text("", 23, R.color.text_main, true);
        box.addView(calculatorTitle);
        calculatorSubtitle = text("", 14, R.color.text_secondary, false);
        LinearLayout.LayoutParams calculatorSubtitleParams = new LinearLayout.LayoutParams(-1, -2);
        calculatorSubtitleParams.setMargins(0, dp(4), 0, dp(16));
        box.addView(calculatorSubtitle, calculatorSubtitleParams);

        amountLayout = addInput(box);
        amountInput = input(amountLayout, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        monthsLayout = addInput(box);
        monthsInput = input(monthsLayout, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rateLayout = addInput(box);
        rateInput = input(rateLayout, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        extraLayout = addInput(box);
        extraInput = input(extraLayout, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        capitalizationSwitch = new SwitchMaterial(this);
        capitalizationSwitch.setText(AppPreferences.tr(this, "Ежемесячная капитализация процентов", "Monthly interest capitalization"));
        capitalizationSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        capitalizationSwitch.setTextSize(15);
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-1, -2);
        switchParams.setMargins(0, 0, 0, dp(12));
        box.addView(capitalizationSwitch, switchParams);

        MaterialButton calculate = new MaterialButton(this);
        calculate.setText(AppPreferences.tr(this, "Рассчитать", "Calculate"));
        calculate.setTextSize(17);
        calculate.setTextColor(Color.WHITE);
        calculate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        calculate.setCornerRadius(dp(14));
        calculate.setOnClickListener(v -> calculate());
        box.addView(calculate, new LinearLayout.LayoutParams(-1, dp(56)));
        return card;
    }

    private MaterialCardView buildResultCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.result_card));
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.addView(box);

        TextView title = text(AppPreferences.tr(this, "Результат", "Result"), 18, R.color.white, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, 0, 0, dp(14));
        box.addView(title, titleParams);

        resultLabel1 = resultLabel();
        resultValue1 = resultValue(27);
        resultLabel2 = resultLabel();
        resultValue2 = resultValue(20);
        resultLabel3 = resultLabel();
        resultValue3 = resultValue(20);
        box.addView(resultLabel1);
        box.addView(resultValue1, valueParams());
        box.addView(resultLabel2);
        box.addView(resultValue2, valueParams());
        box.addView(resultLabel3);
        box.addView(resultValue3);
        return card;
    }

    private void setupMoneyInputs() {
        amountWatcher = new MoneyTextWatcher(amountInput);
        monthsWatcher = new MoneyTextWatcher(monthsInput);
        amountWatcher.setEnabled(true);
        monthsWatcher.setEnabled(false);
        amountInput.addTextChangedListener(amountWatcher);
        monthsInput.addTextChangedListener(monthsWatcher);
    }

    private void setupListeners() {
        creditButton.setOnClickListener(v -> selectMode(CalculatorMode.CREDIT));
        mortgageButton.setOnClickListener(v -> selectMode(CalculatorMode.MORTGAGE));
        autoButton.setOnClickListener(v -> selectMode(CalculatorMode.AUTO));
        installmentButton.setOnClickListener(v -> selectMode(CalculatorMode.INSTALLMENT));
        depositButton.setOnClickListener(v -> selectMode(CalculatorMode.DEPOSIT));
        setupAutoScroll(amountInput);
        setupAutoScroll(monthsInput);
        setupAutoScroll(rateInput);
        setupAutoScroll(extraInput);
    }

    private void selectMode(CalculatorMode mode) {
        currentMode = mode;
        clearInputs();
        resultCard.setVisibility(View.GONE);
        extraLayout.setVisibility(View.GONE);
        capitalizationSwitch.setVisibility(View.GONE);
        capitalizationSwitch.setChecked(false);
        lastPrincipal = 0;
        lastRate = 0;
        lastPayment = 0;
        lastMonths = 0;

        monthsWatcher.setEnabled(mode == CalculatorMode.MORTGAGE || mode == CalculatorMode.AUTO || mode == CalculatorMode.INSTALLMENT);

        switch (mode) {
            case CREDIT:
                calculatorTitle.setText(AppPreferences.tr(this, "Кредит", "Loan"));
                calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте ежемесячный платёж и переплату", "Calculate monthly payment and overpayment"));
                amountLayout.setHint(AppPreferences.tr(this, "Сумма кредита, ₽", "Loan amount, ₽"));
                monthsLayout.setHint(AppPreferences.tr(this, "Срок, месяцев", "Term, months"));
                rateLayout.setHint(AppPreferences.tr(this, "Процентная ставка, % годовых", "Interest rate, % per year"));
                break;
            case MORTGAGE:
                calculatorTitle.setText(AppPreferences.tr(this, "Ипотека", "Mortgage"));
                calculatorSubtitle.setText(AppPreferences.tr(this, "Укажите стоимость жилья, первый взнос, срок и ставку", "Enter property price, down payment, term and rate"));
                amountLayout.setHint(AppPreferences.tr(this, "Стоимость жилья, ₽", "Property price, ₽"));
                monthsLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
                rateLayout.setHint(AppPreferences.tr(this, "Срок ипотеки, лет", "Mortgage term, years"));
                extraLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
                extraLayout.setVisibility(View.VISIBLE);
                break;
            case AUTO:
                calculatorTitle.setText(AppPreferences.tr(this, "Автокредит", "Auto loan"));
                calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте платёж по кредиту на автомобиль", "Calculate your auto loan payment"));
                amountLayout.setHint(AppPreferences.tr(this, "Стоимость автомобиля, ₽", "Car price, ₽"));
                monthsLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
                rateLayout.setHint(AppPreferences.tr(this, "Срок кредита, месяцев", "Loan term, months"));
                extraLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
                extraLayout.setVisibility(View.VISIBLE);
                break;
            case INSTALLMENT:
                calculatorTitle.setText(AppPreferences.tr(this, "Рассрочка", "Installment"));
                calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте платёж без процентов", "Calculate an interest-free installment"));
                amountLayout.setHint(AppPreferences.tr(this, "Стоимость покупки, ₽", "Purchase price, ₽"));
                monthsLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
                rateLayout.setHint(AppPreferences.tr(this, "Срок рассрочки, месяцев", "Installment term, months"));
                break;
            case DEPOSIT:
                calculatorTitle.setText(AppPreferences.tr(this, "Вклад", "Deposit"));
                calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте доход и итоговую сумму вклада", "Calculate deposit income and final amount"));
                amountLayout.setHint(AppPreferences.tr(this, "Сумма вклада, ₽", "Deposit amount, ₽"));
                monthsLayout.setHint(AppPreferences.tr(this, "Срок вклада, месяцев", "Deposit term, months"));
                rateLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
                capitalizationSwitch.setVisibility(View.VISIBLE);
                break;
        }

        updateButtonStyles();
        formCard.setVisibility(View.VISIBLE);
        formCard.post(() -> scrollToView(formCard, 80));
    }

    private void calculate() {
        if (currentMode == null) {
            Toast.makeText(this, AppPreferences.tr(this, "Выберите раздел", "Choose a calculator"), Toast.LENGTH_SHORT).show();
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
                    calculateAuto();
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
            Toast.makeText(this, AppPreferences.tr(this, "Заполните все поля правильно", "Check all entered values"), Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateCredit() {
        double principal = positiveDouble(amountInput);
        int months = positiveInt(monthsInput);
        double rate = nonNegativeDouble(rateInput);
        double[] result = annuity(principal, months, rate);
        remember(principal, rate, months, result[0]);
        showResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), result[0],
                AppPreferences.tr(this, "Общая сумма выплат", "Total payments"), result[1],
                AppPreferences.tr(this, "Переплата", "Overpayment"), result[2]);
    }

    private void calculateMortgage() {
        double price = positiveDouble(amountInput);
        double down = nonNegativeDouble(monthsInput);
        int years = positiveInt(rateInput);
        double rate = nonNegativeDouble(extraInput);
        if (down >= price) throw new IllegalArgumentException();
        int months = Math.multiplyExact(years, 12);
        double principal = price - down;
        double[] result = annuity(principal, months, rate);
        remember(principal, rate, months, result[0]);
        showResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), result[0],
                AppPreferences.tr(this, "Всего выплат банку", "Total paid to bank"), result[1],
                AppPreferences.tr(this, "Переплата по процентам", "Interest overpayment"), result[2]);
    }

    private void calculateAuto() {
        double price = positiveDouble(amountInput);
        double down = nonNegativeDouble(monthsInput);
        int months = positiveInt(rateInput);
        double rate = nonNegativeDouble(extraInput);
        if (down >= price) throw new IllegalArgumentException();
        double principal = price - down;
        double[] result = annuity(principal, months, rate);
        remember(principal, rate, months, result[0]);
        showResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), result[0],
                AppPreferences.tr(this, "Всего выплат банку", "Total paid to bank"), result[1],
                AppPreferences.tr(this, "Переплата по процентам", "Interest overpayment"), result[2]);
    }

    private void calculateInstallment() {
        double price = positiveDouble(amountInput);
        double down = nonNegativeDouble(monthsInput);
        int months = positiveInt(rateInput);
        if (down >= price) throw new IllegalArgumentException();
        double principal = price - down;
        double payment = principal / months;
        remember(principal, 0.0, months, payment);
        showResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), payment,
                AppPreferences.tr(this, "Сумма в рассрочку", "Installment amount"), principal,
                AppPreferences.tr(this, "Общая стоимость", "Total purchase price"), price);
    }

    private void calculateDeposit() {
        double principal = positiveDouble(amountInput);
        int months = positiveInt(monthsInput);
        double rate = nonNegativeDouble(rateInput);
        double finalAmount;
        if (capitalizationSwitch.isChecked()) {
            double monthlyRate = rate / 100.0 / 12.0;
            finalAmount = principal * Math.pow(1.0 + monthlyRate, months);
        } else {
            finalAmount = principal + principal * (rate / 100.0) * (months / 12.0);
        }
        double income = finalAmount - principal;
        remember(principal, rate, months, 0.0);
        resultLabel1.setText(AppPreferences.tr(this, "Доход по вкладу", "Deposit income"));
        resultValue1.setText(FormatUtils.money(this, income));
        resultLabel2.setText(AppPreferences.tr(this, "Итоговая сумма", "Final amount"));
        resultValue2.setText(FormatUtils.money(this, finalAmount));
        resultLabel3.setText(AppPreferences.tr(this, "Капитализация", "Capitalization"));
        resultValue3.setText(capitalizationSwitch.isChecked()
                ? AppPreferences.tr(this, "Ежемесячная", "Monthly")
                : AppPreferences.tr(this, "Без капитализации", "No capitalization"));
        resultCard.setVisibility(View.VISIBLE);
        resultCard.post(() -> scrollToView(resultCard, 80));
    }

    private void remember(double principal, double rate, int months, double payment) {
        lastPrincipal = principal;
        lastRate = rate;
        lastMonths = months;
        lastPayment = payment;
    }

    private void showResult(String label1, double value1, String label2, double value2, String label3, double value3) {
        resultLabel1.setText(label1);
        resultValue1.setText(FormatUtils.money(this, value1));
        resultLabel2.setText(label2);
        resultValue2.setText(FormatUtils.money(this, value2));
        resultLabel3.setText(label3);
        resultValue3.setText(FormatUtils.money(this, value3));
        resultCard.setVisibility(View.VISIBLE);
        resultCard.post(() -> scrollToView(resultCard, 80));
    }

    private double[] annuity(double principal, int months, double annualRate) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        double payment;
        if (monthlyRate == 0.0) {
            payment = principal / months;
        } else {
            double factor = Math.pow(1.0 + monthlyRate, months);
            payment = principal * monthlyRate * factor / (factor - 1.0);
        }
        double total = payment * months;
        return new double[]{payment, total, total - principal};
    }

    private void openAddReminder() {
        Intent intent = new Intent(this, AddReminderActivity.class);
        if (currentMode != null) {
            intent.putExtra(AddReminderActivity.EXTRA_TYPE, modeType(currentMode));
        }
        if (lastPrincipal > 0) intent.putExtra(AddReminderActivity.EXTRA_PRINCIPAL, lastPrincipal);
        if (lastRate >= 0 && lastPrincipal > 0) intent.putExtra(AddReminderActivity.EXTRA_RATE, lastRate);
        if (lastMonths > 0) intent.putExtra(AddReminderActivity.EXTRA_MONTHS, lastMonths);
        if (lastPayment > 0) intent.putExtra(AddReminderActivity.EXTRA_PAYMENT, lastPayment);
        startActivity(intent);
    }

    private String modeType(CalculatorMode mode) {
        switch (mode) {
            case MORTGAGE: return ReminderScheduler.TYPE_MORTGAGE;
            case AUTO: return ReminderScheduler.TYPE_AUTO;
            case INSTALLMENT: return ReminderScheduler.TYPE_INSTALLMENT;
            case DEPOSIT: return ReminderScheduler.TYPE_DEPOSIT;
            case CREDIT:
            default: return ReminderScheduler.TYPE_CREDIT;
        }
    }

    private void setupAutoScroll(View field) {
        field.setOnFocusChangeListener((v, focused) -> {
            if (focused) mainScroll.postDelayed(() -> scrollToView(v, 120), 250);
        });
    }

    private void scrollToView(View view, int topPaddingDp) {
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        mainScroll.offsetDescendantRectToMyCoords(view, rect);
        mainScroll.smoothScrollTo(0, Math.max(0, rect.top - dp(topPaddingDp)));
    }

    private void clearInputs() {
        amountInput.setText("");
        monthsInput.setText("");
        rateInput.setText("");
        extraInput.setText("");
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
        int white = Color.WHITE;
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primary : white));
        button.setTextColor(selected ? white : primary);
        button.setStrokeColor(ColorStateList.valueOf(primary));
        button.setStrokeWidth(dp(1));
    }

    private TextInputLayout addInput(LinearLayout parent) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        parent.addView(layout, lp);
        return layout;
    }

    private TextInputEditText input(TextInputLayout layout, int type) {
        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(type);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setMinHeight(dp(58));
        layout.addView(input, new LinearLayout.LayoutParams(-1, -2));
        return input;
    }

    private MaterialButton calculatorButton(String title) {
        MaterialButton button = new MaterialButton(this);
        button.setText(title);
        button.setTextAllCaps(false);
        button.setTextSize(17);
        button.setTextColor(ContextCompat.getColor(this, R.color.primary));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        button.setStrokeWidth(dp(1));
        button.setCornerRadius(dp(14));
        return button;
    }

    private LinearLayout.LayoutParams calculatorButtonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(54));
        lp.setMargins(0, 0, 0, dp(10));
        return lp;
    }

    private TextView drawerItem(String label) {
        TextView item = text(label, 18, R.color.text_main, false);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(24), 0, dp(16), 0);
        item.setClickable(true);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(64)));
        return item;
    }

    private TextView text(String value, int size, int colorRes, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, colorRes));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView resultLabel() {
        return text("", 14, R.color.result_secondary, false);
    }

    private TextView resultValue(int size) {
        return text("", size, R.color.white, true);
    }

    private LinearLayout.LayoutParams valueParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(2), 0, dp(12));
        return lp;
    }

    private double positiveDouble(TextInputEditText input) {
        double value = Double.parseDouble(clean(text(input)));
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private double nonNegativeDouble(TextInputEditText input) {
        double value = Double.parseDouble(clean(text(input)));
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private int positiveInt(TextInputEditText input) {
        double value = Double.parseDouble(clean(text(input)));
        if (value <= 0 || value != Math.floor(value) || value > Integer.MAX_VALUE) throw new IllegalArgumentException();
        return (int) value;
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private String clean(String value) {
        return value.trim().replace(" ", "").replace("\u00A0", "").replace("\u202F", "").replace(',', '.');
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private static class MoneyTextWatcher implements TextWatcher {
        private final TextInputEditText input;
        private boolean enabled;
        private boolean editing;

        MoneyTextWatcher(TextInputEditText input) {
            this.input = input;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable editable) {
            if (!enabled || editing) return;
            String source = editable.toString().replace(" ", "").replace("\u00A0", "").replace("\u202F", "");
            if (source.isEmpty()) return;
            int comma = source.indexOf(',');
            int dot = source.indexOf('.');
            int separator = comma >= 0 && dot >= 0 ? Math.min(comma, dot) : Math.max(comma, dot);
            String integerPart = separator >= 0 ? source.substring(0, separator) : source;
            String fraction = separator >= 0 ? source.substring(separator + 1) : "";
            integerPart = integerPart.replaceAll("[^0-9]", "");
            fraction = fraction.replaceAll("[^0-9]", "");
            if (integerPart.isEmpty()) return;
            StringBuilder grouped = new StringBuilder();
            for (int i = 0; i < integerPart.length(); i++) {
                if (i > 0 && (integerPart.length() - i) % 3 == 0) grouped.append(' ');
                grouped.append(integerPart.charAt(i));
            }
            if (separator >= 0) grouped.append(',').append(fraction);
            String formatted = grouped.toString();
            if (!formatted.equals(editable.toString())) {
                editing = true;
                input.setText(formatted);
                input.setSelection(formatted.length());
                editing = false;
            }
        }
    }
}
