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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

<<<<<<< HEAD
/** 首屏登录页：邮箱+密码登录，可选择已保存账号；分割线下方可跳转注册页。 */
=======
/**
 * 注册页：Sign up → /api/user/register。Log In：可选中已保存且开启自动登录的账号后一键进入；否则跳转登录页。
 */
>>>>>>> origin/main
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
<<<<<<< HEAD
=======
    private EditText etConfirm;
>>>>>>> origin/main
    private MaterialButton btnSignUp;
    private MaterialButton btnLogIn;
    private TextView tvGuest;

    private LinearLayout layoutSavedAccountsSection;
    private TextView tvSavedAccountsHeader;
    private ScrollView svSavedAccounts;
    private LinearLayout llSavedAccounts;

    private boolean savedAccountsExpanded;
<<<<<<< HEAD
    private String selectedSavedEmail;

    private static final String API_LOGIN = ApiConfig.BASE_URL + "/api/user/login";
=======
    /** 当前选中的已保存邮箱；用于 Log In 行为 */
    private String selectedSavedEmail;

    private static final String API_REGISTER = ApiConfig.BASE_URL + "/api/user/register";
>>>>>>> origin/main

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

<<<<<<< HEAD
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
=======
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
>>>>>>> origin/main
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogIn = findViewById(R.id.btnLogIn);
        tvGuest = findViewById(R.id.tvGuest);

        layoutSavedAccountsSection = findViewById(R.id.layoutSavedAccountsSection);
        tvSavedAccountsHeader = findViewById(R.id.tvSavedAccountsHeader);
        svSavedAccounts = findViewById(R.id.svSavedAccounts);
        llSavedAccounts = findViewById(R.id.llSavedAccounts);

<<<<<<< HEAD
        btnLogIn.setOnClickListener(v -> attemptLogin());
        btnSignUp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        tvSavedAccountsHeader.setOnClickListener(v -> toggleSavedAccountsExpanded());

=======
        btnSignUp.setOnClickListener(v -> attemptSignUp());

        btnLogIn.setOnClickListener(v -> onLogInClicked());

        tvSavedAccountsHeader.setOnClickListener(v -> toggleSavedAccountsExpanded());

>>>>>>> origin/main
        tvGuest.setOnClickListener(v -> goToHome("Continue as guest"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthAccountStorage.migrateLegacyIfNeeded(this);
        refreshSavedAccountsPanel();
    }

    private void toggleSavedAccountsExpanded() {
        savedAccountsExpanded = !savedAccountsExpanded;
        svSavedAccounts.setVisibility(savedAccountsExpanded ? View.VISIBLE : View.GONE);
        updateSavedAccountsHeaderLabel();
    }

    private void updateSavedAccountsHeaderLabel() {
        String arrow = savedAccountsExpanded ? "▲" : "▼";
        tvSavedAccountsHeader.setText("已保存账号 " + arrow);
    }

    private void refreshSavedAccountsPanel() {
        List<SavedAccount> accounts = AuthAccountStorage.loadSortedNewestFirst(this);
        if (accounts.isEmpty()) {
            layoutSavedAccountsSection.setVisibility(View.GONE);
            selectedSavedEmail = null;
            return;
        }

        layoutSavedAccountsSection.setVisibility(View.VISIBLE);
        updateSavedAccountsHeaderLabel();

        if (selectedSavedEmail == null
                || AuthAccountStorage.findByEmail(this, selectedSavedEmail) == null) {
            selectedSavedEmail = accounts.get(0).email;
        }
<<<<<<< HEAD
        if (!TextUtils.isEmpty(selectedSavedEmail)) {
            etEmail.setText(selectedSavedEmail);
        }
=======
>>>>>>> origin/main

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
            if (a.canQuickLogin()) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText("已开启自动登录 · 选中后点 Log In 可免输密码");
            } else {
                tvHint.setVisibility(View.GONE);
            }

            boolean selected = a.email.equalsIgnoreCase(selectedSavedEmail);
            row.findViewById(R.id.layoutSavedAccountRow).setBackgroundResource(
                    selected ? R.drawable.bg_saved_account_row_selected : R.drawable.bg_input_outline);

            String email = a.email;
            row.setOnClickListener(v -> {
                selectedSavedEmail = email;
                refreshSavedAccountsPanel();
            });

            llSavedAccounts.addView(row);
        }
    }

    private static String initialLetter(String nickname) {
        if (TextUtils.isEmpty(nickname)) return "?";
        char c = Character.toUpperCase(nickname.charAt(0));
        return Character.isLetterOrDigit(c) ? String.valueOf(c) : "?";
    }

