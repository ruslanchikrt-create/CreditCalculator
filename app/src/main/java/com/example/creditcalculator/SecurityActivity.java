package com.example.creditcalculator;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executor;

public class SecurityActivity extends AppCompatActivity {
    private static final int REQUEST_DEVICE_CREDENTIAL = 6201;
    private SwitchMaterial enabledSwitch;
    private SwitchMaterial biometricSwitch;
    private Spinner timeoutSpinner;
    private TextView methodText;
    private boolean binding;
    private Runnable pendingDeviceSuccess;

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
        TextView info = text(AppPreferences.tr(this,"Можно использовать PIN или пароль, созданный в приложении, либо системную защиту экрана телефона.","Use a PIN/password created in the app or your phone's system screen lock."),14,R.color.text_secondary,false); LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.setMargins(0,dp(6),0,dp(10));box.addView(info,ip);
        enabledSwitch = new SwitchMaterial(this); enabledSwitch.setText(AppPreferences.tr(this,"Защита приложения","App protection")); enabledSwitch.setTextColor(ContextCompat.getColor(this,R.color.text_main)); enabledSwitch.setTextSize(16); box.addView(enabledSwitch,new LinearLayout.LayoutParams(-1,dp(56)));

        methodText=text("",14,R.color.text_secondary,true);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.setMargins(0,0,0,dp(4));box.addView(methodText,mp);
        MaterialButton change = outline(AppPreferences.tr(this,"Установить / изменить способ защиты","Set / change protection method")); change.setOnClickListener(v->beginChangeProtection()); box.addView(change,buttonParams());

        biometricSwitch = new SwitchMaterial(this); biometricSwitch.setText(AppPreferences.tr(this,"Отпечаток пальца / биометрия","Fingerprint / biometrics")); biometricSwitch.setTextColor(ContextCompat.getColor(this,R.color.text_main)); biometricSwitch.setTextSize(16); box.addView(biometricSwitch,new LinearLayout.LayoutParams(-1,dp(56)));

