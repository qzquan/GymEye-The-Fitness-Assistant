package com.example.strong_body;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnContinue;
    private MaterialButton btnLoginQQ;
    private MaterialButton btnLoginWechat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化控件引用
        etEmail = findViewById(R.id.etEmail);
        btnContinue = findViewById(R.id.btnContinue);
        btnLoginQQ = findViewById(R.id.btnLoginQQ);
        btnLoginWechat = findViewById(R.id.btnLoginWechat);

        // Continue 按钮：基础输入校验（空值 + 邮箱格式）
        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("请输入邮箱地址");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("邮箱格式不正确");
                return;
            }

            // 预留后续注册/登录流程入口
            Toast.makeText(this, "邮箱校验通过：" + email, Toast.LENGTH_SHORT).show();
        });

        // 第三方登录占位逻辑，后续可替换为 SDK 接入代码
        btnLoginQQ.setOnClickListener(v ->
                Toast.makeText(this, "QQ 登录待接入", Toast.LENGTH_SHORT).show()
        );
        btnLoginWechat.setOnClickListener(v ->
                Toast.makeText(this, "WeChat 登录待接入", Toast.LENGTH_SHORT).show()
        );
    }
}