<<<<<<< HEAD
    private void attemptLogin() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

=======
    private void onLogInClicked() {
        if (!TextUtils.isEmpty(selectedSavedEmail)) {
            SavedAccount acc = AuthAccountStorage.findByEmail(this, selectedSavedEmail);
            if (acc != null && acc.canQuickLogin()) {
                AuthAccountStorage.touch(this, acc.email);
                goToHome("欢迎回来");
                return;
            }
            Intent i = new Intent(this, SignInActivity.class);
            i.putExtra(SignInActivity.EXTRA_INITIAL_EMAIL, selectedSavedEmail);
            startActivity(i);
            return;
        }
        startActivity(new Intent(this, SignInActivity.class));
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
>>>>>>> origin/main
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
<<<<<<< HEAD

        loginTask(email, password);
    }

    private void loginTask(String email, String password) {
        new Thread(() -> {
            try {
                URL url = new URL(API_LOGIN);
=======
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
>>>>>>> origin/main
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
<<<<<<< HEAD
=======
                jsonInput.put("nickname", nickname);
>>>>>>> origin/main
                String jsonStr = jsonInput.toString();

                byte[] input = jsonStr.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(input.length));

<<<<<<< HEAD
                Log.d("LoginActivity", "POST " + API_LOGIN + " body=" + jsonStr);
=======
                Log.d("LoginActivity", "POST register: " + jsonStr);
>>>>>>> origin/main

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                Log.d("LoginActivity", "Response Code: " + code);

                String body = readResponseBody(conn, code);
<<<<<<< HEAD
                if (code == 200) {
                    JSONObject res = new JSONObject(body);
                    boolean ok = res.optBoolean("ok", false);
                    if (ok) {
                        new Handler(Looper.getMainLooper()).post(() -> onLoginSuccess(email, res));
=======
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
>>>>>>> origin/main
                    } else {
                        showToast("注册失败");
                    }
<<<<<<< HEAD
                } else if (code == 401) {
                    showToast("邮箱或密码错误");
                } else {
                    showToast("登录失败: " + code);
                }
            } catch (Exception e) {
                Log.e("LoginActivity", "Login error url=" + API_LOGIN, e);
=======
                } else if (code == 409) {
                    showToast("该邮箱已注册，请直接登录");
                } else {
                    showToast("注册失败: " + code);
                }
            } catch (Exception e) {
                Log.e("LoginActivity", "Register error url=" + API_REGISTER, e);
>>>>>>> origin/main
                String detail = e.getMessage() != null ? e.getMessage() : "";
                showToast("连接失败\n" + ApiConfig.BASE_URL
                        + (detail.isEmpty() ? "" : "\n" + detail));
            }
        }).start();
    }

<<<<<<< HEAD
    private void onLoginSuccess(String email, JSONObject res) {
        String token = res.optString("token", "");
        JSONObject user = res.optJSONObject("user");
        String nickname = user != null ? user.optString("nickname", "") : "";
        if (TextUtils.isEmpty(nickname)) {
            nickname = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        }
        SavedAccount selected = AuthAccountStorage.findByEmail(this, selectedSavedEmail);
        boolean autoLogin = selected != null && selected.autoLogin;
        AuthAccountStorage.upsert(this, email, nickname, token, autoLogin);
        goToHome("登录成功");
    }

    private static String readResponseBody(HttpURLConnection conn, int code) throws Exception {
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
=======
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
>>>>>>> origin/main
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
