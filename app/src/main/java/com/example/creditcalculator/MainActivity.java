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

    private enum CalculatorMode { CREDIT, MORTGAGE, AUTO, INSTALLMENT, DEPOSIT }

    private DrawerLayout drawerLayout;
    private ScrollView mainScroll;
    private MaterialCardView formCard;
    private MaterialCardView resultCard;
    private TextInputLayout amountLayout;
    private TextInputLayout secondLayout;
    private TextInputLayout thirdLayout;
    private TextInputLayout fourthLayout;
    private TextInputEditText amountInput;
    private TextInputEditText secondInput;
    private TextInputEditText thirdInput;
    private TextInputEditText fourthInput;
    private TextView calculatorTitle;
    private TextView calculatorSubtitle;
    private TextView resultLabel1;
    private TextView resultLabel2;
    private TextView resultLabel3;
    private TextView resultValue1;
    private TextView resultValue2;
    private TextView resultValue3;
    private SwitchMaterial capitalizationSwitch;
    private MaterialButton creditButton;
    private MaterialButton mortgageButton;
    private MaterialButton autoButton;
    private MaterialButton installmentButton;
    private MaterialButton depositButton;
    private MoneyWatcher amountWatcher;
    private MoneyWatcher secondWatcher;
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
        setContentView(buildScreen());
        setupInputWatchers();
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

    private View buildScreen() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setFitsSystemWindows(true);

        FrameLayout main = new FrameLayout(this);
        drawerLayout.addView(main, new DrawerLayout.LayoutParams(-1, -1));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundResource(R.drawable.app_background);
        main.addView(column, new FrameLayout.LayoutParams(-1, -1));

        column.addView(buildTopBar(), new LinearLayout.LayoutParams(-1, dp(64)));

        mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(true);
        mainScroll.setClipToPadding(false);
        mainScroll.setPadding(0, 0, 0, dp(88));
        column.addView(mainScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(28));
        mainScroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        content.addView(label(AppPreferences.tr(this, "Калькуляторы", "Calculators"), 28, R.color.text_main, true));
        TextView subtitle = label(AppPreferences.tr(this, "Выберите нужный раздел", "Choose a calculator"), 15, R.color.text_secondary, false);
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

        MaterialButton plus = new MaterialButton(this);
        plus.setText("+");
        plus.setTextSize(30);
        plus.setTextColor(Color.WHITE);
        plus.setMinWidth(0);
        plus.setPadding(0, 0, 0, 0);
        plus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        plus.setCornerRadius(dp(30));
        plus.setOnClickListener(v -> openAddReminder());
        FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM | Gravity.END);
        plusParams.setMargins(0, 0, dp(20), dp(20));
        main.addView(plus, plusParams);

        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(dp(310), -1);
        drawerParams.gravity = GravityCompat.START;
        drawerLayout.addView(buildDrawer(), drawerParams);
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

        TextView title = label(AppPreferences.tr(this, "Финансовый калькулятор", "Financial calculator"), 20, R.color.white, true);
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

        TextView icon = label("%", 40, R.color.white, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark));
        header.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView name = label(AppPreferences.tr(this, "Финансовый\nкалькулятор", "Financial\ncalculator"), 22, R.color.white, false);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
        nameParams.setMargins(dp(16), 0, 0, 0);
        header.addView(name, nameParams);

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

        calculatorTitle = label("", 23, R.color.text_main, true);
        box.addView(calculatorTitle);
        calculatorSubtitle = label("", 14, R.color.text_secondary, false);
        LinearLayout.LayoutParams sub = new LinearLayout.LayoutParams(-1, -2);
        sub.setMargins(0, dp(4), 0, dp(16));
        box.addView(calculatorSubtitle, sub);

        amountLayout = addInputLayout(box);
        amountInput = addInput(amountLayout);
        secondLayout = addInputLayout(box);
        secondInput = addInput(secondLayout);
        thirdLayout = addInputLayout(box);
        thirdInput = addInput(thirdLayout);
        fourthLayout = addInputLayout(box);
        fourthInput = addInput(fourthLayout);

        capitalizationSwitch = new SwitchMaterial(this);
        capitalizationSwitch.setText(AppPreferences.tr(this, "Ежемесячная капитализация процентов", "Monthly interest capitalization"));
        capitalizationSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        capitalizationSwitch.setTextSize(15);
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-1, -2);
        switchParams.setMargins(0, 0, 0, dp(12));
        box.addView(capitalizationSwitch, switchParams);

        MaterialButton calculate = new MaterialButton(this);
        calculate.setText(AppPreferences.tr(this, "Рассчитать", "Calculate"));
        calculate.setAllCaps(false);
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

        TextView title = label(AppPreferences.tr(this, "Результат", "Result"), 18, R.color.white, true);
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
        box.addView(resultValue1, resultSpacing());
        box.addView(resultLabel2);
        box.addView(resultValue2, resultSpacing());
        box.addView(resultLabel3);
        box.addView(resultValue3);
        return card;
    }

    private void setupInputWatchers() {
        amountWatcher = new MoneyWatcher(amountInput);
        secondWatcher = new MoneyWatcher(secondInput);
        amountWatcher.setEnabled(true);
        secondWatcher.setEnabled(false);
        amountInput.addTextChangedListener(amountWatcher);
        secondInput.addTextChangedListener(secondWatcher);
    }

    private void setupListeners() {
        creditButton.setOnClickListener(v -> selectMode(CalculatorMode.CREDIT));
        mortgageButton.setOnClickListener(v -> selectMode(CalculatorMode.MORTGAGE));
        autoButton.setOnClickListener(v -> selectMode(CalculatorMode.AUTO));
        installmentButton.setOnClickListener(v -> selectMode(CalculatorMode.INSTALLMENT));
        depositButton.setOnClickListener(v -> selectMode(CalculatorMode.DEPOSIT));
        autoScroll(amountInput);
        autoScroll(secondInput);
        autoScroll(thirdInput);
        autoScroll(fourthInput);
    }

    private void selectMode(CalculatorMode mode) {
        currentMode = mode;
        clearInputs();
        resultCard.setVisibility(View.GONE);
        fourthLayout.setVisibility(View.GONE);
        capitalizationSwitch.setVisibility(View.GONE);
        capitalizationSwitch.setChecked(false);
        secondWatcher.setEnabled(mode == CalculatorMode.MORTGAGE || mode == CalculatorMode.AUTO || mode == CalculatorMode.INSTALLMENT);
        lastPrincipal = 0;
        lastRate = 0;
        lastPayment = 0;
        lastMonths = 0;

        if (mode == CalculatorMode.CREDIT) {
            calculatorTitle.setText(AppPreferences.tr(this, "Кредит", "Loan"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте ежемесячный платёж и переплату", "Calculate monthly payment and overpayment"));
            amountLayout.setHint(AppPreferences.tr(this, "Сумма кредита, ₽", "Loan amount, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Срок, месяцев", "Term, months"));
            thirdLayout.setHint(AppPreferences.tr(this, "Процентная ставка, % годовых", "Interest rate, % per year"));
        } else if (mode == CalculatorMode.MORTGAGE) {
            calculatorTitle.setText(AppPreferences.tr(this, "Ипотека", "Mortgage"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Укажите стоимость жилья, первый взнос, срок и ставку", "Enter property price, down payment, term and rate"));
            amountLayout.setHint(AppPreferences.tr(this, "Стоимость жилья, ₽", "Property price, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
            thirdLayout.setHint(AppPreferences.tr(this, "Срок ипотеки, лет", "Mortgage term, years"));
            fourthLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
            fourthLayout.setVisibility(View.VISIBLE);
        } else if (mode == CalculatorMode.AUTO) {
            calculatorTitle.setText(AppPreferences.tr(this, "Автокредит", "Auto loan"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте платёж по кредиту на автомобиль", "Calculate your auto loan payment"));
            amountLayout.setHint(AppPreferences.tr(this, "Стоимость автомобиля, ₽", "Car price, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
            thirdLayout.setHint(AppPreferences.tr(this, "Срок кредита, месяцев", "Loan term, months"));
            fourthLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
            fourthLayout.setVisibility(View.VISIBLE);
        } else if (mode == CalculatorMode.INSTALLMENT) {
            calculatorTitle.setText(AppPreferences.tr(this, "Рассрочка", "Installment"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте платёж без процентов", "Calculate an interest-free installment"));
            amountLayout.setHint(AppPreferences.tr(this, "Стоимость покупки, ₽", "Purchase price, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
            thirdLayout.setHint(AppPreferences.tr(this, "Срок рассрочки, месяцев", "Installment term, months"));
        } else {
            calculatorTitle.setText(AppPreferences.tr(this, "Вклад", "Deposit"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте доход и итоговую сумму вклада", "Calculate deposit income and final amount"));
            amountLayout.setHint(AppPreferences.tr(this, "Сумма вклада, ₽", "Deposit amount, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Срок вклада, месяцев", "Deposit term, months"));
            thirdLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
            capitalizationSwitch.setVisibility(View.VISIBLE);
        }

        updateButtonStyles();
        formCard.setVisibility(View.VISIBLE);
        formCard.post(() -> scrollTo(formCard, 80));
    }

    private void calculate() {
        if (currentMode == null) {
            Toast.makeText(this, AppPreferences.tr(this, "Выберите раздел", "Choose a calculator"), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (currentMode == CalculatorMode.CREDIT) calculateCredit();
            else if (currentMode == CalculatorMode.MORTGAGE) calculateMortgage();
            else if (currentMode == CalculatorMode.AUTO) calculateAuto();
            else if (currentMode == CalculatorMode.INSTALLMENT) calculateInstallment();
            else calculateDeposit();
        } catch (Exception e) {
            resultCard.setVisibility(View.GONE);
            Toast.makeText(this, AppPreferences.tr(this, "Заполните все поля правильно", "Check all entered values"), Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateCredit() {
        double principal = positive(amountInput);
        int months = positiveInt(secondInput);
        double rate = nonNegative(thirdInput);
        double[] values = annuity(principal, months, rate);
        remember(principal, rate, months, values[0]);
        showMoneyResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), values[0],
                AppPreferences.tr(this, "Общая сумма выплат", "Total payments"), values[1],
                AppPreferences.tr(this, "Переплата", "Overpayment"), values[2]);
    }

    private void calculateMortgage() {
        double propertyPrice = positive(amountInput);
        double down = nonNegative(secondInput);
        int years = positiveInt(thirdInput);
        double rate = nonNegative(fourthInput);
        if (down >= propertyPrice || years > Integer.MAX_VALUE / 12) throw new IllegalArgumentException();
        int months = years * 12;
        double principal = propertyPrice - down;
        double[] values = annuity(principal, months, rate);
        remember(principal, rate, months, values[0]);
        showMoneyResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), values[0],
                AppPreferences.tr(this, "Всего выплат банку", "Total paid to bank"), values[1],
                AppPreferences.tr(this, "Переплата по процентам", "Interest overpayment"), values[2]);
    }

    private void calculateAuto() {
        double carPrice = positive(amountInput);
        double down = nonNegative(secondInput);
        int months = positiveInt(thirdInput);
        double rate = nonNegative(fourthInput);
        if (down >= carPrice) throw new IllegalArgumentException();
        double principal = carPrice - down;
        double[] values = annuity(principal, months, rate);
        remember(principal, rate, months, values[0]);
        showMoneyResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), values[0],
                AppPreferences.tr(this, "Всего выплат банку", "Total paid to bank"), values[1],
                AppPreferences.tr(this, "Переплата по процентам", "Interest overpayment"), values[2]);
    }

    private void calculateInstallment() {
        double price = positive(amountInput);
        double down = nonNegative(secondInput);
        int months = positiveInt(thirdInput);
        if (down >= price) throw new IllegalArgumentException();
        double principal = price - down;
        double payment = principal / months;
        remember(principal, 0, months, payment);
        showMoneyResult(
                AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), payment,
                AppPreferences.tr(this, "Сумма в рассрочку", "Installment amount"), principal,
                AppPreferences.tr(this, "Общая стоимость", "Total purchase price"), price);
    }

    private void calculateDeposit() {
        double principal = positive(amountInput);
        int months = positiveInt(secondInput);
        double rate = nonNegative(thirdInput);
        double finalAmount;
        if (capitalizationSwitch.isChecked()) {
            double monthlyRate = rate / 100.0 / 12.0;
            finalAmount = principal * Math.pow(1 + monthlyRate, months);
        } else {
            finalAmount = principal + principal * rate / 100.0 * (months / 12.0);
        }
        double income = finalAmount - principal;
        remember(principal, rate, months, 0);
        resultLabel1.setText(AppPreferences.tr(this, "Доход по вкладу", "Deposit income"));
        resultValue1.setText(FormatUtils.money(this, income));
        resultLabel2.setText(AppPreferences.tr(this, "Итоговая сумма", "Final amount"));
        resultValue2.setText(FormatUtils.money(this, finalAmount));
        resultLabel3.setText(AppPreferences.tr(this, "Капитализация", "Capitalization"));
        resultValue3.setText(capitalizationSwitch.isChecked()
                ? AppPreferences.tr(this, "Ежемесячная", "Monthly")
                : AppPreferences.tr(this, "Без капитализации", "No capitalization"));
        showResultCard();
    }

    private double[] annuity(double principal, int months, double annualRate) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        double payment;
        if (monthlyRate == 0) {
            payment = principal / months;
        } else {
            double factor = Math.pow(1 + monthlyRate, months);
            payment = principal * monthlyRate * factor / (factor - 1);
        }
        double total = payment * months;
        return new double[]{payment, total, total - principal};
    }

    private void remember(double principal, double rate, int months, double payment) {
        lastPrincipal = principal;
        lastRate = rate;
        lastMonths = months;
        lastPayment = payment;
    }

    private void showMoneyResult(String label1, double value1, String label2, double value2, String label3, double value3) {
        resultLabel1.setText(label1);
        resultValue1.setText(FormatUtils.money(this, value1));
        resultLabel2.setText(label2);
        resultValue2.setText(FormatUtils.money(this, value2));
        resultLabel3.setText(label3);
        resultValue3.setText(FormatUtils.money(this, value3));
        showResultCard();
    }

    private void showResultCard() {
        resultCard.setVisibility(View.VISIBLE);
        resultCard.post(() -> scrollTo(resultCard, 80));
    }

    private void openAddReminder() {
        Intent intent = new Intent(this, AddReminderActivity.class);
        if (currentMode != null) intent.putExtra(AddReminderActivity.EXTRA_TYPE, typeForMode(currentMode));
        if (lastPrincipal > 0) intent.putExtra(AddReminderActivity.EXTRA_PRINCIPAL, lastPrincipal);
        if (lastPrincipal > 0) intent.putExtra(AddReminderActivity.EXTRA_RATE, lastRate);
        if (lastMonths > 0) intent.putExtra(AddReminderActivity.EXTRA_MONTHS, lastMonths);
        if (lastPayment > 0) intent.putExtra(AddReminderActivity.EXTRA_PAYMENT, lastPayment);
        startActivity(intent);
    }

    private String typeForMode(CalculatorMode mode) {
        if (mode == CalculatorMode.MORTGAGE) return ReminderScheduler.TYPE_MORTGAGE;
        if (mode == CalculatorMode.AUTO) return ReminderScheduler.TYPE_AUTO;
        if (mode == CalculatorMode.INSTALLMENT) return ReminderScheduler.TYPE_INSTALLMENT;
        if (mode == CalculatorMode.DEPOSIT) return ReminderScheduler.TYPE_DEPOSIT;
        return ReminderScheduler.TYPE_CREDIT;
    }

    private void autoScroll(View field) {
        field.setOnFocusChangeListener((view, focused) -> {
            if (focused) mainScroll.postDelayed(() -> scrollTo(view, 120), 250);
        });
    }

    private void scrollTo(View view, int topOffset) {
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        mainScroll.offsetDescendantRectToMyCoords(view, rect);
        mainScroll.smoothScrollTo(0, Math.max(0, rect.top - dp(topOffset)));
    }

    private void clearInputs() {
        amountInput.setText("");
        secondInput.setText("");
        thirdInput.setText("");
        fourthInput.setText("");
    }

    private void updateButtonStyles() {
        styleCalculator(creditButton, currentMode == CalculatorMode.CREDIT);
        styleCalculator(mortgageButton, currentMode == CalculatorMode.MORTGAGE);
        styleCalculator(autoButton, currentMode == CalculatorMode.AUTO);
        styleCalculator(installmentButton, currentMode == CalculatorMode.INSTALLMENT);
        styleCalculator(depositButton, currentMode == CalculatorMode.DEPOSIT);
    }

    private void styleCalculator(MaterialButton button, boolean selected) {
        int primary = ContextCompat.getColor(this, R.color.primary);
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primary : Color.WHITE));
        button.setTextColor(selected ? Color.WHITE : primary);
        button.setStrokeColor(ColorStateList.valueOf(primary));
        button.setStrokeWidth(dp(1));
    }

    private MaterialButton calculatorButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setTextColor(ContextCompat.getColor(this, R.color.primary));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        button.setStrokeWidth(dp(1));
        button.setCornerRadius(dp(14));
        return button;
    }

    private LinearLayout.LayoutParams calculatorButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private TextInputLayout addInputLayout(LinearLayout parent) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        parent.addView(layout, params);
        return layout;
    }

    private TextInputEditText addInput(TextInputLayout layout) {
        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setMinHeight(dp(58));
        layout.addView(input, new LinearLayout.LayoutParams(-1, -2));
        return input;
    }

    private TextView drawerItem(String value) {
        TextView item = label(value, 18, R.color.text_main, false);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(24), 0, dp(16), 0);
        item.setClickable(true);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(64)));
        return item;
    }

    private TextView label(String value, int size, int colorRes, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(ContextCompat.getColor(this, colorRes));
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        return text;
    }

    private TextView resultLabel() {
        return label("", 14, R.color.result_secondary, false);
    }

    private TextView resultValue(int size) {
        return label("", size, R.color.white, true);
    }

    private LinearLayout.LayoutParams resultSpacing() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(2), 0, dp(12));
        return params;
    }

    private double positive(TextInputEditText input) {
        double value = Double.parseDouble(clean(input));
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private double nonNegative(TextInputEditText input) {
        double value = Double.parseDouble(clean(input));
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private int positiveInt(TextInputEditText input) {
        double value = Double.parseDouble(clean(input));
        if (value <= 0 || value != Math.floor(value) || value > Integer.MAX_VALUE) throw new IllegalArgumentException();
        return (int) value;
    }

    private String clean(TextInputEditText input) {
        String value = input.getText() == null ? "" : input.getText().toString();
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

    private static class MoneyWatcher implements TextWatcher {
        private final TextInputEditText input;
        private boolean enabled;
        private boolean editing;

        MoneyWatcher(TextInputEditText input) {
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
            int separator;
            if (comma >= 0 && dot >= 0) separator = Math.min(comma, dot);
            else separator = Math.max(comma, dot);

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
