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
 * 多账号本地列表（SharedPreferences JSON），按 {@link SavedAccount#lastUsedMs} 新到旧排序。
 */
public final class AuthAccountStorage {

    private static final String PREFS = "gymeye_auth";
    private static final String KEY_ACCOUNTS_JSON = "saved_accounts_json";

    private AuthAccountStorage() {}

    public static List<SavedAccount> loadSortedNewestFirst(Context ctx) {
        List<SavedAccount> list = loadAll(ctx);
        Collections.sort(list, (a, b) -> Long.compare(b.lastUsedMs, a.lastUsedMs));
        return list;
    }

    public static SavedAccount findByEmail(Context ctx, String email) {
        if (TextUtils.isEmpty(email)) return null;
        String key = email.trim().toLowerCase();
        for (SavedAccount a : loadAll(ctx)) {
            if (key.equals(a.email.toLowerCase())) return a;
        }
        return null;
    }

    /** 最近使用的一条，用于登录页预填邮箱 */
    public static SavedAccount getMostRecentlyUsed(Context ctx) {
        List<SavedAccount> list = loadSortedNewestFirst(ctx);
        return list.isEmpty() ? null : list.get(0);
    }

    public static void upsert(Context ctx, String email, String nickname, String token, boolean autoLogin) {
        if (TextUtils.isEmpty(email)) return;
        String em = email.trim().toLowerCase();
        String nick = TextUtils.isEmpty(nickname) ? em.split("@")[0] : nickname.trim();
        String tok = token != null ? token : "";
        long now = System.currentTimeMillis();

        SavedAccount old = findByEmail(ctx, em);
        String mergedToken = !tok.isEmpty() ? tok : (old != null ? old.token : "");
        SavedAccount merged = new SavedAccount(em, nick, mergedToken, autoLogin, now);

        ArrayList<SavedAccount> next = new ArrayList<>();
        for (SavedAccount a : loadAll(ctx)) {
            if (!em.equalsIgnoreCase(a.email)) {
                next.add(a);
            }
        }
        next.add(merged);
        saveAll(ctx, next);
    }

    /** 仅更新最近使用时间（一键登录成功时） */
    public static void touch(Context ctx, String email) {
        SavedAccount a = findByEmail(ctx, email);
        if (a == null) return;
        upsert(ctx, a.email, a.nickname, a.token, a.autoLogin);
    }

    private static List<SavedAccount> loadAll(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = p.getString(KEY_ACCOUNTS_JSON, "[]");
        ArrayList<SavedAccount> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String email = o.optString("email", "");
                if (email.isEmpty()) continue;
                out.add(new SavedAccount(
                        email,
                        o.optString("nickname", email.split("@")[0]),
                        o.optString("token", ""),
                        o.optBoolean("autoLogin", false),
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
            for (SavedAccount a : accounts) {
                JSONObject o = new JSONObject();
                o.put("email", a.email);
                o.put("nickname", a.nickname);
                o.put("token", a.token);
                o.put("autoLogin", a.autoLogin);
                o.put("lastUsedMs", a.lastUsedMs);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_ACCOUNTS_JSON, arr.toString())
                .apply();
    }

    /** 迁移旧版仅保存邮箱的键（无 token，无法一键登录，但可预填邮箱） */
    public static void migrateLegacyIfNeeded(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.contains(KEY_ACCOUNTS_JSON)) return;
        String oldEmail = p.getString("saved_email", null);
        if (TextUtils.isEmpty(oldEmail)) return;
        boolean oldAuto = p.getBoolean("auto_login", false);
        upsert(ctx, oldEmail, oldEmail.split("@")[0], "", oldAuto);
        p.edit().remove("saved_email").remove("auto_login").apply();
    }
}
