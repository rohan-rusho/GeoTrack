package com.rohan.geotrack.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.rohan.geotrack.MainActivity;
import com.rohan.geotrack.R;
import com.rohan.geotrack.database.GeoTrackDatabase;
import com.rohan.geotrack.database.LocationEntity;
import com.rohan.geotrack.utils.Constants;
import com.rohan.geotrack.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PreferenceManager preferenceManager;
    private GeoTrackDatabase database;
    private ExecutorService executorService;
    private NotificationManager notificationManager;
    private long serviceStartTime;

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        database = GeoTrackDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        serviceStartTime = preferenceManager.getServiceStartTime();

        createNotificationChannel();
        setupLocationCallback();

        try {
            IntentFilter filter = new IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION);
            ContextCompat.registerReceiver(this, locationProviderReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception e) {
            Log.e(TAG, "Error registering locationProviderReceiver", e);
        }
    }

    private void saveIncrementalRuntime() {
        if (serviceStartTime != 0) {
            long now = System.currentTimeMillis();
            long sessionSoFar = now - serviceStartTime;
            long total = preferenceManager.getTotalRuntime() + sessionSoFar;
            preferenceManager.setTotalRuntime(total);
            serviceStartTime = now;
            preferenceManager.setServiceStartTime(serviceStartTime);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null && preferenceManager.isTracking() && !preferenceManager.isPaused()) {
                    saveIncrementalRuntime();
                    saveLocationToDb(lastLocation);
                    updateNotification(lastLocation);
                }
            }
        };
    }

    private void saveLocationToDb(Location location) {
        long now = System.currentTimeMillis();
        
        int intervalSeconds = preferenceManager.getTrackingInterval();
        long intervalMillis = intervalSeconds * 1000L;
        long lastSave = preferenceManager.getLastSaveTime();
        
        // Strict interval check for "perfect" seconds.
        // We use a minimal 1-second buffer to handle minor system jitter, 
        // ensuring we save as close to the exact second as possible.
        if (lastSave != 0 && (now - lastSave) < (intervalMillis - 1000L)) {
            return;
        }

        preferenceManager.setLastSaveTime(now);
        
        // Use the current system time for the record to match the app's runtime clock
        // and the user's expectation of "exact seconds" from when the app started.
        LocationEntity entity = new LocationEntity(
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getProvider(),
                now,
                now
        );
        executorService.execute(() -> {
            try {
                database.locationDao().insertLocation(entity);
                // Broadcast intent to update UI instantly
                Intent updateIntent = new Intent(Constants.ACTION_LOCATION_UPDATED);
                sendBroadcast(updateIntent);
                
                // Show toast ONLY when a record is successfully saved
                if (preferenceManager.isShowToast()) {
                    new Handler(Looper.getMainLooper()).post(() -> showToast(location));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save location to database", e);
            }
        });
    }

    private void showToast(Location location) {
        Toast.makeText(this, "GeoTrack: Location Updated", Toast.LENGTH_SHORT).show();
    }

    private final android.content.BroadcastReceiver locationProviderReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (android.location.LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                android.location.LocationManager lm = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(context, "Location turned OFF. Stopping service.", Toast.LENGTH_LONG).show();
                    stopTracking();
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (Constants.ACTION_STOP_TRACKING.equals(action)) {
                updateTotalRuntime();
                stopTracking();
                return START_NOT_STICKY;
            } else if (Constants.ACTION_PAUSE_TRACKING.equals(action)) {
                updateTotalRuntime(); // Update and reset serviceStartTime
                
                // Store how much time has passed since the last record was saved
                long now = System.currentTimeMillis();
                long lastSave = preferenceManager.getLastSaveTime();
                if (lastSave != 0) {
                    preferenceManager.setPauseElapsedTime(now - lastSave);
                }
                
                preferenceManager.setPaused(true);
                fusedLocationClient.removeLocationUpdates(locationCallback);
                updateNotification(null);
                broadcastStateChange();
                return START_STICKY;
            } else if (Constants.ACTION_RESUME_TRACKING.equals(action)) {
                // Restore the timeline by shifting lastSaveTime forward
                long now = System.currentTimeMillis();
                long elapsed = preferenceManager.getPauseElapsedTime();
                if (elapsed != 0) {
                    preferenceManager.setLastSaveTime(now - elapsed);
                }
                
                serviceStartTime = now;
                preferenceManager.setServiceStartTime(now);

                preferenceManager.setPaused(false);
                requestLocationUpdates();
                updateNotification(null);
                broadcastStateChange();
                return START_STICKY;
            } else if (Constants.ACTION_UPDATE_INTERVAL.equals(action)) {
                requestLocationUpdates(); // Update FusedLocation interval
                return START_STICKY;
            }
        }

        if (serviceStartTime == 0) {
            serviceStartTime = System.currentTimeMillis();
            preferenceManager.setServiceStartTime(serviceStartTime);
            if (preferenceManager.getLastSaveTime() == 0) {
                preferenceManager.setLastSaveTime(serviceStartTime);
            }
        }

        startForeground(Constants.NOTIFICATION_ID, createNotification(null));
        requestLocationUpdates();
        preferenceManager.setTracking(true);

        return START_STICKY;
    }

    private void broadcastStateChange() {
        Intent intent = new Intent(Constants.ACTION_TRACKING_STATE_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void updateTotalRuntime() {
        if (serviceStartTime != 0) {
            long currentSession = System.currentTimeMillis() - serviceStartTime;
            long total = preferenceManager.getTotalRuntime() + currentSession;
            preferenceManager.setTotalRuntime(total);
            preferenceManager.setServiceStartTime(0);
            serviceStartTime = 0;
        }
    }

    private void requestLocationUpdates() {
        int intervalSeconds = preferenceManager.getTrackingInterval();
        long intervalMillis = intervalSeconds * 1000L;

        // Increase frequency to every 1 second when approaching the target
        // to ensure we hit the "perfect" second without a 1-2s delay.
        long fastInterval = 1000L;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, fastInterval)
                .setMinUpdateIntervalMillis(fastInterval)
                .setMaxUpdateDelayMillis(0)
                .setWaitForAccurateLocation(false)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        preferenceManager.setTracking(false);
        preferenceManager.setLastSaveTime(0);
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(@Nullable Location location) {
        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_tracking_collapsed);
        RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_tracking);

        if (location != null) {
            String lat = String.format(Locale.getDefault(), "%.2f", location.getLatitude());
            String lng = String.format(Locale.getDefault(), "%.2f", location.getLongitude());
            String time = formatTimeShort(location.getTime());

            collapsedView.setTextViewText(R.id.tvLat, lat);
            collapsedView.setTextViewText(R.id.tvLng, lng);
            collapsedView.setTextViewText(R.id.tvUpdated, time);

            expandedView.setTextViewText(R.id.tvLat, lat);
            expandedView.setTextViewText(R.id.tvLng, lng);
            expandedView.setTextViewText(R.id.tvUpdated, time);
        }

        String formattedInterval = formatIntervalString(preferenceManager.getTrackingInterval());
        collapsedView.setTextViewText(R.id.tvInterval, formattedInterval);
        expandedView.setTextViewText(R.id.tvInterval, formattedInterval);

        // Actions
        Intent stopIntent = new Intent(this, LocationTrackingService.class);
        stopIntent.setAction(Constants.ACTION_STOP_TRACKING);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        expandedView.setOnClickPendingIntent(R.id.btnStopTracking, stopPendingIntent);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(this, 1, openAppIntent, PendingIntent.FLAG_IMMUTABLE);
        expandedView.setOnClickPendingIntent(R.id.btnOpenApp, openAppPendingIntent);

        return new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(Constants.NOTIFICATION_ID, createNotification(location));
        }
    }

    private String formatIntervalString(int seconds) {
        if (seconds < 60) return seconds + "s";
        int mins = seconds / 60;
        if (mins < 60) return mins + "m";
        int hrs = mins / 60;
        return hrs + "h";
    }

    private String formatTimeShort(long timestamp) {
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        return time.replace(" AM", "am").replace(" PM", "pm");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        updateTotalRuntime();
        try {
            unregisterReceiver(locationProviderReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering locationProviderReceiver", e);
        }
        super.onDestroy();
        executorService.shutdown();
    }
}
