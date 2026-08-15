package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_AVATAR = 5101;

    private ImageView avatarView;
    private TextView fallbackAvatar;
    private TextInputEditText nameInput;
    private String pendingAvatarUri;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPreferences.wrapLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyNightMode(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(buildContent());
        bindValues();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        UiUtils.applyBackground(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(12), 0);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView back = topText("‹", 34);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView title = topText(AppPreferences.tr(this, "Профиль", "Profile"), 20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(36));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView heading = text(AppPreferences.tr(this, "Ваш профиль", "Your profile"), 28, R.color.text_main, true);
        content.addView(heading);

        TextView subtitle = text(AppPreferences.tr(this,
                "Имя и аватар будут показываться в боковом меню.",
                "Your name and avatar will appear in the side menu."), 15, R.color.text_secondary, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.setMargins(0, dp(6), 0, dp(24));
        content.addView(subtitle, subtitleParams);

        FrameLayout avatarBox = new FrameLayout(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        avatarParams.setMargins(0, 0, 0, dp(14));
        content.addView(avatarBox, avatarParams);

        fallbackAvatar = text("%", 46, R.color.white, true);
        fallbackAvatar.setGravity(Gravity.CENTER);
        fallbackAvatar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark));
        avatarBox.addView(fallbackAvatar, new FrameLayout.LayoutParams(-1, -1));

        avatarView = new ImageView(this);
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarView.setVisibility(View.GONE);
        avatarBox.addView(avatarView, new FrameLayout.LayoutParams(-1, -1));

        MaterialButton choose = outlineButton(AppPreferences.tr(this, "Выбрать аватарку", "Choose avatar"));
        choose.setOnClickListener(v -> chooseAvatar());
        content.addView(choose, buttonParams());

        MaterialButton reset = outlineButton(AppPreferences.tr(this, "Убрать аватарку", "Remove avatar"));
        reset.setOnClickListener(v -> {
            pendingAvatarUri = "";
            showAvatar("");
        });
        content.addView(reset, buttonParams());

        TextInputLayout nameLayout = new TextInputLayout(this);
        nameLayout.setHint(AppPreferences.tr(this, "Имя", "Name"));
        nameLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        nameLayout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-1, -2);
        nameLp.setMargins(0, dp(8), 0, dp(18));
        content.addView(nameLayout, nameLp);

        nameInput = new TextInputEditText(this);
        nameInput.setSingleLine(true);
        nameInput.setMinHeight(dp(58));
        nameLayout.addView(nameInput, new TextInputLayout.LayoutParams(-1, -2));

        MaterialButton save = new MaterialButton(this);
        save.setText(AppPreferences.tr(this, "Сохранить", "Save"));
        save.setAllCaps(false);
        save.setTextColor(Color.WHITE);
        save.setTextSize(17);
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        save.setCornerRadius(dp(14));
        save.setOnClickListener(v -> saveProfile());
        content.addView(save, new LinearLayout.LayoutParams(-1, dp(56)));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(0, bars.top, 0, 0);
            scroll.setPadding(0, 0, 0, Math.max(bars.bottom, ime.bottom) + dp(12));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private void bindValues() {
        nameInput.setText(AppPreferences.getProfileName(this));
        pendingAvatarUri = AppPreferences.getAvatarUri(this);
        showAvatar(pendingAvatarUri);
    }

    private void chooseAvatar() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_AVATAR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AVATAR || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        pendingAvatarUri = uri.toString();
        showAvatar(pendingAvatarUri);
    }

    private void showAvatar(String uriText) {
        if (uriText == null || uriText.trim().isEmpty()) {
            avatarView.setImageDrawable(null);
            avatarView.setVisibility(View.GONE);
            fallbackAvatar.setVisibility(View.VISIBLE);
            return;
        }
        try {
            avatarView.setImageURI(Uri.parse(uriText));
            avatarView.setVisibility(View.VISIBLE);
            fallbackAvatar.setVisibility(View.GONE);
        } catch (Exception e) {
            avatarView.setVisibility(View.GONE);
            fallbackAvatar.setVisibility(View.VISIBLE);
        }
    }

    private void saveProfile() {
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        AppPreferences.setProfileName(this, name);
        AppPreferences.setAvatarUri(this, pendingAvatarUri);
        Toast.makeText(this, AppPreferences.tr(this, "Профиль сохранён", "Profile saved"), Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private MaterialButton outlineButton(String title) {
        MaterialButton button = new MaterialButton(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(ContextCompat.getColor(this, R.color.primary));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background)));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        button.setStrokeWidth(dp(1));
        button.setCornerRadius(dp(13));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.setMargins(0, 0, 0, dp(10));
        return lp;
    }

    private TextView topText(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(ContextCompat.getColor(this, color));
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
