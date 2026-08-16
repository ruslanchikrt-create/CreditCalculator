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
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executor;

public class LockActivity extends AppCompatActivity {
    private static final int REQUEST_DEVICE_CREDENTIAL=6301;
    private TextInputEditText secretInput;
    private PatternLockView patternView;
    private int failures;
    private boolean deviceMode;
    private boolean patternMode;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        deviceMode=AppPreferences.isDeviceCredentialSecurity(this);
        patternMode=AppPreferences.isPatternSecurity(this);
        setContentView(build());
        if(deviceMode) getWindow().getDecorView().postDelayed(this::showDeviceAuthentication,250);
        else if (AppPreferences.isBiometricEnabled(this)) getWindow().getDecorView().postDelayed(this::showBiometric, 250);
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(dp(22), dp(24), dp(22), dp(24)); UiUtils.applyBackground(this, root);
        MaterialCardView card = new MaterialCardView(this); card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background)); card.setRadius(dp(22)); card.setStrokeColor(ContextCompat.getColor(this, R.color.border)); card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(24), dp(22), dp(22)); card.addView(box); root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView icon = text("🔒", 42, R.color.text_main, false); icon.setGravity(Gravity.CENTER); box.addView(icon);
        boolean pin = AppPreferences.SECURITY_PIN.equals(AppPreferences.getSecurityKind(this));
        String titleText;
        if(deviceMode) titleText=AppPreferences.tr(this,"Разблокировка устройства","Device authentication");
        else if(patternMode) titleText=AppPreferences.tr(this,"Нарисуйте графический ключ приложения","Draw the app pattern");
        else titleText=AppPreferences.tr(this, pin ? "Введите PIN-код приложения" : "Введите пароль приложения", pin ? "Enter app PIN" : "Enter app password");
        TextView title = text(titleText, 24, R.color.text_main, true); title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.setMargins(0, dp(8), 0, dp(18)); box.addView(title, tp);

        if(deviceMode){
            TextView note=text(AppPreferences.tr(this,"Используется PIN, графический ключ или пароль экрана вашего телефона.","Your phone PIN, pattern, or screen-lock password is used."),14,R.color.text_secondary,false);note.setGravity(Gravity.CENTER);box.addView(note);
            MaterialButton device = new MaterialButton(this); device.setText(AppPreferences.tr(this,"Разблокировать устройством","Use device credential")); device.setAllCaps(false); device.setTextSize(17); device.setTextColor(Color.WHITE); device.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); device.setCornerRadius(dp(14)); device.setOnClickListener(v -> showDeviceAuthentication());
            LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, dp(56)); dpv.setMargins(0, dp(16), 0, 0); box.addView(device, dpv);
        }else if(patternMode){
            TextView note=text(AppPreferences.tr(this,"Соедините точки в том же порядке, который вы установили в настройках безопасности.","Connect the dots in the same order you set in Security settings."),14,R.color.text_secondary,false);note.setGravity(Gravity.CENTER);box.addView(note);
            patternView=new PatternLockView(this);patternView.setOnPatternCompleteListener(this::unlockPattern);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(285));pp.setMargins(0,dp(4),0,0);box.addView(patternView,pp);
        }else{
            TextInputLayout layout = new TextInputLayout(this);
            layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            layout.setBoxBackgroundColor(ContextCompat.getColor(this,R.color.card_background));
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.primary));
            layout.setBoxStrokeWidth(dp(1));
            layout.setBoxStrokeWidthFocused(dp(2));
            layout.setBoxCornerRadii(dp(14),dp(14),dp(14),dp(14));
            layout.setHint(pin?AppPreferences.tr(this,"PIN-код","PIN"):AppPreferences.tr(this,"Пароль","Password"));
            layout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.text_secondary)));
            layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            layout.setEndIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));
            layout.setEndIconContentDescription(AppPreferences.tr(this,"Показать или скрыть пароль","Show or hide password"));

            secretInput = new TextInputEditText(this);
            secretInput.setSingleLine(true);
            secretInput.setTextColor(ContextCompat.getColor(this, R.color.text_main));
            secretInput.setHintTextColor(ContextCompat.getColor(this,R.color.text_secondary));
            secretInput.setBackgroundColor(Color.TRANSPARENT);
            secretInput.setPadding(dp(14),0,dp(10),0);
            secretInput.setInputType(pin ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            secretInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
            secretInput.setOnEditorActionListener((v, actionId, event) -> { if (actionId == EditorInfo.IME_ACTION_DONE) { unlock(); return true; } return false; });
            layout.addView(secretInput, new TextInputLayout.LayoutParams(-1, dp(62)));
            box.addView(layout, new LinearLayout.LayoutParams(-1, -2));

            TextView help=text("ⓘ "+AppPreferences.tr(this,"Нажмите на глаз справа, чтобы показать или скрыть введённый код.","Tap the eye on the right to show or hide what you entered."),13,R.color.text_secondary,false);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(6),0,0);box.addView(help,hp);

            MaterialButton unlock = new MaterialButton(this); unlock.setText(AppPreferences.tr(this, "Разблокировать", "Unlock")); unlock.setAllCaps(false); unlock.setTextSize(17); unlock.setTextColor(Color.WHITE); unlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); unlock.setCornerRadius(dp(14)); unlock.setOnClickListener(v -> unlock());
            LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, dp(56)); up.setMargins(0, dp(14), 0, 0); box.addView(unlock, up);
        }

        if (AppPreferences.isBiometricEnabled(this) && canUseBiometric()) {
            MaterialButton bio = new MaterialButton(this); bio.setText(AppPreferences.tr(this, "Использовать биометрию", "Use biometrics")); bio.setAllCaps(false); bio.setTextColor(ContextCompat.getColor(this, R.color.primary)); bio.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background))); bio.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); bio.setStrokeWidth(dp(1)); bio.setCornerRadius(dp(14)); bio.setOnClickListener(v -> showBiometric());
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(52)); bp.setMargins(0, dp(10), 0, 0); box.addView(bio, bp);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> { Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(dp(22), bars.top + dp(16), dp(22), bars.bottom + dp(16)); return insets; });
        return root;
    }

    private void unlock() {
        if(deviceMode||patternMode||secretInput==null)return;
        if (failures >= 5) {
            secretInput.setEnabled(false);
            secretInput.postDelayed(() -> { failures = 0; secretInput.setEnabled(true); }, 10_000);
            Toast.makeText(this, AppPreferences.tr(this, "Слишком много попыток. Повторите через 10 секунд.", "Too many attempts. Try again in 10 seconds."), Toast.LENGTH_LONG).show();
            return;
        }
        if (AppPreferences.verifyAppSecret(this, secretInput.getText() == null ? "" : secretInput.getText().toString())) {
            success();
        } else {
            failures++;
            secretInput.setText("");
            Toast.makeText(this, AppPreferences.tr(this, "Неверный PIN/пароль", "Wrong PIN/password"), Toast.LENGTH_SHORT).show();
        }
    }

    private void unlockPattern(String pattern){
        if(!patternMode||patternView==null)return;
        if(failures>=5){
            patternView.setEnabled(false);
            patternView.postDelayed(()->{failures=0;patternView.setEnabled(true);patternView.reset();},10_000);
            Toast.makeText(this,AppPreferences.tr(this,"Слишком много попыток. Повторите через 10 секунд.","Too many attempts. Try again in 10 seconds."),Toast.LENGTH_LONG).show();
            return;
        }
        if(AppPreferences.verifyAppSecret(this,pattern)){success();return;}
        failures++;
        patternView.showError();
        Toast.makeText(this,AppPreferences.tr(this,"Неверный графический ключ","Wrong pattern"),Toast.LENGTH_SHORT).show();
    }

    private boolean canUseBiometric() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometric() {
        if (!canUseBiometric() || isFinishing()) return;
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) { super.onAuthenticationSucceeded(result); success(); }
        });
        String negative;
        if(deviceMode) negative=AppPreferences.tr(this,"Использовать пароль устройства","Use device credential");
        else if(patternMode) negative=AppPreferences.tr(this,"Использовать графический ключ","Use app pattern");
        else negative=AppPreferences.tr(this,"Использовать PIN/пароль приложения","Use app PIN/password");
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(AppPreferences.tr(this, "Разблокировать приложение", "Unlock app"))
                .setSubtitle(AppPreferences.tr(this, "Подтвердите личность", "Confirm your identity"))
                .setNegativeButtonText(negative)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
        prompt.authenticate(info);
    }

    private void showDeviceAuthentication(){
        if(!deviceMode||isFinishing())return;
        KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);if(km==null||!km.isDeviceSecure()){Toast.makeText(this,AppPreferences.tr(this,"Защита экрана устройства не настроена. Сначала добавьте её в настройках телефона.","Device screen lock is not configured. Set it in phone settings first."),Toast.LENGTH_LONG).show();return;}
        if(Build.VERSION.SDK_INT>=30){
            Executor executor=ContextCompat.getMainExecutor(this);
            int auth=BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            if(AppPreferences.isBiometricEnabled(this)&&canUseBiometric())auth|=BiometricManager.Authenticators.BIOMETRIC_WEAK;
            BiometricPrompt prompt=new BiometricPrompt(this,executor,new BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){super.onAuthenticationSucceeded(result);success();}});
            BiometricPrompt.PromptInfo info=new BiometricPrompt.PromptInfo.Builder().setTitle(AppPreferences.tr(this,"Разблокировать приложение","Unlock app")).setSubtitle(AppPreferences.tr(this,"Используйте защиту экрана устройства","Use your device screen lock")).setAllowedAuthenticators(auth).build();
            prompt.authenticate(info);
        }else{
            Intent intent=km.createConfirmDeviceCredentialIntent(AppPreferences.tr(this,"Разблокировать приложение","Unlock app"),AppPreferences.tr(this,"Введите PIN, графический ключ или пароль устройства","Enter device PIN, pattern, or password"));if(intent!=null)startActivityForResult(intent,REQUEST_DEVICE_CREDENTIAL);
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQUEST_DEVICE_CREDENTIAL&&resultCode==RESULT_OK)success();}

    private void success() {
        if (getApplication() instanceof CreditApplication) ((CreditApplication) getApplication()).markUnlocked();
        finish();
    }

    @android.annotation.SuppressLint("MissingSuperCall")
    @Override public void onBackPressed() { moveTaskToBack(true); }
    private TextView text(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ContextCompat.getColor(this,color)); if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD); return t; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
