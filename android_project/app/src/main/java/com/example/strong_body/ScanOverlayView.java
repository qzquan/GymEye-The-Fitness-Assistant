package com.example.strong_body;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ScanOverlayView extends View {
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF scanRect = new RectF();
    private ValueAnimator animator;
    private float scanProgress = 0f;

    public ScanOverlayView(Context context) {
        super(context);
        init();
    }

    public ScanOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        maskPaint.setColor(Color.parseColor("#52000000"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1));
        borderPaint.setColor(Color.parseColor("#88FFFFFF"));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dp(4));
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setColor(Color.parseColor("#10C7B5"));
        scanPaint.setStrokeWidth(dp(3));
        scanPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startScanAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateScanRect();

        canvas.drawRect(0, 0, getWidth(), scanRect.top, maskPaint);
        canvas.drawRect(0, scanRect.bottom, getWidth(), getHeight(), maskPaint);
        canvas.drawRect(0, scanRect.top, scanRect.left, scanRect.bottom, maskPaint);
        canvas.drawRect(scanRect.right, scanRect.top, getWidth(), scanRect.bottom, maskPaint);

        canvas.drawRoundRect(scanRect, dp(22), dp(22), borderPaint);
        drawCorners(canvas);
        drawScanLine(canvas);
    }

    private void updateScanRect() {
        float width = getWidth();
        float height = getHeight();
        float frameWidth = Math.min(width * 0.78f, dp(340));
        float frameHeight = Math.min(height * 0.42f, frameWidth * 1.18f);
        float left = (width - frameWidth) / 2f;
        float top = height * 0.25f;
        scanRect.set(left, top, left + frameWidth, top + frameHeight);
    }

    private void drawCorners(Canvas canvas) {
        float len = dp(46);
        float inset = dp(2);
        float left = scanRect.left + inset;
        float top = scanRect.top + inset;
        float right = scanRect.right - inset;
        float bottom = scanRect.bottom - inset;

        canvas.drawLine(left, top, left + len, top, cornerPaint);
        canvas.drawLine(left, top, left, top + len, cornerPaint);
        canvas.drawLine(right, top, right - len, top, cornerPaint);
        canvas.drawLine(right, top, right, top + len, cornerPaint);
        canvas.drawLine(left, bottom, left + len, bottom, cornerPaint);
        canvas.drawLine(left, bottom, left, bottom - len, cornerPaint);
        canvas.drawLine(right, bottom, right - len, bottom, cornerPaint);
        canvas.drawLine(right, bottom, right, bottom - len, cornerPaint);
    }

    private void drawScanLine(Canvas canvas) {
        float y = scanRect.top + dp(22) + (scanRect.height() - dp(44)) * scanProgress;
        scanPaint.setShader(new LinearGradient(
                scanRect.left + dp(18),
                y,
                scanRect.right - dp(18),
                y,
                new int[]{
                        Color.TRANSPARENT,
                        Color.parseColor("#AA10C7B5"),
                        Color.parseColor("#10C7B5"),
                        Color.parseColor("#AA10C7B5"),
                        Color.TRANSPARENT
                },
                null,
                Shader.TileMode.CLAMP));
        canvas.drawLine(scanRect.left + dp(18), y, scanRect.right - dp(18), y, scanPaint);
        scanPaint.setShader(null);
    }

    private void startScanAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400L);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            scanProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
