package com.mid.lib;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

import com.shangyun.p2ptester.R;

import java.util.List;
import java.util.jar.Attributes;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CustomButtonTabWidget extends LinearLayout {


    @BindView(R.id.tab_show) ViewPager mViewPager;
    @BindViews({R.id.menu_record, R.id.menu_photo, R.id.menu_tape, R.id.menu_about})
            List<LinearLayout> mMenuList;
    SparseArray<Integer> mIdToIndex = new SparseArray<>();
    FragmentManager mFragmentManger;
    List<Fragment > mFragmentList;
    TabPagerAdapter mAdapter;

    public CustomButtonTabWidget(Context context) {
        this(context, null, 0);
    }
    public CustomButtonTabWidget(Context context, AttributeSet atrrs){
        this(context, atrrs, 0);
    }

    public CustomButtonTabWidget(Context context, AttributeSet atrrs, int defStyleAttr){
        super(context, atrrs, defStyleAttr);
        View view = View.inflate(context, R.layout.custom_bottom_tab_widget, this);
        ButterKnife.bind(view);
        bindIdIdx();

    }

    private void bindIdIdx(){
        mIdToIndex.put(R.id.menu_about, MENU_ABOUT);
        mIdToIndex.put(R.id.menu_photo, MENU_PHOTO);
        mIdToIndex.put(R.id.menu_record, MENU_RECORD);
        mIdToIndex.put(R.id.menu_tape, MENU_TAPE);
    }
    /**
     * 外部接口，供外部来初始化
     * @param fm
     * @param fragmentList
     */
    public void init(FragmentManager fm, List<Fragment> fragmentList){
        mFragmentList = fragmentList;
        mFragmentManger = fm;
        initViewPager();
    }

    private void initViewPager(){
        mAdapter = new TabPagerAdapter(mFragmentManger, mFragmentList);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position <=MENU_MAX){
                    selectTab(position);
//                    mViewPager.setCurrentItem(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @OnClick({R.id.menu_record, R.id.menu_photo, R.id.menu_tape, R.id.menu_about})
    public void MyClick(View view){
            final int id = view.getId();
            final int idx = mIdToIndex.get(id);
            selectTab(idx);
            mViewPager.setCurrentItem(idx);
    }


    @TargetApi(Build.VERSION_CODES.N)
    public void selectTab(int which){
        mMenuList.stream().forEach(v -> v.setActivated(false));
        mMenuList.get(which).setActivated(true);
    }

    private final static int MENU_RECORD = 0;
    private final static int MENU_PHOTO = 1;
    private final static int MENU_TAPE = 2;
    private final static int MENU_ABOUT = 3;
    private final static int MENU_MAX = MENU_ABOUT+1;
}
