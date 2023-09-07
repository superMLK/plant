package com.example.plant_identify_map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;


public class mapFragment extends Fragment {
    private GoogleMap map;

    private MarkerDatabase markerDatabase;
    private MarkerDao markerDao;

    private PlantDao plantDao;
    private PlantDatabase plantDatabase;

    private double markerlat;
    private double markerlng;
    private String markerName;
    private String plant = null;

    private Marker newmarker;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;

    private Button addmarker;
    private Button deletemarker;
    private Button info;
    private Button only;

    private Marker selectedMarker = null;

    private List<Marker> markerList = new ArrayList<>(); // 建立 List<Marker> 用於存儲 Marker 物件

    private List<Marker> markerIdenList = new ArrayList<>();

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        // 恢復地圖的 UI 和功能
    }

    @Override
    public void onPause() {
        super.onPause();
        // 暫停地圖的 UI 和功能
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 釋放地圖使用的資源
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        markerDatabase = Room.databaseBuilder(getActivity(), MarkerDatabase.class, "marker_database")
                .allowMainThreadQueries()
                .build();
        markerDao = markerDatabase.markerDao();

        plantDatabase = Room.databaseBuilder(getActivity(), PlantDatabase.class, "plant_database")
                .allowMainThreadQueries()
                .build();
        plantDao = plantDatabase.plantDao();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment supportmapfragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.MY_MAP);
        supportmapfragment.getMapAsync(this::onMapReady);

        addmarker = view.findViewById(R.id.addmarker);
        deletemarker = view.findViewById(R.id.deletemarker);
        info = view.findViewById(R.id.info);
        only = view.findViewById(R.id.only);
        deletemarker.setVisibility(View.GONE);
        info.setVisibility(View.GONE);

        addmarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = getArguments(); //從Activity取得辨識結果
                if (bundle != null) {
                    plant = bundle.getString("plant");
                    getLocation();
                    //initializationMarker();
                }
                else{
                    Toast.makeText(getActivity(), "請按下辨識按鈕", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deletemarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedMarker != null) {
                    MarkerEntity markerEntity = new MarkerEntity(markerName, markerlat, markerlng);
                    markerDao.deleteByLatLng(markerlat, markerlng);
                    map.clear();
                    initializationMarker();
                    Toast.makeText(getActivity(), "已刪除所選擇的標記", Toast.LENGTH_SHORT).show();
                    deletemarker.setVisibility(View.GONE);
                    info.setVisibility(View.GONE);
                }
                else{
                    Toast.makeText(getActivity(), "請選擇要刪除的標記", Toast.LENGTH_SHORT).show();
                }
            }
        });

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /////////////////////////////先在mapactivity辨識完後，然後直接顯示Bottom Sheet Dialog，接著就是只有點擊標記才會顯示Bottom Sheet Dialog
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity());
                bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog);

                TextView plantName = bottomSheetDialog.findViewById(R.id.info_title);
                TextView plantInfo = bottomSheetDialog.findViewById(R.id.textView);

                plantName.setText(markerName);

                PlantEntity planteee = plantDao.getPlantByName(markerName);
                String plantInfoeee = planteee.getPlantInfo();
                plantInfo.setText(plantInfoeee);

                bottomSheetDialog.show();
            }
        });

        only.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = getArguments(); //從Activity取得辨識結果
                if (bundle != null) {
                    plant = bundle.getString("plant");
                    hideIdentifyMarkers();
                    Toast.makeText(getActivity(), plant, Toast.LENGTH_SHORT).show();
                    //initializationMarker();
                }
                else{
                    Toast.makeText(getActivity(), "請按下辨識按鈕", Toast.LENGTH_SHORT).show();
                }
            }
        });


        return view;
    }

    @SuppressLint("MissingPermission")
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        //環境設定
        map.getUiSettings().setZoomControlsEnabled(false);//禁用右下角加減號
        map.getUiSettings().setCompassEnabled(true);//啟用指南針
        map.getUiSettings().setMapToolbarEnabled(false);//將工具欄禁用
        LatLng po = new LatLng(24.144610, 120.730310);

        //宿舍 24.145217, 120.727493 中間 24.144610, 120.730310 工業工程 24.144290, 120.727862 青永館 24.144167, 120.727682 工程館 24.144101, 120.728814 行政大樓 24.143983, 120.729579
        //圖書館 24.143843, 120.729689 24.143769, 120.729898 國秀樓 24.143590, 120.732202 二停 24.144849, 120.728885 明秀湖 24.145044, 120.728492 機械館 24.143970, 120.730834 管理館 24.144059, 120.731496

        float zoomLevel = 18.0f;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(po, zoomLevel));//移動鏡頭到勤益

        map.setMyLocationEnabled(true);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                markerName = marker.getTitle();
                markerlat = marker.getPosition().latitude;
                markerlng = marker.getPosition().longitude;
                selectedMarker = marker;
                deletemarker.setVisibility(View.VISIBLE);
                info.setVisibility(View.VISIBLE);
                return false;
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                selectedMarker = null;
                deletemarker.setVisibility(View.GONE);
                info.setVisibility(View.GONE);
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                float zoom = map.getCameraPosition().zoom;

                if(zoom < 19.0f) {
                    hideSpecificMarkers();
                }
                else{
                    showSpecificMarkers();
                }

            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());//

                        PlantEntity plante = plantDao.getPlantByName(plant);
                        String plantType = plante.getPlantType();

                        BitmapDescriptor icon;
                        // 根據植物種類設置對應的圖標顏色
                        switch (plantType) {
                            case "花":
                                icon = BitmapDescriptorFactory.fromResource(R.drawable.red15);
                                break;
                            case "葉":
                                icon = BitmapDescriptorFactory.fromResource(R.drawable.gre15);
                                break;
                            case "樹":
                                icon = BitmapDescriptorFactory.fromResource(R.drawable.blue25);
                                break;
                            default:
                                icon = BitmapDescriptorFactory.fromResource(R.drawable.yel25);
                                break;
                        }

                        newmarker = map.addMarker(new MarkerOptions().position(latLng).title(plant).icon(icon).anchor(0.5f, 0.5f));//

                        markerDao.insert(new MarkerEntity(plant, location.getLatitude(), location.getLongitude()));//
                        initializationMarker();

                        newmarker.setDraggable(true);

                        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                            @Override
                            public void onMarkerDragStart(@NonNull Marker marker) {
                                Toast.makeText(getActivity(), "拖曳中...", Toast.LENGTH_SHORT).show();
                                //initializationMarker();
                            }

                            @Override
                            public void onMarkerDrag(@NonNull Marker marker) {

                            }

                            @Override
                            public void onMarkerDragEnd(@NonNull Marker marker) {
                                markerlat = marker.getPosition().latitude;
                                markerlng = marker.getPosition().longitude;
                                markerDao.insert(new MarkerEntity(plant, markerlat, markerlng));///////////////////////////////////
                                initializationMarker();
                            }
                        });

                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20.0f));
                        Toast.makeText(getActivity(), "已添加 " + plant + " 的位置到地圖上", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(getActivity(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    public void initializationMarker() {
        List<MarkerEntity> markers = markerDao.getAll();
        markerList.clear();

        for (MarkerEntity marker : markers) {
            LatLng position = new LatLng(marker.latitude, marker.longitude);
            String name = marker.name;

            PlantEntity plantee = plantDao.getPlantByName(name);
            String plantType = plantee.getPlantType();
            //Toast.makeText(getActivity(), plantType, Toast.LENGTH_SHORT).show();

            BitmapDescriptor icon;
            // 根據植物種類設置對應的圖標顏色
            switch (plantType) {
                case "花":
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.red15);
                    break;
                case "葉":
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.gre15);
                    break;
                case "樹":
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.blue25);
                    break;
                default:
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.yel25);
                    break;
            }

            Marker markerObj = map.addMarker(new MarkerOptions().position(position).title(name).icon(icon).anchor(0.5f, 0.5f));
            markerList.add(markerObj); // 將 Marker 物件加入 List<Marker>
        }
    }

    public Bitmap getResizeBitmap(Bitmap bm, int newWidth, int newHeight) {
        //長寬
        int width = bm.getWidth();
        int height = bm.getHeight();
        //縮放
        float scaleWidth = ((float) newWidth) / width;
        float scalHeight = ((float) newHeight) / height;
        //矩陣化
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scalHeight);
        //resize bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm,  0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public void hideSpecificMarkers() {
        for (Marker marker : markerList) {
            // 在這裡檢查每個圖標是否屬於要隱藏的圖標
            if(!marker.getTitle().contains("樹")) {
                marker.setVisible(false);
            }
        }
    }

    public void hideIdentifyMarkers() {
        markerIdenList.clear();
        if(plant != null) {
            //Toast.makeText(getActivity(), plant, Toast.LENGTH_SHORT).show();
            for (Marker marker : markerList) {
                // 在這裡檢查每個圖標是否屬於要隱藏的圖標
                if(marker.getTitle().contains(plant)) {
                    markerIdenList.add(marker);
                }
            }

            for (Marker marker : markerList) {
              if (!markerIdenList.contains(marker)) {
                  marker.setVisible(false);
              }
            }
        }
    }

    public void showSpecificMarkers() {
        for (Marker marker : markerList) {
            // 在這裡檢查每個圖標是否屬於要隱藏的圖標
            marker.setVisible(true);
        }
    }
}