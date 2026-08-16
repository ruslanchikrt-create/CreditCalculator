package com.example.creditcalculator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight 3x3 application pattern lock.
 * The saved value is only the node sequence; AppPreferences hashes it before storage.
 */
public class PatternLockView extends View {
    public interface OnPatternCompleteListener {
        void onPatternComplete(String pattern);
    }

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] xs = new float[9];
    private final float[] ys = new float[9];
    private final List<Integer> selected = new ArrayList<>();
    private OnPatternCompleteListener listener;
    private float dotRadius;
    private float hitRadius;
    private float currentX;
    private float currentY;
    private boolean drawing;
    private boolean error;

    public PatternLockView(Context context) {
        super(context);
        init();
    }

    public PatternLockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PatternLockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeWidth(dp(2));
        dotPaint.setColor(ContextCompat.getColor(getContext(), R.color.border));

        selectedPaint.setStyle(Paint.Style.FILL);
        selectedPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(dp(5));
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
        linePaint.setAlpha(205);

        setMinimumHeight(dp(250));
    }

    public void setOnPatternCompleteListener(OnPatternCompleteListener listener) {
        this.listener = listener;
    }

    public void reset() {
        selected.clear();
        drawing = false;
        error = false;
        invalidate();
    }

    public void showError() {
        error = true;
        drawing = false;
        invalidate();
        postDelayed(this::reset, 700);
    }

    public String getPatternString() {
        StringBuilder out = new StringBuilder();
        for (int index : selected) out.append(index + 1);
        return out.toString();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float usableW = Math.max(1, w - getPaddingLeft() - getPaddingRight());
        float usableH = Math.max(1, h - getPaddingTop() - getPaddingBottom());
        float size = Math.min(usableW, usableH);
        float left = getPaddingLeft() + (usableW - size) / 2f;
        float top = getPaddingTop() + (usableH - size) / 2f;
        float step = size / 3f;
        dotRadius = Math.max(dp(8), Math.min(dp(15), step * 0.11f));
        hitRadius = Math.max(dp(30), step * 0.34f);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int i = row * 3 + col;
                xs[i] = left + step * (col + 0.5f);
                ys[i] = top + step * (row + 0.5f);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int activeColor = ContextCompat.getColor(getContext(), error ? R.color.danger : R.color.primary);
        selectedPaint.setColor(activeColor);
        linePaint.setColor(activeColor);

        if (!selected.isEmpty()) {
            Path path = new Path();
            int first = selected.get(0);
            path.moveTo(xs[first], ys[first]);
            for (int i = 1; i < selected.size(); i++) {
                int node = selected.get(i);
                path.lineTo(xs[node], ys[node]);
            }
            if (drawing) path.lineTo(currentX, currentY);
            canvas.drawPath(path, linePaint);
        }

        for (int i = 0; i < 9; i++) {
            boolean active = selected.contains(i);
            if (active) {
                canvas.drawCircle(xs[i], ys[i], dotRadius * 1.35f, selectedPaint);
                Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
                inner.setStyle(Paint.Style.FILL);
                inner.setColor(ContextCompat.getColor(getContext(), R.color.card_background));
                canvas.drawCircle(xs[i], ys[i], dotRadius * 0.52f, inner);
            } else {
                canvas.drawCircle(xs[i], ys[i], dotRadius, dotPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        currentX = event.getX();
        currentY = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!selected.isEmpty()) reset();
                drawing = true;
                addHitNode(currentX, currentY);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                addHitNode(currentX, currentY);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                drawing = false;
                addHitNode(currentX, currentY);
                invalidate();
                performClick();
                if (listener != null) listener.onPatternComplete(getPatternString());
                return true;
            case MotionEvent.ACTION_CANCEL:
                drawing = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void addHitNode(float x, float y) {
        int hit = hitTest(x, y);
        if (hit < 0 || selected.contains(hit)) return;
        if (!selected.isEmpty()) {
            int last = selected.get(selected.size() - 1);
            int middle = middleNode(last, hit);
            if (middle >= 0 && !selected.contains(middle)) selected.add(middle);
        }
        selected.add(hit);
    }

    private int hitTest(float x, float y) {
        float best = Float.MAX_VALUE;
        int result = -1;
        for (int i = 0; i < 9; i++) {
            float dx = x - xs[i];
            float dy = y - ys[i];
            float d2 = dx * dx + dy * dy;
            if (d2 <= hitRadius * hitRadius && d2 < best) {
                best = d2;
                result = i;
            }
        }
        return result;
    }

    /** Adds the skipped centre node for straight standard Android-style moves. */
    private int middleNode(int a, int b) {
        int ar = a / 3, ac = a % 3;
        int br = b / 3, bc = b % 3;
        int dr = br - ar, dc = bc - ac;
        if (Math.abs(dr) == 2 && dc == 0) return ((ar + br) / 2) * 3 + ac;
        if (Math.abs(dc) == 2 && dr == 0) return ar * 3 + ((ac + bc) / 2);
        if (Math.abs(dr) == 2 && Math.abs(dc) == 2) return ((ar + br) / 2) * 3 + ((ac + bc) / 2);
        return -1;
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
