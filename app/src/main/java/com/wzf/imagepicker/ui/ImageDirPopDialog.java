package com.wzf.imagepicker.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.wzf.imagepicker.I.IDirChoose;
import com.wzf.imagepicker.R;
import com.wzf.imagepicker.utils.ScreenUtils;
import com.wzf.imagepicker.adapter.ImageDirAdapter;
import com.wzf.imagepicker.model.ImageFloder;

import java.util.List;

/**
 * @Description:
 * @author: wangzhenfei
 * @date: 2017-03-30 17:08
 */

public class ImageDirPopDialog extends Dialog {
    private IDirChoose<ImageFloder> iListener;
    private RecyclerView rv;
    private ImageDirAdapter mAdapter;
    List<ImageFloder> data;
    private Context mContext;

    public void setiListener(IDirChoose<ImageFloder> iListener) {
        this.iListener = iListener;
    }

    public ImageDirPopDialog(Context context, List<ImageFloder> list) {
        super(context, R.style.style_dir_dialog);
        this.data = list;
        this.mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        Window window = this.getWindow();
        //设置无标题  需在setContentView之前
        window.requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_image_dir);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.gravity = Gravity.BOTTOM;
        attributes.width = ScreenUtils.getScreenWidth(mContext);
        attributes.height = (int) (0.8 * ScreenUtils.getScreenHeight(mContext));
        setCanceledOnTouchOutside(true);
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter = new ImageDirAdapter(mContext, data, false, rv){
            @Override
            public void onItemClickListener(int position) {
                super.onItemClickListener(position);
                if(iListener != null){
                    iListener.choose(mDatas.get(position));
                }
                dismiss();
            }
        };
        rv.setAdapter(mAdapter);
    }
}
