package com.shangyun.p2ptester;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.shangyun.p2ptester.utils.FeiflyJson;
import com.shangyun.p2ptester.utils.P2pnet;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ActivityDoor extends AppCompatActivity {

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
    }

    @OnClick({R.id.door_accept, R.id.door_cancel})
    public void myClick(View view){
        switch (view.getId()){
            case R.id.door_accept:

                break;
            case R.id.door_cancel:
                if(mHandle >= 0){
                    doUnlockDoor();
                }
                finish();
                break;
        }
    }


    private void doUnlockDoor(){
        FeiflyJson.Builder builder = new FeiflyJson.Builder();
        builder.setOperation("door unlock");
        P2pnet.sendData(mHandle, builder.build());
    }
}
