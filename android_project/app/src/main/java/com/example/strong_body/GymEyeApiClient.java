package com.example.strong_body;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GymEyeApiClient {

    private GymEyeApiClient() {}

    public static final class HttpResult {
        public final int code;
        public final String body;

        public HttpResult(int code, String body) {
            this.code = code;
            this.body = body != null ? body : "";
        }

        public JSONObject jsonOrEmpty() {
            try {
                return new JSONObject(body);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }

    public static HttpResult get(String path, String token) throws Exception {
        return request("GET", path, token, null, false);
    }

    public static HttpResult postJson(String path, String token, JSONObject json) throws Exception {
        return request("POST", path, token, json, true);
    }

    public static HttpResult putJson(String path, String token, JSONObject json) throws Exception {
        return request("PUT", path, token, json, true);
    }

    private static HttpResult request(String method, String path, String token, JSONObject jsonBody, boolean writeBody)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ApiConfig.BASE_URL + path).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (writeBody && jsonBody != null) {
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            byte[] bytes = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
        }

        int code = conn.getResponseCode();
        return new HttpResult(code, readBody(conn, code));
    }

    private static String readBody(HttpURLConnection conn, int code) throws Exception {
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
