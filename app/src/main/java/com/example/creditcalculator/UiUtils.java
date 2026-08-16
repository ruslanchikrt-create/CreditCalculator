package com.example.creditcalculator;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class UiUtils {
    private UiUtils() {}

    public static void applyBackground(Context context, View view) {
        String saved=AppPreferences.getBackgroundUri(context);
        if(saved!=null&&!saved.trim().isEmpty()){
            InputStream stream=null;
            try{stream=context.getContentResolver().openInputStream(Uri.parse(saved));Drawable image=Drawable.createFromStream(stream,"user_background");if(image!=null){int overlay=AppPreferences.isDarkMode(context)?0xA60B1220:0xBDF4F7FB;view.setBackground(new LayerDrawable(new Drawable[]{image,new ColorDrawable(overlay)}));return;}}
            catch(Exception ignored){}finally{if(stream!=null)try{stream.close();}catch(Exception ignored){}}
        }
        view.setBackgroundResource(R.drawable.app_background);
    }

    public static ArrayAdapter<String> spinnerAdapter(Context context,List<String> values){ArrayAdapter<String> a=new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,new ArrayList<>(values)){@Override public View getView(int p,View c,ViewGroup parent){View v=super.getView(p,c,parent);styleSpinnerText(context,v,false);return v;}@Override public View getDropDownView(int p,View c,ViewGroup parent){View v=super.getDropDownView(p,c,parent);styleSpinnerText(context,v,true);return v;}};a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return a;}
    public static ArrayAdapter<String> spinnerAdapter(Context context,String[] values){List<String> l=new ArrayList<>();if(values!=null)for(String v:values)l.add(v);return spinnerAdapter(context,l);}
    public static void styleSpinner(Context context,Spinner spinner){if(spinner==null)return;GradientDrawable bg=new GradientDrawable();bg.setColor(ContextCompat.getColor(context,R.color.card_background));bg.setCornerRadius(dp(context,12));bg.setStroke(dp(context,1),ContextCompat.getColor(context,R.color.border));spinner.setBackground(bg);spinner.setPadding(dp(context,12),0,dp(context,12),0);}
    private static void styleSpinnerText(Context context,View view,boolean dropdown){if(!(view instanceof TextView))return;TextView t=(TextView)view;t.setTextColor(ContextCompat.getColor(context,R.color.text_main));t.setTextSize(16);t.setBackgroundColor(ContextCompat.getColor(context,R.color.card_background));t.setPadding(dp(context,12),dropdown?dp(context,10):0,dp(context,12),dropdown?dp(context,10):0);if(dropdown)t.setMinHeight(dp(context,48));}

    public static String termUnit(Context context,int value,boolean years){String l=AppPreferences.getLanguage(context);if("en".equals(l)){if(years)return value==1?"year":"years";return value==1?"month":"months";}if("tr".equals(l))return years?"yıl":"ay";if("es".equals(l)){if(years)return value==1?"año":"años";return value==1?"mes":"meses";}return years?russianYears(value):russianMonths(value);}
    public static String termText(Context context,int months){if(months>0&&months%12==0){int y=months/12;return y+" "+termUnit(context,y,true);}return months+" "+termUnit(context,months,false);}
    private static String russianYears(int v){int m100=v%100,m10=v%10;if(m100>=11&&m100<=14)return "лет";if(m10==1)return "год";if(m10>=2&&m10<=4)return "года";return "лет";}
    private static String russianMonths(int v){int m100=v%100,m10=v%10;if(m100>=11&&m100<=14)return "месяцев";if(m10==1)return "месяц";if(m10>=2&&m10<=4)return "месяца";return "месяцев";}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
