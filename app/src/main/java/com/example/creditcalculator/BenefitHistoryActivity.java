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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BenefitHistoryActivity extends AppCompatActivity {
    public static final String EXTRA_REMINDER_ID="reminder_id";

    @Override protected void attachBaseContext(Context c){super.attachBaseContext(AppPreferences.wrapLocale(c));}
    @Override protected void onCreate(Bundle b){AppPreferences.applyNightMode(this);super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);setContentView(build());}

    private View build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);UiUtils.applyBackground(this,root);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(4),0,dp(12),0);bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back=top("‹",34);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=top(AppPreferences.tr(this,"Ваша выгода","Your savings"),20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(30));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        long id=getIntent().getLongExtra(EXTRA_REMINDER_ID,-1);
        List<ReminderScheduler.BenefitEvent> events=id>0?ReminderScheduler.benefits(ReminderScheduler.findById(this,id)):ReminderScheduler.allBenefits(this);
        Collections.sort(events,(a,b)->Long.compare(b.time,a.time));
        double total=0;for(ReminderScheduler.BenefitEvent e:events)total+=e.savings;
        MaterialCardView summary=card();LinearLayout sb=box(summary);sb.addView(text(AppPreferences.tr(this,"Общая расчётная выгода","Total estimated savings"),14,R.color.text_secondary,false));TextView value=text(FormatUtils.money(this,total),30,total>=0?R.color.success:R.color.danger,true);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.setMargins(0,dp(4),0,dp(6));sb.addView(value,vp);sb.addView(text(AppPreferences.tr(this,"Суммируются результаты применённых досрочных погашений и рефинансирований.","Totals the results of applied early repayments and refinancing actions."),13,R.color.text_secondary,false));content.addView(summary);
        TextView h=text(AppPreferences.tr(this,"История действий","Action history"),22,R.color.text_main,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(22),0,dp(10));content.addView(h,hp);
        if(events.isEmpty()){TextView empty=text(AppPreferences.tr(this,"Пока нет применённых досрочных погашений или рефинансирований.","No applied early repayments or refinancing actions yet."),15,R.color.text_secondary,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(8),dp(28),dp(8),dp(28));content.addView(empty);}else for(ReminderScheduler.BenefitEvent e:events)content.addView(eventCard(e),section());
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,bars.bottom+dp(16));return insets;});return root;
    }

    private View eventCard(ReminderScheduler.BenefitEvent e){MaterialCardView c=card();LinearLayout b=box(c);boolean early=ReminderScheduler.HISTORY_EARLY.equals(e.type);b.addView(text(early?AppPreferences.tr(this,"Досрочное погашение","Early repayment"):AppPreferences.tr(this,"Рефинансирование","Refinancing"),18,R.color.text_main,true));b.addView(text(e.reminderTitle+" · "+FormatUtils.date(this,e.time),13,R.color.text_secondary,false));TextView save=text(AppPreferences.tr(this,"Выгода: ","Savings: ")+FormatUtils.money(this,e.savings),19,e.savings>=0?R.color.success:R.color.danger,true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(10),0,0);b.addView(save,sp);if(early&&e.actionAmount>0)b.addView(text(AppPreferences.tr(this,"Досрочно внесено: ","Early amount: ")+FormatUtils.money(this,e.actionAmount),14,R.color.text_secondary,false));if(e.paymentBefore>0||e.paymentAfter>0)b.addView(text(AppPreferences.tr(this,"Ежемесячный платёж: ","Monthly payment: ")+FormatUtils.money(this,e.paymentBefore)+" → "+FormatUtils.money(this,e.paymentAfter),14,R.color.text_secondary,false));if(e.monthsBefore>0||e.monthsAfter>0)b.addView(text(AppPreferences.tr(this,"Срок: ","Term: ")+e.monthsBefore+AppPreferences.tr(this," мес. → "," mo. → ")+e.monthsAfter+AppPreferences.tr(this," мес."," mo."),14,R.color.text_secondary,false));if(Math.abs(e.rateBefore-e.rateAfter)>.0001)b.addView(text(AppPreferences.tr(this,"Ставка: ","Rate: ")+rate(e.rateBefore)+"% → "+rate(e.rateAfter)+"%",14,R.color.text_secondary,false));return c;}

    private String rate(double v){return String.format(java.util.Locale.US,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","").replace('.',',');}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));return c;}private LinearLayout box(MaterialCardView c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(16),dp(18),dp(16));c.addView(b);return b;}private LinearLayout.LayoutParams section(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));return p;}private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;}private TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,color));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
