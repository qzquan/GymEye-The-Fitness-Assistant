package com.example.strong_body;

/**
 * 后端基地址：只改 {@link #BACKEND_TARGET}（以及真机 Wi‑Fi 模式下的 {@link #LAN_HOST}）即可。
 * <p>
 * 使用说明：<br>
 * <b>模拟器</b>：{@link BackendTarget#EMULATOR} → {@code 10.0.2.2} 表示宿主机本机。<br>
 * <b>真机 + USB</b>：先 {@code adb reverse tcp:8080 tcp:8080}，再选 {@link BackendTarget#DEVICE_USB_REVERSE}，
 * App 访问 {@code 127.0.0.1:8080} 会经 USB 转到电脑上的 8080。<br>
 * <b>真机 + 同一 Wi‑Fi</b>：选 {@link BackendTarget#DEVICE_WIFI}，把 {@link #LAN_HOST} 设为你电脑的 IPv4（如 {@code 192.168.1.5}），
 * 并放行防火墙 8080。
 */
public final class ApiConfig {

    /**
     * 改这一处即可切换运行环境。
     * <p>
     * 注意：用 <b>模拟器</b> 时请选 {@link BackendTarget#EMULATOR}（{@code 10.0.2.2}）。
     * 若选 {@link BackendTarget#DEVICE_USB_REVERSE}，仅适用于<b>真机 USB</b>且已执行
     * {@code adb reverse tcp:8080 tcp:8080}；在模拟器里用 {@code 127.0.0.1} 会连到模拟器自身而非电脑，必然失败。
     */
<<<<<<< HEAD
    public static final BackendTarget BACKEND_TARGET = BackendTarget.DEVICE_USB_REVERSE;
=======
    public static final BackendTarget BACKEND_TARGET = BackendTarget.EMULATOR;
>>>>>>> origin/main

    /**
     * 仅当 {@link #BACKEND_TARGET} 为 {@link BackendTarget#DEVICE_WIFI} 时使用：
     * 不要带 {@code http://}，不要带端口。
     */
    public static final String LAN_HOST = "192.168.1.1";

    public static final String BASE_URL = buildBaseUrl();

    private static String buildBaseUrl() {
        switch (BACKEND_TARGET) {
            case EMULATOR:
                return "http://10.0.2.2:8080";
            case DEVICE_USB_REVERSE:
                return "http://127.0.0.1:8080";
            case DEVICE_WIFI:
                return "http://" + LAN_HOST + ":8080";
            default:
                return "http://10.0.2.2:8080";
        }
    }

    private ApiConfig() {}

    public enum BackendTarget {
        /** Android 模拟器 */
        EMULATOR,
        /** 真机 USB：需 adb reverse tcp:8080 tcp:8080 */
        DEVICE_USB_REVERSE,
        /** 真机与电脑同一局域网 */
        DEVICE_WIFI
    }
}
