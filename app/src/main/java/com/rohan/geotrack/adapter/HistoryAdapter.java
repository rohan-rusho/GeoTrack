package com.rohan.geotrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rohan.geotrack.R;
import com.rohan.geotrack.database.LocationEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<LocationEntity> locations;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LocationEntity location);
    }

    public HistoryAdapter(List<LocationEntity> locations, OnItemClickListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationEntity location = locations.get(position);
        holder.bind(location, listener);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCoords, tvDate, tvTime, tvAccuracy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCoords = itemView.findViewById(R.id.tv_item_coords);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvAccuracy = itemView.findViewById(R.id.tv_item_accuracy);
        }

        public void bind(final LocationEntity location, final OnItemClickListener listener) {
            tvCoords.setText(String.format(Locale.getDefault(), "%.4f, %.4f", location.getLatitude(), location.getLongitude()));
            tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(location.getTimestamp())));
            tvTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(location.getTimestamp())));
            
            // Calculate "time ago" based on minute difference as per user request
            long now = System.currentTimeMillis();
            long recordTime = location.getTimestamp(); 

            long nowMin = now / 60000;
            long recordMin = recordTime / 60000;
            long diffMin = nowMin - recordMin;

            String timeAgo;
            if (diffMin <= 0) {
                timeAgo = "0m ago";
            } else if (diffMin < 60) {
                timeAgo = diffMin + "m ago";
            } else {
                long diffHour = diffMin / 60;
                if (diffHour < 24) {
                    timeAgo = diffHour + "h ago";
                } else {
                    long diffDay = diffHour / 24;
                    timeAgo = diffDay + "d ago";
                }
            }

            tvAccuracy.setText(timeAgo);
            
            itemView.setOnClickListener(v -> listener.onItemClick(location));
        }
    }
}
