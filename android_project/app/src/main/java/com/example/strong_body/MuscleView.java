package com.example.strong_body;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 人体肌肉图自定义View
 * 展示锻炼时主要发力的肌肉群
 */
public class MuscleView extends View {

    private Paint bodyPaint;          // 身体轮廓画笔
    private Paint primaryMusclePaint;   // 主要肌肉群画笔
    private Paint secondaryMusclePaint; // 次要肌肉群画笔
    private Paint textPaint;           // 文字画笔
    private Paint outlinePaint;        // 轮廓线画笔

    // 肌肉区域路径
    private Map<String, Path> musclePaths;
    // 肌肉区域标签
    private Map<String, String> muscleLabels;

    // 当前高亮的肌肉群
    private Set<String> highlightedPrimaryMuscles = new HashSet<>();
    private Set<String> highlightedSecondaryMuscles = new HashSet<>();

    public MuscleView(Context context) {
        super(context);
        init();
    }

    public MuscleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MuscleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 身体轮廓画笔 - 浅灰色
        bodyPaint = new Paint();
        bodyPaint.setColor(0xFFE0E0E0);
        bodyPaint.setStyle(Paint.Style.FILL);
        bodyPaint.setAntiAlias(true);

        // 主要肌肉群画笔 - 红色
        primaryMusclePaint = new Paint();
        primaryMusclePaint.setColor(0xFFFF4444);
        primaryMusclePaint.setStyle(Paint.Style.FILL);
        primaryMusclePaint.setAlpha(180);
        primaryMusclePaint.setAntiAlias(true);

        // 次要肌肉群画笔 - 橙色
        secondaryMusclePaint = new Paint();
        secondaryMusclePaint.setColor(0xFFFF8800);
        secondaryMusclePaint.setStyle(Paint.Style.FILL);
        secondaryMusclePaint.setAlpha(150);
        secondaryMusclePaint.setAntiAlias(true);

        // 文字画笔
        textPaint = new Paint();
        textPaint.setColor(0xFF333333);
        textPaint.setTextSize(28);
        textPaint.setAntiAlias(true);

