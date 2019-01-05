package com.shangyun.p2ptester;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MyPopupWindows {
    private Context mContext;
    private PopupWindow popupWindow;
    private ListView mListView;
    private View contentView;
    private List<String> datas;
    private MyAdapter mAdapter;
    private OnPopupClickListener onPopupClickListener;

    public MyPopupWindows(Context mContext, String[] datas) {
        this.mContext = mContext;
        this.datas = new ArrayList<String>();
        for (int i = 0; i < datas.length; i++) {
            this.datas.add(datas[i]);
        }
        init();
    }

    public MyPopupWindows(Context mContext, List<String> datas) {
        this.mContext = mContext;
        this.datas = datas;
        init();
    }

    private void init() {
        contentView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.popup_window_layout, null);
        mListView = (ListView) contentView.findViewById(R.id.listView1);
        mAdapter = new MyAdapter();
        mListView.setAdapter(mAdapter);
    }

    public void ShowWindow(View targetView) {
        popupWindow = new PopupWindow(contentView, targetView.getWidth(), LayoutParams.WRAP_CONTENT);
        // 需要设置一下此参数，点击外边可消失
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        //设置点击窗口外边窗口消失
        popupWindow.setOutsideTouchable(true);
        // 设置此参数获得焦点，否则无法点击
        popupWindow.setFocusable(true);
        popupWindow.showAsDropDown(targetView, 0, 5);
//		popupWindow.showAtLocation(targetView, Gravity.CENTER, 200, 200);
    }

    public OnPopupClickListener getOnPopupClickListener() {
        return onPopupClickListener;
    }

    public void setOnPopupClickListener(OnPopupClickListener onPopupClickListener) {
        this.onPopupClickListener = onPopupClickListener;
    }

    public interface OnPopupClickListener {
        public void onClick(int position);

        public void onDelete(int position);
    }

    private class MyAdapter extends BaseAdapter {

        private OnClickListener mClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                switch (v.getId()) {
                    case R.id.close_btn:
                        if (onPopupClickListener != null) {
                            onPopupClickListener.onDelete(position);
                        }
                        datas.remove(position);
                        notifyDataSetChanged();
                        if (datas == null || datas.isEmpty()) {
                            popupWindow.dismiss();
                        }
                        break;
                    case R.id.item_layout:
                        if (onPopupClickListener != null) {
                            onPopupClickListener.onClick(position);
                        }
                        popupWindow.dismiss();
                        break;
                }

            }
        };

        @Override
        public int getCount() {
            if (datas == null) {
                return 0;
            }
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            if (datas == null) {
                return null;
            }
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AdapterItem mAdapterItem = null;
            if (convertView == null) {
                mAdapterItem = new AdapterItem();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_popup_window, parent, false);
                mAdapterItem.loginName = (TextView) convertView.findViewById(R.id.login_name);
                mAdapterItem.closeBtn = (ImageButton) convertView.findViewById(R.id.close_btn);
                mAdapterItem.itemLayout = (ViewGroup) convertView.findViewById(R.id.item_layout);
                convertView.setTag(mAdapterItem);
            } else {
                mAdapterItem = (AdapterItem) convertView.getTag();
            }
            //赋值
            mAdapterItem.loginName.setText(datas.get(position));
            mAdapterItem.closeBtn.setTag(position);
            mAdapterItem.itemLayout.setTag(position);

            mAdapterItem.closeBtn.setOnClickListener(mClick);
            mAdapterItem.itemLayout.setOnClickListener(mClick);
            return convertView;
        }

    }

    private class AdapterItem {
        TextView loginName;
        ImageButton closeBtn;
        ViewGroup itemLayout;
    }
}