        box.addView(text(AppPreferences.tr(this,"Автоблокировка","Auto-lock"),16,R.color.text_main,true));
        timeoutSpinner = new Spinner(this); UiUtils.styleSpinner(this,timeoutSpinner); timeoutSpinner.setAdapter(UiUtils.spinnerAdapter(this,new String[]{
                AppPreferences.tr(this,"Сразу","Immediately"),
                AppPreferences.tr(this,"Через 1 минуту","After 1 minute"),
                AppPreferences.tr(this,"Через 5 минут","After 5 minutes"),
                AppPreferences.tr(this,"Через 15 минут","After 15 minutes")
        })); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(56));tp.setMargins(0,dp(8),0,0);box.addView(timeoutSpinner,tp);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,Math.max(bars.bottom,ime.bottom)+dp(8));UiUtils.ensureFocusedFieldVisible(scroll);return insets;});
        return root;
    }

    private void bind() {
        binding=true;
        enabledSwitch.setChecked(AppPreferences.hasConfiguredSecurity(this));
        boolean bioAvailable=canUseBiometric(); biometricSwitch.setEnabled(bioAvailable); biometricSwitch.setChecked(bioAvailable && AppPreferences.isBiometricEnabled(this));
        int m=AppPreferences.getLockTimeoutMinutes(this); timeoutSpinner.setSelection(m==1?1:m==5?2:m==15?3:0);
        updateMethodText();
        binding=false;

        enabledSwitch.setOnCheckedChangeListener((b,checked)->{
            if(binding)return;
            if(checked && !AppPreferences.hasConfiguredSecurity(this)) { binding=true;b.setChecked(false);binding=false;showKindOptions(false); }
            else { AppPreferences.setSecurityEnabled(this,checked); if(!checked) AppPreferences.setBiometricEnabled(this,false); updateMethodText(); }
        });
        biometricSwitch.setOnCheckedChangeListener((b,checked)->{if(binding)return;if(checked&&!AppPreferences.hasConfiguredSecurity(this)){binding=true;b.setChecked(false);binding=false;Toast.makeText(this,AppPreferences.tr(this,"Сначала включите защиту приложения","Enable app protection first"),Toast.LENGTH_SHORT).show();return;}AppPreferences.setBiometricEnabled(this,checked);});
        timeoutSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){@Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(binding)return;AppPreferences.setLockTimeoutMinutes(SecurityActivity.this,pos==1?1:pos==2?5:pos==3?15:0);}@Override public void onNothingSelected(android.widget.AdapterView<?> p){}});
    }

    private void beginChangeProtection(){
        if(!AppPreferences.hasConfiguredSecurity(this)){showKindOptions(false);return;}
        authenticateCurrent(()->showKindOptions(true));
    }

    private void authenticateCurrent(Runnable success){
        if(AppPreferences.isDeviceCredentialSecurity(this)){authenticateDevice(success);return;}
        boolean pin=AppPreferences.SECURITY_PIN.equals(AppPreferences.getSecurityKind(this));
        EditText old=input(pin,AppPreferences.tr(this,pin?"Введите старый PIN-код":"Введите старый пароль",pin?"Enter old PIN":"Enter old password"));
        int pad=dp(22);old.setPadding(pad,0,pad,0);
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle(AppPreferences.tr(this,"Подтвердите текущую защиту","Confirm current protection"))
                .setMessage(AppPreferences.tr(this,pin?"Используется PIN, созданный в приложении.":"Используется пароль, созданный в приложении.",pin?"This is the PIN created in the app.":"This is the password created in the app."))
                .setView(old)
                .setNegativeButton(AppPreferences.tr(this,"Отмена","Cancel"),null)
                .setPositiveButton(AppPreferences.tr(this,"Продолжить","Continue"),null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(AppPreferences.verifyAppSecret(this,old.getText().toString())){dialog.dismiss();success.run();}else{old.setError(AppPreferences.tr(this,"Неверный PIN/пароль","Wrong PIN/password"));}}));
        dialog.show();
    }

    private void showKindOptions(boolean currentAlreadyVerified) {
        String[] options={AppPreferences.tr(this,"PIN приложения","App PIN"),AppPreferences.tr(this,"Пароль приложения","App password"),AppPreferences.tr(this,"Пароль / PIN устройства","Device PIN / password")};
        new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Способ защиты","Protection method")).setItems(options,(d,w)->{
            if(w==0)askSecret(AppPreferences.SECURITY_PIN);
            else if(w==1)askSecret(AppPreferences.SECURITY_PASSWORD);
            else chooseDeviceProtection(currentAlreadyVerified);
        }).show();
    }

    private void chooseDeviceProtection(boolean currentAlreadyVerified){
        if(!isDeviceSecure()){Toast.makeText(this,AppPreferences.tr(this,"Сначала установите PIN, графический ключ или пароль экрана в настройках телефона.","First set a PIN, pattern, or screen-lock password in phone settings."),Toast.LENGTH_LONG).show();return;}
        if(currentAlreadyVerified){applyDeviceProtection();return;}
        authenticateDevice(this::applyDeviceProtection);
    }

    private void applyDeviceProtection(){
        AppPreferences.setDeviceCredentialSecurity(this);
        AppPreferences.setSecurityEnabled(this,true);
        binding=true;enabledSwitch.setChecked(true);binding=false;
        updateMethodText();
        Toast.makeText(this,AppPreferences.tr(this,"Будет использоваться защита экрана устройства","Device screen lock will be used"),Toast.LENGTH_SHORT).show();
    }

    private void askSecret(String kind) {
        boolean pin=AppPreferences.SECURITY_PIN.equals(kind);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(4),dp(22),0);
        EditText a=input(pin,AppPreferences.tr(this,pin?"Введите PIN-код":"Введите пароль",pin?"Enter PIN":"Enter password"));
        EditText b=input(pin,AppPreferences.tr(this,pin?"Повторите PIN-код":"Повторите пароль",pin?"Repeat PIN":"Repeat password"));
        box.addView(a,new LinearLayout.LayoutParams(-1,dp(58)));box.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
        CheckBox show=new CheckBox(this);show.setText(AppPreferences.tr(this,"Показать пароль / PIN","Show password / PIN"));show.setTextColor(ContextCompat.getColor(this,R.color.text_main));show.setOnCheckedChangeListener((button,checked)->{setSecretVisible(a,pin,checked);setSecretVisible(b,pin,checked);});box.addView(show,new LinearLayout.LayoutParams(-1,dp(48)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Установить защиту","Set protection")).setView(box)
                .setNegativeButton(AppPreferences.tr(this,"Отмена","Cancel"),null)
                .setPositiveButton(AppPreferences.tr(this,"Сохранить","Save"),null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String first=a.getText().toString(),second=b.getText().toString();
            if(first.length()<4||(pin&&first.length()>6)){a.setError(AppPreferences.tr(this,pin?"PIN должен содержать 4–6 цифр":"Пароль должен содержать минимум 4 символа",pin?"PIN must contain 4–6 digits":"Password must contain at least 4 characters"));return;}
            if(!first.equals(second)){b.setError(AppPreferences.tr(this,"Значения не совпадают","Values do not match"));return;}
            AppPreferences.setAppSecret(this,kind,first);binding=true;enabledSwitch.setChecked(true);binding=false;updateMethodText();dialog.dismiss();Toast.makeText(this,AppPreferences.tr(this,"Защита включена","Protection enabled"),Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void setSecretVisible(EditText e,boolean pin,boolean visible){int pos=e.getSelectionStart();e.setInputType(visible?(pin?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT):(pin?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD));if(pos>=0&&pos<=e.length())e.setSelection(pos);}
    private EditText input(boolean pin,String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setInputType(pin?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);e.setTextColor(ContextCompat.getColor(this,R.color.text_main));e.setHintTextColor(ContextCompat.getColor(this,R.color.text_secondary));return e;}

    private void authenticateDevice(Runnable success){
        if(!isDeviceSecure()){Toast.makeText(this,AppPreferences.tr(this,"Защита экрана устройства не настроена","Device screen lock is not configured"),Toast.LENGTH_LONG).show();return;}
        if(Build.VERSION.SDK_INT>=30){
            Executor executor=ContextCompat.getMainExecutor(this);
            BiometricPrompt prompt=new BiometricPrompt(this,executor,new BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){super.onAuthenticationSucceeded(result);success.run();}});
            BiometricPrompt.PromptInfo info=new BiometricPrompt.PromptInfo.Builder().setTitle(AppPreferences.tr(this,"Подтвердите пароль устройства","Confirm device credential")).setSubtitle(AppPreferences.tr(this,"Используйте PIN, графический ключ или пароль телефона","Use your phone PIN, pattern, or password")).setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL).build();
            prompt.authenticate(info);
        }else{
            KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);Intent intent=km==null?null:km.createConfirmDeviceCredentialIntent(AppPreferences.tr(this,"Подтвердите пароль устройства","Confirm device credential"),AppPreferences.tr(this,"Используйте защиту экрана телефона","Use your phone screen lock"));if(intent==null){Toast.makeText(this,AppPreferences.tr(this,"Не удалось открыть проверку устройства","Could not open device authentication"),Toast.LENGTH_LONG).show();return;}pendingDeviceSuccess=success;startActivityForResult(intent,REQUEST_DEVICE_CREDENTIAL);
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQUEST_DEVICE_CREDENTIAL){Runnable r=pendingDeviceSuccess;pendingDeviceSuccess=null;if(resultCode==RESULT_OK&&r!=null)r.run();}}
    private boolean isDeviceSecure(){KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);return km!=null&&km.isDeviceSecure();}
    private boolean canUseBiometric(){int r=BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK|BiometricManager.Authenticators.BIOMETRIC_STRONG);return r==BiometricManager.BIOMETRIC_SUCCESS;}
    private void updateMethodText(){if(methodText==null)return;String method;if(!AppPreferences.hasConfiguredSecurity(this))method=AppPreferences.tr(this,"Не установлен","Not set");else if(AppPreferences.SECURITY_DEVICE.equals(AppPreferences.getSecurityKind(this)))method=AppPreferences.tr(this,"Пароль / PIN устройства","Device PIN / password");else if(AppPreferences.SECURITY_PASSWORD.equals(AppPreferences.getSecurityKind(this)))method=AppPreferences.tr(this,"Пароль приложения","App password");else method=AppPreferences.tr(this,"PIN приложения","App PIN");methodText.setText(AppPreferences.tr(this,"Текущий способ: ","Current method: ")+method);}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));return c;}
    private LinearLayout box(MaterialCardView c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(18),dp(18),dp(18));c.addView(b);return b;}
    private MaterialButton outline(String s){MaterialButton b=new MaterialButton(this);b.setText(s);b.setAllCaps(false);b.setTextColor(ContextCompat.getColor(this,R.color.primary));b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.card_background)));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));b.setStrokeWidth(dp(1));b.setCornerRadius(dp(14));return b;}
    private LinearLayout.LayoutParams buttonParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(8),0,dp(8));return p;}
    private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);t.setFocusable(true);return t;}
    private TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,color));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
