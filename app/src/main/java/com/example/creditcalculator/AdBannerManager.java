package com.example.creditcalculator;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

import java.util.WeakHashMap;

final class AdBannerManager {
    private static final WeakHashMap<Activity, BannerAdView> ADS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, FrameLayout> CONTAINERS = new WeakHashMap<>();

    private AdBannerManager() {}

    static void attach(Activity activity) {
        if (!isEligible(activity) || activity.isFinishing() || activity.isDestroyed() || ADS.containsKey(activity)) return;
        activity.getWindow().getDecorView().post(() -> attachNow(activity));
    }

    static void destroy(Activity activity) {
        BannerAdView ad = ADS.remove(activity);
        if (ad != null) {
            try { ad.destroy(); } catch (Exception ignored) {}
        }
        FrameLayout container = CONTAINERS.remove(activity);
        if (container != null && container.getParent() instanceof ViewGroup) {
            ((ViewGroup) container.getParent()).removeView(container);
        }
    }

    private static boolean isEligible(Activity activity) {
        return activity instanceof PaymentsActivity
                || activity instanceof MainActivity
                || activity instanceof FinancialStatsActivity;
    }

    private static void attachNow(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed() || ADS.containsKey(activity)) return;
        LinearLayout host = findMainColumn(activity);
        if (host == null) return;

        FrameLayout container = new FrameLayout(activity);
        container.setVisibility(View.INVISIBLE);
        container.setClipToPadding(false);
        host.addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        BannerAdView banner = new BannerAdView(activity);
        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        container.addView(banner, bannerParams);

        ADS.put(activity, banner);
        CONTAINERS.put(activity, container);

        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!container.getViewTreeObserver().isAlive()) return;
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (activity.isFinishing() || activity.isDestroyed()) {
                    destroy(activity);
                    return;
                }
                load(activity, container, banner);
            }
        });
    }

    private static void load(Activity activity, FrameLayout container, BannerAdView banner) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthPixels = container.getWidth();
        if (widthPixels <= 0) widthPixels = metrics.widthPixels;
        int widthDp = Math.max(1, Math.round(widthPixels / metrics.density));

        banner.setAdSize(BannerAdSize.sticky(activity, widthDp));
        banner.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    destroy(activity);
                    return;
                }
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                container.setVisibility(View.GONE);
            }

            @Override
            public void onAdClicked() {
                // Yandex SDK records clicks automatically.
            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {
                // Yandex SDK records impressions automatically.
            }
        });

        AdRequest request = new AdRequest.Builder(activity.getString(R.string.yandex_banner_ad_unit_id)).build();
        banner.loadAd(request);
    }

    @Nullable
    private static LinearLayout findMainColumn(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return null;
        ViewGroup contentGroup = (ViewGroup) content;
        if (contentGroup.getChildCount() == 0) return null;

        View root = contentGroup.getChildAt(0);
        if (root instanceof DrawerLayout) {
            DrawerLayout drawer = (DrawerLayout) root;
            if (drawer.getChildCount() == 0) return null;
            root = drawer.getChildAt(0);
        }

        if (root instanceof LinearLayout) {
            LinearLayout linear = (LinearLayout) root;
            if (linear.getOrientation() == LinearLayout.VERTICAL) return linear;
        }

        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout linear = (LinearLayout) child;
                    if (linear.getOrientation() == LinearLayout.VERTICAL) return linear;
                }
            }
        }
        return null;
    }
}
