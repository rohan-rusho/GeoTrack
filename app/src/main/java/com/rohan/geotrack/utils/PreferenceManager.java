package com.rohan.geotrack.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        Context storageContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            storageContext = context.createDeviceProtectedStorageContext();
        } else {
            storageContext = context;
        }
        sharedPreferences = storageContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
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
        sharedPreferences.edit().putBoolean("is_paused", paused).apply();
    }

    public boolean isPaused() {
        return sharedPreferences.getBoolean("is_paused", false);
    }

    public void setServiceStartTime(long startTime) {
        sharedPreferences.edit().putLong("service_start_time", startTime).apply();
    }

    public long getServiceStartTime() {
        return sharedPreferences.getLong("service_start_time", 0);
    }

    public void setTotalRuntime(long runtime) {
        sharedPreferences.edit().putLong("total_runtime", runtime).apply();
    }

    public long getTotalRuntime() {
        return sharedPreferences.getLong("total_runtime", 0);
    }

    public void setLastSaveTime(long time) {
        sharedPreferences.edit().putLong("last_save_time", time).apply();
    }

    public long getLastSaveTime() {
        return sharedPreferences.getLong("last_save_time", 0);
    }

    public boolean isFirstRun() {
        return sharedPreferences.getBoolean("is_first_run", true);
    }

    public void setFirstRun(boolean firstRun) {
        sharedPreferences.edit().putBoolean("is_first_run", firstRun).apply();
    }
}
