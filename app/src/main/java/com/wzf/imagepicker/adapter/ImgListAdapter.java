package com.wzf.imagepicker.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.wzf.imagepicker.ImageLoader;
import com.wzf.imagepicker.ImagePickerActivity;
import com.wzf.imagepicker.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description:
 * @author: wangzhenfei
 * @date: 2017-03-30 15:15
 */

public class ImgListAdapter extends RcyCommonAdapter<String> {
    private ArrayList<String> imgs ;
    public String basePath;
    /**
     * @param context
     * @param datas
     * @param loadMore 是否需要底部加载更多
     * @param rv
     */
    public ImgListAdapter(Context context, List<String> datas, boolean loadMore, RecyclerView rv,  ArrayList<String> imgs) {
        super(context, datas, loadMore, rv);
        this.imgs = imgs;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void convert(RcyViewHolder holder, String s) {
        String path = basePath + File.separator + s;
        ImageView ivImage = holder.getView(R.id.iv_image);
        ImageButton imSelect = holder.getView(R.id.im_select);
        //先初始化
        ivImage.setImageResource(R.mipmap.pictures_no);
        if(imgs.contains(path)){
            imSelect.setImageResource(R.mipmap.pictures_selected);
            ivImage.setColorFilter(Color.parseColor("#77000000"));
        }else {
            imSelect.setImageResource(R.mipmap.picture_unselected);
            ivImage.setColorFilter(null);
        }
        ImageLoader.getInstance().loadImage(path, ivImage);
    }

    @Override
    public int getLayoutId(int position) {
        return R.layout.item_rcy_image;
    }

}
