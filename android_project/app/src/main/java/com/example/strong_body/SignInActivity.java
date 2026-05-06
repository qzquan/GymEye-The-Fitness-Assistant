package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Existing-account sign in: email + password.
 */
public class SignInActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_EMAIL = "com.example.strong_body.extra.INITIAL_EMAIL";

    private static final String API_LOGIN = ApiConfig.BASE_URL + "/api/user/login";

    private EditText etEmail;
    private EditText etPassword;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private CheckBox cbRememberAccount;
    private CheckBox cbAutoLogin;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        AuthAccountStorage.migrateLegacyIfNeeded(this);

        etEmail = findViewById(R.id.etSignInEmail);
        etPassword = findViewById(R.id.etSignInPassword);
        emailInputLayout = findTextInputLayout(etEmail);
        passwordInputLayout = findTextInputLayout(etPassword);
        cbRememberAccount = findViewById(R.id.cbRememberAccount);
        cbAutoLogin = findViewById(R.id.cbAutoLogin);
        btnSubmit = findViewById(R.id.btnSignInSubmit);
        ImageButton btnBack = findViewById(R.id.btnSignInBack);
        TextView tvBack = findViewById(R.id.tvBackToRegister);

        String extraEmail = getIntent().getStringExtra(EXTRA_INITIAL_EMAIL);
        if (!TextUtils.isEmpty(extraEmail)) {
            etEmail.setText(extraEmail.trim());
            SavedAccount selected = AuthAccountStorage.findByEmail(this, extraEmail.trim());
            applySavedAccountState(selected);
        } else {
            SavedAccount recent = AuthAccountStorage.getMostRecentlyUsed(this);
            if (recent != null) {
                etEmail.setText(recent.email);
                applySavedAccountState(recent);
            }
        }

        clearLayoutErrorOnTextChange(etEmail, emailInputLayout);
        clearLayoutErrorOnTextChange(etPassword, passwordInputLayout);

        cbAutoLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbRememberAccount.setChecked(true);
            }
        });

        btnSubmit.setOnClickListener(v -> attemptLogin());
        btnBack.setOnClickListener(v -> finish());

        tvBack.setOnClickListener(v -> {
            Intent i = new Intent(SignInActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }

    private void applySavedAccountState(SavedAccount account) {
        if (account == null) return;
        cbRememberAccount.setChecked(account.remembered || account.autoLogin);
        cbAutoLogin.setChecked(account.autoLogin);
    }

    private void attemptLogin() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        boolean valid = true;
        EditText firstInvalid = null;

        clearInputError(emailInputLayout, etEmail);
        clearInputError(passwordInputLayout, etPassword);

        if (TextUtils.isEmpty(email)) {
            showInputError(emailInputLayout, etEmail, "请输入邮箱");
            firstInvalid = etEmail;
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showInputError(emailInputLayout, etEmail, "邮箱格式不正确");
            firstInvalid = etEmail;
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            showInputError(passwordInputLayout, etPassword, "请输入密码");
            if (firstInvalid == null) firstInvalid = etPassword;
            valid = false;
        } else if (password.length() < 6) {
            showInputError(passwordInputLayout, etPassword, "密码至少 6 位");
            if (firstInvalid == null) firstInvalid = etPassword;
            valid = false;
        }

        if (!valid) {
            if (firstInvalid != null) {
                firstInvalid.requestFocus();
            }
            return;
        }

        loginTask(email, password);
    }

    private static TextInputLayout findTextInputLayout(EditText editText) {
        ViewParent parent = editText.getParent();
        while (parent != null) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private static void clearLayoutErrorOnTextChange(EditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearInputError(layout, editText);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private static void showInputError(TextInputLayout layout, EditText editText, String message) {
        if (layout != null) {
            layout.setErrorEnabled(true);
            layout.setError(message);
        } else {
            editText.setError(message);
        }
    }

    private static void clearInputError(TextInputLayout layout, EditText editText) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        } else {
            editText.setError(null);
        }
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

                Log.d("SignInActivity", "POST " + API_LOGIN + " email=" + email);

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

        boolean autoLogin = cbAutoLogin.isChecked();
        boolean rememberAccount = cbRememberAccount.isChecked();
        AuthAccountStorage.saveLoginState(this, email, nickname, token, rememberAccount, autoLogin);

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
