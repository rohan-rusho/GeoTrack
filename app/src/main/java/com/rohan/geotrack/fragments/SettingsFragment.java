package com.rohan.geotrack.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.rohan.geotrack.R;
import com.rohan.geotrack.database.GeoTrackDatabase;
import com.rohan.geotrack.utils.PreferenceManager;

public class SettingsFragment extends Fragment {
    private PreferenceManager preferenceManager;
    private RadioGroup rgInterval;
    private TextInputEditText etCustomValue;
    private AutoCompleteTextView spinnerUnit;
    private View llCustomInput, llIntervalActions;
    private MaterialSwitch switchToast, switchAutoStart;
    private TextView tvPermLoc, tvPermBg, tvPermNotif, tvBatteryStatus;
    private boolean isInitialized = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        preferenceManager = new PreferenceManager(requireContext());
        initViews(view);
        return view;
    }

    private void initViews(View v) {
        rgInterval = v.findViewById(R.id.rg_interval);
        llCustomInput = v.findViewById(R.id.ll_custom_input);
        llIntervalActions = v.findViewById(R.id.ll_interval_actions);
        etCustomValue = v.findViewById(R.id.et_custom_value);
        spinnerUnit = v.findViewById(R.id.spinner_unit);
        switchToast = v.findViewById(R.id.switch_toast);
        switchAutoStart = v.findViewById(R.id.switch_auto_start);
        tvPermLoc = v.findViewById(R.id.tv_perm_loc);
        tvPermBg = v.findViewById(R.id.tv_perm_bg);
        tvPermNotif = v.findViewById(R.id.tv_perm_notif);
        tvBatteryStatus = v.findViewById(R.id.tv_battery_status);
        Button btnSaveInterval = v.findViewById(R.id.btn_interval_save);
        Button btnCancelInterval = v.findViewById(R.id.btn_interval_cancel);
        Button btnExit = v.findViewById(R.id.btn_exit_app);

        String[] units = {"sec", "min", "hr"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, units);
        spinnerUnit.setAdapter(adapter);

        rgInterval.setOnCheckedChangeListener((group, checkedId) -> {
            llCustomInput.setVisibility(checkedId == R.id.rb_custom ? View.VISIBLE : View.GONE);
            if (isInitialized) {
                llIntervalActions.setVisibility(View.VISIBLE);
            }
        });

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInitialized) {
                    llIntervalActions.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };
        etCustomValue.addTextChangedListener(watcher);
        spinnerUnit.addTextChangedListener(watcher);
        
        switchToast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialized) preferenceManager.setShowToast(isChecked);
        });
        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialized) preferenceManager.setAutoStart(isChecked);
        });

        btnSaveInterval.setOnClickListener(v1 -> {
            saveIntervalSettings();
            llIntervalActions.setVisibility(View.GONE);
        });
        btnCancelInterval.setOnClickListener(v1 -> {
            loadSettings();
            llIntervalActions.setVisibility(View.GONE);
        });

        btnExit.setOnClickListener(v1 -> showExitConfirmation());
        v.findViewById(R.id.card_permissions).setOnClickListener(v1 -> requestMissingPermissions());
        v.findViewById(R.id.card_battery_optimization).setOnClickListener(v1 -> openBatteryOptimizationSettings());
        
        v.findViewById(R.id.btn_reset_data).setOnClickListener(v1 -> showResetDataConfirmation());
        
        loadSettings();
        isInitialized = true;
    }

    private void showResetDataConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reset Data")
                .setMessage("Are you sure you want to delete all tracked location history? This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    new Thread(() -> {
                        GeoTrackDatabase.getInstance(requireContext()).locationDao().deleteAllLocations();
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "All data deleted", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveIntervalSettings() {
        int interval = 300;
        int checkedId = rgInterval.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_5) interval = 300;
        else if (checkedId == R.id.rb_10) interval = 600;
        else if (checkedId == R.id.rb_15) interval = 900;
        else if (checkedId == R.id.rb_30) interval = 1800;
        else if (checkedId == R.id.rb_custom) {
            String valStr = etCustomValue.getText().toString();
            if (!valStr.isEmpty()) {
                int val = Integer.parseInt(valStr);
                String unit = spinnerUnit.getText().toString();
                if (unit.equals("min")) val *= 60;
                else if (unit.equals("hr")) val *= 3600;
                interval = val;
            }
        }
        preferenceManager.setTrackingInterval(interval);
        preferenceManager.setLastSaveTime(System.currentTimeMillis());
        
        Intent serviceIntent = new Intent(requireContext(), com.rohan.geotrack.service.LocationTrackingService.class);
        serviceIntent.setAction(com.rohan.geotrack.utils.Constants.ACTION_UPDATE_INTERVAL);
        requireContext().startService(serviceIntent);

        Toast.makeText(requireContext(), "Interval updated and countdown reset", Toast.LENGTH_SHORT).show();
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> requireActivity().finishAffinity())
                .setNegativeButton("No", null)
                .show();
    }

    private void openBatteryOptimizationSettings() {
        PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(requireContext().getPackageName())) {
            Toast.makeText(requireContext(), "Already unrestricted", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent();
        try {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        }
    }

    private void requestMissingPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Background Location")
                    .setMessage("Please select 'Allow all the time' to enable tracking.")
                    .setPositiveButton("Configure", (dialog, which) -> {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 102);
                    })
                    .show();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 103);
        } else {
            Toast.makeText(requireContext(), "All permissions granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBatteryStatus() {
        PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());
            tvBatteryStatus.setText(isIgnoring ? "Unrestricted" : "Optimized");
            tvBatteryStatus.setTextColor(ContextCompat.getColor(requireContext(), isIgnoring ? R.color.success : R.color.error));
        }
    }

    private void loadSettings() {
        boolean wasInitialized = isInitialized;
        isInitialized = false;
        int interval = preferenceManager.getTrackingInterval();
        if (interval == 300) rgInterval.check(R.id.rb_5);
        else if (interval == 600) rgInterval.check(R.id.rb_10);
        else if (interval == 900) rgInterval.check(R.id.rb_15);
        else if (interval == 1800) rgInterval.check(R.id.rb_30);
        else {
            rgInterval.check(R.id.rb_custom);
            if (interval % 3600 == 0) {
                etCustomValue.setText(String.valueOf(interval / 3600));
                spinnerUnit.setText("hr", false);
            } else if (interval % 60 == 0) {
                etCustomValue.setText(String.valueOf(interval / 60));
                spinnerUnit.setText("min", false);
            } else {
                etCustomValue.setText(String.valueOf(interval));
                spinnerUnit.setText("sec", false);
            }
        }
        switchToast.setChecked(preferenceManager.isShowToast());
        switchAutoStart.setChecked(preferenceManager.isAutoStart());
        isInitialized = wasInitialized;
    }

    private void updatePermissionStatus() {
        boolean locGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        tvPermLoc.setText(locGranted ? "Granted" : "Denied");
        tvPermLoc.setTextColor(ContextCompat.getColor(requireContext(), locGranted ? R.color.success : R.color.error));
        boolean bgGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        tvPermBg.setText(bgGranted ? "Granted" : "Denied");
        tvPermBg.setTextColor(ContextCompat.getColor(requireContext(), bgGranted ? R.color.success : R.color.error));
        boolean notifGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        tvPermNotif.setText(notifGranted ? "Granted" : "Denied");
        tvPermNotif.setTextColor(ContextCompat.getColor(requireContext(), notifGranted ? R.color.success : R.color.error));
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionStatus();
        updateBatteryStatus();
    }
}
