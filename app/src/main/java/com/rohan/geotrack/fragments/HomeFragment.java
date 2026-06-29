package com.rohan.geotrack.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.rohan.geotrack.R;
import com.rohan.geotrack.activities.ServiceStatusActivity;
import com.rohan.geotrack.database.GeoTrackDatabase;
import com.rohan.geotrack.database.LocationEntity;
import com.rohan.geotrack.service.LocationTrackingService;
import com.rohan.geotrack.utils.Constants;
import com.rohan.geotrack.utils.PreferenceManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private PreferenceManager preferenceManager;
    private GeoTrackDatabase database;
    private TextView tvStatusBadge, tvLastUpdated, tvInterval, tvLat, tvLng, tvTimestamp, tvAccuracy, tvTotal, tvToday, tvRuntime, tvBattery;
    private MaterialButton btnStart, btnStop, btnPauseResume;
    private View layoutActiveControls;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        preferenceManager = new PreferenceManager(requireContext());
        database = GeoTrackDatabase.getInstance(requireContext());

        initViews(view);
        observeDatabase();
        updateUI();
        startRuntimeTimer();

        return view;
    }

    private void initViews(View v) {
        tvStatusBadge = v.findViewById(R.id.tv_status_badge);
        tvLastUpdated = v.findViewById(R.id.tv_home_last_updated);
        tvInterval = v.findViewById(R.id.tv_home_interval);
        tvLat = v.findViewById(R.id.tv_home_lat);
        tvLng = v.findViewById(R.id.tv_home_lng);
        tvTimestamp = v.findViewById(R.id.tv_home_timestamp);
        tvAccuracy = v.findViewById(R.id.tv_home_accuracy);
        tvTotal = v.findViewById(R.id.tv_stat_total);
        tvToday = v.findViewById(R.id.tv_stat_today);
        tvRuntime = v.findViewById(R.id.tv_stat_runtime);
        tvBattery = v.findViewById(R.id.tv_stat_battery);
        btnStart = v.findViewById(R.id.btn_main_start);
        btnStop = v.findViewById(R.id.btn_main_stop);
        btnPauseResume = v.findViewById(R.id.btn_main_pause_resume);
        layoutActiveControls = v.findViewById(R.id.layout_active_controls);

        btnStart.setOnClickListener(v1 -> startTracking());
        btnStop.setOnClickListener(v1 -> stopTracking());
        btnPauseResume.setOnClickListener(v1 -> togglePauseResume());
        
        v.findViewById(R.id.card_status).setOnClickListener(v1 -> startActivity(new Intent(requireContext(), ServiceStatusActivity.class)));
        
        v.findViewById(R.id.card_nav_history).setOnClickListener(v1 -> {
            androidx.fragment.app.FragmentActivity activity = getActivity();
            if (activity != null) {
                ViewPager2 vp = activity.findViewById(R.id.view_pager);
                if (vp != null) vp.setCurrentItem(1);
            }
        });
        
        v.findViewById(R.id.card_nav_settings).setOnClickListener(v1 -> {
            androidx.fragment.app.FragmentActivity activity = getActivity();
            if (activity != null) {
                ViewPager2 vp = activity.findViewById(R.id.view_pager);
                if (vp != null) vp.setCurrentItem(2);
            }
        });

        // Listen for tracking state changes from other activities
        IntentFilter filter = new IntentFilter(Constants.ACTION_TRACKING_STATE_CHANGED);
        ContextCompat.registerReceiver(requireContext(), trackingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private final android.content.BroadcastReceiver trackingReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    public void updateUI() {
        if (!isAdded()) return;
        boolean isTracking = preferenceManager.isTracking();
        boolean isPaused = preferenceManager.isPaused();

        tvStatusBadge.setText(isTracking ? (isPaused ? "PAUSED" : "ACTIVE") : "INACTIVE");
        tvStatusBadge.setBackgroundResource(isTracking ? (isPaused ? R.drawable.red_badge_bg : R.drawable.green_badge_bg) : R.drawable.red_badge_bg);
        
        btnStart.setVisibility(isTracking ? View.GONE : View.VISIBLE);
        layoutActiveControls.setVisibility(isTracking ? View.VISIBLE : View.GONE);

        if (isPaused) {
            btnPauseResume.setText("Resume");
            btnPauseResume.setIconResource(R.drawable.ic_play_white);
        } else {
            btnPauseResume.setText("Pause");
            btnPauseResume.setIconResource(R.drawable.ic_pause_white);
        }

        tvInterval.setText(formatInterval(preferenceManager.getTrackingInterval()));

        // Battery
        BatteryManager bm = (BatteryManager) requireContext().getSystemService(Context.BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        tvBattery.setText(batLevel + "%");
    }

    private void togglePauseResume() {
        boolean isPaused = preferenceManager.isPaused();
        preferenceManager.setPaused(!isPaused);
        updateUI();
        
        // Notify Service
        Intent intent = new Intent(requireContext(), LocationTrackingService.class);
        intent.setAction(isPaused ? Constants.ACTION_RESUME_TRACKING : Constants.ACTION_PAUSE_TRACKING);
        requireContext().startService(intent);

        // Notify other UI components
        Intent broadcastIntent = new Intent(Constants.ACTION_TRACKING_STATE_CHANGED);
        broadcastIntent.setPackage(requireContext().getPackageName());
        requireContext().sendBroadcast(broadcastIntent);
    }


    private void observeDatabase() {
        database.locationDao().getAllLocationsLive().observe(getViewLifecycleOwner(), locations -> {
            if (!isAdded()) return;
            
            tvTotal.setText(String.valueOf(locations.size()));
            if (!locations.isEmpty()) {
                LocationEntity last = locations.get(0);
                tvLat.setText(String.format(Locale.getDefault(), "%.4f", last.getLatitude()));
                tvLng.setText(String.format(Locale.getDefault(), "%.4f", last.getLongitude()));
                tvTimestamp.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(last.getTimestamp())));
                tvAccuracy.setText(String.format(Locale.getDefault(), "%.1f m", last.getAccuracy()));
                tvLastUpdated.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(last.getTimestamp())));
            } else {
                tvLat.setText("--");
                tvLng.setText("--");
                tvTimestamp.setText("--");
                tvAccuracy.setText("--");
                tvLastUpdated.setText("--");
            }
            
            long todayStart = getTodayStartTime();
            int todayCount = 0;
            for (LocationEntity loc : locations) {
                if (loc.getCreatedAt() >= todayStart) todayCount++;
            }
            tvToday.setText(String.valueOf(todayCount));
        });
    }

    private void startRuntimeTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean isTracking = preferenceManager.isTracking();
                boolean isPaused = preferenceManager.isPaused();
                
                if (isTracking) {
                    long lastSave = preferenceManager.getLastSaveTime();
                    long displayMillis = 0;
                    
                    if (isPaused) {
                        displayMillis = preferenceManager.getPauseElapsedTime();
                    } else if (lastSave != 0) {
                        displayMillis = System.currentTimeMillis() - lastSave;
                    }
                    
                    if (displayMillis < 0) displayMillis = 0;

                    int seconds = (int) (displayMillis / 1000);
                    int minutes = seconds / 60;
                    int hours = minutes / 60;
                    seconds %= 60;
                    minutes %= 60;
                    tvRuntime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                } else {
                    tvRuntime.setText("00:00:00");
                }

                handler.postDelayed(this, 1000);
            }
        });
    }

    private long getTodayStartTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String formatInterval(int seconds) {
        if (seconds < 60) return seconds + " Seconds";
        int mins = seconds / 60;
        return mins + " Minutes";
    }

    private void startTracking() {
        android.location.LocationManager lm = (android.location.LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Location Disabled")
                    .setMessage("Device location is turned off. Please enable it to start tracking.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }
        
        // Reset session stats for fresh start
        preferenceManager.setTotalRuntime(0);
        preferenceManager.setServiceStartTime(0);
        preferenceManager.setLastSaveTime(0);
        preferenceManager.setPauseElapsedTime(0);
        
        preferenceManager.setTracking(true);
        preferenceManager.setPaused(false);
        Intent intent = new Intent(requireContext(), LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
        updateUI();
        // Notify other components
        Intent startIntent = new Intent(Constants.ACTION_TRACKING_STATE_CHANGED);
        startIntent.setPackage(requireContext().getPackageName());
        requireContext().sendBroadcast(startIntent);
    }

    private void stopTracking() {
        preferenceManager.setTracking(false);
        Intent intent = new Intent(requireContext(), LocationTrackingService.class);
        intent.setAction(Constants.ACTION_STOP_TRACKING);
        requireContext().startService(intent);
        updateUI();
        // Notify other components
        Intent stopIntent = new Intent(Constants.ACTION_TRACKING_STATE_CHANGED);
        stopIntent.setPackage(requireContext().getPackageName());
        requireContext().sendBroadcast(stopIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        // Database is already being observed live
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        Context context = getContext();
        if (context != null) {
            try {
                context.unregisterReceiver(trackingReceiver);
            } catch (Exception ignored) {}
        }
    }
}
