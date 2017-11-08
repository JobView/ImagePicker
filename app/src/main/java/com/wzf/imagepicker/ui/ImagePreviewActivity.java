package com.wzf.imagepicker.ui;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.wzf.imagepicker.R;
import com.wzf.imagepicker.adapter.CommonPageAdapter;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;

/**
 * @Description:
 * @author: wangzhenfei
 * @date: 2017-04-01 15:48
 *  图片预览
 */

public class ImagePreviewActivity extends AppCompatActivity{
    public static final String DATA_IMAGES = "DATA_IMAGES";
    private ViewPager vp;
    private List<String> imgs;
    private TextView tvImgCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        imgs = getIntent().getStringArrayListExtra(DATA_IMAGES);
        if(imgs == null || imgs.size() == 0){
            finish();
            return;
        }
        initView();
    }

    private void initView() {
        findViewById(R.id.iv_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        tvImgCount = (TextView) findViewById(R.id.tv_img_count);
        tvImgCount.setText("1/" + imgs.size());
        vp = (ViewPager) findViewById(R.id.vp);
        LayoutInflater inflater = getLayoutInflater();
        View view = null;
        List<View>  views = new ArrayList<>();
        PhotoView photoView;
        for(String path : imgs){
            view = inflater.inflate(R.layout.item_viewpage_preview, null, false);
            if(view != null){
                photoView = (PhotoView) view.findViewById(R.id.photo_view);
//                ImageLoader.getInstance().loadImage(path, photoView);
                photoView.setImageBitmap(BitmapFactory.decodeFile(path));
                views.add(view);
            }

            
        }
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tvImgCount.setText((position + 1) + "/" + imgs.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        vp.setAdapter(new CommonPageAdapter(views));
    }
}
