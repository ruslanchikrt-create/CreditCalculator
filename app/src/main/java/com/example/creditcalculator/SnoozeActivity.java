package com.example.creditcalculator;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;

public class SnoozeActivity extends AppCompatActivity {
    private long reminderId;
    private int index;
    private int notificationId;

    @Override protected void attachBaseContext(Context newBase){super.attachBaseContext(AppPreferences.wrapLocale(newBase));}
    @Override public void onCreate(Bundle b){AppPreferences.applyNightMode(this);super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);reminderId=getIntent().getLongExtra("reminder_id",-1);index=getIntent().getIntExtra("payment_index",-1);notificationId=getIntent().getIntExtra("notification_id",-1);setContentView(build());}

    private View build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(22),dp(20),dp(22),dp(20));UiUtils.applyBackground(this,root);MaterialCardView card=new MaterialCardView(this);card.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));card.setRadius(dp(20));card.setStrokeColor(ContextCompat.getColor(this,R.color.border));card.setStrokeWidth(dp(1));LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(20),dp(20),dp(18));card.addView(box);root.addView(card,new LinearLayout.LayoutParams(-1,-2));TextView title=text(AppPreferences.tr(this,"Напомнить позже","Remind later"),22,R.color.text_main,true);title.setGravity(Gravity.CENTER);box.addView(title);TextView sub=text(AppPreferences.tr(this,"Через сколько снова напомнить об этом платеже?","When should the app remind you again?"),14,R.color.text_secondary,false);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(6),0,dp(14));box.addView(sub,sp);
        add(box,AppPreferences.tr(this,"5 минут","5 minutes"),5);add(box,AppPreferences.tr(this,"10 минут","10 minutes"),10);add(box,AppPreferences.tr(this,"15 минут","15 minutes"),15);add(box,AppPreferences.tr(this,"30 минут","30 minutes"),30);add(box,AppPreferences.tr(this,"1 час","1 hour"),60);
        MaterialButton manual=button(AppPreferences.tr(this,"Выбрать вручную","Choose manually"));manual.setOnClickListener(v->manual());box.addView(manual,params());
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(dp(22),bars.top+dp(16),dp(22),bars.bottom+dp(16));return insets;});return root;}

    private void add(LinearLayout box,String label,int minutes){MaterialButton b=button(label);b.setOnClickListener(v->apply(System.currentTimeMillis()+minutes*60_000L));box.addView(b,params());}
    private void manual(){Calendar now=Calendar.getInstance();new TimePickerDialog(this,(v,h,m)->{Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_MONTH,1);apply(c.getTimeInMillis());},now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();}
    private void apply(long when){ReminderScheduler.snoozePayment(this,reminderId,index,when);if(notificationId>=0)NotificationManagerCompat.from(this).cancel(notificationId);Toast.makeText(this,AppPreferences.tr(this,"Напоминание перенесено","Reminder postponed"),Toast.LENGTH_SHORT).show();finish();}
    private MaterialButton button(String s){MaterialButton b=new MaterialButton(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(ContextCompat.getColor(this,R.color.primary));b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.card_background)));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));b.setStrokeWidth(dp(1));b.setCornerRadius(dp(13));return b;}private LinearLayout.LayoutParams params(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,0,0,dp(9));return p;}private TextView text(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,c));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
