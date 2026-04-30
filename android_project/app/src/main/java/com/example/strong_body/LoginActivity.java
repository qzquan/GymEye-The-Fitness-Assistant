package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
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

/** 首屏登录页：邮箱+密码登录，可选择已保存账号；分割线下方可跳转注册页。 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbAutoLogin;
    private MaterialButton btnSignUp;
    private MaterialButton btnLogIn;
    private TextView tvGuest;

    private LinearLayout layoutSavedAccountsSection;
    private TextView tvSavedAccountsHeader;
    private ScrollView svSavedAccounts;
    private LinearLayout llSavedAccounts;

    private boolean savedAccountsExpanded;
    private String selectedSavedEmail;

    private static final String API_LOGIN = ApiConfig.BASE_URL + "/api/user/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbAutoLogin = findViewById(R.id.cbAutoLoginMain);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogIn = findViewById(R.id.btnLogIn);
        tvGuest = findViewById(R.id.tvGuest);

        layoutSavedAccountsSection = findViewById(R.id.layoutSavedAccountsSection);
        tvSavedAccountsHeader = findViewById(R.id.tvSavedAccountsHeader);
        svSavedAccounts = findViewById(R.id.svSavedAccounts);
        llSavedAccounts = findViewById(R.id.llSavedAccounts);

        cbAutoLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String currentEmail = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            SavedAccount current = AuthAccountStorage.findByEmail(this, currentEmail);
            // 取消勾选时立即持久化，确保“下次必须输入密码”立即生效。
            if (!isChecked && current != null && current.autoLogin) {
                AuthAccountStorage.upsert(this, current.email, current.nickname, current.token, false);
            }
        });
        setupPasswordToggle(etPassword);

        btnLogIn.setOnClickListener(v -> attemptLogin());
        btnSignUp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        tvSavedAccountsHeader.setOnClickListener(v -> toggleSavedAccountsExpanded());
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
        if (!TextUtils.isEmpty(selectedSavedEmail)) {
            etEmail.setText(selectedSavedEmail);
            SavedAccount selected = AuthAccountStorage.findByEmail(this, selectedSavedEmail);
            cbAutoLogin.setChecked(selected != null && selected.autoLogin);
        }

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
        SavedAccount selected = AuthAccountStorage.findByEmail(this, email);

        // 仅当“上次启用自动登录 + 本次仍勾选”同时成立时，才允许免输密码快速进入。
        if (TextUtils.isEmpty(password)) {
            if (selected != null && selected.canQuickLogin() && cbAutoLogin.isChecked()) {
                AuthAccountStorage.touch(this, selected.email);
                goToHome("欢迎回来");
                return;
            }
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

                Log.d("LoginActivity", "POST " + API_LOGIN + " body=" + jsonStr);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                Log.d("LoginActivity", "Response Code: " + code);

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
                Log.e("LoginActivity", "Login error url=" + API_LOGIN, e);
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
        boolean autoLogin = cbAutoLogin.isChecked();
        AuthAccountStorage.upsert(this, email, nickname, token, autoLogin);
        goToHome("登录成功");
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
