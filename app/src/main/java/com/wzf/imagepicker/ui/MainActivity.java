package com.wzf.imagepicker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.wzf.imagepicker.ImageLoader.ImageLoader;
import com.wzf.imagepicker.R;
import com.wzf.imagepicker.adapter.RcyCommonAdapter;
import com.wzf.imagepicker.adapter.RcyViewHolder;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoView;

/**
 * @Description:
 * @author: wangzhenfei
 * @date: 2017-03-31 11:45
 */

public class MainActivity extends AppCompatActivity {
    private RcyCommonAdapter mAdapter;
    private RecyclerView rv;
    private PhotoView pv;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePickerActivity.startMethod(MainActivity.this,6);
            }
        });

        findViewById(R.id.btn_net).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoadNetImageActivity.class));
            }
        });
        pv = (PhotoView) findViewById(R.id.pv);

        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.setAdapter( getAdapter());
    }

    private RcyCommonAdapter getAdapter() {
        return mAdapter =  new RcyCommonAdapter<String>(this, new ArrayList<String>(),false,rv) {
            @Override
            public void convert(RcyViewHolder holder, String s) {
                ImageView im = holder.getView(R.id.iv_image);
                ImageLoader.getInstance().loadImage(s, im);
            }

            @Override
            public int getLayoutId(int position) {
                return R.layout.item_rcy_image;
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK){
            return;
        }
        if(requestCode == ImagePickerActivity.REQUEST_CODE_IMAGE_PICKER){
            if(data != null){
                ArrayList<String> imgs = data.getStringArrayListExtra(ImagePickerActivity.DATA_KEY_FOR_IMAGES);
                mAdapter.refresh(imgs);
                ImageLoader.getInstance().loadImage(imgs.get(0), pv);
                startActivity(new Intent(this, ImagePreviewActivity.class).putExtra(ImagePreviewActivity.DATA_IMAGES, imgs));
            }

        }
    }
}
