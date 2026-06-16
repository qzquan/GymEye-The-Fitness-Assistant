package com.example.strong_body;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 扫描器械入口
        View btnScan = findViewById(R.id.btnScan);
        startScanWaveAnimation();
        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        });

        // 2. 器械知识快捷入口
        findViewById(R.id.btnEquipmentKnowledge).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EquipmentListActivity.class);
            startActivity(intent);
        });

        // 3. 训练记录（打卡统计）入口
        View btnTrainingRecord = findViewById(R.id.btnTrainingRecord);
        btnTrainingRecord.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        // 4. 退出登录按钮 (保留了你原本的 AuthAccountStorage 逻辑)
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // 清除登录状态
            AuthAccountStorage.logout(this);
            // 跳转回登录页并清空 Activity 栈
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void startScanWaveAnimation() {
        animateWave(findViewById(R.id.scanWaveDark), "translationX", 0f, 12f, 4200L);
        animateWave(findViewById(R.id.scanWaveDark), "alpha", 0.72f, 1f, 4200L);
        animateWave(findViewById(R.id.scanWaveMid), "translationY", 0f, -10f, 5200L);
        animateWave(findViewById(R.id.scanWaveMid), "scaleX", 1f, 1.08f, 5200L);
        animateWave(findViewById(R.id.scanWaveLight), "translationX", 0f, -14f, 6200L);
        animateWave(findViewById(R.id.scanWaveLight), "scaleY", 1f, 1.1f, 6200L);
    }

    private void animateWave(View view, String property, float start, float end, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, property, start, end);
        animator.setDuration(duration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }
}
