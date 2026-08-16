package com.example.creditcalculator;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
    private TextInputEditText secretInput;
    private int failures;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(build());
        if (AppPreferences.isBiometricEnabled(this)) secretInput.postDelayed(this::showBiometric, 250);
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(dp(22), dp(24), dp(22), dp(24)); UiUtils.applyBackground(this, root);
        MaterialCardView card = new MaterialCardView(this); card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background)); card.setRadius(dp(22)); card.setStrokeColor(ContextCompat.getColor(this, R.color.border)); card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(24), dp(22), dp(22)); card.addView(box); root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView icon = text("🔒", 42, R.color.text_main, false); icon.setGravity(Gravity.CENTER); box.addView(icon);
        boolean pin = !"password".equals(AppPreferences.getSecurityKind(this));
        TextView title = text(AppPreferences.tr(this, pin ? "Введите PIN-код" : "Введите пароль", pin ? "Enter PIN" : "Enter password"), 24, R.color.text_main, true); title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.setMargins(0, dp(8), 0, dp(18)); box.addView(title, tp);

        TextInputLayout layout = new TextInputLayout(this); layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE); layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.primary)); layout.setBoxCornerRadii(dp(14),dp(14),dp(14),dp(14));
        secretInput = new TextInputEditText(this); secretInput.setSingleLine(true); secretInput.setTextColor(ContextCompat.getColor(this, R.color.text_main)); secretInput.setInputType(pin ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); secretInput.setImeOptions(EditorInfo.IME_ACTION_DONE); secretInput.setOnEditorActionListener((v, actionId, event) -> { if (actionId == EditorInfo.IME_ACTION_DONE) { unlock(); return true; } return false; });
        layout.addView(secretInput, new TextInputLayout.LayoutParams(-1, dp(58))); box.addView(layout, new LinearLayout.LayoutParams(-1, -2));

        MaterialButton unlock = new MaterialButton(this); unlock.setText(AppPreferences.tr(this, "Разблокировать", "Unlock")); unlock.setAllCaps(false); unlock.setTextSize(17); unlock.setTextColor(Color.WHITE); unlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); unlock.setCornerRadius(dp(14)); unlock.setOnClickListener(v -> unlock());
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, dp(56)); up.setMargins(0, dp(14), 0, 0); box.addView(unlock, up);

        if (AppPreferences.isBiometricEnabled(this) && canUseBiometric()) {
            MaterialButton bio = new MaterialButton(this); bio.setText(AppPreferences.tr(this, "Использовать биометрию", "Use biometrics")); bio.setAllCaps(false); bio.setTextColor(ContextCompat.getColor(this, R.color.primary)); bio.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background))); bio.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))); bio.setStrokeWidth(dp(1)); bio.setCornerRadius(dp(14)); bio.setOnClickListener(v -> showBiometric());
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(52)); bp.setMargins(0, dp(10), 0, 0); box.addView(bio, bp);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> { Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(dp(22), bars.top + dp(16), dp(22), bars.bottom + dp(16)); return insets; });
        return root;
    }

    private void unlock() {
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

    private boolean canUseBiometric() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometric() {
        if (!canUseBiometric() || isFinishing()) return;
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) { super.onAuthenticationSucceeded(result); success(); }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(AppPreferences.tr(this, "Разблокировать приложение", "Unlock app"))
                .setSubtitle(AppPreferences.tr(this, "Подтвердите личность", "Confirm your identity"))
                .setNegativeButtonText(AppPreferences.tr(this, "Использовать PIN/пароль", "Use PIN/password"))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
        prompt.authenticate(info);
    }

    private void success() {
        if (getApplication() instanceof CreditApplication) ((CreditApplication) getApplication()).markUnlocked();
        finish();
    }

    @android.annotation.SuppressLint("MissingSuperCall")
    @Override public void onBackPressed() { moveTaskToBack(true); }
    private TextView text(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ContextCompat.getColor(this,color)); if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD); return t; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
