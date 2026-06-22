package com.rohan.geotrack.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rohan.geotrack.R;
import com.rohan.geotrack.activities.HistoryDetailsActivity;
import com.rohan.geotrack.adapter.HistoryAdapter;
import com.rohan.geotrack.database.GeoTrackDatabase;
import com.rohan.geotrack.database.LocationEntity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<LocationEntity> allLocations = new ArrayList<>();
    private List<LocationEntity> filteredLocations = new ArrayList<>();
    private GeoTrackDatabase database;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            handler.postDelayed(this, 60 * 1000L); // Refresh every minute
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        database = GeoTrackDatabase.getInstance(requireContext());
        setupRecyclerView(view);
        setupSearch(view);
        setupFilters(view);
        observeDatabase();
        handler.post(timeRefreshRunnable);
        
        return view;
    }

    private void observeDatabase() {
        database.locationDao().getAllLocationsLive().observe(getViewLifecycleOwner(), locations -> {
            boolean isNewAddition = locations.size() > allLocations.size() && !allLocations.isEmpty();
            allLocations.clear();
            allLocations.addAll(locations);
            
            if (isNewAddition) {
                refreshFilter(true);
            } else {
                refreshFilter(false);
            }
        });
    }

    private void refreshFilter(boolean isNewAddition) {
        View v = getView();
        if (v == null) return;
        com.google.android.material.chip.ChipGroup chipGroup = v.findViewById(R.id.chip_group);
        int selectedId = chipGroup.getCheckedChipId();
        if (selectedId != View.NO_ID) {
            applyFilter(selectedId, isNewAddition);
        } else {
            applyFilter(R.id.chip_all, isNewAddition);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timeRefreshRunnable);
    }

    private void setupFilters(View v) {
        com.google.android.material.chip.ChipGroup chipGroup = v.findViewById(R.id.chip_group);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                applyFilter(checkedIds.get(0), false);
            }
        });
    }

    private void setupSearch(View v) {
        EditText etSearch = v.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterData(etSearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void filterData(String query) {
        new Thread(() -> {
            List<LocationEntity> result = new ArrayList<>();
            for (LocationEntity loc : allLocations) {
                String date = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date(loc.getTimestamp()));
                String time = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date(loc.getTimestamp()));
                if (date.toLowerCase().contains(query.toLowerCase()) || time.toLowerCase().contains(query.toLowerCase())) {
                    result.add(loc);
                }
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    filteredLocations.clear();
                    filteredLocations.addAll(result);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                        if (recyclerView != null) {
                            recyclerView.scheduleLayoutAnimation();
                        }
                    }
                });
            }
        }).start();
    }

    private void setupRecyclerView(View v) {
        recyclerView = v.findViewById(R.id.rv_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        android.view.animation.LayoutAnimationController animation = 
            android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), 
            R.anim.layout_animation);
        recyclerView.setLayoutAnimation(animation);

        adapter = new HistoryAdapter(filteredLocations, location -> {
            Intent intent = new Intent(requireContext(), HistoryDetailsActivity.class);
            intent.putExtra("location_id", location.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void applyFilter(int chipId, boolean isNewAddition) {
        if (!isAdded()) return;
        long now = System.currentTimeMillis();
        long filterStartTime = 0;
        
        if (chipId == R.id.chip_hour) filterStartTime = now - (3600 * 1000);
        else if (chipId == R.id.chip_today) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            filterStartTime = cal.getTimeInMillis();
        }
        else if (chipId == R.id.chip_7days) filterStartTime = now - (7L * 24 * 3600 * 1000);
        else if (chipId == R.id.chip_30days) filterStartTime = now - (30L * 24 * 3600 * 1000);

        List<LocationEntity> result = new ArrayList<>();
        for (LocationEntity loc : allLocations) {
            if (filterStartTime == 0 || loc.getCreatedAt() >= filterStartTime) {
                result.add(loc);
            }
        }
        
        if (isNewAddition && !result.isEmpty() && filteredLocations.size() < result.size()) {
            filteredLocations.clear();
            filteredLocations.addAll(result);
            if (adapter != null) {
                adapter.notifyItemInserted(0);
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(0);
                }
            }
        } else {
            filteredLocations.clear();
            filteredLocations.addAll(result);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                if (recyclerView != null) {
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
