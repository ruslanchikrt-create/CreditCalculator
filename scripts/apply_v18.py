from pathlib import Path


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f"Pattern not found: {label}")
    return text.replace(old, new, 1)


add_path = Path("app/src/main/java/com/example/creditcalculator/AddReminderActivity.java")
s = add_path.read_text(encoding="utf-8")

if "EXTRA_EDIT_ID" not in s:
    s = replace_once(
        s,
        '    public static final String EXTRA_PAYMENT = "payment";\n',
        '    public static final String EXTRA_PAYMENT = "payment";\n'
        '    public static final String EXTRA_EDIT_ID = "edit_reminder_id";\n',
        "edit extra",
    )

if "private ReminderScheduler.PaymentReminder editReminder;" not in s:
    s = replace_once(
        s,
        "    private boolean titleEditedByUser;\n",
        "    private boolean titleEditedByUser;\n"
        "    private ReminderScheduler.PaymentReminder editReminder;\n",
        "edit field",
    )

old_on_create = '''        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(buildContent());'''
new_on_create = '''        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        long editId = getIntent().getLongExtra(EXTRA_EDIT_ID, -1L);
        if (editId > 0) editReminder = ReminderScheduler.findById(this, editId);
        setContentView(buildContent());'''
if "long editId = getIntent().getLongExtra(EXTRA_EDIT_ID" not in s:
    s = replace_once(s, old_on_create, new_on_create, "edit init")

old_title = '        barTitle.setText(AppPreferences.tr(this, "Новое напоминание", "New reminder"));'
new_title = '        barTitle.setText(editReminder == null ? AppPreferences.tr(this, "Новое напоминание", "New reminder") : AppPreferences.tr(this, "Редактировать запись", "Edit item"));'
if old_title in s:
    s = replace_once(s, old_title, new_title, "toolbar title")

start_marker = "    private void applySuggestions() {\n"
end_marker = "\n    private double financedPrincipal() {"
start = s.find(start_marker)
end = s.find(end_marker, start)
if start < 0 or end < 0:
    raise RuntimeError("applySuggestions boundaries not found")

new_apply = '''    private void applySuggestions() {
        if (editReminder != null) {
            String chosenType = ReminderScheduler.normalizeType(editReminder.type);
            typeSpinner.setSelection(FormatUtils.typePosition(chosenType));
            updateFieldsForType(false);

            updatingTitle = true;
            titleInput.setText(editReminder.title);
            titleInput.setSelection(titleInput.length());
            updatingTitle = false;
            titleEditedByUser = true;

            if (editReminder.baseAmount > 0) principalInput.setText(formatInput(editReminder.baseAmount));
            if (editReminder.downPayment > 0) downPaymentInput.setText(formatInput(editReminder.downPayment));
            if (editReminder.insurance > 0) insuranceInput.setText(formatInput(editReminder.insurance));
            rateInput.setText(trimNumber(editReminder.annualRate));

            if (editReminder.months > 0 && editReminder.months % 12 == 0) {
                termInput.setText(String.valueOf(editReminder.months / 12));
                termUnitSpinner.setSelection(1);
            } else {
                termInput.setText(String.valueOf(Math.max(1, editReminder.months)));
                termUnitSpinner.setSelection(0);
            }
            updateTermUnitLabels();

            selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(editReminder.firstPaymentMillis);
            dateInput.setText(FormatUtils.date(this, editReminder.firstPaymentMillis));
            daysSpinner.setSelection(Math.max(0, Math.min(6, editReminder.daysBefore - 1)));

            if (!ReminderScheduler.TYPE_DEPOSIT.equals(chosenType) && editReminder.amount > 0) {
                updatingPayment = true;
                paymentInput.setText(formatInput(editReminder.amount));
                paymentInput.setSelection(paymentInput.length());
                updatingPayment = false;
            }
            updateFieldsForType(false);
            return;
        }

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type != null) typeSpinner.setSelection(FormatUtils.typePosition(type));
        String chosenType = FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());
        updateFieldsForType(false);
        titleEditedByUser = false;
        setDefaultTitle(chosenType);

        double legacyPrincipal = getIntent().getDoubleExtra(EXTRA_PRINCIPAL, 0.0);
        double baseAmount = getIntent().getDoubleExtra(EXTRA_BASE_AMOUNT, legacyPrincipal);
        double downPayment = getIntent().getDoubleExtra(EXTRA_DOWN_PAYMENT, 0.0);
        double insurance = getIntent().getDoubleExtra(EXTRA_INSURANCE, 0.0);
        double rate = getIntent().getDoubleExtra(EXTRA_RATE, 0.0);
        int months = getIntent().getIntExtra(EXTRA_MONTHS, 0);
        double payment = getIntent().getDoubleExtra(EXTRA_PAYMENT, 0.0);

        if (baseAmount > 0) principalInput.setText(formatInput(baseAmount));
        if (downPayment > 0) downPaymentInput.setText(formatInput(downPayment));
        if (insurance > 0) insuranceInput.setText(formatInput(insurance));
        if (rate >= 0 && (baseAmount > 0 || rate > 0)) rateInput.setText(trimNumber(rate));

        if (months > 0 && months % 12 == 0) {
            termInput.setText(String.valueOf(months / 12));
            termUnitSpinner.setSelection(1);
        } else if (months > 0) {
            termInput.setText(String.valueOf(months));
            termUnitSpinner.setSelection(0);
        } else {
            termUnitSpinner.setSelection(0);
        }
        updateTermUnitLabels();

        if (!ReminderScheduler.TYPE_DEPOSIT.equals(chosenType) && payment > 0) {
            updatingPayment = true;
            paymentInput.setText(formatInput(payment));
            updatingPayment = false;
        } else {
            updateAutoPayment();
        }
        updateFieldsForType(false);
    }'''
