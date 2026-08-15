package com.example.creditcalculator;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText amountInput, monthsInput, rateInput;
    private TextView monthlyPaymentText, totalPaymentText, overpaymentText;
    private MaterialCardView resultCard;

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
        resultCard = findViewById(R.id.resultCard);
        MaterialButton calcButton = findViewById(R.id.calcButton);

        resultCard.setVisibility(View.GONE);
        calcButton.setOnClickListener(v -> calculate());
    }

    private void calculate() {
        String amountValue = value(amountInput);
        String monthsValue = value(monthsInput);
        String rateValue = value(rateInput);

        if (amountValue.isEmpty() || monthsValue.isEmpty() || rateValue.isEmpty()) {
            resultCard.setVisibility(View.GONE);
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double principal = Double.parseDouble(amountValue);
            int months = Integer.parseInt(monthsValue);
            double annualRate = Double.parseDouble(rateValue);

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
            resultCard.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            resultCard.setVisibility(View.GONE);
            Toast.makeText(this, "Проверьте введённые данные", Toast.LENGTH_SHORT).show();
        }
    }

    private String value(TextInputEditText editText) {
        return editText.getText() == null ? "" :
                editText.getText().toString().trim().replace(",", ".");
    }
}
