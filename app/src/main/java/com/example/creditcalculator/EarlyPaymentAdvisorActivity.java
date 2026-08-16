package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EarlyPaymentAdvisorActivity extends AppCompatActivity {
    private ScrollView scroll;
    private LinearLayout results;
    private TextInputEditText amountInput;

    private static class Option {
        ReminderScheduler.PaymentReminder reminder;
        ReminderScheduler.EarlyRepaymentSimulation sim;
        double amount;
        double savings;
        boolean reduceTerm;
    }

    @Override protected void attachBaseContext(Context c){super.attachBaseContext(AppPreferences.wrapLocale(c));}
    @Override protected void onCreate(Bundle b){AppPreferences.applyNightMode(this);super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);setContentView(build());}

    private View build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);UiUtils.applyBackground(this,root);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(4),0,dp(12),0);bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back=top("‹",34);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=top(AppPreferences.tr(this,"Досрочный платёж","Early payment"),20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(34));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));

        LinearLayout introRow=labelRow(AppPreferences.tr(this,"Куда выгоднее внести деньги?","Where is an extra payment most useful?"),AppPreferences.tr(this,"Это калькулятор для сравнения. Он ничего не меняет в ваших графиках. Чтобы применить результат, откройте Мои платежи → нужный кредит → Досрочное погашение. Для рефинансирования откройте там же пункт Рефинансирование.","This is a comparison calculator. It does not change your schedules. To apply a result, open My payments → the loan → Early repayment. Refinancing is applied from the same loan details screen."));content.addView(introRow);
        TextView intro=text(AppPreferences.tr(this,"Введите свободную сумму. Приложение сравнит активные кредиты и покажет, где эта сумма даст наибольшую расчётную экономию на процентах.","Enter the spare amount. The app compares active debts and shows where it gives the largest estimated interest saving."),14,R.color.text_secondary,false);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.setMargins(0,dp(6),0,dp(18));content.addView(intro,ip);

        content.addView(labelRow(AppPreferences.tr(this,"Сумма досрочного платежа, ₽","Extra payment amount, ₽"),AppPreferences.tr(this,"Введите сумму, которую вы готовы внести сверх обычного платежа. Расчёт сравнит её влияние на каждый активный кредит.","Enter the amount you can pay in addition to the normal payment. The calculator compares its effect on every active loan.")));
        TextInputLayout layout=new TextInputLayout(this);layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);layout.setBoxBackgroundColor(ContextCompat.getColor(this,R.color.card_background));layout.setBoxStrokeColor(ContextCompat.getColor(this,R.color.primary));layout.setBoxCornerRadii(dp(14),dp(14),dp(14),dp(14));amountInput=new TextInputEditText(this);amountInput.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amountInput.setSingleLine(true);amountInput.setTextColor(ContextCompat.getColor(this,R.color.text_main));layout.addView(amountInput,new TextInputLayout.LayoutParams(-1,dp(60)));content.addView(layout,new LinearLayout.LayoutParams(-1,-2));UiUtils.attachMoneyFormatting(amountInput);UiUtils.keepFieldVisibleOnFocus(scroll,amountInput);

        MaterialButton calculate=new MaterialButton(this);calculate.setText(AppPreferences.tr(this,"Сравнить варианты","Compare options"));calculate.setAllCaps(false);calculate.setTextSize(17);calculate.setTextColor(Color.WHITE);calculate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));calculate.setCornerRadius(dp(14));calculate.setOnClickListener(v->calculate());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(56));cp.setMargins(0,dp(14),0,dp(18));content.addView(calculate,cp);
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.addView(results,new LinearLayout.LayoutParams(-1,-2));
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,Math.max(bars.bottom,ime.bottom)+dp(24));UiUtils.ensureFocusedFieldVisible(scroll);return insets;});return root;
    }

    private void calculate(){
        double requested;
        try{requested=parse(amountInput.getText()==null?"":amountInput.getText().toString());if(requested<=0)throw new Exception();}catch(Exception e){Toast.makeText(this,AppPreferences.tr(this,"Введите сумму больше 0","Enter an amount above 0"),Toast.LENGTH_SHORT).show();return;}
        List<Option> options=new ArrayList<>();long now=System.currentTimeMillis();
        for(ReminderScheduler.PaymentReminder r:ReminderScheduler.load(this)){
            if(ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(r.type)))continue;
            double balance=ReminderScheduler.balanceAtDate(r,now);if(balance<=.01)continue;
            double used=Math.min(requested,balance);
            try{ReminderScheduler.EarlyRepaymentSimulation sim=ReminderScheduler.simulateEarlyRepayment(r,now,used);Option o=new Option();o.reminder=r;o.sim=sim;o.amount=used;o.reduceTerm=sim.savingsWithReducedTerm>=sim.savingsWithReducedPayment;o.savings=o.reduceTerm?sim.savingsWithReducedTerm:sim.savingsWithReducedPayment;options.add(o);}catch(Exception ignored){}
        }
        Collections.sort(options,(a,b)->Double.compare(b.savings,a.savings));render(options,requested);
    }

    private void render(List<Option> options,double requested){results.removeAllViews();TextView h=text(AppPreferences.tr(this,"Результат сравнения","Comparison result"),21,R.color.text_main,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,0,0,dp(10));results.addView(h,hp);if(options.isEmpty()){results.addView(text(AppPreferences.tr(this,"Нет активных кредитов, которые можно сравнить.","There are no active debts to compare."),15,R.color.text_secondary,false));return;}for(int i=0;i<options.size();i++)results.addView(optionCard(options.get(i),i==0,requested),section());}

    private View optionCard(Option o,boolean best,double requested){MaterialCardView c=card();c.setStrokeWidth(dp(2));c.setStrokeColor(ContextCompat.getColor(this,best?R.color.success:R.color.danger));LinearLayout b=box(c);int accent=best?R.color.success:R.color.danger;if(best)b.addView(text(AppPreferences.tr(this,"★ Самый выгодный вариант","★ Best estimated option"),14,R.color.success,true));else b.addView(text(AppPreferences.tr(this,"Менее выгодный вариант","Less beneficial option"),14,R.color.danger,true));b.addView(text(o.reminder.title,20,R.color.text_main,true));b.addView(text(FormatUtils.typeLabel(this,o.reminder.type)+" · "+rate(o.reminder.annualRate)+"%",13,R.color.text_secondary,false));LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,-2);gap.setMargins(0,dp(10),0,0);TextView save=text(AppPreferences.tr(this,"Экономия на процентах: ","Estimated interest saving: ")+FormatUtils.money(this,o.savings),18,accent,true);b.addView(save,gap);b.addView(text(AppPreferences.tr(this,"Внести досрочно: ","Extra payment: ")+FormatUtils.money(this,o.amount),14,R.color.text_main,false));if(o.amount+0.01<requested)b.addView(text(AppPreferences.tr(this,"Остаток этого кредита меньше введённой суммы, поэтому для расчёта использован только его остаток.","This loan balance is below the entered amount, so only its remaining balance is used."),12,R.color.warning,false));b.addView(text(o.reduceTerm?AppPreferences.tr(this,"Рекомендация: сократить срок","Recommendation: reduce the term"):AppPreferences.tr(this,"Рекомендация: уменьшить платёж","Recommendation: reduce the monthly payment"),15,best?R.color.primary:R.color.danger,true));double newPayment=o.reduceTerm?o.sim.keptPayment:o.sim.reducedPayment;int newMonths=o.reduceTerm?o.sim.reducedMonths:o.sim.remainingMonths;b.addView(text(AppPreferences.tr(this,"Новый платёж: ","New payment: ")+FormatUtils.money(this,newPayment),13,R.color.text_secondary,false));b.addView(text(AppPreferences.tr(this,"Новый срок: ","New term: ")+newMonths+AppPreferences.tr(this," мес."," mo."),13,R.color.text_secondary,false));MaterialButton open=new MaterialButton(this);open.setText(AppPreferences.tr(this,"Открыть кредит","Open loan"));open.setAllCaps(false);open.setTextColor(ContextCompat.getColor(this,best?R.color.primary:R.color.danger));open.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.card_background)));open.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,best?R.color.primary:R.color.danger)));open.setStrokeWidth(dp(1));open.setCornerRadius(dp(12));open.setOnClickListener(v->{Intent in=new Intent(this,PaymentDetailsActivity.class);in.putExtra(PaymentDetailsActivity.EXTRA_REMINDER_ID,o.reminder.id);startActivity(in);});LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,dp(48));op.setMargins(0,dp(12),0,0);b.addView(open,op);return c;}

    private LinearLayout labelRow(String title,String help){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=text(title,15,R.color.text_main,true);row.addView(t,new LinearLayout.LayoutParams(0,-2,1f));TextView i=text("ⓘ",22,R.color.primary,true);i.setGravity(Gravity.CENTER);i.setClickable(true);i.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(title).setMessage(help).setPositiveButton(AppPreferences.tr(this,"Понятно","OK"),null).show());row.addView(i,new LinearLayout.LayoutParams(dp(42),dp(42)));return row;}
    private double parse(String s){return Double.parseDouble(s.replace(" ","").replace("\u00A0","").replace("\u202F","").replace(',','.').trim());}private String rate(double v){return String.format(java.util.Locale.US,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","").replace('.',',');}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));return c;}private LinearLayout box(MaterialCardView c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(16),dp(18),dp(16));c.addView(b);return b;}private LinearLayout.LayoutParams section(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));return p;}private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;}private TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,color));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