s = s[:start] + new_apply + s[end:]

s = s.replace('        picker.getDatePicker().setMinDate(System.currentTimeMillis() - 86400000L);\n', '')

expired = '''
            Calendar lastPayment = ReminderScheduler.buildDueDate(selectedDate.getTimeInMillis(), months - 1);
            if (lastPayment.getTimeInMillis() < System.currentTimeMillis()) {
                Toast.makeText(this, AppPreferences.tr(this,
                        "Срок уже закончился", "The term has already ended"), Toast.LENGTH_SHORT).show();
                return;
            }
'''
if expired in s:
    s = s.replace(expired, "\n", 1)

old_save = '''            ReminderScheduler.PaymentReminder reminder = new ReminderScheduler.PaymentReminder(
                    System.currentTimeMillis(), type, title,
                    baseAmount, downPayment, insurance, principal,
                    annualRate, payment, selectedDate.getTimeInMillis(), months, daysBefore);
            ReminderScheduler.add(this, reminder);
            requestNotificationPermissionIfNeeded();
            Toast.makeText(this, AppPreferences.tr(this, "Запись сохранена", "Saved"), Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();'''
new_save = '''            long reminderId = editReminder == null ? System.currentTimeMillis() : editReminder.id;
            ReminderScheduler.PaymentReminder reminder = new ReminderScheduler.PaymentReminder(
                    reminderId, type, title,
                    baseAmount, downPayment, insurance, principal,
                    annualRate, payment, selectedDate.getTimeInMillis(), months, daysBefore);
            if (editReminder == null) ReminderScheduler.add(this, reminder);
            else ReminderScheduler.updateEdited(this, reminder);
            requestNotificationPermissionIfNeeded();
            Toast.makeText(this, editReminder == null
                    ? AppPreferences.tr(this, "Запись сохранена", "Saved")
                    : AppPreferences.tr(this, "Изменения сохранены", "Changes saved"), Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();'''
if old_save in s:
    s = s.replace(old_save, new_save, 1)
elif "ReminderScheduler.updateEdited(this, reminder);" not in s:
    raise RuntimeError("save block not found")

add_path.write_text(s, encoding="utf-8")

main_path = Path("app/src/main/java/com/example/creditcalculator/MainActivity.java")
m = main_path.read_text(encoding="utf-8")
old_about = '''        TextView about = drawerItem("ⓘ   " + AppPreferences.tr(this, "О приложении", "About"));
        about.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            new AlertDialog.Builder(this)
                    .setTitle(AppPreferences.tr(this, "Финансовый калькулятор", "Financial calculator"))
                    .setMessage(AppPreferences.tr(this,
                            "Кредит, ипотека, автокредит, рассрочка и вклад. Сохраняйте платежи, смотрите полный график, архив и получайте уведомления заранее.",
                            "Loan, mortgage, auto loan, installment and deposit calculators. Save payments, view schedules and archive, and receive reminders."))
                    .setPositiveButton("OK", null).show();
        });
        drawer.addView(about);'''
new_about = '''        TextView about = drawerItem("ⓘ   " + AppPreferences.tr(this, "О приложении", "About"));
        about.setOnClickListener(v -> openDrawerPage(AboutActivity.class));
        drawer.addView(about);'''
if old_about in m:
    m = m.replace(old_about, new_about, 1)
elif "openDrawerPage(AboutActivity.class)" not in m:
    raise RuntimeError("about block not found")
main_path.write_text(m, encoding="utf-8")

print("v1.8 patches applied")
