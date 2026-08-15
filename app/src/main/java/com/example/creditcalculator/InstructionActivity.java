package com.example.creditcalculator;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class InstructionActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); UiUtils.applyBackground(this, root);
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary)); root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView back = topText("‹", 34); back.setOnClickListener(v -> finish()); bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView title = topText(AppPreferences.tr(this, "Инструкция", "User guide"), 20); title.setTypeface(null, android.graphics.Typeface.BOLD); title.setGravity(Gravity.CENTER_VERTICAL); bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.TRANSPARENT); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(22), dp(20), dp(32)); scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        content.addView(text(AppPreferences.tr(this, "Как пользоваться приложением", "How to use the app"), 27, R.color.text_main, true));
        TextView intro = text(AppPreferences.tr(this, "Короткая инструкция по основным функциям.", "A quick guide to the main features."), 15, R.color.text_secondary, false); LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2); ip.setMargins(0, dp(6), 0, dp(16)); content.addView(intro, ip);

        addSection(content, "1", AppPreferences.tr(this, "Добавление записи", "Adding an item"), AppPreferences.tr(this,
                "Нажмите +, выберите тип: кредит, ипотека, автокредит, рассрочка или вклад. Укажите сумму, ставку, срок и дату первого платежа. Дата может быть как будущей, так и прошедшей.",
                "Tap +, choose loan, mortgage, auto loan, installment or deposit, then enter amount, rate, term and first payment date. Past dates are supported."));
        addSection(content, "2", AppPreferences.tr(this, "Мои платежи", "My payments"), AppPreferences.tr(this,
                "Здесь отображаются активные записи. По умолчанию первым показывается обязательство с ближайшим платежом. Используйте фильтр по типу и сортировку по дате, названию, сумме, ставке и другим параметрам.",
                "Active items are shown here. By default the nearest payment is first. Use type filters and sorting by date, name, amount, rate and more."));
        addSection(content, "3", AppPreferences.tr(this, "Переплата и проценты", "Interest and overpayment"), AppPreferences.tr(this,
                "В карточке и подробностях показываются общая переплата, уже выплаченные проценты и проценты, которые ещё остаётся выплатить. Для записей с прошедшей датой приложение считает платежи по плановому графику.",
                "Cards and details show total interest, interest already paid and interest remaining. For past dates, payments are estimated from the planned schedule."));
        addSection(content, "4", AppPreferences.tr(this, "Досрочное погашение", "Early repayment"), AppPreferences.tr(this,
                "Откройте кредит и нажмите «Досрочное погашение». Введите сумму и дату. Приложение сравнит два варианта: уменьшить ежемесячный платёж или уменьшить срок, покажет новую переплату и экономию. Изменения применяются только после подтверждения.",
                "Open a loan and choose Early repayment. Enter amount and date. The app compares reducing the payment versus reducing the term, including interest savings, before anything is applied."));
        addSection(content, "5", AppPreferences.tr(this, "Рефинансирование", "Refinancing"), AppPreferences.tr(this,
                "Введите новую ставку, срок, комиссию, страховку и дату рефинансирования. Перед применением приложение сравнит текущий кредит и новый вариант: платёж, переплату, дату окончания и расчётную экономию.",
                "Enter the new rate, term, commission, insurance and refinancing date. The app compares the current loan with the new terms before applying them."));
        addSection(content, "6", AppPreferences.tr(this, "Редактирование и история", "Editing and history"), AppPreferences.tr(this,
                "Карандаш рядом с названием открывает редактирование записи. Через меню ⋮ откройте «Историю изменений». Там сохраняются дата создания, редактирования, досрочные погашения, рефинансирования и другие действия.",
                "Use the pencil next to the title to edit. Open Change history from ⋮ to see creation, edits, early repayments, refinancing and other actions."));
        addSection(content, "7", AppPreferences.tr(this, "Архив и корзина", "Archive and trash"), AppPreferences.tr(this,
                "Ненужную активную запись можно перенести в архив или корзину. Из корзины запись можно восстановить в течение 30 дней, после чего она удаляется автоматически.",
                "Move inactive items to archive or trash. Trash items can be restored for 30 days before automatic deletion."));
        addSection(content, "8", AppPreferences.tr(this, "Напоминания", "Reminders"), AppPreferences.tr(this,
                "При создании записи выберите, за сколько дней напомнить о платеже. В настройках можно включить или выключить звук и вибрацию, а также выбрать свой звук.",
                "Choose how many days before a payment to be reminded. Settings let you control sound, vibration and custom notification audio."));
        addSection(content, "9", AppPreferences.tr(this, "Резервная копия", "Backup"), AppPreferences.tr(this,
                "Откройте Настройки → Резервное копирование. Можно сохранить файл без пароля или защитить его паролем. На новом телефоне установите приложение и выберите «Восстановить из файла». Пароль защищённой копии восстановить невозможно, если он забыт.",
                "Open Settings → Backup. Save a plain or password-protected file. On a new phone install the app and choose Restore from file. A forgotten backup password cannot be recovered."));
        addSection(content, "?", AppPreferences.tr(this, "Почему расчёт может отличаться от банка?", "Why can bank figures differ?"), AppPreferences.tr(this,
                "Банк может использовать собственные правила округления, даты начисления процентов, комиссии, страховки и особенности договора. Поэтому расчёты приложения являются ориентировочными.",
                "Banks can use different rounding, interest dates, fees, insurance and contract rules. App calculations are estimates."));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> { Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(0, bars.top, 0, 0); scroll.setPadding(0, 0, 0, bars.bottom + dp(8)); return insets; }); ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void addSection(LinearLayout parent, String number, String title, String body) {
        MaterialCardView card = new MaterialCardView(this); card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background)); card.setRadius(dp(16)); card.setStrokeColor(ContextCompat.getColor(this, R.color.border)); card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16), dp(14), dp(16), dp(14)); card.addView(box);
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); box.addView(row);
        TextView badge = text(number, 14, R.color.white, true); badge.setGravity(Gravity.CENTER); badge.setBackgroundResource(R.drawable.circle_primary); row.addView(badge, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView t = text(title, 18, R.color.text_main, true); LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f); tp.setMargins(dp(12), 0, 0, 0); row.addView(t, tp);
        TextView b = text(body, 14, R.color.text_secondary, false); LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.setMargins(0, dp(10), 0, 0); box.addView(b, bp);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, 0, 0, dp(10)); parent.addView(card, cp);
    }

    private TextView topText(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER); v.setClickable(true); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(ContextCompat.getColor(this, color)); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
