package com.rohan.geotrack.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_records")
public class LocationEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private double latitude;
    private double longitude;
    private float accuracy;
    private String provider;
    private long timestamp;
    private long createdAt;

    public LocationEntity(double latitude, double longitude, float accuracy, String provider, long timestamp, long createdAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.provider = provider;
        this.timestamp = timestamp;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
