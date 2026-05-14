package com.example.strong_body;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local account list stored as SharedPreferences JSON.
 */
public final class AuthAccountStorage {

    private static final String PREFS = "gymeye_auth";
    private static final String KEY_ACCOUNTS_JSON = "saved_accounts_json";

    private AuthAccountStorage() {}

    public static List<SavedAccount> loadSortedNewestFirst(Context ctx) {
        List<SavedAccount> list = new ArrayList<>();
        for (SavedAccount account : loadAll(ctx)) {
            if (account.remembered) {
                list.add(account);
            }
        }
        sortNewestFirst(list);
        return list;
    }

    public static SavedAccount findByEmail(Context ctx, String email) {
        if (TextUtils.isEmpty(email)) return null;
        String key = normalizeEmail(email);
        for (SavedAccount account : loadAll(ctx)) {
            if (key.equals(account.email.toLowerCase())) return account;
        }
        return null;
    }

    public static SavedAccount getMostRecentlyUsed(Context ctx) {
        List<SavedAccount> list = loadSortedNewestFirst(ctx);
        return list.isEmpty() ? null : list.get(0);
    }

    public static SavedAccount getAutoLoginAccount(Context ctx) {
        List<SavedAccount> list = loadAll(ctx);
        sortNewestFirst(list);
        for (SavedAccount account : list) {
            if (account.canQuickLogin()) {
                return account;
            }
        }
        return null;
    }

    public static String getSessionToken(Context ctx) {
        SavedAccount account = getAutoLoginAccount(ctx);
        if (account != null && account.token != null && !account.token.isEmpty()) {
            return account.token;
        }
        account = getMostRecentlyUsed(ctx);
        if (account != null && account.token != null && !account.token.isEmpty()) {
            return account.token;
        }
        return "";
    }

    public static void saveLoginState(
            Context ctx,
            String email,
            String nickname,
            String token,
            boolean rememberAccount,
            boolean autoLogin
    ) {
        if (TextUtils.isEmpty(email)) return;
        String normalizedEmail = normalizeEmail(email);

        if (!rememberAccount && !autoLogin) {
            remove(ctx, normalizedEmail);
            return;
        }

        String nick = TextUtils.isEmpty(nickname) ? normalizedEmail.split("@")[0] : nickname.trim();
        String storedToken = autoLogin && token != null ? token : "";
        SavedAccount merged = new SavedAccount(
                normalizedEmail,
                nick,
                rememberAccount,
                storedToken,
                autoLogin,
                System.currentTimeMillis()
        );

        ArrayList<SavedAccount> next = new ArrayList<>();
        for (SavedAccount account : loadAll(ctx)) {
            if (!normalizedEmail.equalsIgnoreCase(account.email)) {
                next.add(account);
            }
        }
        next.add(merged);
        saveAll(ctx, next);
    }

    public static void upsert(Context ctx, String email, String nickname, String token, boolean autoLogin) {
        saveLoginState(ctx, email, nickname, token, true, autoLogin);
    }

    public static void touch(Context ctx, String email) {
        SavedAccount account = findByEmail(ctx, email);
        if (account == null) return;

        ArrayList<SavedAccount> next = new ArrayList<>();
        for (SavedAccount existing : loadAll(ctx)) {
            if (!account.email.equalsIgnoreCase(existing.email)) {
                next.add(existing);
            }
        }
        next.add(new SavedAccount(
                account.email,
                account.nickname,
                account.remembered,
                account.token,
                account.autoLogin,
                System.currentTimeMillis()
        ));
        saveAll(ctx, next);
    }

    public static void logout(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ACCOUNTS_JSON).apply();
    }

    private static void remove(Context ctx, String email) {
        ArrayList<SavedAccount> next = new ArrayList<>();
        for (SavedAccount account : loadAll(ctx)) {
            if (!email.equalsIgnoreCase(account.email)) {
                next.add(account);
            }
        }
        saveAll(ctx, next);
    }

    private static List<SavedAccount> loadAll(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_ACCOUNTS_JSON, "[]");
        ArrayList<SavedAccount> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String email = normalizeEmail(o.optString("email", ""));
                if (email.isEmpty()) continue;

                boolean autoLogin = o.optBoolean("autoLogin", false);
                boolean remembered = o.optBoolean("remembered", true);
                String token = autoLogin ? o.optString("token", "") : "";
                if (!remembered && !autoLogin) continue;

                out.add(new SavedAccount(
                        email,
                        o.optString("nickname", email.split("@")[0]),
                        remembered,
                        token,
                        autoLogin,
                        o.optLong("lastUsedMs", 0L)
                ));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void saveAll(Context ctx, List<SavedAccount> accounts) {
        JSONArray arr = new JSONArray();
        try {
            for (SavedAccount account : accounts) {
                if (!account.remembered && !account.autoLogin) continue;
                JSONObject o = new JSONObject();
                o.put("email", account.email);
                o.put("nickname", account.nickname);
                o.put("remembered", account.remembered);
                o.put("token", account.autoLogin ? account.token : "");
                o.put("autoLogin", account.autoLogin);
                o.put("lastUsedMs", account.lastUsedMs);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_ACCOUNTS_JSON, arr.toString())
                .apply();
    }

    public static void migrateLegacyIfNeeded(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_ACCOUNTS_JSON)) {
            saveAll(ctx, loadAll(ctx));
            return;
        }

        String oldEmail = prefs.getString("saved_email", null);
        if (TextUtils.isEmpty(oldEmail)) return;
        saveLoginState(ctx, oldEmail, oldEmail.split("@")[0], "", true, false);
        prefs.edit().remove("saved_email").remove("auto_login").apply();
    }

    private static void sortNewestFirst(List<SavedAccount> list) {
        Collections.sort(list, (a, b) -> Long.compare(b.lastUsedMs, a.lastUsedMs));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
