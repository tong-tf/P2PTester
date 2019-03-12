package com.shangyun.p2ptester;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.p2p.pppp_api.Config;
import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_NetInfo;
import com.p2p.pppp_api.st_PPCS_Session;
import com.shangyun.p2ptester.utils.DataUtil;
import com.shangyun.p2ptester.utils.ErrMsg;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MonitorService extends Service {
    private final String TAG = "MonitorService";
    private MonitorRunnable mRun;
    private Thread mThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
    }

    private Notification makeNotification(){
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        Intent intent = new Intent(this, Main2Activity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setContentText("启动P2P")
                .setContentTitle("start");
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started by me");

        if(mThread == null){
            startForeground(101, makeNotification());
            int sesssion = ((P2pApplication)getApplication()).getmHandleSession();
            Log.i(TAG, "Service started any thread now ");
            mRun = new MonitorRunnable(this, (P2pApplication) getApplication());
            mThread = new Thread(mRun);
            mThread.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        stopForeground(true);
        super.onDestroy();
    }

    public static final class MonitorRunnable implements Runnable {
        private final static String TAG = "MonitorRunnable";
        private int mHandleSession;
        Context mContext;
        P2pApplication mApp;

        public MonitorRunnable(Context context, P2pApplication app){
            super();
            mContext = context;
            mApp = app;
            mHandleSession = app.getmHandleSession();
        }

        /**
         * 执行连接服务器操作， 如果连接成功
         * @return true 成功， false 失败
         */
        private boolean doConnection(){
            final int max_retry_count = 5;
            final long maxWaitTime = 10000; // 10s at most
            long wt = 0;
            int rv  = 0;
            int retry = 0;
            Log.i(TAG, "initConnection START: ");
            if(mHandleSession >= 0){
                st_PPCS_Session SInfo = new st_PPCS_Session();
                if (PPCS_APIs.PPCS_Check(mHandleSession, SInfo) == PPCS_APIs.ERROR_PPCS_SUCCESSFUL){
                    Log.i(TAG, "connection already estalished, OK, mHandleSession= " + mHandleSession);
                    return true;
                }
            }
            rv = PPCS_APIs.PPCS_Initialize(Config.INIT_STRING.getBytes());
            Log.d(TAG, "PPCS_Initialize " + ErrMsg.getErrorMessage(rv));
            if(rv != PPCS_APIs.ERROR_PPCS_SUCCESSFUL &&
                    rv != PPCS_APIs.ERROR_PPCS_ALREADY_INITIALIZED){
                return false;
            }
            st_PPCS_NetInfo NetInfo = new st_PPCS_NetInfo();
            rv  = PPCS_APIs.PPCS_NetworkDetect(NetInfo, 0);
            Log.d(TAG, "PPCS_NetworkDetect " + ErrMsg.getErrorMessage(rv));
            if(rv != PPCS_APIs.ERROR_PPCS_SUCCESSFUL){
                return false;
            }
            for(retry = 0; wt < maxWaitTime; retry++){
                // PPCS_Connect的连接超时为10s，
                mHandleSession = PPCS_APIs.PPCS_Connect(Config.DID, Config.MODE, Config.UDP_Port);
                if (mHandleSession >= 0) {
                    mApp.setmHandleSession(mHandleSession);
                    Log.i(TAG, "Connect OK, session=" + mHandleSession);
                    return true;
                } else {
                    wt = getWaitTime(retry);
                    Log.i(TAG, String.format("PPCS_Connect failed(%d) : %s , retry=%d, waitTime=%dms\n",
                            mHandleSession, ErrMsg.getErrorMessage(mHandleSession), retry, wt));
                    try {
                        Thread.sleep(wt);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // if fail to connect, then we disconnect it.
            PPCS_APIs.PPCS_Close(mHandleSession);
            mApp.setmHandleSession(-1);
            return false;
        }
        /**
         *  根据传递的重试次数，用指数算法来计算出等待时间
         * @param retry 重试次数
         * @return 等待时间， 单位毫秒
         */
        public static  long getWaitTime(int retry){
            return (long)(Math.pow(2, retry) * 100L);
        }

        @Override
        public void run() {
            while(true){
                if(doConnection()){
                    Log.i(TAG, "connection Ok, start receive data-->");
  //                  mContext.startActivity(new Intent(mContext, Main2Activity.class));
                    while(true){
                        String out = receiveData(1);
                        if (out != null) {
                            Log.i(TAG, "receiveData: " + out);
                            try {
                                JSONObject js = new JSONObject(out);
                                String operation = js.getString("operation");
                                if(operation != null && operation.contains("door call")){
                                    Intent intent = new Intent(mContext, Main2Activity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else{
                            // meaning connection fail
                            st_PPCS_Session sInfo = new st_PPCS_Session();
                            if(PPCS_APIs.PPCS_Check(mHandleSession, sInfo) == PPCS_APIs.ERROR_PPCS_SUCCESSFUL){
                                continue;
                            }
                            closeSession();
                            break;
                        }
                    }

                }
            }
        }

        private  void closeSession(){
            PPCS_APIs.PPCS_Close(mHandleSession);
            mApp.setmHandleSession(-1);
        }

        public String receiveData(int channel) {
            int TotalSize = 0;
            int currenSize = 0;  //
            int left = 0;
            int rsize = 0;
            int timeout_ms = 2000;
            byte[] data;
            byte[] buffer = new byte[1024];
            int[] size = new int[1];
            int retry = 5;
            int ret = 0;
            byte[] header = new byte[8];

            do {
                size[0] = header.length;
                ret = PPCS_APIs.PPCS_Read(mHandleSession, (byte) channel, header, size, timeout_ms);
                Log.i(TAG, "PPCS_Read, ret=" + ret + " , Error=" + ErrMsg.getErrorMessage(ret));
                if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret ||
                        PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret ||
                        PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_CALLED == ret
                        || retry==0) {

                    return null;
                }
                rsize = size[0];
                if(rsize > 0 ) { // we fetch data or retry exceed.
                    Log.i(TAG, "rsize = " + rsize + " buffer[0] = " + buffer[0] + ", buffer[1]=" + buffer[1]);
                    break;
                }
            }while(ret == PPCS_APIs.ERROR_PPCS_TIME_OUT );
            if (rsize >= 8 && (header[0] == 35 && header[1] == 35)) {  // json data begin with ##xxxx$$
                TotalSize = DataUtil.byte4int(Arrays.copyOfRange(header, 2, 6));
                currenSize = 0;
                data = new byte[TotalSize];
            } else {
                Log.i(TAG, "Data invalid: " + new String(buffer, 0, rsize));
                return null;
            }
            left = TotalSize - (rsize - 8);
            if (left <= 0) {
                System.arraycopy(header, 8, data, 0, TotalSize);
            }

            while (left > 0) {
                size[0] = left;
                // we read 1024 byte in a chunk
                ret = PPCS_APIs.PPCS_Read(mHandleSession, (byte) channel, buffer, size, timeout_ms);
                rsize = size[0];  // how much data we read this time
                if (rsize > 0) {
                    if (rsize >= left) {
                        rsize = left;
                    }
                    System.arraycopy(buffer, 0, data, currenSize, rsize);
                    left -= rsize;
                } else {
                    Log.i(TAG, "PPCS_Read, ret=" + ret + " , Error=" + ErrMsg.getErrorMessage(ret));
                }
            }
            return new String(data);
        }
    }
}
