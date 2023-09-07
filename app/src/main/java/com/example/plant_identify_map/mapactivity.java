package com.example.plant_identify_map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class mapactivity extends AppCompatActivity implements LocationListener {
    public static final String DATA = "data";
    public static final String CONTENT = "content";
    public static int nIsReminder = 1;      //1下次啟動顯示，0不顯示
    public static int REMINDER_NO = 0;
    public static int REMINDER_YES = 1;
    private SupportMapFragment mapView;
    private GoogleMap mMap;
    private Button iden;
    private PlantDao plantDao;
    private PlantDatabase plantDatabase;
    Fragment fragment = new mapFragment();

    private Uri uri;    //保存uri
    ImageView imageView;
    //設定預設值
    private static final int RESULTS_TO_SHOW = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private Interpreter tflite;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();    //模型初始化設定
    private List<String> labelList;     //模型標籤的list
    private ByteBuffer imgData = null;  //圖片資料類型為Byte
    private  float[][] labelProbArray = null;   //保存標籤機率
    private  String[] topLabels = null;     //保存最高機率標籤
    private String[] topConfidence = null;      //最高機率標籤的array
    private String provider;
    //初始化成mobilenet規定的圖像尺寸
    private int DIM_IMG_SIZE_X = 224;
    private int DIM_IMG_SIZE_Y = 224;
    private int DIM_PIXEL_SIZE = 3;
    private int[] intValues;    //保存圖像數據
    //將辨識結果保存至sortedLabels
    private PriorityQueue<Map.Entry<String,Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> a1, Map.Entry<String, Float> a2) {
                            return (a1.getValue()).compareTo(a2.getValue());
                        }
                    });
    private ImageView selected_image11;   //imageview物件
    private LocationManager locationManager;  //定位管理器
    //類型顯示
    private TextView label11;
    private TextView label12;
    private TextView label13;
    //信心指數(機率)顯示
    private TextView Confidence11;
    private TextView Confidence12;
    private TextView Confidence13;
    private TextView locationText11;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(getWindow().FEATURE_NO_TITLE);     //設定沒有title
        setContentView(R.layout.activity_map);

        //確認GPS權限
        if(ActivityCompat.checkSelfPermission(mapactivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mapactivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(mapactivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(mapactivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        plantDatabase = Room.databaseBuilder(mapactivity.this, PlantDatabase.class, "plant_database")
                .allowMainThreadQueries()
                .build();
        plantDao = plantDatabase.plantDao();

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
        iden = (Button) findViewById(R.id.iden);

        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];   //圖像數據為224*224

        try{
            tflite = new org.tensorflow.lite.Interpreter(loadModelFile(),tfliteOptions);    //讀取模型檔案並且使用初始設定
            labelList = loadLabelList();    //讀取標籤list
        }catch(Exception e){
            e.printStackTrace();
        }

        //初始化Byte array大小
        imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        //初始化各機率array
        labelProbArray = new float[1][labelList.size()];
        topLabels = new String[RESULTS_TO_SHOW];
        topConfidence = new String[RESULTS_TO_SHOW];
        //指派物件
        label11 = (TextView) findViewById(R.id.label11);
        label12 = (TextView) findViewById(R.id.label12);
        label13 = (TextView) findViewById(R.id.label13);
        Confidence11 = (TextView) findViewById(R.id.Confidence11);
        Confidence12 = (TextView) findViewById(R.id.Confidence12);
        Confidence13 = (TextView) findViewById(R.id.Confidence13);
        //classify_button = (Button) findViewById(R.id.classify_image);
        //choose = (Button) findViewById(R.id.choose);
        //introbutton = (Button) findViewById(R.id.introbutton);
        //clear = (Button) findViewById(R.id.clear);
        selected_image11 = (ImageView) findViewById(R.id.selected_image11);
        locationText11 = (TextView) findViewById(R.id.location11);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //label11.bringToFront();
        label11.setBackgroundColor(Color.parseColor("#99000000"));


        iden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //檢查SDK版本
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 檢查讀取權限和相機權限
                    if(ContextCompat.checkSelfPermission(mapactivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(mapactivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mapactivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
                    } else {
                        // 啟動選取器並裁剪
                        bringImagePicker();
                    }
                } else {
                    // 啟動選取器並裁剪
                    bringImagePicker();
                }
            }
        });
    }


    //Cropper啟動選取器並啟動裁剪
    private void bringImagePicker() {
        PackageManager packageManager = getPackageManager();
        CropImage.getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, true);
        CropImage.activity()
                .setInitialCropWindowPaddingRatio(0)    //初始化裁剪窗口為全部
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(3,4)//設定比例為1：1
                .setMinCropResultSize(  300,  300)      //設定最小剪裁尺寸
                .setRequestedSize( 300, 300)
                .setAllowCounterRotation(false)     //關閉反向旋轉
                .setAllowFlipping(false)        //關閉翻轉
                .setAllowRotation(false)        //關閉迴轉
                .setCropShape(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? CropImageView.CropShape.RECTANGLE : CropImageView.CropShape.OVAL)    //設置剪裁形狀
                .start(mapactivity.this);
    }

    //覆蓋onActivityResult方法以獲取裁剪結果並展示至imageview
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {
                uri = result.getUri();
                Log.d("getUri", "getUri is OK:");
                try{
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    selected_image11.setImageBitmap(bitmap);
                }catch (IOException e){
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error =result.getError();
                label11.setText("Error Loafing Image" + error);
            }
        }

        Bitmap bitmap = ((BitmapDrawable) selected_image11.getDrawable()).getBitmap();    //取得圖片
        Bitmap bitmapIMAGE = getResizeBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);   //resize圖片
        converBitmapToByBuffer(bitmapIMAGE);    //轉換模型輸入格式
        tflite.run(imgData, labelProbArray);    //啟動模型
        ptintTopKLabels();      //顯示結果


        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mapactivity.this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog);

        TextView plantName = bottomSheetDialog.findViewById(R.id.info_title);
        TextView plantInfo = bottomSheetDialog.findViewById(R.id.textView);

        plantName.setText("辨識結果：" + topLabels[2]);

        PlantEntity plante = plantDao.getPlantByName(topLabels[2]);
        String plantInfoe = plante.getPlantInfo();
        plantInfo.setText(plantInfoe);

        bottomSheetDialog.show();

        //將經緯度傳至fragment
        Bundle bundle = new Bundle();
        //bundle.putDouble("latitude", latitude);
        //bundle.putDouble("longitude", longitude);
        bundle.putString("plant",topLabels[2]);
        fragment.setArguments(bundle);

    }

    //resize bitmap圖片
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

    //轉換bitmap至Byte
    private void converBitmapToByBuffer(Bitmap bitmap) {
        //檢查是否有圖片
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        //讀取所有像素並將其RGB值轉換成Byte
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
    }

    //顯示辨識結果
    private void ptintTopKLabels() {
        //保存結果至sortedLabels
        for (int i = 0;i<labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        //選取最好的結果並依信心指數依序排出
        final int size = sortedLabels.size();
        Log.d("result", String.format("size = %d", size));
        for (int i= 0; i<size; ++i) {
            Map.Entry<String, Float> label= sortedLabels.poll();
            Log.d("result", String.format(label.getKey()));
            topLabels[i] = label.getKey();
            topConfidence[i] = String.format("%.0f%%",label.getValue()*100);
        }

        //依序顯示結果
        label11.setText(topLabels[2]);
        label12.setText("2. "+topLabels[1]);
        label13.setText("3. "+topLabels[0]);
        Confidence11.setText(topConfidence[2]);
        Confidence12.setText(topConfidence[1]);
        Confidence13.setText(topConfidence[0]);

        //textView.setText(topLabels[2]);
    }

    //讀取tflite模型
    private MappedByteBuffer loadModelFile() throws IOException,IllegalArgumentException {
        //AssetFileDescriptor fileDescriptor = this.getAssets().openFd("MobileNet.tflite");
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("modelXX.tflite");  //模型匯入
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //讀取txt標籤
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                //new BufferedReader(new InputStreamReader(this.getAssets().open("Lung_Label.txt")));
                new BufferedReader(new InputStreamReader(this.getAssets().open("plant_label.txt")));   //標籤匯入
        String line;
        while((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation(){
        LocationProvider provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        Location location = locationManager.getLastKnownLocation(provider.getName());
        return location;
    }

    //按兩下才返回退出APP功能
    private long mExitTime;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if((System.currentTimeMillis() - mExitTime) > 2000) {
                Object mHelperUtils;
                Toast.makeText(this, "再按一次返回退出APP", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            }
            else {
                finish();
                System.exit(0);;
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}