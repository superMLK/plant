package com.example.plant_identify_map;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PlantEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "plant_name")
    private String plantName;

    @ColumnInfo(name = "plant_type")
    private String plantType;

    @ColumnInfo(name = "plant_info")
    private String plantInfo;

    public PlantEntity(String plantName, String plantType, String plantInfo) {
        this.plantName = plantName;
        this.plantType = plantType;
        this.plantInfo = plantInfo;
    }

    public int getId() {
        return id;
    }

    public String getPlantName() {
        return plantName;
    }

    public String getPlantType() {
        return plantType;
    }

    public String getPlantInfo() {
        return plantInfo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public void setPlantInfo(String plantInfo) {
        this.plantInfo = plantInfo;
    }
// 建構函式、Getter 和 Setter 省略...
}
