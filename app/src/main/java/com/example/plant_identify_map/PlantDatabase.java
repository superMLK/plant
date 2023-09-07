package com.example.plant_identify_map;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PlantEntity.class}, version = 1)
public abstract class PlantDatabase extends RoomDatabase {
    public abstract PlantDao plantDao();

    private static PlantDatabase instance;

    public static synchronized PlantDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    PlantDatabase.class, "plant_database")
                    .build();
        }
        return instance;
    }
}

