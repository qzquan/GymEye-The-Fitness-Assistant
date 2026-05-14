package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
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

public class RegisterActivity extends AppCompatActivity {

    private static final String API_REGISTER = ApiConfig.BASE_URL + "/api/user/register";

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        MaterialButton btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);
        setupPasswordToggle(etPassword);
        setupPasswordToggle(etConfirm);

        btnSignUp.setOnClickListener(v -> attemptSignUp());
        tvBackToLogin.setOnClickListener(v -> {
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }

    private void setupPasswordToggle(EditText editText) {
        setPasswordVisibility(editText, false);
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP
                    && event.getX() >= editText.getWidth() - editText.getTotalPaddingRight()) {
                boolean visible = Boolean.TRUE.equals(editText.getTag());
                setPasswordVisibility(editText, !visible);
                editText.performClick();
                return true;
            }
            return false;
        });
    }

    private void setPasswordVisibility(EditText editText, boolean visible) {
        int cursorPos = Math.max(0, editText.getSelectionStart());
        int inputType = InputType.TYPE_CLASS_TEXT
                | (visible ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editText.setInputType(inputType);
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                visible ? android.R.drawable.ic_menu_view : android.R.drawable.presence_invisible,
                0
        );
        editText.setTag(visible);
        editText.setSelection(Math.min(cursorPos, editText.length()));
    }

    private void attemptSignUp() {
        String nickname = etUsername.getText() == null ? "" : etUsername.getText().toString().trim();
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString();

        if (TextUtils.isEmpty(nickname)) {
            etUsername.setError("请输入显示名称（将用于主页头像旁）");
            return;
        }
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
        if (!password.equals(confirm)) {
            etConfirm.setError("两次密码不一致");
            return;
        }

        registerTask(email, password, nickname);
    }

    private void registerTask(String email, String password, String nickname) {
        new Thread(() -> {
            try {
                URL url = new URL(API_REGISTER);
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
                jsonInput.put("nickname", nickname);
                String jsonStr = jsonInput.toString();

                byte[] input = jsonStr.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(input.length));

                Log.d("RegisterActivity", "POST register: " + jsonStr);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                String body = readResponseBody(conn, code);
                if (code == 201 || code == 200) {
                    JSONObject res = new JSONObject(body);
                    boolean ok = res.optBoolean("ok", false);
                    if (ok) {
                        String token = res.optString("token", "");
                        JSONObject user = res.optJSONObject("user");
                        String nick = user != null ? user.optString("nickname", nickname) : nickname;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            AuthAccountStorage.upsert(RegisterActivity.this, email, nick, token, false);
                            Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(i);
                            finish();
                        });
                    } else {
                        showToast("注册失败");
                    }
                } else if (code == 409) {
                    showToast("该邮箱已注册，请直接登录");
                } else {
                    showToast("注册失败: " + code);
                }
            } catch (Exception e) {
                Log.e("RegisterActivity", "Register error url=" + API_REGISTER, e);
                String detail = e.getMessage() != null ? e.getMessage() : "";
                showToast("连接失败\n" + ApiConfig.BASE_URL
                        + (detail.isEmpty() ? "" : "\n" + detail));
            }
        }).start();
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