        // 轮廓线画笔
        outlinePaint = new Paint();
        outlinePaint.setColor(0xFF666666);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2);
        outlinePaint.setAntiAlias(true);

        musclePaths = new HashMap<>();
        muscleLabels = new HashMap<>();
    }

    /**
     * 设置主要肌肉群
     */
    public void setPrimaryMuscles(Set<String> muscles) {
        this.highlightedPrimaryMuscles = muscles;
        invalidate();
    }

    /**
     * 设置次要肌肉群
     */
    public void setSecondaryMuscles(Set<String> muscles) {
        this.highlightedSecondaryMuscles = muscles;
        invalidate();
    }

    /**
     * 同时设置主要和次要肌肉群
     */
    public void setMuscles(Set<String> primary, Set<String> secondary) {
        this.highlightedPrimaryMuscles = primary;
        this.highlightedSecondaryMuscles = secondary;
        invalidate();
    }

    /**
     * 清除所有高亮
     */
    public void clearHighlight() {
        highlightedPrimaryMuscles.clear();
        highlightedSecondaryMuscles.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2;

        // 绘制整个人体轮廓作为背景
        drawBodyOutline(canvas, centerX, height);

        // 绘制肌肉区域
        drawMuscles(canvas, centerX, height);

        // 绘制肌肉标签
        drawMuscleLabels(canvas, centerX, height);
    }

    /**
     * 绘制人体轮廓
     */
    private void drawBodyOutline(Canvas canvas, float centerX, float height) {
        Path bodyPath = new Path();

        float scale = height / 800f;

        // 头部
        bodyPath.addOval(centerX - 40 * scale, 20 * scale,
                         centerX + 40 * scale, 100 * scale, Path.Direction.CW);

        // 颈部
        bodyPath.moveTo(centerX - 15 * scale, 100 * scale);
        bodyPath.lineTo(centerX - 15 * scale, 130 * scale);
        bodyPath.lineTo(centerX + 15 * scale, 130 * scale);
        bodyPath.lineTo(centerX + 15 * scale, 100 * scale);

        // 躯干
        bodyPath.moveTo(centerX - 60 * scale, 130 * scale);
        bodyPath.lineTo(centerX - 70 * scale, 350 * scale);
        bodyPath.lineTo(centerX + 70 * scale, 350 * scale);
        bodyPath.lineTo(centerX + 60 * scale, 130 * scale);
        bodyPath.close();

        // 左臂
        bodyPath.moveTo(centerX - 60 * scale, 140 * scale);
        bodyPath.lineTo(centerX - 90 * scale, 200 * scale);
        bodyPath.lineTo(centerX - 85 * scale, 300 * scale);
        bodyPath.lineTo(centerX - 75 * scale, 300 * scale);
        bodyPath.lineTo(centerX - 80 * scale, 200 * scale);
        bodyPath.lineTo(centerX - 55 * scale, 145 * scale);

        // 右臂
        bodyPath.moveTo(centerX + 60 * scale, 140 * scale);
        bodyPath.lineTo(centerX + 90 * scale, 200 * scale);
        bodyPath.lineTo(centerX + 85 * scale, 300 * scale);
        bodyPath.lineTo(centerX + 75 * scale, 300 * scale);
        bodyPath.lineTo(centerX + 80 * scale, 200 * scale);
        bodyPath.lineTo(centerX + 55 * scale, 145 * scale);

        // 左腿
        bodyPath.moveTo(centerX - 55 * scale, 350 * scale);
        bodyPath.lineTo(centerX - 60 * scale, 600 * scale);
        bodyPath.lineTo(centerX - 45 * scale, 600 * scale);
        bodyPath.lineTo(centerX - 35 * scale, 350 * scale);

        // 右腿
        bodyPath.moveTo(centerX + 55 * scale, 350 * scale);
        bodyPath.lineTo(centerX + 60 * scale, 600 * scale);
        bodyPath.lineTo(centerX + 45 * scale, 600 * scale);
        bodyPath.lineTo(centerX + 35 * scale, 350 * scale);

        canvas.drawPath(bodyPath, bodyPaint);
        canvas.drawPath(bodyPath, outlinePaint);
    }

    /**
     * 绘制肌肉高亮区域
     */
    private void drawMuscles(Canvas canvas, float centerX, float height) {
        float scale = height / 800f;

        // 三角肌
        drawShoulder(canvas, centerX, scale);

        // 胸肌
        drawChest(canvas, centerX, scale);

        // 腹肌
        drawAbs(canvas, centerX, scale);

        // 背部
        drawBack(canvas, centerX, scale);

        // 肱二头肌
        drawBiceps(canvas, centerX, scale);

        // 肱三头肌
        drawTriceps(canvas, centerX, scale);

        // 股四头肌
        drawQuadriceps(canvas, centerX, scale);

        // 腘绳肌
        drawHamstrings(canvas, centerX, scale);

        // 臀大肌
        drawGlutes(canvas, centerX, scale);

        // 小腿
        drawCalves(canvas, centerX, scale);
    }

    private void drawShoulder(Canvas canvas, float centerX, float scale) {
        // 左三角肌
        if (highlightedPrimaryMuscles.contains("shoulders") ||
            highlightedSecondaryMuscles.contains("shoulders")) {
            Path leftShoulder = new Path();
            leftShoulder.addOval(centerX - 75 * scale, 125 * scale,
                                  centerX - 45 * scale, 175 * scale, Path.Direction.CW);
            Paint paint = highlightedPrimaryMuscles.contains("shoulders") ?
                          primaryMusclePaint : secondaryMusclePaint;
            canvas.drawPath(leftShoulder, paint);

            // 右三角肌
            Path rightShoulder = new Path();
            rightShoulder.addOval(centerX + 45 * scale, 125 * scale,
                                   centerX + 75 * scale, 175 * scale, Path.Direction.CW);
            canvas.drawPath(rightShoulder, paint);
        }
    }

    private void drawChest(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("chest") ||
            highlightedSecondaryMuscles.contains("chest")) {
            Path leftChest = new Path();
            leftChest.moveTo(centerX - 15 * scale, 140 * scale);
            leftChest.lineTo(centerX - 60 * scale, 150 * scale);
            leftChest.lineTo(centerX - 55 * scale, 220 * scale);
            leftChest.lineTo(centerX - 15 * scale, 210 * scale);
            leftChest.close();
            Paint paint = highlightedPrimaryMuscles.contains("chest") ?
                          primaryMusclePaint : secondaryMusclePaint;
            canvas.drawPath(leftChest, paint);

            // 右胸
            Path rightChest = new Path();
            rightChest.moveTo(centerX + 15 * scale, 140 * scale);
            rightChest.lineTo(centerX + 60 * scale, 150 * scale);
            rightChest.lineTo(centerX + 55 * scale, 220 * scale);
            rightChest.lineTo(centerX + 15 * scale, 210 * scale);
            rightChest.close();
            canvas.drawPath(rightChest, paint);
        }
    }

    private void drawAbs(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("abs") ||
            highlightedSecondaryMuscles.contains("abs")) {
            Path abs = new Path();
            abs.moveTo(centerX - 35 * scale, 220 * scale);
            abs.lineTo(centerX + 35 * scale, 220 * scale);
            abs.lineTo(centerX + 30 * scale, 330 * scale);
            abs.lineTo(centerX - 30 * scale, 330 * scale);
            abs.close();
            Paint paint = highlightedPrimaryMuscles.contains("abs") ?
                          primaryMusclePaint : secondaryMusclePaint;
            canvas.drawPath(abs, paint);
        }
    }

    private void drawBack(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("back") ||
            highlightedSecondaryMuscles.contains("back")) {
            Path back = new Path();
            back.moveTo(centerX - 55 * scale, 140 * scale);
            back.lineTo(centerX + 55 * scale, 140 * scale);
            back.lineTo(centerX + 60 * scale, 340 * scale);
            back.lineTo(centerX - 60 * scale, 340 * scale);
            back.close();
            Paint paint = highlightedPrimaryMuscles.contains("back") ?
                          primaryMusclePaint : secondaryMusclePaint;
            canvas.drawPath(back, paint);
        }
    }

    private void drawBiceps(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("biceps") ||
            highlightedSecondaryMuscles.contains("biceps")) {
            // 左肱二头肌
            Path leftBiceps = new Path();
            leftBiceps.addOval(centerX - 95 * scale, 170 * scale,
                               centerX - 75 * scale, 260 * scale, Path.Direction.CW);
            Paint paint = highlightedPrimaryMuscles.contains("biceps") ?
                          primaryMusclePaint : secondaryMusclePaint;
            canvas.drawPath(leftBiceps, paint);

            // 右肱二头肌
            Path rightBiceps = new Path();
            rightBiceps.addOval(centerX + 75 * scale, 170 * scale,
                                centerX + 95 * scale, 260 * scale, Path.Direction.CW);
            canvas.drawPath(rightBiceps, paint);
        }
    }

    private void drawTriceps(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("triceps") ||
            highlightedSecondaryMuscles.contains("triceps")) {
            // 左肱三头肌
            Path leftTriceps = new Path();
            leftTriceps.addOval(centerX - 100 * scale, 200 * scale,
                                centerX - 80 * scale, 290 * scale, Path.Direction.CW);
            Paint paint = highlightedPrimaryMuscles.contains("triceps") ?
                          primaryMusclePaint : secondaryMusclePaint;
            canvas.drawPath(leftTriceps, paint);

            // 右肱三头肌
            Path rightTriceps = new Path();
            rightTriceps.addOval(centerX + 80 * scale, 200 * scale,
                                 centerX + 100 * scale, 290 * scale, Path.Direction.CW);
            canvas.drawPath(rightTriceps, paint);
        }
    }

    private void drawQuadriceps(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("quadriceps") ||
            highlightedSecondaryMuscles.contains("quadriceps")) {
            Paint paint = highlightedPrimaryMuscles.contains("quadriceps") ?
                          primaryMusclePaint : secondaryMusclePaint;

            // 左股四头肌
            Path leftQuad = new Path();
            leftQuad.moveTo(centerX - 50 * scale, 350 * scale);
            leftQuad.lineTo(centerX - 55 * scale, 560 * scale);
            leftQuad.lineTo(centerX - 35 * scale, 560 * scale);
            leftQuad.lineTo(centerX - 30 * scale, 350 * scale);
            leftQuad.close();
            canvas.drawPath(leftQuad, paint);

            // 右股四头肌
            Path rightQuad = new Path();
            rightQuad.moveTo(centerX + 50 * scale, 350 * scale);
            rightQuad.lineTo(centerX + 55 * scale, 560 * scale);
            rightQuad.lineTo(centerX + 35 * scale, 560 * scale);
            rightQuad.lineTo(centerX + 30 * scale, 350 * scale);
            rightQuad.close();
            canvas.drawPath(rightQuad, paint);
        }
    }

    private void drawHamstrings(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("hamstrings") ||
            highlightedSecondaryMuscles.contains("hamstrings")) {
            Paint paint = highlightedPrimaryMuscles.contains("hamstrings") ?
                          primaryMusclePaint : secondaryMusclePaint;

            // 左腘绳肌
            Path leftHam = new Path();
            leftHam.moveTo(centerX - 50 * scale, 360 * scale);
            leftHam.lineTo(centerX - 45 * scale, 560 * scale);
            leftHam.lineTo(centerX - 30 * scale, 560 * scale);
            leftHam.lineTo(centerX - 28 * scale, 360 * scale);
            leftHam.close();
            canvas.drawPath(leftHam, paint);

            // 右腘绳肌
            Path rightHam = new Path();
            rightHam.moveTo(centerX + 50 * scale, 360 * scale);
            rightHam.lineTo(centerX + 45 * scale, 560 * scale);
            rightHam.lineTo(centerX + 30 * scale, 560 * scale);
            rightHam.lineTo(centerX + 28 * scale, 360 * scale);
            rightHam.close();
            canvas.drawPath(rightHam, paint);
        }
    }

    private void drawGlutes(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("glutes") ||
            highlightedSecondaryMuscles.contains("glutes")) {
            Paint paint = highlightedPrimaryMuscles.contains("glutes") ?
                          primaryMusclePaint : secondaryMusclePaint;

            // 左臀大肌
            Path leftGlute = new Path();
            leftGlute.addOval(centerX - 55 * scale, 340 * scale,
                              centerX - 25 * scale, 400 * scale, Path.Direction.CW);
            canvas.drawPath(leftGlute, paint);

            // 右臀大肌
            Path rightGlute = new Path();
            rightGlute.addOval(centerX + 25 * scale, 340 * scale,
                               centerX + 55 * scale, 400 * scale, Path.Direction.CW);
            canvas.drawPath(rightGlute, paint);
        }
    }

    private void drawCalves(Canvas canvas, float centerX, float scale) {
        if (highlightedPrimaryMuscles.contains("calves") ||
            highlightedSecondaryMuscles.contains("calves")) {
            Paint paint = highlightedPrimaryMuscles.contains("calves") ?
                          primaryMusclePaint : secondaryMusclePaint;

            // 左小腿
            Path leftCalf = new Path();
            leftCalf.moveTo(centerX - 45 * scale, 560 * scale);
            leftCalf.lineTo(centerX - 50 * scale, 700 * scale);
            leftCalf.lineTo(centerX - 30 * scale, 700 * scale);
            leftCalf.lineTo(centerX - 25 * scale, 560 * scale);
            leftCalf.close();
            canvas.drawPath(leftCalf, paint);

            // 右小腿
            Path rightCalf = new Path();
            rightCalf.moveTo(centerX + 45 * scale, 560 * scale);
            rightCalf.lineTo(centerX + 50 * scale, 700 * scale);
            rightCalf.lineTo(centerX + 30 * scale, 700 * scale);
            rightCalf.lineTo(centerX + 25 * scale, 560 * scale);
            rightCalf.close();
            canvas.drawPath(rightCalf, paint);
        }
    }

    /**
     * 绘制肌肉标签说明
     */
    private void drawMuscleLabels(Canvas canvas, float centerX, float height) {
        float labelY = height - 80;
        float labelX = 20;
        float lineHeight = 35;

        // 图例说明
        Paint legendPaint = new Paint();
        legendPaint.setTextSize(24);
        legendPaint.setAntiAlias(true);

        // 主要肌肉
        Paint primaryLegend = new Paint(legendPaint);
        primaryLegend.setColor(0xFFFF4444);

        // 次要肌肉
        Paint secondaryLegend = new Paint(legendPaint);
        secondaryLegend.setColor(0xFFFF8800);

        // 绘制主要肌肉图例
        canvas.drawRect(labelX, labelY - 15, labelX + 20, labelY + 5, primaryLegend);
        canvas.drawText("主要发力肌肉", labelX + 30, labelY, primaryLegend);

        // 绘制次要肌肉图例
        canvas.drawRect(labelX + 180, labelY - 15, labelX + 200, labelY + 5, secondaryLegend);
        canvas.drawText("辅助肌肉", labelX + 210, labelY, secondaryLegend);
    }
}
