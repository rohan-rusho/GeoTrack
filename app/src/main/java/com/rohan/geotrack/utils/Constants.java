package com.rohan.geotrack.utils;

public class Constants {
    // SharedPreferences Keys
    public static final String PREF_NAME = "GeoTrackPrefs";
    public static final String KEY_TRACKING_INTERVAL = "tracking_interval";
    public static final String KEY_SHOW_TOAST = "show_toast";
    public static final String KEY_AUTO_START = "auto_start";
    public static final String KEY_IS_TRACKING = "is_tracking";

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
    public static final String ACTION_START_TRACKING = "com.rohan.geotrack.START_TRACKING";
    public static final String ACTION_PAUSE_TRACKING = "com.rohan.geotrack.PAUSE_TRACKING";
    public static final String ACTION_RESUME_TRACKING = "com.rohan.geotrack.RESUME_TRACKING";
    public static final String ACTION_UPDATE_INTERVAL = "com.rohan.geotrack.UPDATE_INTERVAL";
}
