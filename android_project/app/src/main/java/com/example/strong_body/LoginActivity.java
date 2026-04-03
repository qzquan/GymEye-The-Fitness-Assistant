package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnContinue;
    private MaterialButton btnLoginQQ;
    private MaterialButton btnLoginWechat;
    private TextView btnGuestLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化控件引用
        etEmail = findViewById(R.id.etEmail);
        btnContinue = findViewById(R.id.btnContinue);
        btnLoginQQ = findViewById(R.id.btnLoginQQ);
        btnLoginWechat = findViewById(R.id.btnLoginWechat);
        btnGuestLogin = findViewById(R.id.btnGuestLogin);

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

            // 邮箱校验通过后，进入应用首页
            goToHome("邮箱登录成功");
        });

        // 第三方登录 / 游客登录：当前先统一跳转首页，后续可替换为真实鉴权流程
        btnLoginQQ.setOnClickListener(v -> goToHome("QQ 登录成功"));
        btnLoginWechat.setOnClickListener(v -> goToHome("微信登录成功"));
        btnGuestLogin.setOnClickListener(v -> goToHome("游客登录成功"));
    }

    /**
     * 统一跳转到应用首页（MainActivity），并关闭登录页避免返回到登录页。
     */
    private void goToHome(String toastMessage) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
