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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String OCR_UPLOAD_URL = "http://59.110.234.164:8070/api/ocr/upFile";

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

        decodeReceipt("/sdcard/receipt/json/receipt1.jpg.json");
        decodeReceipt("/sdcard/receipt/json/receipt2.jpg.json");
        decodeReceipt("/sdcard/receipt/json/receipt3.jpg.json");
        decodeReceipt("/sdcard/receipt/json/receipt4.jpg.json");
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
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Utils.saveAssets2File(this);
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Utils.saveAssets2File(this);
        }*/
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
                displayReceipt(jsonFile, 1f, 0.4f);
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
                            runOnUiThread(() -> displayReceipt(jsonFile, 1f, 0.4f));
                        }
                    }
                });
            }
        }
    }

    private void decodeReceipt(String path) {
        ReceiptBean bean = Utils.decodeJson(path);

        Receipt receipt = new Receipt();
        receipt.mType = bean.type;
        receipt.mLogId = bean.log_id;

        final int type = bean.type;

        String datePattern = null;
        String timePattern = null;

        String shopPattern = null;
        int shopOffset;

        int totalPerceOffset = 0;

        if (type == Receipt.TYPE_MEITUAN) {
            datePattern = Receipt.DATE_PATTERN_1;
            timePattern = Receipt.TIME_PATTERN_5;
            shopPattern = "美团外卖";
            shopOffset = 1;
        } else if (type == Receipt.TYPE_ELE) {
            datePattern = Receipt.DATE_PATTERN_2;
            timePattern = Receipt.TIME_PATTERN_5;
            shopPattern = "饿了么订单";
            shopOffset = 1;
        } else if (type == Receipt.TYPE_WALMART) {
            datePattern = Receipt.DATE_PATTERN_3;
            timePattern = Receipt.TIME_PATTERN_5;
            shopPattern = "本店受理";
            shopOffset = 1;
        } else if (type == Receipt.TYPE_OTHER) {
            datePattern = Receipt.DATE_PATTERN_4;
            timePattern = Receipt.TIME_PATTERN_6;
            shopPattern = "欢迎光临";
            shopOffset = 0;
            totalPerceOffset = 1;
        } else {
            return;
        }

        //小票时间
        ReceiptBean.WordBean dateWord = match(bean, datePattern, 0);
        ReceiptBean.WordBean timeWord = match(bean, timePattern, 0);
        final String dateStr = dateWord != null ? match(datePattern, dateWord.words) : "";
        final String timeStr = timeWord != null ? match(timePattern, timeWord.words) : "";
        receipt.mTime = ((dateStr != null) ? dateStr : "") + ((timeStr != null) ? timeStr : "");
        //店铺名
        ReceiptBean.WordBean shopBean = match(bean, shopPattern, shopOffset);
        receipt.mShop = shopBean == null ? "" : (type != Receipt.TYPE_OTHER ? shopBean.words : shopBean.words.substring(4));
        //总计金额
        ReceiptBean.WordBean totalPriceBean = match(bean, Receipt.TOTAL_PRICE_PATTERN, totalPerceOffset);
        final String priceTitle = totalPriceBean != null ? match(Receipt.TOTAL_PRICE_PATTERN, totalPriceBean.words) : "";
        final String totalPrice = priceTitle != null ? totalPriceBean.words.substring(priceTitle.length()) : totalPriceBean.words;
        receipt.mActualPrice = Float.valueOf(totalPrice);
        //商品
        if (type == Receipt.TYPE_MEITUAN || type == Receipt.TYPE_ELE) {
            List<ReceiptBean.WordBean> goodsTemp = matches(bean, Receipt.GOODS_COUNT_PATTERN);
            for (ReceiptBean.WordBean goodsCountWord : goodsTemp) {
                int index = bean.words_result.indexOf(goodsCountWord);
                if (index >= 1 && index <= bean.words_result.size() - 2) {
                    ReceiptBean.WordBean goodsNameWord = bean.words_result.get(index - 1);
                    ReceiptBean.WordBean goodsPriceWord = bean.words_result.get(index + 1);

                    Receipt.Goods goods = receipt.new Goods();
                    goods.mName = goodsNameWord.words;
                    goods.price = matchNumber(goodsPriceWord.words);
                    int count = Integer.parseInt(goodsCountWord.words.substring(1));

                    receipt.mGoods.put(goods, count);
                }
            }
        } else if (type == Receipt.TYPE_WALMART) {
            int start = bean.words_result.indexOf(match(bean, "商店[0-9]*店员[0-9]*", 1));
            int end = bean.words_result.indexOf(totalPriceBean) - 1;

            for (;start <= end; start ++) {
                ReceiptBean.WordBean word = bean.words_result.get(start);
                String serial = match("[0-9]{10,}", word.words);
                if (!TextUtils.isEmpty(serial)) {
                    Receipt.Goods goods = receipt.new Goods();
                    goods.price = matchNumber(bean.words_result.get(start + 1).words);
                    if (serial.equals(word.words)) {
                        goods.mName = bean.words_result.get(start - 1).words;
                    } else {
                        goods.mName = word.words.substring(0, word.words.indexOf(serial));
                    }
                    receipt.mGoods.put(goods, 1);
                }
            }
        } else if (type == Receipt.TYPE_OTHER) {
            for (ReceiptBean.WordBean titleBean : matches(bean, "商品号[0-9]*")) {
                ReceiptBean.WordBean goodsName = bean.words_result.get(bean.words_result.indexOf(titleBean) + 1);
                ReceiptBean.WordBean goodsPrice = bean.words_result.get(bean.words_result.indexOf(titleBean) + 2);

                Receipt.Goods goods = receipt.new Goods();
                goods.mName = goodsName.words;
                goods.price = matchNumber(goodsPrice.words);

                receipt.mGoods.put(goods, 1);
            }
        }

        if (receipt.mGoods.size() > 0) {
            int count = 0;
            float price = 0;
            for (Receipt.Goods goods : receipt.mGoods.keySet()) {
                int cnt = receipt.mGoods.get(goods);
                float pce = goods.price;
                if (cnt > 0 && pce > 0) {
                    count += cnt;
                    price += pce * cnt;
                }
            }

            receipt.mTotalPrice = price;
            receipt.mTotalGoods = count;
        }
        Log.i(TAG, receipt.toString());
    }

    private ReceiptBean.WordBean match(ReceiptBean bean, String pattern, int offset) {
        if (bean.words_result_num > 0) {
            Pattern ptn = Pattern.compile(pattern);
            for (ReceiptBean.WordBean word : bean.words_result) {
                Matcher matcher = ptn.matcher(word.words);
                if (matcher.find()) {
                    int matchIndex = bean.words_result.indexOf(word);
                    return bean.words_result.get(matchIndex + offset);
                }
            }
        }
        return null;
    }

    private List<ReceiptBean.WordBean> matches(ReceiptBean bean, String pattern) {
        List<ReceiptBean.WordBean> beans = new ArrayList<>();
        if (bean.words_result_num > 0) {
            Pattern ptn = Pattern.compile(pattern);
            for (ReceiptBean.WordBean word : bean.words_result) {
                Matcher matcher = ptn.matcher(word.words);
                if (matcher.find()) {
                    beans.add(word);
                }
            }
        }
        return beans;
    }

    private String match(String pattern, String input) {
        Pattern ptn = Pattern.compile(pattern);
        Matcher matcher = ptn.matcher(input);
        if (matcher.find()) {
            return input.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private float matchNumber(String input) {
        Pattern ptn = Pattern.compile("^[1-9]*[0-9]?(\\.[0-9]{1,2})?$");
        Matcher mth = ptn.matcher(input);
        if (mth.find()) {
            return Float.parseFloat(input.substring(mth.start(), mth.end()));
        }

        return Float.MIN_VALUE;
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
