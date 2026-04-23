package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 已有账户登录：email + password。成功时保存 JWT 与昵称；勾选「自动登录」后可在注册页一键进入。
 */
public class SignInActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_EMAIL = "com.example.strong_body.extra.INITIAL_EMAIL";

    private static final String API_LOGIN = ApiConfig.BASE_URL + "/api/user/login";

    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbAutoLogin;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        AuthAccountStorage.migrateLegacyIfNeeded(this);

        etEmail = findViewById(R.id.etSignInEmail);
        etPassword = findViewById(R.id.etSignInPassword);
        cbAutoLogin = findViewById(R.id.cbAutoLogin);
        btnSubmit = findViewById(R.id.btnSignInSubmit);
        TextView tvBack = findViewById(R.id.tvBackToRegister);

        String extraEmail = getIntent().getStringExtra(EXTRA_INITIAL_EMAIL);
        if (!TextUtils.isEmpty(extraEmail)) {
            etEmail.setText(extraEmail.trim());
            SavedAccount sel = AuthAccountStorage.findByEmail(this, extraEmail.trim());
            if (sel != null) {
                cbAutoLogin.setChecked(sel.autoLogin);
            }
        } else {
            SavedAccount recent = AuthAccountStorage.getMostRecentlyUsed(this);
            if (recent != null) {
                etEmail.setText(recent.email);
                cbAutoLogin.setChecked(recent.autoLogin);
            }
        }

        btnSubmit.setOnClickListener(v -> attemptLogin());

        tvBack.setOnClickListener(v -> {
            Intent i = new Intent(SignInActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
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
        if (password.length() < 6) {
            etPassword.setError("密码至少 6 位");
            return;
        }

        loginTask(email, password);
    }

    private void loginTask(String email, String password) {
        new Thread(() -> {
            try {
                URL url = new URL(API_LOGIN);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("email", email);
                jsonInput.put("password", password);
                String jsonStr = jsonInput.toString();

                byte[] input = jsonStr.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(input.length));

                Log.d("SignInActivity", "POST " + API_LOGIN + " body=" + jsonStr);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                Log.d("SignInActivity", "Response Code: " + code);

                String body = readResponseBody(conn, code);
                if (code == 200) {
                    JSONObject res = new JSONObject(body);
                    boolean ok = res.optBoolean("ok", false);
                    if (ok) {
                        new Handler(Looper.getMainLooper()).post(() -> onLoginSuccess(email, res));
                    } else {
                        showToast("登录失败");
                    }
                } else if (code == 401) {
                    showToast("邮箱或密码错误");
                } else {
                    showToast("登录失败: " + code);
                }
            } catch (Exception e) {
                Log.e("SignInActivity", "Login error url=" + API_LOGIN, e);
                String detail = e.getMessage() != null ? e.getMessage() : "";
                showToast("连接失败\n" + ApiConfig.BASE_URL
                        + (detail.isEmpty() ? "" : "\n" + detail));
            }
        }).start();
    }

    private void onLoginSuccess(String email, JSONObject res) {
        String token = res.optString("token", "");
        JSONObject user = res.optJSONObject("user");
        String nickname = user != null ? user.optString("nickname", "") : "";
        if (TextUtils.isEmpty(nickname)) {
            nickname = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        }
        AuthAccountStorage.upsert(this, email, nickname, token, cbAutoLogin.isChecked());

        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showToast(String msg) {
        int len = msg.indexOf('\n') >= 0 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, msg, len).show());
    }

    private static String readResponseBody(HttpURLConnection conn, int code) throws Exception {
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
        }
        return sb.toString();
    }
}
