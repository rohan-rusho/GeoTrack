package com.rohan.geotrack.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.rohan.geotrack.R;
import com.rohan.geotrack.database.GeoTrackDatabase;
import com.rohan.geotrack.database.LocationEntity;
import com.rohan.geotrack.utils.UIUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDetailsActivity extends AppCompatActivity {
    private GeoTrackDatabase database;
    private LocationEntity location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_details);

        database = GeoTrackDatabase.getInstance(this);
        int locationId = getIntent().getIntExtra("location_id", -1);

        setupToolbar();
        
        if (locationId != -1) {
            new Thread(() -> {
                location = database.locationDao().getLocationById(locationId);
                if (location != null) {
                    runOnUiThread(this::displayDetails);
                }
            }).start();
        }

        findViewById(R.id.btn_det_copy).setOnClickListener(v -> copyToClipboard());
        findViewById(R.id.btn_det_delete).setOnClickListener(v -> deleteRecord());
        findViewById(R.id.btn_det_share).setOnClickListener(v -> shareLocation());
    }

    private void shareLocation() {
        if (location == null) return;
        String text = "Check out my location:\n" +
                "Latitude: " + location.getLatitude() + "\n" +
                "Longitude: " + location.getLongitude() + "\n" +
                "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_details);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(UIUtils.getStyledAppName(this));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void displayDetails() {
        ((TextView) findViewById(R.id.tv_det_lat)).setText("Latitude: " + location.getLatitude());
        ((TextView) findViewById(R.id.tv_det_lng)).setText("Longitude: " + location.getLongitude());
        ((TextView) findViewById(R.id.tv_det_acc)).setText("Accuracy: " + location.getAccuracy() + " m");
        ((TextView) findViewById(R.id.tv_det_prov)).setText("Provider: " + location.getProvider());
        
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        SimpleDateFormat sdfFull = new SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault());

        ((TextView) findViewById(R.id.tv_det_date)).setText("Date: " + sdfDate.format(new Date(location.getTimestamp())));
        ((TextView) findViewById(R.id.tv_det_time)).setText("Time: " + sdfTime.format(new Date(location.getTimestamp())));
        ((TextView) findViewById(R.id.tv_det_created)).setText("Created At: " + sdfFull.format(new Date(location.getCreatedAt())));
    }

    private void copyToClipboard() {
        if (location == null) return;
        String text = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Location", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Coordinates copied", Toast.LENGTH_SHORT).show();
    }

    private void deleteRecord() {
        if (location == null) return;
        new Thread(() -> {
            database.locationDao().deleteLocation(location);
            runOnUiThread(() -> {
                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
