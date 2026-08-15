package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private enum CalculatorMode { CREDIT, MORTGAGE, AUTO, INSTALLMENT, DEPOSIT }

    private DrawerLayout drawerLayout;
    private FrameLayout mainFrame;
    private LinearLayout drawerView;
    private LinearLayout mainColumn;
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

    private Spinner secondTermSpinner;
    private Spinner thirdTermSpinner;
    private ArrayAdapter<String> secondTermAdapter;
    private ArrayAdapter<String> thirdTermAdapter;
    private SwitchMaterial capitalizationSwitch;

    private TextView calculatorTitle;
    private TextView calculatorSubtitle;
    private TextView resultLabel1;
    private TextView resultLabel2;
    private TextView resultLabel3;
    private TextView resultValue1;
    private TextView resultValue2;
    private TextView resultValue3;

    private ImageView drawerAvatar;
    private TextView drawerAvatarFallback;
    private TextView drawerName;

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
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        loadedLanguage = AppPreferences.getLanguage(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(buildScreen());
        setupInputWatchers();
        setupListeners();
        formCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        refreshProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loadedLanguage != null && !loadedLanguage.equals(AppPreferences.getLanguage(this))) {
            recreate();
            return;
        }
        if (mainColumn != null) UiUtils.applyBackground(this, mainColumn);
        refreshProfile();
    }

    private View buildScreen() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setFitsSystemWindows(false);
        drawerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        mainFrame = new FrameLayout(this);
        drawerLayout.addView(mainFrame, new DrawerLayout.LayoutParams(-1, -1));

        mainColumn = new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        UiUtils.applyBackground(this, mainColumn);
        mainFrame.addView(mainColumn, new FrameLayout.LayoutParams(-1, -1));

        mainColumn.addView(buildTopBar(), new LinearLayout.LayoutParams(-1, dp(56)));

        mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(true);
        mainScroll.setClipToPadding(false);
        mainScroll.setBackgroundColor(Color.TRANSPARENT);
        mainColumn.addView(mainScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

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
        plus.setAllCaps(false);
        plus.setTextSize(30);
        plus.setTextColor(Color.WHITE);
        plus.setMinWidth(0);
        plus.setPadding(0, 0, 0, 0);
        plus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        plus.setCornerRadius(dp(30));
        plus.setOnClickListener(v -> openAddReminder());
        FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM | Gravity.END);
        plusParams.setMargins(0, 0, dp(20), dp(20));
        mainFrame.addView(plus, plusParams);

        drawerView = (LinearLayout) buildDrawer();
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(dp(310), -1);
        drawerParams.gravity = GravityCompat.START;
        drawerLayout.addView(drawerView, drawerParams);

        applyInsets();
        return drawerLayout;
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            mainFrame.setPadding(0, bars.top, 0, bars.bottom);
            drawerView.setPadding(0, bars.top, 0, bars.bottom);
            int bottom = Math.max(dp(88), ime.bottom + dp(16));
            mainScroll.setPadding(0, 0, 0, bottom);
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                mainScroll.postDelayed(this::scrollCurrentFocusIntoView, 100);
                mainScroll.postDelayed(this::scrollCurrentFocusIntoView, 320);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(drawerLayout);
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(12), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));

        TextView menu = label("☰", 30, R.color.white, false);
        menu.setGravity(Gravity.CENTER);
        menu.setClickable(true);
        menu.setFocusable(true);
        menu.setBackgroundResource(android.R.drawable.list_selector_background);
        menu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        bar.addView(menu, new LinearLayout.LayoutParams(dp(60), dp(56)));

        TextView title = label(AppPreferences.tr(this, "Финансовый калькулятор", "Financial calculator"), 20, R.color.white, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));
        return bar;
    }

    private View buildDrawer() {
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(20), dp(12), dp(20));
        header.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        drawer.addView(header, new LinearLayout.LayoutParams(-1, dp(168)));

        FrameLayout avatarBox = new FrameLayout(this);
        header.addView(avatarBox, new LinearLayout.LayoutParams(dp(72), dp(72)));

        drawerAvatarFallback = label("%", 40, R.color.white, true);
        drawerAvatarFallback.setGravity(Gravity.CENTER);
        drawerAvatarFallback.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark));
        avatarBox.addView(drawerAvatarFallback, new FrameLayout.LayoutParams(-1, -1));

        drawerAvatar = new ImageView(this);
        drawerAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        drawerAvatar.setVisibility(View.GONE);
        avatarBox.addView(drawerAvatar, new FrameLayout.LayoutParams(-1, -1));

        drawerName = label(AppPreferences.tr(this, "Финансовый\nкалькулятор", "Financial\ncalculator"), 22, R.color.white, false);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
        nameParams.setMargins(dp(16), 0, dp(4), 0);
        header.addView(drawerName, nameParams);

        TextView edit = label("✎", 25, R.color.white, false);
        edit.setGravity(Gravity.CENTER);
        edit.setClickable(true);
        edit.setFocusable(true);
        edit.setBackgroundResource(android.R.drawable.list_selector_background);
        edit.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ProfileActivity.class));
        });
        header.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView calculators = drawerItem("▶   " + AppPreferences.tr(this, "Калькуляторы", "Calculators"));
        calculators.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            mainScroll.smoothScrollTo(0, 0);
        });
        drawer.addView(calculators);

        TextView payments = drawerItem("☷   " + AppPreferences.tr(this, "Мои платежи", "My payments"));
        payments.setOnClickListener(v -> openDrawerPage(PaymentsActivity.class));
        drawer.addView(payments);

        TextView archive = drawerItem("▣   " + AppPreferences.tr(this, "Архив", "Archive"));
        archive.setOnClickListener(v -> openDrawerPage(ArchiveActivity.class));
        drawer.addView(archive);

        TextView trash = drawerItem("⌫   " + AppPreferences.tr(this, "Корзина", "Trash"));
        trash.setOnClickListener(v -> openDrawerPage(TrashActivity.class));
        drawer.addView(trash);

        TextView settings = drawerItem("⚙   " + AppPreferences.tr(this, "Настройки", "Settings"));
        settings.setOnClickListener(v -> openDrawerPage(SettingsActivity.class));
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
                            "Кредит, ипотека, автокредит, рассрочка и вклад. Сохраняйте платежи, смотрите полный график, архив и получайте уведомления заранее.",
                            "Loan, mortgage, auto loan, installment and deposit calculators. Save payments, view schedules and archive, and receive reminders."))
                    .setPositiveButton("OK", null).show();
        });
        drawer.addView(about);

        TextView exit = drawerItem("↪   " + AppPreferences.tr(this, "Выход", "Exit"));
        exit.setOnClickListener(v -> finishAffinity());
        drawer.addView(exit);
        return drawer;
    }

    private void openDrawerPage(Class<?> page) {
        drawerLayout.closeDrawer(GravityCompat.START);
        startActivity(new Intent(this, page));
    }

    private void refreshProfile() {
        if (drawerName == null || drawerAvatar == null || drawerAvatarFallback == null) return;
        String profileName = AppPreferences.getProfileName(this);
        drawerName.setText(profileName.isEmpty()
                ? AppPreferences.tr(this, "Финансовый\nкалькулятор", "Financial\ncalculator")
                : profileName);
        String avatar = AppPreferences.getAvatarUri(this);
        if (avatar == null || avatar.trim().isEmpty()) {
            drawerAvatar.setImageDrawable(null);
            drawerAvatar.setVisibility(View.GONE);
            drawerAvatarFallback.setVisibility(View.VISIBLE);
            return;
        }
        try {
            drawerAvatar.setImageURI(Uri.parse(avatar));
            drawerAvatar.setVisibility(View.VISIBLE);
            drawerAvatarFallback.setVisibility(View.GONE);
        } catch (Exception e) {
            drawerAvatar.setVisibility(View.GONE);
            drawerAvatarFallback.setVisibility(View.VISIBLE);
        }
    }

    private MaterialCardView buildFormCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
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

        amountLayout = addFullInput(box);
        amountInput = addInput(amountLayout);

        LinearLayout secondRow = createFieldRow(box);
        secondLayout = createInputLayout();
        secondRow.addView(secondLayout, new LinearLayout.LayoutParams(0, -2, 1f));
        secondInput = addInput(secondLayout);
        secondTermSpinner = createTermSpinner();
        secondTermAdapter = createTermAdapter(1);
        secondTermSpinner.setAdapter(secondTermAdapter);
        secondRow.addView(secondTermSpinner, unitParams());

        LinearLayout thirdRow = createFieldRow(box);
        thirdLayout = createInputLayout();
        thirdRow.addView(thirdLayout, new LinearLayout.LayoutParams(0, -2, 1f));
        thirdInput = addInput(thirdLayout);
        thirdTermSpinner = createTermSpinner();
        thirdTermAdapter = createTermAdapter(1);
        thirdTermSpinner.setAdapter(thirdTermAdapter);
        thirdRow.addView(thirdTermSpinner, unitParams());

        fourthLayout = addFullInput(box);
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

    private LinearLayout createFieldRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        parent.addView(row, params);
        return row;
    }

    private Spinner createTermSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(android.R.drawable.editbox_background);
        spinner.setPadding(dp(8), 0, dp(8), 0);
        spinner.setVisibility(View.GONE);
        return spinner;
    }

    private ArrayAdapter<String> createTermAdapter(int value) {
        return new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{UiUtils.termUnit(this, value, false), UiUtils.termUnit(this, value, true)});
    }

    private void updateTermAdapter(ArrayAdapter<String> adapter, Spinner spinner, TextInputEditText input) {
        if (adapter == null || spinner == null || input == null) return;
        int value = 1;
        try {
            String raw = clean(text(input));
            double parsed = Double.parseDouble(raw);
            if (parsed > 0 && parsed <= Integer.MAX_VALUE) value = Math.max(1, (int) parsed);
        } catch (Exception ignored) {}
        int selected = spinner.getSelectedItemPosition();
        adapter.clear();
        adapter.add(UiUtils.termUnit(this, value, false));
        adapter.add(UiUtils.termUnit(this, value, true));
        adapter.notifyDataSetChanged();
        spinner.setSelection(Math.max(0, selected));
    }

    private LinearLayout.LayoutParams unitParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(128), dp(58));
        params.setMargins(dp(8), 0, 0, 0);
        return params;
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
        secondInput.addTextChangedListener(new SimpleWatcher(() -> updateTermAdapter(secondTermAdapter, secondTermSpinner, secondInput)));
        thirdInput.addTextChangedListener(new SimpleWatcher(() -> updateTermAdapter(thirdTermAdapter, thirdTermSpinner, thirdInput)));
    }

    private void setupListeners() {
        creditButton.setOnClickListener(v -> selectMode(CalculatorMode.CREDIT));
        mortgageButton.setOnClickListener(v -> selectMode(CalculatorMode.MORTGAGE));
        autoButton.setOnClickListener(v -> selectMode(CalculatorMode.AUTO));
        installmentButton.setOnClickListener(v -> selectMode(CalculatorMode.INSTALLMENT));
        depositButton.setOnClickListener(v -> selectMode(CalculatorMode.DEPOSIT));
        setupAutoScroll(amountInput);
        setupAutoScroll(secondInput);
        setupAutoScroll(thirdInput);
        setupAutoScroll(fourthInput);
    }

    private void setupAutoScroll(View field) {
        field.setOnFocusChangeListener((view, focused) -> {
            if (focused) {
                mainScroll.postDelayed(() -> scrollFieldIntoView(view), 180);
                mainScroll.postDelayed(() -> scrollFieldIntoView(view), 420);
            }
        });
    }

    private void scrollCurrentFocusIntoView() {
        View focused = getCurrentFocus();
        if (focused != null && focused != mainScroll) scrollFieldIntoView(focused);
    }

    private void scrollFieldIntoView(View field) {
        Rect rect = new Rect();
        field.getDrawingRect(rect);
        mainScroll.offsetDescendantRectToMyCoords(field, rect);
        mainScroll.smoothScrollTo(0, Math.max(0, rect.top - dp(72)));
    }

    private void selectMode(CalculatorMode mode) {
        currentMode = mode;
        clearInputs();
        resultCard.setVisibility(View.GONE);
        fourthLayout.setVisibility(View.GONE);
        capitalizationSwitch.setVisibility(View.GONE);
        capitalizationSwitch.setChecked(false);
        secondTermSpinner.setVisibility(View.GONE);
        thirdTermSpinner.setVisibility(View.GONE);
        secondWatcher.setEnabled(mode == CalculatorMode.MORTGAGE || mode == CalculatorMode.AUTO || mode == CalculatorMode.INSTALLMENT);
        lastPrincipal = 0;
        lastRate = 0;
        lastPayment = 0;
        lastMonths = 0;

        if (mode == CalculatorMode.CREDIT) {
            calculatorTitle.setText(AppPreferences.tr(this, "Кредит", "Loan"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте ежемесячный платёж и переплату", "Calculate monthly payment and overpayment"));
            amountLayout.setHint(AppPreferences.tr(this, "Сумма кредита, ₽", "Loan amount, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Срок", "Term"));
            thirdLayout.setHint(AppPreferences.tr(this, "Процентная ставка, % годовых", "Interest rate, % per year"));
            secondTermSpinner.setVisibility(View.VISIBLE);
            secondTermSpinner.setSelection(0);
        } else if (mode == CalculatorMode.MORTGAGE) {
            calculatorTitle.setText(AppPreferences.tr(this, "Ипотека", "Mortgage"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Укажите стоимость жилья, первый взнос, срок и ставку", "Enter property price, down payment, term and rate"));
            amountLayout.setHint(AppPreferences.tr(this, "Стоимость жилья, ₽", "Property price, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
            thirdLayout.setHint(AppPreferences.tr(this, "Срок ипотеки", "Mortgage term"));
            fourthLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
            fourthLayout.setVisibility(View.VISIBLE);
            thirdTermSpinner.setVisibility(View.VISIBLE);
            thirdTermSpinner.setSelection(1);
        } else if (mode == CalculatorMode.AUTO) {
            calculatorTitle.setText(AppPreferences.tr(this, "Автокредит", "Auto loan"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте платёж по кредиту на автомобиль", "Calculate your auto loan payment"));
            amountLayout.setHint(AppPreferences.tr(this, "Стоимость автомобиля, ₽", "Car price, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
            thirdLayout.setHint(AppPreferences.tr(this, "Срок кредита", "Loan term"));
            fourthLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
            fourthLayout.setVisibility(View.VISIBLE);
            thirdTermSpinner.setVisibility(View.VISIBLE);
            thirdTermSpinner.setSelection(0);
        } else if (mode == CalculatorMode.INSTALLMENT) {
            calculatorTitle.setText(AppPreferences.tr(this, "Рассрочка", "Installment"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте платёж без процентов", "Calculate an interest-free installment"));
            amountLayout.setHint(AppPreferences.tr(this, "Стоимость покупки, ₽", "Purchase price, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Первоначальный взнос, ₽", "Down payment, ₽"));
            thirdLayout.setHint(AppPreferences.tr(this, "Срок рассрочки", "Installment term"));
            thirdTermSpinner.setVisibility(View.VISIBLE);
            thirdTermSpinner.setSelection(0);
        } else {
            calculatorTitle.setText(AppPreferences.tr(this, "Вклад", "Deposit"));
            calculatorSubtitle.setText(AppPreferences.tr(this, "Рассчитайте доход и итоговую сумму вклада", "Calculate deposit income and final amount"));
            amountLayout.setHint(AppPreferences.tr(this, "Сумма вклада, ₽", "Deposit amount, ₽"));
            secondLayout.setHint(AppPreferences.tr(this, "Срок вклада", "Deposit term"));
            thirdLayout.setHint(AppPreferences.tr(this, "Ставка, % годовых", "Interest rate, % per year"));
            capitalizationSwitch.setVisibility(View.VISIBLE);
            secondTermSpinner.setVisibility(View.VISIBLE);
            secondTermSpinner.setSelection(0);
        }
        updateTermAdapter(secondTermAdapter, secondTermSpinner, secondInput);
        updateTermAdapter(thirdTermAdapter, thirdTermSpinner, thirdInput);
        updateButtonStyles();
        formCard.setVisibility(View.VISIBLE);
        formCard.post(() -> mainScroll.smoothScrollTo(0, formCard.getTop()));
    }

    private Spinner activeTermSpinner() {
        return (currentMode == CalculatorMode.CREDIT || currentMode == CalculatorMode.DEPOSIT)
                ? secondTermSpinner : thirdTermSpinner;
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
        int months = termMonths(secondInput);
        double rate = nonNegative(thirdInput);
        double[] values = annuity(principal, months, rate);
        remember(principal, rate, months, values[0]);
        showMoneyResult(AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), values[0],
                AppPreferences.tr(this, "Общая сумма выплат", "Total payments"), values[1],
                AppPreferences.tr(this, "Переплата", "Overpayment"), values[2]);
    }

    private void calculateMortgage() {
        double price = positive(amountInput);
        double down = nonNegative(secondInput);
        int months = termMonths(thirdInput);
        double rate = nonNegative(fourthInput);
        if (down >= price) throw new IllegalArgumentException();
        double principal = price - down;
        double[] values = annuity(principal, months, rate);
        remember(principal, rate, months, values[0]);
        showMoneyResult(AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), values[0],
                AppPreferences.tr(this, "Всего выплат банку", "Total paid to bank"), values[1],
                AppPreferences.tr(this, "Переплата по процентам", "Interest overpayment"), values[2]);
    }

    private void calculateAuto() {
        double price = positive(amountInput);
        double down = nonNegative(secondInput);
        int months = termMonths(thirdInput);
        double rate = nonNegative(fourthInput);
        if (down >= price) throw new IllegalArgumentException();
        double principal = price - down;
        double[] values = annuity(principal, months, rate);
        remember(principal, rate, months, values[0]);
        showMoneyResult(AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), values[0],
                AppPreferences.tr(this, "Всего выплат банку", "Total paid to bank"), values[1],
                AppPreferences.tr(this, "Переплата по процентам", "Interest overpayment"), values[2]);
    }

    private void calculateInstallment() {
        double price = positive(amountInput);
        double down = nonNegative(secondInput);
        int months = termMonths(thirdInput);
        if (down >= price) throw new IllegalArgumentException();
        double principal = price - down;
        double payment = principal / months;
        remember(principal, 0, months, payment);
        showMoneyResult(AppPreferences.tr(this, "Ежемесячный платёж", "Monthly payment"), payment,
                AppPreferences.tr(this, "Сумма в рассрочку", "Installment amount"), principal,
                AppPreferences.tr(this, "Общая стоимость", "Total purchase price"), price);
    }

    private void calculateDeposit() {
        double principal = positive(amountInput);
        int months = termMonths(secondInput);
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

    private int termMonths(TextInputEditText input) {
        int value = positiveInt(input);
        if (activeTermSpinner().getSelectedItemPosition() == 1) {
            if (value > Integer.MAX_VALUE / 12) throw new IllegalArgumentException();
            return value * 12;
        }
        return value;
    }

    private double[] annuity(double principal, int months, double annualRate) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        double payment;
        if (monthlyRate == 0) payment = principal / months;
        else {
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

    private void showMoneyResult(String l1, double v1, String l2, double v2, String l3, double v3) {
        resultLabel1.setText(l1);
        resultValue1.setText(FormatUtils.money(this, v1));
        resultLabel2.setText(l2);
        resultValue2.setText(FormatUtils.money(this, v2));
        resultLabel3.setText(l3);
        resultValue3.setText(FormatUtils.money(this, v3));
        showResultCard();
    }

    private void showResultCard() {
        resultCard.setVisibility(View.VISIBLE);
        resultCard.post(() -> scrollFieldIntoView(resultCard));
    }

    private void openAddReminder() {
        Intent intent = new Intent(this, AddReminderActivity.class);
        if (currentMode != null) intent.putExtra(AddReminderActivity.EXTRA_TYPE, typeForMode(currentMode));
        if (lastPrincipal > 0) {
            intent.putExtra(AddReminderActivity.EXTRA_PRINCIPAL, lastPrincipal);
            intent.putExtra(AddReminderActivity.EXTRA_RATE, lastRate);
        }
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

    private void clearInputs() {
        amountInput.setText("");
        secondInput.setText("");
        thirdInput.setText("");
        fourthInput.setText("");
        amountInput.clearFocus();
        secondInput.clearFocus();
        thirdInput.clearFocus();
        fourthInput.clearFocus();
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
        int card = ContextCompat.getColor(this, R.color.card_background);
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primary : card));
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
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background)));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        button.setStrokeWidth(dp(1));
        button.setCornerRadius(dp(15));
        return button;
    }

    private LinearLayout.LayoutParams calculatorButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private TextInputLayout addFullInput(LinearLayout parent) {
        TextInputLayout layout = createInputLayout();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        parent.addView(layout, params);
        return layout;
    }

    private TextInputLayout createInputLayout() {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14));
        return layout;
    }

    private TextInputEditText addInput(TextInputLayout layout) {
        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);
        input.setMinHeight(dp(58));
        layout.addView(input, new TextInputLayout.LayoutParams(-1, -2));
        return input;
    }

    private TextView drawerItem(String text) {
        TextView item = label(text, 18, R.color.text_main, false);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(24), 0, dp(16), 0);
        item.setClickable(true);
        item.setFocusable(true);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(58)));
        return item;
    }

    private TextView resultLabel() { return label("", 14, R.color.result_secondary, false); }
    private TextView resultValue(int size) { return label("", size, R.color.white, true); }

    private LinearLayout.LayoutParams resultSpacing() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private TextView label(String text, int size, int colorRes, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, colorRes));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private double positive(TextInputEditText input) {
        double value = Double.parseDouble(clean(text(input)));
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private double nonNegative(TextInputEditText input) {
        String source = clean(text(input));
        double value = source.isEmpty() ? 0.0 : Double.parseDouble(source);
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private int positiveInt(TextInputEditText input) {
        double raw = Double.parseDouble(clean(text(input)));
        if (raw <= 0 || raw != Math.floor(raw) || raw > Integer.MAX_VALUE || Double.isNaN(raw) || Double.isInfinite(raw)) {
            throw new IllegalArgumentException();
        }
        return (int) raw;
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private String clean(String value) {
        return value.trim().replace(" ", "").replace("\u00A0", "").replace("\u202F", "").replace(',', '.');
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
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
        private boolean enabled;
        private boolean editing;
        MoneyWatcher(TextInputEditText input) { this.input = input; }
        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (enabled && input.getText() != null && input.getText().length() > 0) afterTextChanged(input.getText());
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable editable) {
            if (!enabled || editing) return;
            String formatted = group(editable.toString());
            if (formatted.equals(editable.toString())) return;
            editing = true;
            input.setText(formatted);
            input.setSelection(formatted.length());
            editing = false;
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
}
