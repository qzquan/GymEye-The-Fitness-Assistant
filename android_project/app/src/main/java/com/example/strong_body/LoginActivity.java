package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirm;
    private MaterialButton btnSignUp;
    private MaterialButton btnLogIn;
    private MaterialButton btnRegisterSubmit;
    private TextView tvGuest;
    private TextView tvCancelRegister;

    private LinearLayout layoutRegisterPanel;
    private LinearLayout layoutSavedAccountsSection;
    private TextView tvSavedAccountsHeader;
    private ScrollView scrollAuthWelcome;
    private ScrollView svSavedAccounts;
    private LinearLayout llSavedAccounts;

    private boolean savedAccountsExpanded = true;

    private static final String API_REGISTER = ApiConfig.BASE_URL + "/api/user/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogIn = findViewById(R.id.btnLogIn);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvGuest = findViewById(R.id.tvGuest);
        tvCancelRegister = findViewById(R.id.tvCancelRegister);

        layoutRegisterPanel = findViewById(R.id.layoutRegisterPanel);
        layoutSavedAccountsSection = findViewById(R.id.layoutSavedAccountsSection);
        tvSavedAccountsHeader = findViewById(R.id.tvSavedAccountsHeader);
        scrollAuthWelcome = findViewById(R.id.scrollAuthWelcome);
        svSavedAccounts = findViewById(R.id.svSavedAccounts);
        llSavedAccounts = findViewById(R.id.llSavedAccounts);

        btnLogIn.setOnClickListener(v -> startActivity(new Intent(this, SignInActivity.class)));
        btnSignUp.setOnClickListener(v -> showRegisterPanel());
        btnRegisterSubmit.setOnClickListener(v -> attemptSignUp());
        tvCancelRegister.setOnClickListener(v -> hideRegisterPanel());
        tvSavedAccountsHeader.setOnClickListener(v -> toggleSavedAccountsExpanded());
        tvGuest.setOnClickListener(v -> goToHome("Continue as guest"));

        maybeAutoLogin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthAccountStorage.migrateLegacyIfNeeded(this);
        refreshSavedAccountsPanel();
    }

    private void maybeAutoLogin() {
        AuthAccountStorage.migrateLegacyIfNeeded(this);
        SavedAccount account = AuthAccountStorage.getAutoLoginAccount(this);
        if (account == null) {
            return;
        }
        AuthAccountStorage.touch(this, account.email);
        goToHome("Welcome back");
    }

    private void showRegisterPanel() {
        layoutRegisterPanel.setVisibility(View.VISIBLE);
        layoutRegisterPanel.post(() -> scrollAuthWelcome.smoothScrollTo(0, layoutRegisterPanel.getBottom()));
    }

    private void hideRegisterPanel() {
        layoutRegisterPanel.setVisibility(View.GONE);
    }

    private void toggleSavedAccountsExpanded() {
        savedAccountsExpanded = !savedAccountsExpanded;
        svSavedAccounts.setVisibility(savedAccountsExpanded ? View.VISIBLE : View.GONE);
        updateSavedAccountsHeaderLabel();
    }

    private void updateSavedAccountsHeaderLabel() {
        String arrow = savedAccountsExpanded ? "▲" : "▼";
        tvSavedAccountsHeader.setText("最近登录账号 " + arrow);
    }

    private void refreshSavedAccountsPanel() {
        List<SavedAccount> accounts = AuthAccountStorage.loadSortedNewestFirst(this);
        if (accounts.isEmpty()) {
            layoutSavedAccountsSection.setVisibility(View.GONE);
            return;
        }

        layoutSavedAccountsSection.setVisibility(View.VISIBLE);
        svSavedAccounts.setVisibility(savedAccountsExpanded ? View.VISIBLE : View.GONE);
        updateSavedAccountsHeaderLabel();

        llSavedAccounts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SavedAccount a : accounts) {
            View row = inflater.inflate(R.layout.item_saved_account, llSavedAccounts, false);
            TextView tvLetter = row.findViewById(R.id.tvAvatarLetter);
            TextView tvNick = row.findViewById(R.id.tvSavedNickname);
            TextView tvMail = row.findViewById(R.id.tvSavedEmail);
            TextView tvHint = row.findViewById(R.id.tvQuickLoginHint);

            tvLetter.setText(initialLetter(a.nickname));
            tvNick.setText(a.nickname);
            tvMail.setText(a.email);
            tvHint.setVisibility(View.VISIBLE);
            tvHint.setText(a.canQuickLogin() ? "已开启自动登录" : "点击使用此邮箱登录");
            row.findViewById(R.id.layoutSavedAccountRow).setBackgroundResource(R.drawable.bg_gymeye_card);

            String email = a.email;
            row.setOnClickListener(v -> openSignIn(email));

            llSavedAccounts.addView(row);
        }
    }

    private static String initialLetter(String nickname) {
        if (TextUtils.isEmpty(nickname)) return "?";
        char c = Character.toUpperCase(nickname.charAt(0));
        return Character.isLetterOrDigit(c) ? String.valueOf(c) : "?";
    }

    private void openSignIn(String email) {
        Intent i = new Intent(this, SignInActivity.class);
        if (!TextUtils.isEmpty(email)) {
            i.putExtra(SignInActivity.EXTRA_INITIAL_EMAIL, email);
        }
        startActivity(i);
    }

    private void attemptSignUp() {
        String nickname = etUsername.getText() == null ? "" : etUsername.getText().toString().trim();
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString();

        if (TextUtils.isEmpty(nickname)) {
            etUsername.setError("请输入显示名称");
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

                Log.d("LoginActivity", "POST register email=" + email);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                Log.d("LoginActivity", "Response Code: " + code);

                String body = readResponseBody(conn, code);
                if (code == 201 || code == 200) {
                    JSONObject res = new JSONObject(body);
                    boolean ok = res.optBoolean("ok", false);
                    if (ok) {
                        String token = res.optString("token", "");
                        JSONObject user = res.optJSONObject("user");
                        String nick = user != null ? user.optString("nickname", nickname) : nickname;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            AuthAccountStorage.upsert(LoginActivity.this, email, nick, token, false);
                            goToHome("注册成功");
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
                Log.e("LoginActivity", "Register error url=" + API_REGISTER, e);
                String detail = e.getMessage() != null ? e.getMessage() : "";
                showToast("连接失败\n" + ApiConfig.BASE_URL
                        + (detail.isEmpty() ? "" : "\n" + detail));
            }
        }).start();
    }

    private static String readResponseBody(HttpURLConnection conn, int code) throws Exception {
        java.io.InputStreamReader reader;
        if (code >= 400) {
            var es = conn.getErrorStream();
            if (es == null) return "";
            reader = new InputStreamReader(es, StandardCharsets.UTF_8);
        } else {
            reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
        }
        return sb.toString();
    }

    private void showToast(String msg) {
        int len = msg.indexOf('\n') >= 0 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, msg, len).show());
    }

    private void goToHome(String toastMessage) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
