package com.rohan.geotrack;

import android.Manifest;
import android.graphics.Color;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.rohan.geotrack.activities.AboutActivity;
import com.rohan.geotrack.fragments.HistoryFragment;
import com.rohan.geotrack.fragments.HomeFragment;
import com.rohan.geotrack.fragments.SettingsFragment;
import com.rohan.geotrack.service.LocationTrackingService;
import com.rohan.geotrack.utils.PreferenceManager;
import com.rohan.geotrack.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private PreferenceManager preferenceManager;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceManager = new PreferenceManager(this);
        setupNavigation();
        checkAllRequiredPermissions();
        handleIntent(getIntent());

        // Handle Back Button
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(0);
                } else {
                    showExitConfirmation();
                }
            }
        });

        autoStartTrackingIfFirstRun();
    }

    private void autoStartTrackingIfFirstRun() {
        if (preferenceManager.isFirstRun() || (preferenceManager.isAutoStart() && !preferenceManager.isTracking())) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
                if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    // Location is disabled, prompt user
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Location Disabled")
                            .setMessage("Device location is turned off. Please enable it to start tracking.")
                            .setPositiveButton("Enable", (dialog, which) -> {
                                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return;
                }

                Intent serviceIntent = new Intent(this, LocationTrackingService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                preferenceManager.setFirstRun(false);
                Toast.makeText(this, "Tracking started automatically", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showExitConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", null)
                .show();
    }


    private void checkAllRequiredPermissions() {
        boolean locDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean bgLocDenied = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgLocDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED;
        }
        boolean notifDenied = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        }
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
        boolean batteryDenied = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            batteryDenied = !pm.isIgnoringBatteryOptimizations(getPackageName());
        }

        if (locDenied || bgLocDenied || notifDenied || batteryDenied) {
            showUnifiedPermissionDialog(locDenied, bgLocDenied, notifDenied, batteryDenied);
        }
    }

    private void showUnifiedPermissionDialog(boolean loc, boolean bg, boolean notif, boolean batt) {
        StringBuilder message = new StringBuilder("GeoTrack needs the following to work perfectly:\n");
        if (loc) message.append("\n• Location Access (Mandatory)");
        if (bg) message.append("\n• Background Location (Mandatory)");
        if (notif) message.append("\n• Notifications (Recommended)");
        if (batt) message.append("\n• Unrestricted Battery (Recommended)");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage(message.toString())
                .setPositiveButton("Grant Now", (dialog, which) -> {
                    if (loc) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
                    } else if (bg) {
                        requestBackgroundLocation();
                    } else if (notif) {
                        requestNotificationPermission();
                    } else if (batt) {
                        requestBatteryOptimization();
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Background Location")
                    .setMessage("Please select 'Allow all the time' in the next screen to enable tracking while the app is closed.")
                    .setPositiveButton("Configure", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 102);
                    })
                    .show();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 103);
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("select_tab")) {
            int tabIndex = intent.getIntExtra("select_tab", 0);
            viewPager.setCurrentItem(tabIndex, false);
        }
    }

    private void setupNavigation() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(UIUtils.getStyledAppName(this));
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set styled app name in navigation header
        android.view.View headerView = navigationView.getHeaderView(0);
        android.widget.TextView tvNavAppName = headerView.findViewById(R.id.tv_nav_app_name);
        if (tvNavAppName != null) {
            tvNavAppName.setText(UIUtils.getStyledAppName(this));
        }

        android.view.View exitHeader = headerView.findViewById(R.id.btn_exit_header);
        if (exitHeader != null) {
            exitHeader.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showExitConfirmation();
            });
        }

        // Make Exit Application red
        MenuItem exitItem = navigationView.getMenu().findItem(R.id.drawer_exit);
        if (exitItem != null) {
            android.text.SpannableString s = new android.text.SpannableString(exitItem.getTitle());
            s.setSpan(new android.text.style.ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            exitItem.setTitle(s);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                exitItem.setIconTintList(android.content.res.ColorStateList.valueOf(Color.RED));
            } else if (exitItem.getIcon() != null) {
                androidx.core.graphics.drawable.DrawableCompat.setTint(exitItem.getIcon(), Color.RED);
            }
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Ensure the hamburger menu button is white
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_navigation);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new HomeFragment());
        fragments.add(new HistoryFragment());
        fragments.add(new SettingsFragment());

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments.get(position);
            }

            @Override
            public int getItemCount() {
                return fragments.size();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNav.setSelectedItemId(R.id.nav_home); break;
                    case 1: bottomNav.setSelectedItemId(R.id.nav_history); break;
                    case 2: bottomNav.setSelectedItemId(R.id.nav_settings); break;
                }
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) viewPager.setCurrentItem(0);
            else if (id == R.id.nav_history) viewPager.setCurrentItem(1);
            else if (id == R.id.nav_settings) viewPager.setCurrentItem(2);
            return true;
        });
    }



    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.drawer_home) {
            viewPager.setCurrentItem(0);
        } else if (id == R.id.drawer_history) {
            viewPager.setCurrentItem(1);
        } else if (id == R.id.drawer_settings) {
            viewPager.setCurrentItem(2);
        } else if (id == R.id.drawer_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.drawer_exit) {
            showExitConfirmation();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (granted) {
                autoStartTrackingIfFirstRun();
            } else {
                Toast.makeText(this, "Location permissions are required for tracking.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 102) {
            if (!granted) {
                Toast.makeText(this, "Background location is recommended for better tracking.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 103) {
            if (!granted) {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
        
        // Re-check for other missing permissions/settings
        checkAllRequiredPermissions();
    }
}
