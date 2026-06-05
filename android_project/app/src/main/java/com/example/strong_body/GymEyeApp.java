package com.example.strong_body;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class GymEyeApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
