package com.example.creditcalculator;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ScrollView;
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

    public static void attachMoneyFormatting(EditText editText){
        if(editText==null)return;
        editText.addTextChangedListener(new TextWatcher(){
            private boolean changing;
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable editable){
                if(changing)return;
                String old=editable==null?"":editable.toString();
                String formatted=formatEditableMoney(old);
                if(old.equals(formatted))return;
                changing=true;editText.setText(formatted);editText.setSelection(formatted.length());changing=false;
            }
        });
    }

    public static String formatEditableMoney(String value){
        if(value==null)return "";
        String s=value.replace("\u00A0","").replace("\u202F","").replace(" ","").trim();
        if(s.isEmpty())return "";
        int comma=s.lastIndexOf(','),dot=s.lastIndexOf('.'),sep=Math.max(comma,dot);char sepChar=comma>=dot?',':'.';
        String whole=sep>=0?s.substring(0,sep):s,frac=sep>=0?s.substring(sep+1):"";
        whole=whole.replaceAll("[^0-9]","");frac=frac.replaceAll("[^0-9]","");if(whole.isEmpty())whole="0";
        int first=0;while(first<whole.length()-1&&whole.charAt(first)=='0')first++;whole=whole.substring(first);
        StringBuilder grouped=new StringBuilder();for(int i=0;i<whole.length();i++){if(i>0&&(whole.length()-i)%3==0)grouped.append(' ');grouped.append(whole.charAt(i));}
        if(sep>=0){grouped.append(sepChar);grouped.append(frac);}return grouped.toString();
    }

    /** Bind a field so Android never leaves the active value hidden behind the IME. */
    public static void keepFieldVisibleOnFocus(ScrollView scroll,View field){
        if(scroll==null||field==null)return;
        View.OnFocusChangeListener previous=field.getOnFocusChangeListener();
        field.setOnFocusChangeListener((v,hasFocus)->{
            if(previous!=null)previous.onFocusChange(v,hasFocus);
            if(hasFocus)recheckField(scroll,v);
        });
        field.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(v.hasFocus())recheckField(scroll,v);});
    }

    private static void recheckField(ScrollView scroll,View field){
        long[] delays={40,140,300,520,800};
        for(long delay:delays)field.postDelayed(()->scrollFieldIntoView(scroll,field),delay);
    }

    /** Re-check the currently focused field after IME insets/layout have changed. */
    public static void ensureFocusedFieldVisible(ScrollView scroll){
        if(scroll==null)return;
        View focused=scroll.findFocus();
        if(focused!=null)recheckField(scroll,focused);
        else scroll.postDelayed(()->{View f=scroll.findFocus();if(f!=null)recheckField(scroll,f);},120);
    }

    private static void scrollFieldIntoView(ScrollView scroll,View field){
        if(scroll==null||field==null||!field.isShown()||!field.hasFocus())return;
        try{
            Rect rect=new Rect();field.getDrawingRect(rect);scroll.offsetDescendantRectToMyCoords(field,rect);
            int margin=dp(scroll.getContext(),120);
            int viewportTop=scroll.getScrollY()+dp(scroll.getContext(),12);
            int viewportBottom=Math.max(viewportTop+Math.max(field.getHeight(),dp(scroll.getContext(),56)),scroll.getScrollY()+scroll.getHeight()-scroll.getPaddingBottom()-margin);
            int target=scroll.getScrollY();
            if(rect.bottom>viewportBottom)target+=rect.bottom-viewportBottom;
            else if(rect.top<viewportTop)target-=viewportTop-rect.top;
            target=Math.max(0,target);
            if(Math.abs(target-scroll.getScrollY())>2)scroll.smoothScrollTo(0,target);
            Rect request=new Rect(0,0,field.getWidth(),field.getHeight()+margin);field.requestRectangleOnScreen(request,true);
        }catch(Exception ignored){}
    }

    public static String formatMoney(Context context,double value){return FormatUtils.money(context,value);}
    public static String termUnit(Context context,int value,boolean years){String l=AppPreferences.getLanguage(context);if("en".equals(l)){if(years)return value==1?"year":"years";return value==1?"month":"months";}if("tr".equals(l))return years?"yıl":"ay";if("es".equals(l)){if(years)return value==1?"año":"años";return value==1?"mes":"meses";}return years?russianYears(value):russianMonths(value);}
    public static String termText(Context context,int months){if(months>0&&months%12==0){int y=months/12;return y+" "+termUnit(context,y,true);}return months+" "+termUnit(context,months,false);}
    private static String russianYears(int v){int m100=v%100,m10=v%10;if(m100>=11&&m100<=14)return "лет";if(m10==1)return "год";if(m10>=2&&m10<=4)return "года";return "лет";}
    private static String russianMonths(int v){int m100=v%100,m10=v%10;if(m100>=11&&m100<=14)return "месяцев";if(m10==1)return "месяц";if(m10>=2&&m10<=4)return "месяца";return "месяцев";}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
