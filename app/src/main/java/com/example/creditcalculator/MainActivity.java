package com.example.creditcalculator;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText amountInput, monthsInput, rateInput;
    private TextView monthlyPaymentText, totalPaymentText, overpaymentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        amountInput = findViewById(R.id.amountInput);
        monthsInput = findViewById(R.id.monthsInput);
        rateInput = findViewById(R.id.rateInput);
        monthlyPaymentText = findViewById(R.id.monthlyPaymentText);
        totalPaymentText = findViewById(R.id.totalPaymentText);
        overpaymentText = findViewById(R.id.overpaymentText);
        MaterialButton calcButton = findViewById(R.id.calcButton);

        calcButton.setOnClickListener(v -> calculate());
        calculate();
    }

    private void calculate() {
        try {
            double principal = Double.parseDouble(value(amountInput));
            int months = Integer.parseInt(value(monthsInput));
            double annualRate = Double.parseDouble(value(rateInput));

            if (principal <= 0 || months <= 0 || annualRate < 0) {
                throw new IllegalArgumentException();
            }

            double monthlyRate = annualRate / 100.0 / 12.0;
            double monthlyPayment;

            if (monthlyRate == 0) {
                monthlyPayment = principal / months;
            } else {
                double pow = Math.pow(1 + monthlyRate, months);
                monthlyPayment = principal * monthlyRate * pow / (pow - 1);
            }

            double totalPayment = monthlyPayment * months;
            double overpayment = totalPayment - principal;

            NumberFormat money = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
            money.setMaximumFractionDigits(2);
            money.setMinimumFractionDigits(2);

            monthlyPaymentText.setText(money.format(monthlyPayment) + " ₽");
            totalPaymentText.setText(money.format(totalPayment) + " ₽");
            overpaymentText.setText(money.format(overpayment) + " ₽");

        } catch (Exception e) {
            Toast.makeText(this, "Проверьте введённые данные", Toast.LENGTH_SHORT).show();
        }
    }

    private String value(TextInputEditText editText) {
        return editText.getText() == null ? "" :
                editText.getText().toString().trim().replace(",", ".");
    }
}
