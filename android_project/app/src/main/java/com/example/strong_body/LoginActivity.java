package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnContinue;
    private MaterialButton btnLoginQQ;
    private MaterialButton btnLoginWechat;
    private TextView btnGuestLogin;

    // AVD 访问电脑宿主机的特殊 IP
    private static final String API_URL = "http://10.0.2.2:8080/api/user/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化控件引用
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnContinue = findViewById(R.id.btnContinue);
        btnLoginQQ = findViewById(R.id.btnLoginQQ);
        btnLoginWechat = findViewById(R.id.btnLoginWechat);
        btnGuestLogin = findViewById(R.id.btnGuestLogin);

        // Continue 按钮：点击后调用后端登录接口
        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("请输入邮箱地址");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("邮箱格式不正确");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("请输入密码");
                return;
            }

            // 发起网络请求
            loginTask(email, password);
        });

        // 其他按钮保持跳转（模拟）
        btnLoginQQ.setOnClickListener(v -> goToHome("QQ 登录成功"));
        btnLoginWechat.setOnClickListener(v -> goToHome("微信登录成功"));
        btnGuestLogin.setOnClickListener(v -> goToHome("游客登录成功"));
    }

    /**
     * 发起后端登录请求（在子线程运行）
     */
    private void loginTask(String email, String password) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 构造请求体 JSON
                JSONObject jsonInput = new JSONObject();
                jsonInput.put("email", email);
                jsonInput.put("password", password);
                String jsonStr = jsonInput.toString();

                byte[] input = jsonStr.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(input.length));

                Log.d("LoginActivity", "Sending Body: " + jsonStr);

                // 发送数据
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                Log.d("LoginActivity", "Response Code: " + code);
                if (code == 200) {
                    // 读取响应
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                    
                    JSONObject res = new JSONObject(response.toString());
                    boolean ok = res.optBoolean("ok", false);
                    String mode = res.optString("mode", "");

                    if (ok) {
                        new Handler(Looper.getMainLooper()).post(() -> goToHome(mode.equals("registered") ? "注册并登录成功" : "登录成功"));
                    } else {
                        showToast("登录失败");
                    }
                } else {
                    showToast("网络请求失败: " + code);
                }
            } catch (Exception e) {
                Log.e("LoginActivity", "Login error", e);
                showToast("连接服务器失败，请检查后端是否启动");
            }
        }).start();
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    /**
     * 统一跳转到应用首页（MainActivity）
     */
    private void goToHome(String toastMessage) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
