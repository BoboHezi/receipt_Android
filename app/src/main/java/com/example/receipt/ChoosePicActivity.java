package com.example.receipt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChoosePicActivity extends AppCompatActivity {

    private static final String TAG = "ChoosePicActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pic);

        List<String> filePictures = Utils.getAvailablePics(this);
        for (String item : filePictures) {
            filePictures.set(filePictures.indexOf(item), "file#" + item);
            Log.i(TAG, "item: " + item);
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

            if (type == 1) {
                holder.thumbnail.setImageBitmap(Utils.getAssetsBitmap(ChoosePicActivity.this, path));
                holder.description.setText(path);
            } else if (type == 2) {
                holder.thumbnail.setImageBitmap(Utils.revisionImageSize(path, 800, 800));
                holder.description.setText(path);
            }
            holder.itemView.setOnClickListener(view -> {
                setResult(RESULT_OK, new Intent().putExtra("file_path", pictures.get(position)));
                finish();
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
}
