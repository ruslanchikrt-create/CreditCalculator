package com.example.creditcalculator;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.Calendar;

public class AddReminderActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_PRINCIPAL = "principal";
    public static final String EXTRA_RATE = "rate";
    public static final String EXTRA_MONTHS = "months";
    public static final String EXTRA_PAYMENT = "payment";

    private static final int NOTIFICATION_PERMISSION_REQUEST = 3010;

    private ScrollView formScroll;
    private Spinner typeSpinner;
    private Spinner termUnitSpinner;
    private Spinner daysSpinner;
    private TextInputEditText titleInput;
    private TextInputEditText principalInput;
    private TextInputEditText rateInput;
    private TextInputEditText termInput;
    private TextInputEditText paymentInput;
    private TextInputEditText dateInput;
    private Calendar selectedDate;
    private boolean updatingPayment;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPreferences.wrapLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(buildContent());
        setupSpinners();
        setupInputs();
        applySuggestions();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(12), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setBackgroundResource(android.R.drawable.list_selector_background);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView barTitle = new TextView(this);
        barTitle.setText(AppPreferences.tr(this, "Новое напоминание", "New reminder"));
        barTitle.setTextColor(Color.WHITE);
        barTitle.setTextSize(20);
        barTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        barTitle.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(barTitle, new LinearLayout.LayoutParams(0, -1, 1f));

        formScroll = new ScrollView(this);
        formScroll.setFillViewport(true);
        formScroll.setClipToPadding(false);
        root.addView(formScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        formScroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView heading = text(AppPreferences.tr(this, "Добавить платёж", "Add payment"), 28, R.color.text_main, true);
        content.addView(heading);

        TextView subtitle = text(AppPreferences.tr(this,
                "Приложение рассчитает даты платежей и заранее напомнит об оплате.",
                "The app will calculate payment dates and remind you before each payment."),
                15, R.color.text_secondary, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.setMargins(0, dp(6), 0, dp(20));
        content.addView(subtitle, subtitleParams);

        addLabel(content, AppPreferences.tr(this, "Тип", "Type"));
        typeSpinner = createSpinner();
        content.addView(typeSpinner, fullSpinnerParams());

        titleInput = addField(content, AppPreferences.tr(this, "Название", "Name"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        principalInput = addField(content, AppPreferences.tr(this, "Сумма, которую взяли, ₽", "Amount borrowed, ₽"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rateInput = addField(content, AppPreferences.tr(this, "Процентная ставка, % годовых", "Interest rate, % per year"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        addTermRow(content);

        paymentInput = addField(content,
                AppPreferences.tr(this, "Ежемесячный платёж, ₽ (можно изменить)", "Monthly payment, ₽ (editable)"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        dateInput = addField(content, AppPreferences.tr(this, "Дата первого платежа", "First payment date"), InputType.TYPE_NULL);
        dateInput.setFocusable(false);
        dateInput.setClickable(true);
        dateInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_my_calendar, 0);
        dateInput.setOnClickListener(v -> showDatePicker());

        addLabel(content, AppPreferences.tr(this, "Напомнить до платежа", "Remind before payment"));
        daysSpinner = createSpinner();
        content.addView(daysSpinner, fullSpinnerParams());

        MaterialButton save = new MaterialButton(this);
        save.setText(AppPreferences.tr(this, "Сохранить", "Save"));
        save.setAllCaps(false);
        save.setTextSize(17);
        save.setTextColor(Color.WHITE);
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        save.setCornerRadius(dp(14));
        save.setOnClickListener(v -> saveReminder());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(56));
        saveParams.setMargins(0, dp(6), 0, dp(16));
        content.addView(save, saveParams);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(0, bars.top, 0, 0);
            int bottom = Math.max(bars.bottom, ime.bottom);
            formScroll.setPadding(0, 0, 0, bottom + dp(16));
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                formScroll.postDelayed(this::scrollCurrentFocusIntoView, 120);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void addTermRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(12));
        parent.addView(row, rowParams);

        TextInputLayout layout = createInputLayout(AppPreferences.tr(this, "Срок", "Term"));
        LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(layout, inputLayoutParams);

        termInput = createInput(InputType.TYPE_CLASS_NUMBER);
        layout.addView(termInput, new TextInputLayout.LayoutParams(-1, -2));

        termUnitSpinner = createSpinner();
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(dp(112), dp(58));
        unitParams.setMargins(dp(8), 0, 0, 0);
        row.addView(termUnitSpinner, unitParams);
    }

    private void setupSpinners() {
        String[] types = {
                FormatUtils.typeLabel(this, ReminderScheduler.TYPE_CREDIT),
                FormatUtils.typeLabel(this, ReminderScheduler.TYPE_MORTGAGE),
                FormatUtils.typeLabel(this, ReminderScheduler.TYPE_AUTO),
                FormatUtils.typeLabel(this, ReminderScheduler.TYPE_INSTALLMENT),
                FormatUtils.typeLabel(this, ReminderScheduler.TYPE_DEPOSIT),
                FormatUtils.typeLabel(this, ReminderScheduler.TYPE_OTHER)
        };
        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        termUnitSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{AppPreferences.tr(this, "мес.", "mo."), AppPreferences.tr(this, "лет", "yr.")}));

        String[] days = new String[7];
        for (int i = 0; i < days.length; i++) {
            int value = i + 1;
            days[i] = AppPreferences.isEnglish(this)
                    ? value + (value == 1 ? " day before" : " days before")
                    : "За " + value + " " + russianDays(value);
        }
        daysSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, days));
        daysSpinner.setSelection(2);
    }

    private void setupInputs() {
        principalInput.addTextChangedListener(new MoneyWatcher(principalInput, this::updateAutoPayment));
        paymentInput.addTextChangedListener(new MoneyWatcher(paymentInput, null));
        rateInput.addTextChangedListener(new SimpleWatcher(this::updateAutoPayment));
        termInput.addTextChangedListener(new SimpleWatcher(this::updateAutoPayment));
        termUnitSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(this::updateAutoPayment));
        typeSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(() -> {
            updateRateAvailability();
            updateAutoPayment();
        }));

        setupAutoScroll(titleInput);
        setupAutoScroll(principalInput);
        setupAutoScroll(rateInput);
        setupAutoScroll(termInput);
        setupAutoScroll(paymentInput);
    }

    private void setupAutoScroll(View field) {
        field.setOnFocusChangeListener((view, focused) -> {
            if (focused) {
                formScroll.postDelayed(() -> scrollFieldIntoView(view), 220);
                formScroll.postDelayed(() -> scrollFieldIntoView(view), 480);
            }
        });
    }

    private void scrollCurrentFocusIntoView() {
        View focused = getCurrentFocus();
        if (focused != null && focused != formScroll) scrollFieldIntoView(focused);
    }

    private void scrollFieldIntoView(View field) {
        if (formScroll == null || field == null) return;
        Rect rect = new Rect();
        field.getDrawingRect(rect);
        formScroll.offsetDescendantRectToMyCoords(field, rect);
        int target = Math.max(0, rect.top - dp(72));
        formScroll.smoothScrollTo(0, target);
    }

    private void applySuggestions() {
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type != null) typeSpinner.setSelection(FormatUtils.typePosition(type));

        double principal = getIntent().getDoubleExtra(EXTRA_PRINCIPAL, 0.0);
        double rate = getIntent().getDoubleExtra(EXTRA_RATE, 0.0);
        int months = getIntent().getIntExtra(EXTRA_MONTHS, 0);
        double payment = getIntent().getDoubleExtra(EXTRA_PAYMENT, 0.0);

        String chosenType = FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());
        titleInput.setText(FormatUtils.typeLabel(this, chosenType));
        if (principal > 0) principalInput.setText(formatInput(principal));
        if (rate >= 0 && (principal > 0 || rate > 0)) rateInput.setText(trimNumber(rate));

        if (months > 0 && months % 12 == 0) {
            termUnitSpinner.setSelection(1);
            termInput.setText(String.valueOf(months / 12));
        } else if (months > 0) {
            termUnitSpinner.setSelection(0);
            termInput.setText(String.valueOf(months));
        } else {
            termUnitSpinner.setSelection(0);
        }

        if (payment > 0) {
            updatingPayment = true;
            paymentInput.setText(formatInput(payment));
            updatingPayment = false;
        } else {
            updateAutoPayment();
        }
        updateRateAvailability();
    }

    private void updateRateAvailability() {
        String type = FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());
        boolean installment = ReminderScheduler.TYPE_INSTALLMENT.equals(type);
        boolean depositOrOther = ReminderScheduler.TYPE_DEPOSIT.equals(type) || ReminderScheduler.TYPE_OTHER.equals(type);
        rateInput.setEnabled(!installment && !depositOrOther);
        if (installment) rateInput.setText("0");
        else if (depositOrOther) rateInput.setText("");
    }

    private void updateAutoPayment() {
        if (updatingPayment || principalInput == null || termInput == null || termUnitSpinner == null || typeSpinner == null) return;
        try {
            double principal = parsePositive(principalInput);
            int months = termMonths();
            String type = FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());
            double payment;
            if (ReminderScheduler.TYPE_INSTALLMENT.equals(type)) {
                payment = principal / months;
            } else if (ReminderScheduler.TYPE_CREDIT.equals(type)
                    || ReminderScheduler.TYPE_MORTGAGE.equals(type)
                    || ReminderScheduler.TYPE_AUTO.equals(type)) {
                payment = annuity(principal, months, parseNonNegative(rateInput));
            } else {
                return;
            }
            updatingPayment = true;
            paymentInput.setText(formatInput(payment));
            paymentInput.setSelection(paymentInput.length());
            updatingPayment = false;
        } catch (Exception ignored) {}
    }

    private int termMonths() {
        int value = Integer.parseInt(text(termInput).trim());
        if (value <= 0) throw new IllegalArgumentException();
        if (termUnitSpinner.getSelectedItemPosition() == 1) {
            if (value > Integer.MAX_VALUE / 12) throw new IllegalArgumentException();
            return value * 12;
        }
        return value;
    }

    private void showDatePicker() {
        Calendar base = selectedDate == null ? Calendar.getInstance() : selectedDate;
        DatePickerDialog picker = new DatePickerDialog(this, (dialog, year, month, dayOfMonth) -> {
            Calendar value = Calendar.getInstance();
            value.clear();
            value.set(year, month, dayOfMonth, 9, 0, 0);
            selectedDate = value;
            dateInput.setText(FormatUtils.date(this, value.getTimeInMillis()));
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(System.currentTimeMillis() - 86400000L);
        picker.show();
    }

    private void saveReminder() {
        try {
            String type = FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());
            String title = text(titleInput).trim();
            if (title.isEmpty()) title = FormatUtils.typeLabel(this, type);
            double principal = parsePositive(principalInput);
            double annualRate = ReminderScheduler.TYPE_INSTALLMENT.equals(type)
                    || ReminderScheduler.TYPE_DEPOSIT.equals(type)
                    || ReminderScheduler.TYPE_OTHER.equals(type)
                    ? 0.0 : parseNonNegative(rateInput);
            double payment = parsePositive(paymentInput);
            int months = termMonths();
            int daysBefore = daysSpinner.getSelectedItemPosition() + 1;

            if (selectedDate == null) {
                Toast.makeText(this, AppPreferences.tr(this,
                        "Выберите дату первого платежа", "Choose the first payment date"), Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar lastPayment = ReminderScheduler.buildDueDate(selectedDate.getTimeInMillis(), months - 1);
            if (lastPayment.getTimeInMillis() < System.currentTimeMillis()) {
                Toast.makeText(this, AppPreferences.tr(this,
                        "Срок платежей уже закончился", "The payment term has already ended"), Toast.LENGTH_SHORT).show();
                return;
            }

            ReminderScheduler.PaymentReminder reminder = new ReminderScheduler.PaymentReminder(
                    System.currentTimeMillis(), type, title, principal, annualRate, payment,
                    selectedDate.getTimeInMillis(), months, daysBefore);
            ReminderScheduler.add(this, reminder);
            requestNotificationPermissionIfNeeded();
            Toast.makeText(this, AppPreferences.tr(this, "Платёж сохранён", "Payment saved"), Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, AppPreferences.tr(this,
                    "Проверьте заполненные поля", "Check the entered values"), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private double annuity(double principal, int months, double annualRate) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        if (monthlyRate == 0.0) return principal / months;
        double factor = Math.pow(1.0 + monthlyRate, months);
        return principal * monthlyRate * factor / (factor - 1.0);
    }

    private TextInputEditText addField(LinearLayout parent, String hint, int inputType) {
        TextInputLayout layout = createInputLayout(hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        parent.addView(layout, params);
        TextInputEditText input = createInput(inputType);
        layout.addView(input, new TextInputLayout.LayoutParams(-1, -2));
        return input;
    }

    private TextInputLayout createInputLayout(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14));
        return layout;
    }

    private TextInputEditText createInput(int inputType) {
        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(inputType);
        input.setSingleLine(true);
        input.setMinHeight(dp(58));
        return input;
    }

    private Spinner createSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(android.R.drawable.editbox_background);
        spinner.setPadding(dp(10), 0, dp(10), 0);
        return spinner;
    }

    private LinearLayout.LayoutParams fullSpinnerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(56));
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private void addLabel(LinearLayout parent, String value) {
        TextView label = text(value, 14, R.color.text_secondary, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(6));
        parent.addView(label, params);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, color));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private double parsePositive(TextInputEditText input) {
        double value = Double.parseDouble(cleanNumber(text(input)));
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private double parseNonNegative(TextInputEditText input) {
        String source = cleanNumber(text(input));
        double value = source.isEmpty() ? 0.0 : Double.parseDouble(source);
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private String cleanNumber(String value) {
        return value.replace(" ", "").replace("\u00A0", "").replace("\u202F", "").replace(',', '.');
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private String formatInput(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(FormatUtils.locale(this));
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(value).replace('\u00A0', ' ').replace('\u202F', ' ');
    }

    private String trimNumber(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.valueOf(value).replace('.', ',');
    }

    private String russianDays(int value) {
        int mod100 = value % 100;
        int mod10 = value % 10;
        if (mod100 >= 11 && mod100 <= 14) return "дней";
        if (mod10 == 1) return "день";
        if (mod10 >= 2 && mod10 <= 4) return "дня";
        return "дней";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SimpleWatcher implements TextWatcher {
        private final Runnable callback;
        SimpleWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { if (callback != null) callback.run(); }
    }

    private static class MoneyWatcher implements TextWatcher {
        private final TextInputEditText input;
        private final Runnable callback;
        private boolean editing;
        MoneyWatcher(TextInputEditText input, Runnable callback) {
            this.input = input;
            this.callback = callback;
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable editable) {
            if (!editing) {
                String formatted = group(editable.toString());
                if (!formatted.equals(editable.toString())) {
                    editing = true;
                    input.setText(formatted);
                    input.setSelection(formatted.length());
                    editing = false;
                }
            }
            if (callback != null) callback.run();
        }
        private String group(String source) {
            String clean = source.replace(" ", "").replace("\u00A0", "").replace("\u202F", "");
            if (clean.isEmpty()) return "";
            int comma = clean.indexOf(',');
            int dot = clean.indexOf('.');
            int separator = comma >= 0 && dot >= 0 ? Math.min(comma, dot) : Math.max(comma, dot);
            String integerPart = separator >= 0 ? clean.substring(0, separator) : clean;
            String fractionPart = separator >= 0 ? clean.substring(separator + 1) : "";
            integerPart = integerPart.replaceAll("[^0-9]", "");
            fractionPart = fractionPart.replaceAll("[^0-9]", "");
            if (integerPart.isEmpty()) integerPart = "0";
            StringBuilder grouped = new StringBuilder();
            for (int i = 0; i < integerPart.length(); i++) {
                if (i > 0 && (integerPart.length() - i) % 3 == 0) grouped.append(' ');
                grouped.append(integerPart.charAt(i));
            }
            if (separator >= 0) grouped.append(',').append(fractionPart);
            return grouped.toString();
        }
    }

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable callback;
        SimpleItemSelectedListener(Runnable callback) { this.callback = callback; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            if (callback != null) callback.run();
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }
}
