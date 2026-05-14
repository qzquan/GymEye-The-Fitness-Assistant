package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 扫描器械入口
        Button btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        });

        // 2. 器械知识快捷入口
        findViewById(R.id.btnEquipmentKnowledge).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EquipmentListActivity.class);
            startActivity(intent);
        });

        // 🚀 3. 训练记录（打卡统计）入口 - 对应你刚才 XML 里的 id
        TextView btnTrainingRecord = findViewById(R.id.btnTrainingRecord);
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
}