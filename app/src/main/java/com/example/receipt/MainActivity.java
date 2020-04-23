package com.example.receipt;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

        private static final String TAG = "MainActivity";

    public static final String OCR_UPLOAD_URL = "http://59.110.234.164:8070/api/ocr/upFile";

    private FrameLayout receiptContainer;

    private static final String[] NEEDED_PERMISSIONS = {
            Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        requestPermission();

        receiptContainer = findViewById(R.id.receipt_container);

        new DecodeTask().execute(this, "/sdcard/receipt/json/receipt1.jpg.json", null);
        new DecodeTask().execute(this, "/sdcard/receipt/json/receipt2.jpg.json", null);
        new DecodeTask().execute(this, "/sdcard/receipt/json/receipt3.jpg.json", null);
        new DecodeTask().execute(this, "/sdcard/receipt/json/receipt4.jpg.json", receiptContainer);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> request = new ArrayList();
            for (String item : NEEDED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                    request.add(item);
                }
            }
            if (request.isEmpty()) {
                /*Utils.saveAssets2File(this);*/
                return;
            }
            String[] items = new String[request.size()];
            for (String item : request) {
                items[request.indexOf(item)] = item;
            }
            ActivityCompat.requestPermissions(this, items, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 123) {
            final String path = data.getStringExtra("file_path");
            final int type = data.getIntExtra("type", 0);

            Log.i(TAG, "onActivityResult: " + path + ", type: " + Integer.toBinaryString(type));
            String fileName = new File(path).getName();
            final String jsonFile = "/sdcard/receipt/json/" + fileName + ".json";

            if (new File(jsonFile).exists()) {
                new DecodeTask().execute(this, jsonFile, findViewById(R.id.receipt_container));
                //displayReceipt(jsonFile, 1f, 0.4f);
                Log.i(TAG, "result already exist.");
            } else {
                Utils.uploadFile(path, OCR_UPLOAD_URL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.i(TAG, "onFailure: " + e.toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String result = response.body().string();
                        Log.i(TAG, "onResponse result: \n" + result);

                        ReceiptBean bean = new Gson().fromJson(result, ReceiptBean.class);
                        if (bean != null && bean.log_id > 0) {
                            bean.type = type;
                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.setPrettyPrinting();
                            Gson gson = gsonBuilder.create();

                            Utils.saveJson(gson.toJson(bean), jsonFile);
                            runOnUiThread(() -> new DecodeTask().execute(MainActivity.this, jsonFile, findViewById(R.id.receipt_container)));
                        }
                    }
                });
            }
        }
    }

    private void displayReceipt(String path, float sizeScale, float textScale) {
        ReceiptBean bean = Utils.decodeJson(path);
        FrameLayout container = findViewById(R.id.receipt_container);

        if (bean.words_result_num > 0) {
            int maxWidth = 0;
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            for (ReceiptBean.WordBean word : bean.words_result) {
                int width = word.location.left + word.location.width;
                maxWidth = width > maxWidth ? width : maxWidth;
            }

            float scale = ((float) screenWidth / (float) maxWidth);

            Log.i(TAG, "screenWidth: " + screenWidth + ", maxWidth: " + maxWidth + ", scale: " + scale);

            container.removeAllViews();
            for (ReceiptBean.WordBean word : bean.words_result) {
                TextView tv = new TextView(this);
                tv.setText(word.words);
                tv.setTextSize(word.location.height * textScale * scale);
                tv.setMinWidth((int) (word.location.width * sizeScale * scale));
                tv.setMinHeight((int) (word.location.height * sizeScale * scale));
                tv.setLineSpacing(1, 10);
                container.addView(tv);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tv.getLayoutParams();
                lp.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                lp.height = FrameLayout.LayoutParams.WRAP_CONTENT;

                lp.topMargin = (int) (word.location.top * sizeScale * scale);
                lp.leftMargin = (int) (word.location.left * sizeScale * scale);
                tv.setLayoutParams(lp);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_choose_pic) {
            startActivityForResult(new Intent(this, ChoosePicActivity.class), 123);
        }

        return super.onOptionsItemSelected(item);
    }
}
