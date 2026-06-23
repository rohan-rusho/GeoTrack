package com.rohan.geotrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.rohan.geotrack.MainActivity;
import com.rohan.geotrack.R;
import com.rohan.geotrack.database.GeoTrackDatabase;
import com.rohan.geotrack.database.LocationEntity;
import com.rohan.geotrack.service.LocationTrackingService;
import com.rohan.geotrack.utils.Constants;
import com.rohan.geotrack.utils.PreferenceManager;
import com.rohan.geotrack.utils.UIUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceStatusActivity extends AppCompatActivity {
    private static final String TAG = "ServiceStatusActivity";
    private PreferenceManager preferenceManager;
    private GeoTrackDatabase database;
    private TextView tvStatus, tvCoords, tvSync, tvFreq, tvSaved, tvRuntime;
    private MaterialCardView cardStatusBg;
    private MaterialButton btnPauseResume, btnStop;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runtimeUpdater = new Runnable() {
        @Override
        public void run() {
            updateRuntimeDisplay();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_status);

        preferenceManager = new PreferenceManager(this);
        database = GeoTrackDatabase.getInstance(this);

        setupToolbar();
        initViews();
        updateUI();
        handler.post(runtimeUpdater);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_TRACKING_STATE_CHANGED);
        filter.addAction(Constants.ACTION_LOCATION_UPDATED);
        ContextCompat.registerReceiver(this, trackingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private final BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    private void updateRuntimeDisplay() {
        if (preferenceManager.isTracking() && !preferenceManager.isPaused()) {
            long startTime = preferenceManager.getServiceStartTime();
            if (startTime != 0) {
                long displayRuntime = System.currentTimeMillis() - startTime;
                tvRuntime.setText(formatDuration(displayRuntime));
            }
        } else if (!preferenceManager.isTracking()) {
            tvRuntime.setText("00:00:00");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runtimeUpdater);
        try {
            unregisterReceiver(trackingReceiver);
        } catch (Exception ignored) {}
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_status);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(UIUtils.getStyledAppName(this));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status_main);
        tvCoords = findViewById(R.id.tv_status_coords);
        tvSync = findViewById(R.id.tv_status_sync);
        tvFreq = findViewById(R.id.tv_status_freq);
        tvSaved = findViewById(R.id.tv_status_saved);
        tvRuntime = findViewById(R.id.tv_status_runtime);
        cardStatusBg = findViewById(R.id.card_status_bg);
        btnPauseResume = findViewById(R.id.btn_status_pause_resume);
        btnStop = findViewById(R.id.btn_status_stop);

        btnStop.setOnClickListener(v -> {
            preferenceManager.setTracking(false);
            updateUI();
            Intent intent = new Intent(this, LocationTrackingService.class);
            intent.setAction(Constants.ACTION_STOP_TRACKING);
            startService(intent);
            // Notify other components
            broadcastStateChange();
        });
        
        findViewById(R.id.card_status_freq).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("select_tab", 2); // Settings tab
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.card_status_records).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("select_tab", 1); // History tab
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnPauseResume.setOnClickListener(v -> {
            boolean isPaused = preferenceManager.isPaused();
            preferenceManager.setPaused(!isPaused);
            
            Intent intent = new Intent(this, LocationTrackingService.class);
            intent.setAction(isPaused ? Constants.ACTION_RESUME_TRACKING : Constants.ACTION_PAUSE_TRACKING);
            startService(intent);
            
            updateUI();
            broadcastStateChange();
            
            Toast.makeText(this, isPaused ? "Tracking Resumed" : "Tracking Paused", Toast.LENGTH_SHORT).show();
        });
    }

    private void broadcastStateChange() {
        Intent updateIntent = new Intent(Constants.ACTION_TRACKING_STATE_CHANGED);
        updateIntent.setPackage(getPackageName());
        sendBroadcast(updateIntent);
    }

    private void updateUI() {
        boolean isTracking = preferenceManager.isTracking();
        boolean isPaused = preferenceManager.isPaused();

        if (!isTracking) {
            tvStatus.setText("Inactive");
            ((TextView) findViewById(R.id.tv_status_desc)).setText("Service is currently stopped");
            cardStatusBg.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error));
            btnPauseResume.setEnabled(false);
            btnStop.setEnabled(false);
        } else if (isPaused) {
            tvStatus.setText("Paused");
            ((TextView) findViewById(R.id.tv_status_desc)).setText("Tracking is temporarily suspended");
            cardStatusBg.setCardBackgroundColor(ContextCompat.getColor(this, R.color.brown));
            btnPauseResume.setText("Resume");
            btnPauseResume.setIconResource(R.drawable.ic_play_white);
            btnPauseResume.setEnabled(true);
            btnStop.setEnabled(true);
        } else {
            tvStatus.setText("Active");
            ((TextView) findViewById(R.id.tv_status_desc)).setText("Running in Foreground Service");
            cardStatusBg.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green));
            btnPauseResume.setText("Pause");
            btnPauseResume.setIconResource(R.drawable.ic_pause_white);
            btnPauseResume.setEnabled(true);
            btnStop.setEnabled(true);
        }

        tvFreq.setText("Every " + formatInterval(preferenceManager.getTrackingInterval()));

        new Thread(() -> {
            try {
                int count = database.locationDao().getLocationCount();
                LocationEntity last = database.locationDao().getLatestLocation();
                runOnUiThread(() -> {
                    tvSaved.setText(String.valueOf(count));
                    if (last != null) {
                        tvCoords.setText(String.format(Locale.getDefault(), "%.4f, %.4f", last.getLatitude(), last.getLongitude()));
                        tvSync.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(last.getTimestamp())));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching location stats", e);
            }
        }).start();
    }

    private String formatInterval(int seconds) {
        if (seconds < 60) return seconds + " Seconds";
        int mins = seconds / 60;
        return mins + " Minutes";
    }

    private String formatDuration(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
