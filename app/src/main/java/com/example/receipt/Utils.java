package com.example.receipt;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Utils {

    private static final String TAG = "Utils";

    /**
     * 接口上传图片
     *
     * @param path     本地路径
     * @param type     类型
     * @param url      接口路径
     * @param callback
     */
    public static void uploadFile(String path, int type, String url, Callback callback) {
        // 文件
        File mFile = new File(path);

        OkHttpClient client = new OkHttpClient.Builder().build();

        // 设置文件以及上传类型
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), mFile);

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("ocr", mFile.getName(), requestBody)
                .addFormDataPart("type", String.valueOf(type))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();

        Call call = client.newCall(request);

        call.enqueue(callback);
    }

    /**
     * 保存json
     *
     * @param data
     * @param path
     */
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

    /**
     * 获取assets中的图片文件
     *
     * @param context
     * @return
     */
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

    /**
     * 保存assets图片到文件下
     *
     * @param context
     */
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

    /**
     * 获取指定大小的图片
     *
     * @param path
     * @param maxWidth
     * @param maxHeight
     * @return
     */
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

    /**
     * 解析文件json为指定类型
     *
     * @param path
     * @param type
     * @param <T>
     * @return
     */
    public static <T> Object decodeJson(String path, Class<T> type) {
        T o = null;
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
                    o = gson.fromJson(str, type);
                }
            } catch (FileNotFoundException e) {
                Log.i(TAG, "FileNotFoundException: " + e.getMessage());
            } catch (IOException e) {
                Log.i(TAG, "IOException: " + e.getMessage());
            }
        }
        return o;
    }

    /**
     * 解析assets json为指定类型
     *
     * @param path
     * @param type
     * @param <T>
     * @return
     */
    public static <T> Object decodeAssetsJson(Context context, String path, Class<T> type) {
        T o = null;
        try {
            AssetManager am = context.getAssets();
            InputStream inputStream = am.open(path);
            byte[] bytes = new byte[inputStream.available()];

            inputStream.read(bytes);
            inputStream.close();
            String str = new String(bytes);

            if (!TextUtils.isEmpty(str)) {
                Gson gson = new Gson();
                o = gson.fromJson(str, type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return o;
    }

    /**
     *
     * @param image
     * @param max kb
     * @return
     */
    public static Bitmap compressImage(Bitmap image, int max) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 90;
        while (baos.toByteArray().length / 1024 > max && options > 0) {
            baos.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    /**
     * 缩放图片
     *
     * @param bgimage
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static Bitmap zoomImage(Bitmap bgimage, double newWidth, double newHeight) {
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width, (int) height, matrix, true);
        return bitmap;
    }
}
