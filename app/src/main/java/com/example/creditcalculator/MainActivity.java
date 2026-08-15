package com.example.creditcalculator;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

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

    private MaterialButton creditButton;
    private MaterialButton mortgageButton;
    private MaterialButton autoButton;
    private MaterialButton installmentButton;
    private MaterialButton depositButton;

    private CalculatorMode currentMode;
    private NumberFormat moneyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupMoneyFormat();
        setupModeButtons();

        formCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
    }

    private void bindViews() {
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
        calcButton.setOnClickListener(v -> calculate());
    }

    private void setupMoneyFormat() {
        moneyFormat = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        moneyFormat.setMaximumFractionDigits(2);
        moneyFormat.setMinimumFractionDigits(2);
    }

    private void setupModeButtons() {
        creditButton.setOnClickListener(v -> selectMode(CalculatorMode.CREDIT));
        mortgageButton.setOnClickListener(v -> selectMode(CalculatorMode.MORTGAGE));
        autoButton.setOnClickListener(v -> selectMode(CalculatorMode.AUTO));
        installmentButton.setOnClickListener(v -> selectMode(CalculatorMode.INSTALLMENT));
        depositButton.setOnClickListener(v -> selectMode(CalculatorMode.DEPOSIT));
    }

    private void selectMode(CalculatorMode mode) {
        currentMode = mode;
        clearInputs();
        resultCard.setVisibility(View.GONE);
        extraLayout.setVisibility(View.GONE);
        capitalizationSwitch.setVisibility(View.GONE);
        capitalizationSwitch.setChecked(false);

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

        double principal = propertyPrice - downPayment;
        int months = Math.multiplyExact(years, 12);
        double[] result = annuity(principal, months, annualRate);

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

        resultLabel1.setText("Доход по вкладу");
        monthlyPaymentText.setText(formatMoney(income));
        resultLabel2.setText("Итоговая сумма");
        totalPaymentText.setText(formatMoney(finalAmount));
        resultLabel3.setText("Капитализация");
        overpaymentText.setText(capitalizationSwitch.isChecked() ? "Ежемесячная" : "Без капитализации");
        resultCard.setVisibility(View.VISIBLE);
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
        if (!Double.isFinite(number) || number <= 0.0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    private double nonNegativeDouble(TextInputEditText editText) {
        double number = Double.parseDouble(value(editText));
        if (!Double.isFinite(number) || number < 0.0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    private int positiveInt(TextInputEditText editText) {
        double raw = Double.parseDouble(value(editText));
        if (!Double.isFinite(raw) || raw <= 0.0 || raw != Math.floor(raw) || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        return (int) raw;
    }

    private String value(TextInputEditText editText) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim().replace(" ", "").replace(",", ".");
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value) + " ₽";
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
}
