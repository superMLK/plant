package com.example.plant_identify_map;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MarkerDao {
    @Query("SELECT * FROM MarkerEntity")
    List<MarkerEntity> getAll();

    @Insert
    void insert(MarkerEntity marker);

    @Query("DELETE FROM MarkerEntity WHERE latitude = :latitude AND longitude = :longitude")
    void deleteByLatLng(double latitude, double longitude);

    @Delete
    void delete(MarkerEntity marker);

    @Update
    void update(MarkerEntity marker);

    @Query("DELETE FROM MarkerEntity")
    void deleteAll();

    @Query("SELECT * FROM MarkerEntity WHERE id = :id")
    MarkerEntity getMarkerById(int id);
}
