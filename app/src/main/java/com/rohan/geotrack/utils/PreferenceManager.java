package com.rohan.geotrack.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.createDeviceProtectedStorageContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setTrackingInterval(int seconds) {
        sharedPreferences.edit().putInt(Constants.KEY_TRACKING_INTERVAL, seconds).apply();
    }

    public int getTrackingInterval() {
        return sharedPreferences.getInt(Constants.KEY_TRACKING_INTERVAL, Constants.DEFAULT_INTERVAL);
    }

    public void setShowToast(boolean show) {
        sharedPreferences.edit().putBoolean(Constants.KEY_SHOW_TOAST, show).apply();
    }

    public boolean isShowToast() {
        return sharedPreferences.getBoolean(Constants.KEY_SHOW_TOAST, Constants.DEFAULT_SHOW_TOAST);
    }

    public void setAutoStart(boolean autoStart) {
        sharedPreferences.edit().putBoolean(Constants.KEY_AUTO_START, autoStart).apply();
    }

    public boolean isAutoStart() {
        return sharedPreferences.getBoolean(Constants.KEY_AUTO_START, Constants.DEFAULT_AUTO_START);
    }

    public void setTracking(boolean tracking) {
        sharedPreferences.edit().putBoolean(Constants.KEY_IS_TRACKING, tracking).apply();
    }

    public boolean isTracking() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_TRACKING, false);
    }

    public void setPaused(boolean paused) {
        sharedPreferences.edit().putBoolean(Constants.KEY_IS_PAUSED, paused).apply();
    }

    public boolean isPaused() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_PAUSED, false);
    }

    public void setServiceStartTime(long startTime) {
        sharedPreferences.edit().putLong(Constants.KEY_SERVICE_START_TIME, startTime).apply();
    }

    public long getServiceStartTime() {
        return sharedPreferences.getLong(Constants.KEY_SERVICE_START_TIME, 0);
    }

    public void setTotalRuntime(long runtime) {
        sharedPreferences.edit().putLong(Constants.KEY_TOTAL_RUNTIME, runtime).apply();
    }

    public long getTotalRuntime() {
        return sharedPreferences.getLong(Constants.KEY_TOTAL_RUNTIME, 0);
    }

    public void setLastSaveTime(long time) {
        sharedPreferences.edit().putLong(Constants.KEY_LAST_SAVE_TIME, time).apply();
    }

    public long getLastSaveTime() {
        return sharedPreferences.getLong(Constants.KEY_LAST_SAVE_TIME, 0);
    }

    public boolean isFirstRun() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_FIRST_RUN, true);
    }

    public void setFirstRun(boolean firstRun) {
        sharedPreferences.edit().putBoolean(Constants.KEY_IS_FIRST_RUN, firstRun).apply();
    }
}
