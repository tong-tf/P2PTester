package com.shangyun.p2ptester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.shangyun.p2ptester.utils.FeiflyJson;
import com.shangyun.p2ptester.utils.P2pnet;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ActivityDoor extends Activity {

    private final static int MSG_TIMEOUT = 0;

    @BindView(R.id.door_info)
    TextView doorInfo;
    @BindString(R.string.outside_call_info) String outsideCallInfo;

    private String did;
    private int mHandle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        did = intent.getStringExtra("peer_key");
        mHandle = intent.getIntExtra("handle", -1);
        doorInfo.setText(String.format(outsideCallInfo, did));
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT), 45 *1000); // 45S TIMEOUT
    }

    @OnClick({R.id.door_accept, R.id.door_cancel,R.id.clooud_unlock})
    public void myClick(View view){
        mHandler.removeMessages(MSG_TIMEOUT);
        switch (view.getId()){

            case R.id.clooud_unlock:
                if(mHandle >= 0){
                    doUnlockDoor();
                }
                finish();
                break;

            case R.id.door_accept:
                Intent mIntent =  new Intent();
                mIntent.setClass(ActivityDoor.this, Main3Activity.class);
                startActivity(mIntent);
                finish();
                break;

            case R.id.door_cancel:

                if(mHandle >= 0){
                    hangUp();
                }
                finish();
                break;
        }
    }


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_TIMEOUT:
                    hangUp();
                    finish();
                    break;
            }
        }
    };

    private void doUnlockDoor(){
        FeiflyJson.Builder builder = new FeiflyJson.Builder();
        builder.setOperation("open door");
        P2pnet.sendData(mHandle, builder.build());
    }

    private void hangUp(){
        FeiflyJson.Builder builder = new FeiflyJson.Builder();
        builder.setOperation("hang up");
        P2pnet.sendData(mHandle, builder.build());
    }
}
