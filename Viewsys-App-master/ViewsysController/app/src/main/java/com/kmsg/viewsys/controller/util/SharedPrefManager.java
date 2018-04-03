package com.kmsg.viewsys.controller.util;

import android.content.Context;
import android.content.SharedPreferences;


public class SharedPrefManager {

    private static final String SP_GLOBAL = "VIEW_SYS_GLOBAL";
    private static final String SP_NAME = "VIEW_SYS_C";
    private static SharedPreferences gSharedPreference;
    private static SharedPreferences sharedPreference;

    public static void getGlobalSharedPreferences(Context context) {
        if (gSharedPreference == null) {
            gSharedPreference = context.getSharedPreferences(SP_GLOBAL, Context.MODE_PRIVATE);
        }
    }

    public static void getSharedPreferences(Context context) {
        if (sharedPreference == null) {
            sharedPreference = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
    }

    public static void putString(String key, String value) {
        sharedPreference.edit().putString(key, value).apply();
    }

    public static void putInt(String key, int value) {
        sharedPreference.edit().putInt(key, value).apply();
    }

    public static void putLong(String key, long value) {
        sharedPreference.edit().putLong(key, value).apply();
    }

    public static void putFloat(String key, float value) {
        sharedPreference.edit().putFloat(key, value).apply();
    }

    public static void putBoolean(String key, boolean value) {
        sharedPreference.edit().putBoolean(key, value).apply();
    }

    public static String getString(String key) {
        return sharedPreference.getString(key, "");
    }

    public static String getString(String key, String defaultStr) {
        return sharedPreference.getString(key, defaultStr);
    }

    public static int getInt(String key) {
        return sharedPreference.getInt(key, 0);
    }

    public static int getInt(String key, int defaultValue) {
        return sharedPreference.getInt(key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return sharedPreference.getLong(key, defaultValue);
    }

    public static float getFloat(String key) {
        return sharedPreference.getFloat(key, 0f);
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return sharedPreference.getBoolean(key, defValue);
    }

    public static void remove(String key) {
        sharedPreference.edit().remove(key).apply();
    }

    public static void removeAll() {
        sharedPreference.edit().clear().apply();
    }


    public static void putGlobalString(String key, String value) {
        gSharedPreference.edit().putString(key, value).apply();
    }

    public static String getGlobalString(String key, String defaultStr) {
        return gSharedPreference.getString(key, defaultStr);
    }
}
