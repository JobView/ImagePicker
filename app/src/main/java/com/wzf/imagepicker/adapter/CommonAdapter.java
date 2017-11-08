package com.wzf.imagepicker.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Author: wangzhenfei Date: 15-4-15 Time: 10:07
 * Description:通用的listview，gridview的适配器，一般情况下不要修改这个类,并且其它的适配器也应extends这个类
 * Version: 1.0
 */
public abstract class CommonAdapter<T> extends BaseAdapter {
	protected List<T> mDatas;
	protected Context mContext;
	protected final int mItemLayoutId;
	protected int position;

	/**
	 * 构造方法
	 * 
	 * @param mDatas
	 *            数据源
	 * @param mContext
	 *            上下文
	 * @param mItemLayoutId
	 *            布局文件的id
	 */
	public CommonAdapter(List<T> mDatas, Context mContext, int mItemLayoutId) {
		this.mItemLayoutId = mItemLayoutId;
		this.mDatas = mDatas;
		this.mContext = mContext;
	}

	@Override
	public int getCount() {
		return mDatas.size();
	}

	@Override
	public T getItem(int i) {
		return mDatas.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		position = i;
		final ViewHolder viewHolder = getViewHolder(view, viewGroup);
		convert(viewHolder, getItem(i));
		return viewHolder.getConvertView();
	}

	/**
	 * 最核心的回调方法
	 * 
	 * @param viewHolder
	 * @param item
	 */
	public abstract void convert(ViewHolder viewHolder, T item);

	private ViewHolder getViewHolder( View convertView,
			ViewGroup parent) {
		return ViewHolder.get(mContext, convertView, parent, mItemLayoutId);
	}
}
