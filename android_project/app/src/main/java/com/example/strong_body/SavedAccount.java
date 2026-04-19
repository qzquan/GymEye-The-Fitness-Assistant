package com.example.strong_body;

/**
 * 本地缓存的账号信息：展示昵称、JWT（用于自动登录与后续接口），不保存密码。
 */
public final class SavedAccount {

    public final String email;
    public final String nickname;
    /** 登录/注册接口返回的 JWT；可能为空表示仅记录邮箱 */
    public final String token;
    /** 用户曾在登录页勾选「自动登录」 */
    public final boolean autoLogin;
    public final long lastUsedMs;

    public SavedAccount(String email, String nickname, String token, boolean autoLogin, long lastUsedMs) {
        this.email = email;
        this.nickname = nickname;
        this.token = token != null ? token : "";
        this.autoLogin = autoLogin;
        this.lastUsedMs = lastUsedMs;
    }

    public boolean canQuickLogin() {
        return autoLogin && token != null && !token.isEmpty();
    }
}
