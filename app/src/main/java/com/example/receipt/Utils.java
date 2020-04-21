package com.example.receipt;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utils {

    private static final String TAG = "Utils";

    public static void uploadFile(String path, String url, Callback callback) {
        // 文件
        File mFile = new File(path);

        OkHttpClient client = new OkHttpClient.Builder().build();

        // 设置文件以及上传类型
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), mFile);

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("ocr", mFile.getName(), requestBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();

        Call call = client.newCall(request);

        call.enqueue(callback);
    }

    public static void saveJson(String data, String path) {
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Log.i(TAG, "save failure : sdcard not mounted");
            return;
        }

        Log.i(TAG, "savePath: " + path);
        try {
            File file = new File(path);

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileOutputStream out = new FileOutputStream(file);
            out.write(data.getBytes());
            out.close();
        } catch (IOException e) {
            Log.i(TAG, "testSave: " + e.getMessage());
        }
    }

    /**
     * 根据路径获取Bitmap图片
     *
     * @param path
     * @return
     */
    public static Bitmap getAssetsBitmap(Context context, String path) {
        AssetManager am = context.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = am.open(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }

    public static List<String> getAssetPics(Context context) {
        AssetManager am = context.getAssets();
        String[] path = null;
        try {
            path = am.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> pciPaths = new ArrayList<>();
        for (int i = 0; i < path.length; i++) {
            if (path[i].endsWith(".jpg")) {
                pciPaths.add(path[i]);
            }
        }
        return pciPaths;
    }

    public static List<String> getAvailablePics(Context context) {
        File folder = new File("/sdcard/receipt/");

        List<String> pciPaths = new ArrayList<>();
        if (folder.exists() && folder.listFiles() != null) {
            for (File item : folder.listFiles()) {
                if (!item.isDirectory() && item.getPath().endsWith(".jpg")) {
                    pciPaths.add(item.getPath());
                }
            }
        }
        return pciPaths;
    }

    public static void saveAssets2File(Context context) {
        String destination = "/sdcard/receipt/";
        for (String item : getAssetPics(context)) {
            saveBitmap(getAssetsBitmap(context, item), destination + item);
        }
    }

    /**
     * Bitmap 2 file
     *
     * @param bitmap Bitmap
     */
    public static void saveBitmap(Bitmap bitmap, String path) {
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            savePath = path;
        } else {
            Log.e(TAG, "saveBitmap failure : sdcard not mounted");
            return;
        }
        try {
            filePic = new File(savePath);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            } else {
                return;
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "saveBitmap: " + e.getMessage());
            return;
        }
        Log.i(TAG, "saveBitmap success: " + filePic.getAbsolutePath());
    }

    public static Bitmap revisionImageSize(String path, int maxWidth, int maxHeight) {
        Bitmap bitmap = null;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                    new File(path)));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            in.close();
            int i = 0;
            while (true) {
                if ((options.outWidth >> i <= maxWidth)
                        && (options.outHeight >> i <= maxHeight)) {
                    in = new BufferedInputStream(
                            new FileInputStream(new File(path)));
                    options.inSampleSize = (int) Math.pow(2.0D, i);
                    options.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeStream(in, null, options);
                    break;
                }
                i += 1;
            }
        } catch (Exception e) {
            return null;
        }
        return bitmap;
    }

    public static ReceiptBean decodeJson(String path) {
        ReceiptBean bean = null;
        File file = new File(path);
        if (file.exists()) {
            FileInputStream fis;
            String str;
            try {
                fis = new FileInputStream(file);
                byte[] bytes = new byte[fis.available()];
                fis.read(bytes);
                fis.close();
                str = new String(bytes);

                if (!TextUtils.isEmpty(str)) {
                    Gson gson = new Gson();
                    bean = gson.fromJson(str, ReceiptBean.class);
                }
            } catch (FileNotFoundException e) {
                Log.i(TAG, "FileNotFoundException: " + e.getMessage());
            } catch (IOException e) {
                Log.i(TAG, "IOException: " + e.getMessage());
            }
        }
        return bean;
    }

    public static Bitmap compressImage(Bitmap image, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        int options = 90;
        while (baos.toByteArray().length / 1024 > 100) {
            baos.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    public static Bitmap getimage(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        float hh = 800f;
        float ww = 480f;
        int be = 1;
        if (w > h && w > ww) {
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap, 100);
    }
}
