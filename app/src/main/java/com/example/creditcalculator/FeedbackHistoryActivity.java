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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedbackHistoryActivity extends AppCompatActivity {
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(AppPreferences.wrapLocale(c));}
    @Override protected void onCreate(Bundle b){AppPreferences.applyNightMode(this);super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);setContentView(build());}

    private View build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);UiUtils.applyBackground(this,root);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back=top("‹",34);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=top(AppPreferences.tr(this,"Мои обращения","My requests"),20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(32));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        List<FeedbackStore.Entry> entries=FeedbackStore.load(this);if(entries.isEmpty()){TextView empty=text(AppPreferences.tr(this,"Сохранённых обращений пока нет.","No saved requests yet."),16,R.color.text_secondary,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(10),dp(54),dp(10),dp(40));content.addView(empty);}else for(FeedbackStore.Entry e:entries)content.addView(card(e),cardParams());
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,bars.bottom+dp(12));return insets;});return root;
    }

    private View card(FeedbackStore.Entry e){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(17));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(17),dp(15),dp(17),dp(16));c.addView(b);LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);b.addView(top);TextView title=text(e.title,18,R.color.text_main,true);top.addView(title,new LinearLayout.LayoutParams(0,-2,1f));TextView date=text(formatDate(e.createdAt),12,R.color.text_secondary,false);top.addView(date);TextView type=text(e.type,13,R.color.primary,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.setMargins(0,dp(5),0,dp(8));b.addView(type,tp);b.addView(text(e.text,14,R.color.text_main,false));return c;}
    private String formatDate(long v){try{return new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date(v));}catch(Exception e){return "";}}
    private LinearLayout.LayoutParams cardParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));return p;}private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;}private TextView text(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,c));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
