package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 首页作为待开发页面，仅负责跳转到扫一扫页
        Button btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        });

        // 器械知识快捷入口
        findViewById(R.id.btnEquipmentKnowledge).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EquipmentListActivity.class);
            startActivity(intent);
        });

        // 退出登录按钮
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            AuthAccountStorage.logout(this);
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
