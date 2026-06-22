package com.rohan.geotrack.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LocationEntity.class}, version = 1, exportSchema = false)
public abstract class GeoTrackDatabase extends RoomDatabase {
    private static GeoTrackDatabase instance;

    public abstract LocationDao locationDao();

    public static synchronized GeoTrackDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    GeoTrackDatabase.class, "GeoTrackDatabase")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
