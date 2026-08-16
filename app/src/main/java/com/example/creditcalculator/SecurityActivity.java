package com.example.creditcalculator;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SecurityActivity extends AppCompatActivity {
    private SwitchMaterial enabledSwitch;
    private SwitchMaterial biometricSwitch;
    private Spinner timeoutSpinner;
    private boolean binding;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(build());
        bind();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); UiUtils.applyBackground(this, root);
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(4),0,dp(12),0); bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary)); root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back = top("‹",34); back.setOnClickListener(v->finish()); bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title = top(AppPreferences.tr(this,"Безопасность","Security"),20); title.setTypeface(null,android.graphics.Typeface.BOLD); title.setGravity(Gravity.CENTER_VERTICAL); bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(22),dp(20),dp(32)); scroll.addView(content,new ScrollView.LayoutParams(-1,-2));

        MaterialCardView card = card(); LinearLayout box = box(card); content.addView(card);
        box.addView(text(AppPreferences.tr(this,"Защита приложения","App protection"),20,R.color.text_main,true));
        TextView info = text(AppPreferences.tr(this,"PIN-код или пароль защитит кредиты, платежи и настройки от случайных изменений.","A PIN or password protects your loans, payments and settings from accidental changes."),14,R.color.text_secondary,false); LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.setMargins(0,dp(6),0,dp(10));box.addView(info,ip);
        enabledSwitch = new SwitchMaterial(this); enabledSwitch.setText(AppPreferences.tr(this,"Защита приложения","App protection")); enabledSwitch.setTextColor(ContextCompat.getColor(this,R.color.text_main)); enabledSwitch.setTextSize(16); box.addView(enabledSwitch,new LinearLayout.LayoutParams(-1,dp(56)));

        MaterialButton change = outline(AppPreferences.tr(this,"Установить / изменить PIN или пароль","Set / change PIN or password")); change.setOnClickListener(v->chooseKind()); box.addView(change,buttonParams());

        biometricSwitch = new SwitchMaterial(this); biometricSwitch.setText(AppPreferences.tr(this,"Отпечаток пальца / биометрия","Fingerprint / biometrics")); biometricSwitch.setTextColor(ContextCompat.getColor(this,R.color.text_main)); biometricSwitch.setTextSize(16); box.addView(biometricSwitch,new LinearLayout.LayoutParams(-1,dp(56)));

        box.addView(text(AppPreferences.tr(this,"Автоблокировка","Auto-lock"),16,R.color.text_main,true));
        timeoutSpinner = new Spinner(this); UiUtils.styleSpinner(this,timeoutSpinner); timeoutSpinner.setAdapter(UiUtils.spinnerAdapter(this,new String[]{
                AppPreferences.tr(this,"Сразу","Immediately"),
                AppPreferences.tr(this,"Через 1 минуту","After 1 minute"),
                AppPreferences.tr(this,"Через 5 минут","After 5 minutes"),
                AppPreferences.tr(this,"Через 15 минут","After 15 minutes")
        })); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(56));tp.setMargins(0,dp(8),0,0);box.addView(timeoutSpinner,tp);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,bars.bottom+dp(8));return insets;});
        return root;
    }

    private void bind() {
        binding=true;
        enabledSwitch.setChecked(AppPreferences.isSecurityEnabled(this));
        boolean bioAvailable=canUseBiometric(); biometricSwitch.setEnabled(bioAvailable); biometricSwitch.setChecked(bioAvailable && AppPreferences.isBiometricEnabled(this));
        int m=AppPreferences.getLockTimeoutMinutes(this); timeoutSpinner.setSelection(m==1?1:m==5?2:m==15?3:0);
        binding=false;

        enabledSwitch.setOnCheckedChangeListener((b,checked)->{
            if(binding)return;
            if(checked && !AppPreferences.hasAppSecret(this)) { b.setChecked(false); chooseKind(); }
            else { AppPreferences.setSecurityEnabled(this,checked); if(!checked) AppPreferences.setBiometricEnabled(this,false); }
        });
        biometricSwitch.setOnCheckedChangeListener((b,checked)->{if(binding)return;if(checked&&!AppPreferences.isSecurityEnabled(this)){b.setChecked(false);Toast.makeText(this,AppPreferences.tr(this,"Сначала включите защиту приложения","Enable app protection first"),Toast.LENGTH_SHORT).show();return;}AppPreferences.setBiometricEnabled(this,checked);});
        timeoutSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){@Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(binding)return;AppPreferences.setLockTimeoutMinutes(SecurityActivity.this,pos==1?1:pos==2?5:pos==3?15:0);}@Override public void onNothingSelected(android.widget.AdapterView<?> p){}});
    }

    private void chooseKind() {
        String[] options={AppPreferences.tr(this,"PIN-код","PIN"),AppPreferences.tr(this,"Пароль","Password")};
        new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Тип защиты","Protection type")).setItems(options,(d,w)->askSecret(w==0?"pin":"password")).show();
    }

    private void askSecret(String kind) {
        boolean pin="pin".equals(kind);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(4),dp(22),0);
        EditText a=input(pin,AppPreferences.tr(this,pin?"Введите PIN-код":"Введите пароль",pin?"Enter PIN":"Enter password"));
        EditText b=input(pin,AppPreferences.tr(this,pin?"Повторите PIN-код":"Повторите пароль",pin?"Repeat PIN":"Repeat password"));
        box.addView(a,new LinearLayout.LayoutParams(-1,dp(58)));box.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
        new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Установить защиту","Set protection")).setView(box)
                .setNegativeButton(AppPreferences.tr(this,"Отмена","Cancel"),null)
                .setPositiveButton(AppPreferences.tr(this,"Сохранить","Save"),(d,w)->{
                    String x=a.getText().toString(),y=b.getText().toString();int min=pin?4:4;
                    if(x.length()<min||(pin&&x.length()>6)){Toast.makeText(this,AppPreferences.tr(this,"PIN должен содержать 4–6 цифр","PIN must contain 4–6 digits"),Toast.LENGTH_LONG).show();return;}
                    if(!x.equals(y)){Toast.makeText(this,AppPreferences.tr(this,"Значения не совпадают","Values do not match"),Toast.LENGTH_LONG).show();return;}
                    AppPreferences.setAppSecret(this,kind,x);enabledSwitch.setChecked(true);Toast.makeText(this,AppPreferences.tr(this,"Защита включена","Protection enabled"),Toast.LENGTH_SHORT).show();
                }).show();
    }

    private EditText input(boolean pin,String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setInputType(pin?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);e.setTextColor(ContextCompat.getColor(this,R.color.text_main));e.setHintTextColor(ContextCompat.getColor(this,R.color.text_secondary));return e;}
    private boolean canUseBiometric(){int r=BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK|BiometricManager.Authenticators.BIOMETRIC_STRONG);return r==BiometricManager.BIOMETRIC_SUCCESS;}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));return c;}
    private LinearLayout box(MaterialCardView c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(18),dp(18),dp(18));c.addView(b);return b;}
    private MaterialButton outline(String s){MaterialButton b=new MaterialButton(this);b.setText(s);b.setAllCaps(false);b.setTextColor(ContextCompat.getColor(this,R.color.primary));b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.card_background)));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));b.setStrokeWidth(dp(1));b.setCornerRadius(dp(14));return b;}
    private LinearLayout.LayoutParams buttonParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(8),0,dp(8));return p;}
    private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);t.setFocusable(true);return t;}
    private TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,color));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
