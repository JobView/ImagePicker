package com.wzf.imagepicker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.TextView;

import com.wzf.imagepicker.ImageLoader;
import com.wzf.imagepicker.R;
import com.wzf.imagepicker.model.ImageFloder;

import java.util.List;

/**
 * @Description:
 * @author: wangzhenfei
 * @date: 2017-03-30 17:30
 */

public class ImageDirAdapter extends RcyCommonAdapter<ImageFloder> {
    /**
     * @param context
     * @param datas
     * @param loadMore 是否需要底部加载更多
     * @param rv
     */
    public ImageDirAdapter(Context context, List<ImageFloder> datas, boolean loadMore, RecyclerView rv) {
        super(context, datas, loadMore, rv);
    }

    @Override
    public void convert(RcyViewHolder holder, ImageFloder imageFloder) {
        ImageView ivDirItemImage = holder.getView(R.id.iv_dir_item_image);
        TextView tvDirItemName = holder.getView(R.id.tv_dir_item_name);
        TextView tvDirItemCount = holder.getView(R.id.tv_dir_item_count);
        tvDirItemName.setText(imageFloder.getName());
        ImageLoader.getInstance().loadImage(imageFloder.getFirstImagePath(), ivDirItemImage);
        tvDirItemCount.setText(imageFloder.getCount() + "张");
    }

    @Override
    public int getLayoutId(int position) {
        return R.layout.list_dir_item;
    }
}
