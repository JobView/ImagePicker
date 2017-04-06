package com.wzf.imagepicker.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.wzf.imagepicker.I.IPisotion;
import com.wzf.imagepicker.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhenfei.wang on 2016/7/12.
 * recycleview 通用的Adapter
 * 只支持单一布局
 */
public abstract class RcyCommonAdapter<T> extends RecyclerView.Adapter<RcyViewHolder> implements IPisotion {
    private final int LOADING_STATUS_LOADING = 11;          // 正在加载
    private final int LOADING_STATUS_INIT = 12;             // 加载初始化
    private final int LOADING_STATUS_NO_MORE_DATA = 13;     // 没有更多加载内容
    protected Context mContext;
    protected List<T> mDatas;
    protected LayoutInflater mInflater;
    protected boolean loadMore;
    protected boolean loadFinish; // 加载完成
    private String footerText;

    /**
     * @param context
     * @param datas
     * @param loadMore 是否需要底部加载更多
     * @param rv
     */
    public RcyCommonAdapter(Context context, List<T> datas, boolean loadMore, RecyclerView rv) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDatas = datas;
        this.loadMore = loadMore;
        setSpanCount(rv);
    }

    public List<T> getmDatas() {
        return mDatas;
    }

    /**
     * 设置每个条目占用的列数
     *
     * @param recyclerView recycleView
     */
    private void setSpanCount(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    // 若是最后一个 且需要加载更多，则强制让最后一个条目占满横屏
                    if (type == R.layout.item_list_footer) {
                        return gridLayoutManager.getSpanCount();
                    } else {
                        return 1;
                    }
                }
            });
        }
    }

    /**
     * 加载更多
     *
     * @param datas
     */
    public void loadMore(List<T> datas) {
        if (loadMore && !loadFinish) {
            if (datas == null || datas.size() == 0) { // 结束标志
                loadFinish = true;
            } else {
                mDatas.addAll(datas);
            }
            notifyDataSetChanged();
        }
    }

    /**
     * 刷新数据
     *
     * @param datas
     */
    public void refresh(List<T> datas) {
        if (datas == null) { // 错误处理

        } else {
            loadFinish = false;
            mDatas = new ArrayList<T>();
            mDatas.addAll(datas);
            notifyDataSetChanged();
        }
    }


    /**
     * 增加一项
     *
     * @param d
     */
    public void addItem(T d) {
        mDatas.add(d);
        notifyDataSetChanged();
    }

    /**
     * 删除一项
     *
     * @param d
     */
    public void removeItem(T d) {
        mDatas.remove(d);
        notifyDataSetChanged();
    }

    public boolean isLoadFinish() {
        return loadFinish;
    }

    public void setLoadFinish(boolean loadFinish) {
        this.loadFinish = loadFinish;
    }

    @Override
    public RcyViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        return RcyViewHolder.get(mContext, parent, viewType, this);
    }

    @Override
    public int getItemViewType(int position) {
        int lId = 0;
        if (loadMore && position + 1 == getItemCount()) {
            lId = R.layout.item_list_footer;
        } else {
            lId = getLayoutId(position);
        }
        return lId;
    }

    @Override
    final public void onBindViewHolder(RcyViewHolder holder, int position) {
        if (getItemViewType(position) == R.layout.item_list_footer) {
            checkLoadStatus(holder);
        } else {
            convert(holder, mDatas.get(position));

        }
    }

    private void checkLoadStatus(RcyViewHolder holder) {
        int status;
        if (loadFinish) {
            status = LOADING_STATUS_NO_MORE_DATA;
        } else if (getItemCount() <= 11) {
            status = LOADING_STATUS_INIT;
        } else {
            status = LOADING_STATUS_LOADING;
        }
        changeLoadStatus(holder, status);
    }

    private void changeLoadStatus(RcyViewHolder holder, int status) {
        View pb = holder.getView(R.id.pb_item_footer_loading);
        TextView tvLoading = holder.getView(R.id.tv_item_footer_load_more);
        switch (status) {
            case LOADING_STATUS_INIT:
                pb.setVisibility(View.GONE);
                tvLoading.setVisibility(View.GONE);
                break;
            case LOADING_STATUS_LOADING:
                pb.setVisibility(View.VISIBLE);
                tvLoading.setVisibility(View.GONE);
                break;
            case LOADING_STATUS_NO_MORE_DATA:
                if (TextUtils.isEmpty(footerText)) {
                    tvLoading.setText("已经全部加载完成");
                } else {
                    tvLoading.setText(footerText);
                }
                pb.setVisibility(View.GONE);
                tvLoading.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }


    public abstract void convert(RcyViewHolder holder, T t);

    public abstract int getLayoutId(int position);

    public void onItemClickListener(int position) {
    }

    ;

    public void onItemLongClickListener(int position) {
    }


    @Override
    public int getItemCount() {
        return loadMore ? mDatas.size() + 1 : mDatas.size();
    }

    @Override
    public void clickPosition(int position) {
        if (getItemViewType(position) != R.layout.item_list_footer) {
            onItemClickListener(position);
        }
    }

    @Override
    public void longClickPosition(int position) {
        if (getItemViewType(position) != R.layout.item_list_footer) {
            onItemLongClickListener(position);
        }
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public String getFooterText() {
        return footerText;
    }
}