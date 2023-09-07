package com.example.plant_identify_map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.tensorflow.lite.Interpreter;

//import android.content.pm.PackageManager;

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


public class MainActivity extends AppCompatActivity implements LocationListener {

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
    private ImageView selected_image;   //imageview物件
    private Button classify_button;     //辨識按鈕
    private Button choose;      //選擇圖片按鈕
    private Button introbutton;     //進入APP介紹按鈕
    private Button clear;   //清除按鈕
    private LocationManager locationManager;  //定位管理器
    //類型顯示
    private TextView label1;
    private TextView label2;
    private TextView label3;
    //信心指數(機率)顯示
    private TextView Confidence1;
    private TextView Confidence2;
    private TextView Confidence3;
    private TextView locationText;

    public double latitude;   //緯度
    public double longitude;   //經度


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(getWindow().FEATURE_NO_TITLE);     //設定沒有title
        setContentView(R.layout.activity_main);

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
        label1 = (TextView) findViewById(R.id.label1);
        label2 = (TextView) findViewById(R.id.label2);
        label3 = (TextView) findViewById(R.id.label3);
        Confidence1 = (TextView) findViewById(R.id.Confidence1);
        Confidence2 = (TextView) findViewById(R.id.Confidence2);
        Confidence3 = (TextView) findViewById(R.id.Confidence3);
        classify_button = (Button) findViewById(R.id.classify_image);
        choose = (Button) findViewById(R.id.choose);
        introbutton = (Button) findViewById(R.id.introbutton);
        clear = (Button) findViewById(R.id.clear);
        selected_image = (ImageView) findViewById(R.id.selected_image);

        locationText = (TextView) findViewById(R.id.location);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        label1.setVisibility(View.GONE);
        label2.setVisibility(View.GONE);
        label3.setVisibility(View.GONE);
        Confidence1.setVisibility(View.GONE);
        Confidence2.setVisibility(View.GONE);
        Confidence3.setVisibility(View.GONE);

        //清除按鈕功能
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                label1.setText("");
                label2.setText("");
                label3.setText("");
                Confidence1.setText("");
                Confidence2.setText("");
                Confidence3.setText("");
                locationText.setText("");
                selected_image.setImageBitmap(null);
                Toast.makeText(MainActivity.this,"已清除",Toast.LENGTH_SHORT).show();
            }
        });

        //選擇圖片按鈕功能
        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //檢查SDK版本
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 檢查讀取權限和相機權限
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
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

        //辨識按鈕功能
        classify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Bitmap bitmap = ((BitmapDrawable) selected_image.getDrawable()).getBitmap();    //取得圖片
                    Bitmap bitmapIMAGE = getResizeBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);   //resize圖片
                    converBitmapToByBuffer(bitmapIMAGE);    //轉換模型輸入格式
                    tflite.run(imgData, labelProbArray);    //啟動模型
                    ptintTopKLabels();      //顯示結果

                    if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    }

                    //取得經緯度
                    Location location = getLastKnownLocation();
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    locationText.setText("緯度: " + latitude + "\n經度: " + longitude);



                }catch (Exception e){
                    Toast toast = Toast.makeText(getApplicationContext(), "請選擇圖片", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.NO_GRAVITY, 0, 0);
                    toast.show();//如果沒有選擇圖片則顯示"請選擇圖片"
                }
            }
        });

        //地圖按鈕功能
        introbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,mapactivity.class);
                Bundle bundle = new Bundle();
                bundle.putDouble("latitude",latitude);
                bundle.putDouble("longitude",longitude);
                intent.putExtras(bundle);
                setResult(Activity.RESULT_OK,intent);
                startActivity(intent);
            }
        });

    }

    //創建選單
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        //進入關於
        if(id == R.id.about){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("關於");
            builder.setMessage(" 成員：李偉聖、趙昱凱、翁安邑");
            builder.setPositiveButton("確定",null);
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    */

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
                System.exit(0);;
            }
        return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    //PackageManager packageManager = getPackageManager();
    //Cropper啟動選取器並啟動裁剪
    private void bringImagePicker() {
        PackageManager packageManager = getPackageManager();
        CropImage.getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, true);
        CropImage.activity()
                .setInitialCropWindowPaddingRatio(0)    //初始化裁剪窗口為全部
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)    //設定比例為1：1
                .setMinCropResultSize(  300,  300)      //設定最小剪裁尺寸
                .setRequestedSize( 300, 300)
                .setAllowCounterRotation(false)     //關閉反向旋轉
                .setAllowFlipping(false)        //關閉翻轉
                .setAllowRotation(false)        //關閉迴轉
                .setCropShape(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? CropImageView.CropShape.RECTANGLE : CropImageView.CropShape.OVAL)    //設置剪裁形狀
                .start(MainActivity.this);
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
                    selected_image.setImageBitmap(bitmap);
                }catch (IOException e){
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error =result.getError();
                label1.setText("Error Loafing Image" + error);
            }
        }
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
        label1.setText("1. "+topLabels[2]);
        label2.setText("2. "+topLabels[1]);
        label3.setText("3. "+topLabels[0]);
        Confidence1.setText(topConfidence[2]);
        Confidence2.setText(topConfidence[1]);
        Confidence3.setText(topConfidence[0]);
    }

    //讀取tflite模型
    private MappedByteBuffer loadModelFile() throws IOException,IllegalArgumentException {
        //AssetFileDescriptor fileDescriptor = this.getAssets().openFd("MobileNet.tflite");
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("modelXP.tflite");  //模型匯入
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