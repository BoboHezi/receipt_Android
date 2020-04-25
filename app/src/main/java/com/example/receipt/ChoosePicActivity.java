package com.example.receipt;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ChoosePicActivity extends AppCompatActivity {

    private static final String TAG = "ChoosePicActivity";

    //1048576
    public static final int MAX_FILE_SIZE = 400 * 1024;

    public static final Map<String, String> TEMPLATES = new HashMap<String, String>() {
        {
            put("receipt1.jpg", "美团订单");
            put("receipt2.jpg", "饿了么订单");
            put("receipt3.jpg", "优衣库小票");
            put("receipt4.jpg", "超市小票");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pic);

        //加载assets里的模板
        List<String> filePictures = Utils.getAssetPics(this);
        for (String item : filePictures) {
            filePictures.set(filePictures.indexOf(item), "assets#" + item);
        }

        RecyclerView rv = findViewById(R.id.receipt_rv);
        PicsAdapter adapter = new PicsAdapter(this, filePictures);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    class PicsAdapter extends RecyclerView.Adapter<PicsAdapter.PictureHolder> {

        private List<String> pictures;

        private Context context;

        public PicsAdapter(Context context, List<String> pictures) {
            this.context = context;
            this.pictures = pictures;
        }

        @NonNull
        @Override
        public PictureHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PictureHolder(LayoutInflater.from(context).inflate(R.layout.item_picture, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PictureHolder holder, int position) {
            String path = pictures.get(position);
            int type = 0;
            if (path.split("#")[0].equals("assets")) {
                type = 1;
            } else if (path.split("#")[0].equals("file")) {
                type = 2;
            } else {
                return;
            }
            path = path.split("#")[1];

            //显示信息
            if (type == 1) {
                holder.thumbnail.setImageBitmap(Utils.getAssetsBitmap(ChoosePicActivity.this, path));
            } else if (type == 2) {
                holder.thumbnail.setImageBitmap(Utils.revisionImageSize(path, 800, 800));
            }

            for (String key : TEMPLATES.keySet()) {
                if (path.contains(key)) {
                    holder.description.setText(TEMPLATES.get(key));
                    break;
                }
            }

            String finalPath = path;
            holder.itemView.setOnClickListener(view -> {
                int requestCode = 0;
                if (finalPath.contains("receipt1")) {
                    requestCode = BillInfo.TYPE_MEITUAN;
                } else if (finalPath.contains("receipt2")) {
                    requestCode = BillInfo.TYPE_ELE;
                } else if (finalPath.contains("receipt3")) {
                    requestCode = BillInfo.TYPE_MARKET;
                } else if (finalPath.contains("receipt4")) {
                    requestCode = BillInfo.TYPE_OTHER;
                }
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, requestCode);
            });
        }

        @Override
        public int getItemCount() {
            return pictures.size();
        }

        class PictureHolder extends RecyclerView.ViewHolder {

            ImageView thumbnail;
            TextView description;

            public PictureHolder(@NonNull View itemView) {
                super(itemView);

                thumbnail = itemView.findViewById(R.id.thumbnail);
                description = itemView.findViewById(R.id.description);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if ((requestCode & 0b000) == 0b000 && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();

            //获取选中的图片文件
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Log.i(TAG, "picturePath: " + picturePath);

            final String cacheFile = "/sdcard/receipt/" + new File(picturePath).getName();

            new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    try {
                        /*if (new File(cacheFile).exists()) {
                            return true;
                        }*/

                        //压缩图片到指定大小, 保存到固定路径下
                        int rawSize = new FileInputStream(new File(picturePath)).available();
                        int quality = rawSize > MAX_FILE_SIZE ? (MAX_FILE_SIZE * 100 / rawSize) : 100;
                        quality = quality <= 0 ? 100 : quality;
                        float sizeScale = (float) quality / 100f;

                        Bitmap raw = Glide.with(ChoosePicActivity.this).asBitmap()
                                .load(new File(picturePath))
                                .skipMemoryCache(true)
                                .submit()
                                .get();

                        int width = (int) (raw.getWidth() * sizeScale);
                        int height = (int) (raw.getHeight() * sizeScale);
                        Bitmap cache = Utils.zoomImage(raw, width, height);
                        Utils.saveBitmap(cache, cacheFile);

                        Log.i(TAG, "rawSize: " + rawSize +
                                ", cacheSize: " + new FileInputStream(new File(cacheFile)).available() + "" +
                                ", quality: " + quality);

                        /*int sedRawSize = new FileInputStream(new File(cacheFile)).available();
                        if (sedRawSize > MAX_FILE_SIZE) {
                            int sedQuality = sedRawSize > MAX_FILE_SIZE ? (MAX_FILE_SIZE * 100 / rawSize) : 100;
                            sedQuality = Math.max(sedQuality, 10);
                            float sedScale = (float) sedQuality / 100f;
                            int SedWidth = (int) (cache.getWidth() * sedScale);
                            int sedHeight = (int) (cache.getHeight() * sedScale);
                            Bitmap sedCache = Utils.zoomImage(cache, SedWidth, sedHeight);
                            Utils.saveBitmap(sedCache, cacheFile);
                            Log.i(TAG, "cache: " + cache.getWidth() + "x" + cache.getHeight() + ", sedCacheBmp: " + SedWidth + "x" + sedHeight);
                        }*/

                        return true;
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "FileNotFoundException: ", e);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException: ", e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    if ((Boolean) o) {
                        //图片处理完毕，返回数据到MainActivity
                        setResult(RESULT_OK, new Intent()
                                .putExtra("file_path", cacheFile)
                                .putExtra("type", requestCode));
                        finish();
                    }
                }
            }.execute();
        }
    }
}
