package com.example.plant_identify_map;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MarkerEntity.class}, version = 3, exportSchema = false)
public abstract class MarkerDatabase extends RoomDatabase {
    public abstract MarkerDao markerDao();

    private static MarkerDatabase instance;

    public static synchronized MarkerDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    MarkerDatabase.class, "marker_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
