package com.example.plant_identify_map;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlantDao {
    @Query("SELECT * FROM PlantEntity")
    List<PlantEntity> getAllPlants();

    @Insert
    void insert(PlantEntity plant);

    @Query("SELECT * FROM PlantEntity WHERE plant_name = :name")
    PlantEntity getPlantByName(String name);

    @Query("DELETE FROM PlantEntity")
    void deleteAll();
    // 其他操作方法...
}
