package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

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
    }
}
