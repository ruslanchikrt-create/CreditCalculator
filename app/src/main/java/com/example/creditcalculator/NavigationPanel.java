package com.example.creditcalculator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

final class NavigationPanel {
    static final String PAGE_PAYMENTS="payments";
    static final String PAGE_CALCULATORS="calculators";

    private NavigationPanel(){}

    static int drawerWidth(Activity a){return (int)(a.getResources().getDisplayMetrics().widthPixels*.80f);}

    static LinearLayout build(Activity a,DrawerLayout drawer,String selected){
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setBackgroundColor(ContextCompat.getColor(a,R.color.card_background));
        panel.addView(header(a,drawer),new LinearLayout.LayoutParams(-1,dp(a,116)));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_today,AppPreferences.tr(a,"Мои платежи","My payments"),PAGE_PAYMENTS.equals(selected),PaymentsActivity.class,false));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_manage,AppPreferences.tr(a,"Калькуляторы","Calculators"),PAGE_CALCULATORS.equals(selected),MainActivity.class,true));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_send,AppPreferences.tr(a,"Досрочный платёж","Early payment"),false,EarlyPaymentAdvisorActivity.class,false));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_myplaces,AppPreferences.tr(a,"Личный кабинет","Personal cabinet"),false,PersonalCabinetActivity.class,false));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_agenda,AppPreferences.tr(a,"Архив","Archive"),false,ArchiveActivity.class,false));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_delete,AppPreferences.tr(a,"Корзина","Trash"),false,TrashActivity.class,false));
        add(panel,item(a,drawer,android.R.drawable.ic_menu_preferences,AppPreferences.tr(a,"Настройки","Settings"),false,SettingsActivity.class,false));
        View spacer=new View(a);panel.addView(spacer,new LinearLayout.LayoutParams(-1,0,1f));
        View line=new View(a);line.setBackgroundColor(ContextCompat.getColor(a,R.color.border));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,1));lp.setMargins(dp(a,16),0,dp(a,16),0);panel.addView(line,lp);
        add(panel,item(a,drawer,android.R.drawable.ic_menu_info_details,AppPreferences.tr(a,"О приложении","About"),false,AboutActivity.class,false));
        TextView exit=navText(a,android.R.drawable.ic_menu_close_clear_cancel,AppPreferences.tr(a,"Выход","Exit"),false);exit.setOnClickListener(v->{drawer.closeDrawer(GravityCompat.START);a.finishAffinity();});add(panel,exit);
        return panel;
    }

    private static View header(Activity a,DrawerLayout drawer){
        LinearLayout h=new LinearLayout(a);h.setOrientation(LinearLayout.HORIZONTAL);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(a,18),dp(a,14),dp(a,14),dp(a,14));h.setBackgroundColor(ContextCompat.getColor(a,R.color.primary));
        FrameLayout avatarBox=new FrameLayout(a);h.addView(avatarBox,new LinearLayout.LayoutParams(dp(a,58),dp(a,58)));
        GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(ContextCompat.getColor(a,R.color.primary_dark));
        TextView fallback=new TextView(a);fallback.setGravity(Gravity.CENTER);fallback.setTextSize(24);fallback.setTextColor(Color.WHITE);fallback.setTypeface(null,android.graphics.Typeface.BOLD);fallback.setBackground(circle);
        String fio=AppPreferences.getCabinetFullName(a);String profile=fio.isEmpty()?AppPreferences.getProfileName(a):fio;String first=profile.trim().isEmpty()?"₽":profile.trim().substring(0,1).toUpperCase();fallback.setText(first);avatarBox.addView(fallback,new FrameLayout.LayoutParams(-1,-1));
        String avatarUri=AppPreferences.getAvatarUri(a);if(avatarUri!=null&&!avatarUri.isEmpty())try{ImageView iv=new ImageView(a);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setImageURI(Uri.parse(avatarUri));iv.setBackground(circle);iv.setClipToOutline(true);avatarBox.addView(iv,new FrameLayout.LayoutParams(-1,-1));}catch(Exception ignored){}
        LinearLayout names=new LinearLayout(a);names.setOrientation(LinearLayout.VERTICAL);names.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,-2,1f);nlp.setMargins(dp(a,14),0,0,0);h.addView(names,nlp);
        boolean hasProfile=!profile.trim().isEmpty();TextView main=new TextView(a);main.setText(hasProfile?profile:AppPreferences.tr(a,"Финансовый калькулятор","Financial calculator"));main.setTextSize(hasProfile?18:19);main.setTextColor(Color.WHITE);main.setTypeface(null,android.graphics.Typeface.BOLD);main.setMaxLines(2);names.addView(main);
        TextView sub=new TextView(a);sub.setText(hasProfile?AppPreferences.tr(a,"Финансовый калькулятор","Financial calculator"):AppPreferences.tr(a,"Ваши финансы в одном месте","Your finances in one place"));sub.setTextSize(12);sub.setTextColor(Color.argb(220,255,255,255));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(a,3),0,0);names.addView(sub,sp);
        h.setClickable(true);h.setOnClickListener(v->{drawer.closeDrawer(GravityCompat.START);if(!(a instanceof PersonalCabinetActivity))a.startActivity(new Intent(a,PersonalCabinetActivity.class));});return h;
    }

    private static TextView item(Activity a,DrawerLayout drawer,int icon,String label,boolean selected,Class<?> target,boolean calculators){TextView t=navText(a,icon,label,selected);t.setOnClickListener(v->{drawer.closeDrawer(GravityCompat.START);if((target==PaymentsActivity.class&&a instanceof PaymentsActivity)||(target==MainActivity.class&&a instanceof MainActivity))return;Intent i=new Intent(a,target);if(calculators)i.putExtra(MainActivity.EXTRA_SHOW_CALCULATORS,true);a.startActivity(i);});return t;}
    private static TextView navText(Activity a,int iconRes,String label,boolean selected){TextView t=new TextView(a);t.setText(label);t.setTextSize(16);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(a,14),0,dp(a,12),0);t.setCompoundDrawablePadding(dp(a,14));int color=ContextCompat.getColor(a,selected?R.color.primary:R.color.text_main);t.setTextColor(color);if(selected)t.setTypeface(null,android.graphics.Typeface.BOLD);Drawable d=AppCompatResources.getDrawable(a,iconRes);if(d!=null){d=DrawableCompat.wrap(d.mutate());DrawableCompat.setTint(d,color);d.setBounds(0,0,dp(a,22),dp(a,22));t.setCompoundDrawablesRelative(d,null,null,null);}if(selected){GradientDrawable bg=new GradientDrawable();bg.setColor(Color.argb(24,47,111,235));bg.setCornerRadius(dp(a,12));t.setBackground(bg);}return t;}
    private static void add(LinearLayout panel,TextView v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(v.getContext(),50));p.setMargins(dp(v.getContext(),10),dp(v.getContext(),2),dp(v.getContext(),10),dp(v.getContext(),2));panel.addView(v,p);}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
