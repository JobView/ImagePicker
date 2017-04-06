package com.wzf.imagepicker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import com.wzf.imagepicker.I.IPisotion;


/**
 * Created by zhenfei.wang on 2016/7/12.
 * recycleview的通用ViewHolder
 */
public class RcyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,OnLongClickListener{
    private SparseArray<View> mViews;
    private View mConvertView;
    private Context mContext;
    private IPisotion iP;
    public RcyViewHolder(Context context, View itemView, ViewGroup parent, IPisotion i)
    {
        super(itemView);
        mContext = context;
        mConvertView = itemView;
        mViews = new SparseArray<View>();
        iP = i;
        mConvertView.setOnClickListener(this);
        mConvertView.setOnLongClickListener(this);
    }


    public static RcyViewHolder get(Context context, ViewGroup parent, int layoutId, IPisotion i)
    {
        View itemView = LayoutInflater.from(context).inflate(layoutId, parent,
                false);
        RcyViewHolder holder = new RcyViewHolder(context, itemView, parent, i);
        return holder;
    }

    /**
     * 通过viewId获取控件
     * @param viewId
     * @return
     */
    public <T extends View> T getView(int viewId)
    {
        View view = mViews.get(viewId);
        if (view == null)
        {
            view = mConvertView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T) view;
    }

    @Override
    public void onClick(View view) {
        if(iP != null){
            iP.clickPosition(getAdapterPosition());
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if(iP != null){
            iP.longClickPosition(getAdapterPosition());
        }
        return false;
    }
}
