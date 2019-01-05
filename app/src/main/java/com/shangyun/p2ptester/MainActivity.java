package com.shangyun.p2ptester;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements android.view.View.OnClickListener {

    private RelativeLayout tab_listen;
    private RelativeLayout tab_connect;
    private RelativeLayout tab_rwtester;
    private RelativeLayout tab_rwtester1;

    private CheckedTextView ct_listen;
    private CheckedTextView ct_connect;
    private CheckedTextView ct_rwtester;
    private CheckedTextView ct_rwtester1;
    private int mCurrentPos;

    private ListenFragment mListenFragment;
    private ConnectionFragment mConnectFragment;
    private ReadWriteTesterFragment mRWTFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();


    }

    private void initViews() {
        // TODO Auto-generated method stub
        tab_listen = (RelativeLayout) findViewById(R.id.tab_listen);
        tab_connect = (RelativeLayout) findViewById(R.id.tab_connect);
        tab_rwtester = (RelativeLayout) findViewById(R.id.tab_rwtester);
        tab_rwtester1 = (RelativeLayout) findViewById(R.id.tab_rwtester1);
        ct_listen = (CheckedTextView) findViewById(R.id.ct_listen);
        ct_connect = (CheckedTextView) findViewById(R.id.ct_connect);
        ct_rwtester = (CheckedTextView) findViewById(R.id.ct_rwtester);

        tab_listen.setOnClickListener(this);
        tab_connect.setOnClickListener(this);
        tab_rwtester.setOnClickListener(this);
        tab_rwtester1.setOnClickListener(this);
        setDefaultFragment();
    }

    private void setDefaultFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        mListenFragment = new ListenFragment();
        transaction.add(R.id.fl_tab_container, mListenFragment, "LISTEN");
        transaction.commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showSureDialog(this);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void showSureDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("确认退出程序?");
        builder.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListenFragment.isListening = false;
                        finish();
                    }
                });
        builder.setNegativeButton("取消", null);
        builder.show();
    }


    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        FragmentManager fm = getFragmentManager();
        // 开启Fragment事务  
        FragmentTransaction transaction = fm.beginTransaction();

        switch (v.getId()) {
            case R.id.tab_listen:
                if (mCurrentPos != 0) {
                    ct_listen.setChecked(true);
                    ct_connect.setChecked(false);
                    ct_rwtester.setChecked(false);
                    mCurrentPos = 0;

                    if (mConnectFragment != null && mConnectFragment.isAdded()) {
                        transaction.hide(mConnectFragment);
                    }

                    if (mRWTFragment != null && mRWTFragment.isAdded()) {
                        transaction.hide(mRWTFragment);

                    }
                    transaction.show(mListenFragment);
                }
                break;

            case R.id.tab_connect:
                if (mCurrentPos != 1) {
                    ct_listen.setChecked(false);
                    ct_connect.setChecked(true);
                    ct_rwtester.setChecked(false);
                    mCurrentPos = 1;

                    if (mConnectFragment == null) {
                        mConnectFragment = new ConnectionFragment();
                        transaction.add(R.id.fl_tab_container, mConnectFragment, "CONNECT");
                    }

                    //transaction.replace(R.id.fl_tab_container, mConnectFragment);
                    transaction.hide(mListenFragment);
                    if (mRWTFragment != null && mRWTFragment.isAdded()) {
                        transaction.hide(mRWTFragment);

                    }
                    transaction.show(mConnectFragment);
                }
                break;
            case R.id.tab_rwtester:
                if (mCurrentPos != 2) {
                    ct_listen.setChecked(false);
                    ct_connect.setChecked(false);
                    ct_rwtester.setChecked(true);
                    mCurrentPos = 2;
                    if (mRWTFragment == null) {
                        mRWTFragment = new ReadWriteTesterFragment();
                        transaction.add(R.id.fl_tab_container, mRWTFragment, "READWRITETESTER");
                    }

                    transaction.hide(mListenFragment);
                    if (mConnectFragment != null && mConnectFragment.isAdded()) {
                        transaction.hide(mConnectFragment);

                    }
                    transaction.show(mRWTFragment);

                }
                break;

            case R.id.tab_rwtester1:
                startActivity(new Intent(MainActivity.this, TestActivity.class));

            default:
                break;
        }

        // transaction.addToBackStack();
        // transaction submission
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        System.out.println("main onDestroy");
    }

}
