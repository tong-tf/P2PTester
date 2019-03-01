package com.shangyun.p2ptester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompledReceiver extends BroadcastReceiver {
    private final String TAG = "BootCompledReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "BootCompledReceiver: " + action);
        Intent service = new Intent(context, MonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(service);
        }else{
            context.startService(service);
        }
    }
}
