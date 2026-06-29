package com.rohan.geotrack.utils;

public class Constants {
    // SharedPreferences Keys
    public static final String PREF_NAME = "GeoTrackPrefs";
    public static final String KEY_TRACKING_INTERVAL = "tracking_interval";
    public static final String KEY_SHOW_TOAST = "show_toast";
    public static final String KEY_AUTO_START = "auto_start";
    public static final String KEY_IS_TRACKING = "is_tracking";
    public static final String KEY_IS_PAUSED = "is_paused";
    public static final String KEY_SERVICE_START_TIME = "service_start_time";
    public static final String KEY_TOTAL_RUNTIME = "total_runtime";
    public static final String KEY_LAST_SAVE_TIME = "last_save_time";
    public static final String KEY_LAST_LATITUDE = "last_latitude";
    public static final String KEY_LAST_LONGITUDE = "last_longitude";
    public static final String KEY_IS_FIRST_RUN = "is_first_run";
    public static final String KEY_PAUSE_ELAPSED_TIME = "pause_elapsed_time";

    // Default Values
    public static final int DEFAULT_INTERVAL = 300; // 5 Minutes in seconds
    public static final boolean DEFAULT_SHOW_TOAST = true;
    public static final boolean DEFAULT_AUTO_START = true;

    // Notification
    public static final String CHANNEL_ID = "GeoTrackChannel";
    public static final int NOTIFICATION_ID = 123;
    public static final String CHANNEL_NAME = "Location Tracking Service";

    // Intent Actions
    public static final String ACTION_STOP_TRACKING = "com.rohan.geotrack.STOP_TRACKING";
    public static final String ACTION_PAUSE_TRACKING = "com.rohan.geotrack.PAUSE_TRACKING";
    public static final String ACTION_RESUME_TRACKING = "com.rohan.geotrack.RESUME_TRACKING";
    public static final String ACTION_UPDATE_INTERVAL = "com.rohan.geotrack.UPDATE_INTERVAL";
    public static final String ACTION_TRACKING_STATE_CHANGED = "com.rohan.geotrack.TRACKING_STATE_CHANGED";
    public static final String ACTION_LOCATION_UPDATED = "com.rohan.geotrack.LOCATION_UPDATED";
}
