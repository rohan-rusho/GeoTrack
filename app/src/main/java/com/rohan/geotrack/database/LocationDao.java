package com.rohan.geotrack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    void insertLocation(LocationEntity location);

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC")
    LiveData<List<LocationEntity>> getAllLocationsLive();

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC")
    List<LocationEntity> getAllLocations();

    @Delete
    void deleteLocation(LocationEntity location);

    @Query("DELETE FROM location_records")
    void deleteAllLocations();

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 1")
    LocationEntity getLatestLocation();

    @Query("SELECT COUNT(*) FROM location_records")
    int getLocationCount();

    @Query("SELECT * FROM location_records WHERE id = :id")
    LocationEntity getLocationById(int id);
}
