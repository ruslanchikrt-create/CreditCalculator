package com.example.creditcalculator;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class CreditApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private int started;
    private long backgroundAt;
    private boolean unlockedThisForeground;
    private boolean gateLaunching;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        boolean enteringForeground = started == 0;
        started++;
        if (!enteringForeground) return;
        unlockedThisForeground = false;
        handler.post(() -> showGateIfNeeded(activity));
    }

    private void showGateIfNeeded(Activity activity) {
        if (gateLaunching || activity instanceof LanguageSelectionActivity || activity instanceof LockActivity) return;
        if (!AppPreferences.isLanguageChosen(this)) {
            gateLaunching = true;
            Intent intent = new Intent(this, LanguageSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            handler.postDelayed(() -> gateLaunching = false, 500);
            return;
        }
        if (!AppPreferences.hasConfiguredSecurity(this)) return;
        int timeout = AppPreferences.getLockTimeoutMinutes(this);
        long elapsed = backgroundAt <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() - backgroundAt;
        boolean needsLock = !unlockedThisForeground && (backgroundAt <= 0 || timeout == 0 || elapsed >= timeout * 60_000L);
        if (needsLock) {
            gateLaunching = true;
            Intent intent = new Intent(this, LockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            handler.postDelayed(() -> gateLaunching = false, 500);
        } else {
            unlockedThisForeground = true;
        }
    }

    public void markUnlocked() {
        unlockedThisForeground = true;
        gateLaunching = false;
    }

    public void forceLockNextForeground() {
        unlockedThisForeground = false;
        backgroundAt = 0L;
    }

    @Override public void onActivityStopped(Activity activity) {
        started = Math.max(0, started - 1);
        if (started == 0) backgroundAt = System.currentTimeMillis();
    }
    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
