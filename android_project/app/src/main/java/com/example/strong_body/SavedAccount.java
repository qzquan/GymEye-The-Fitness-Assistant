package com.example.strong_body;

/**
 * Locally cached account metadata. Passwords are never stored.
 */
public final class SavedAccount {

    public final String email;
    public final String nickname;
    public final boolean remembered;
    /**
     * JWT returned by login/register. Stored only when autoLogin is enabled.
     */
    public final String token;
    public final boolean autoLogin;
    public final long lastUsedMs;

    public SavedAccount(
            String email,
            String nickname,
            boolean remembered,
            String token,
            boolean autoLogin,
            long lastUsedMs
    ) {
        this.email = email;
        this.nickname = nickname;
        this.remembered = remembered;
        this.token = token != null ? token : "";
        this.autoLogin = autoLogin;
        this.lastUsedMs = lastUsedMs;
    }

    public boolean canQuickLogin() {
        return autoLogin && token != null && !token.isEmpty();
    }
}
