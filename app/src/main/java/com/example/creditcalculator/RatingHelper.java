package com.example.creditcalculator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

final class RatingHelper {
    private static final String KEY_LAST_PROMPT="rating_last_prompt_time";
    private static final String KEY_RATED="rating_prompt_completed";
    private static final long AGAIN_AFTER=45L*24L*60L*60L*1000L;

    private RatingHelper(){}

    static void maybePrompt(Activity activity){
        if(activity==null||activity.isFinishing()||!AppPreferences.isPaymentsDisclaimerAccepted(activity))return;
        SharedPreferences p=activity.getSharedPreferences(AppPreferences.PREFS_NAME,Context.MODE_PRIVATE);
        if(p.getBoolean(KEY_RATED,false))return;
        if(countPaid(activity)<3)return;
        long last=p.getLong(KEY_LAST_PROMPT,0);if(last>0&&System.currentTimeMillis()-last<AGAIN_AFTER)return;
        p.edit().putLong(KEY_LAST_PROMPT,System.currentTimeMillis()).apply();
        new AlertDialog.Builder(activity)
                .setTitle(AppPreferences.tr(activity,"Нравится приложение?","Enjoying the app?"))
                .setMessage(AppPreferences.tr(activity,"Если приложение помогает следить за платежами, будем благодарны за оценку. Это помогает развитию проекта.","If the app helps you keep track of payments, we would appreciate a rating. It helps the project grow."))
                .setPositiveButton(AppPreferences.tr(activity,"Оценить","Rate"),(d,w)->{p.edit().putBoolean(KEY_RATED,true).apply();openStore(activity);})
                .setNegativeButton(AppPreferences.tr(activity,"Не сейчас","Not now"),null)
                .show();
    }

    static void openStore(Context context){
        String id=context.getPackageName();
        Intent market=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+id));market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{context.startActivity(market);}catch(Exception e){Intent web=new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+id));web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{context.startActivity(web);}catch(Exception ignored){}}
    }

    private static int countPaid(Context c){int count=0;List<ReminderScheduler.PaymentReminder> all=ReminderScheduler.loadAll(c);for(ReminderScheduler.PaymentReminder r:all){if(ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(r.type)))continue;for(int i=0;i<r.months;i++)if(ReminderScheduler.isPaid(r,i))count++;}return count;}
}
