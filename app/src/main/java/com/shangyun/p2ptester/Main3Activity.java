package com.shangyun.p2ptester;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mid.lib.CustomButtonTabWidget;
import com.mid.lib.fragment.AboutFragment;
import com.mid.lib.fragment.PhotoFragment;
import com.mid.lib.fragment.RecordFragment;
import com.mid.lib.fragment.TapeFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public class Main3Activity extends AppCompatActivity {

    @BindView(R.id.tabWidget)
    CustomButtonTabWidget mTabWidget;

    List<Fragment> mFragmentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        ButterKnife.bind(this);
        init();
    }


    private void init(){
        mFragmentList.add(new AboutFragment());
        mFragmentList.add(new TapeFragment());
        mFragmentList.add(new PhotoFragment());
        mFragmentList.add(new RecordFragment());
        mTabWidget.init(getSupportFragmentManager(), mFragmentList);

        Intent intent = new Intent(Main3Activity.this, MonitorService.class);
        startService(intent);
    }
}
