package com.shangyun.p2ptester.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mid.lib.FrameCallback;
import com.mid.lib.ISink;
import com.mid.lib.ISource;
import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.utils.DataUtil;
import com.shangyun.p2ptester.utils.ErrMsg;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class ReceiveHandler extends Handler {
    public final static int MSG_RECEIVE_JSON = 1;
    public final static int MSG_RECEIVE_VIDEO = 2;
    private final static String TAG = "rwtest";
    public int readTimeoutMs = 200; //
    private int mHandleSession = -1;
    ISource mSource;
    public ReceiveHandler(Looper looper, int handleSession, ISource source) {
        super(looper);
        mHandleSession = handleSession;
        mSource = source;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_RECEIVE_JSON:
                String out = receiveData(1);
                if (out != null) {
                    Log.i(TAG, "receiveData: " + out);
                    int channel = -1;
                    try {
                        JSONObject js = new JSONObject(out);
                        channel = js.getInt("channels");
                        Log.i(TAG, "channels is: " + channel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(channel != -1){
                       new Thread(new VideoDataProcessor(mHandleSession, channel, mSource)).start();
                    }
                }
                break;
            case MSG_RECEIVE_VIDEO:

                break;
            default:
                break;
        }
    }

    public static final  class VideoDataProcessor implements  Runnable{
        private byte mChannel;
        private int mSession;
        private ISource mSource;
        int timeout_ms = 200;
        private final int BUFFER_SIZE = 16*1024;
        VideoDataProcessor(int session, int channel, ISource source){
            mChannel = (byte)channel;
            mSession = session;
            mSource = source;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            int[] size = new int[1];
            String filename = "/sdcard/test.data";
            File file = new File(filename);
            OutputStream fs = null;
            size[0] = BUFFER_SIZE;
            if(file.exists()){
                file.delete();
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                fs = new FileOutputStream(filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (!Thread.interrupted()){
                int ret = PPCS_APIs.PPCS_Read(mSession, mChannel, buffer, size, 0xFFFFFFF);
                if (ret < 0) {
                    if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret) {
                        //updateStatus("Session Closed TimeOUT!!\n");
                        break;
                    } else if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret) {
                       // updateStatus("Session Remote Close!!\n");
                        break;
                    }
                }else{
                    int recvSize = size[0];
                    if(recvSize != BUFFER_SIZE){
                        // TODO: less than need
                    }
                    byte[] data = Arrays.copyOfRange(buffer, 0, recvSize);
                    Log.i(TAG, "PPCS_PktRecv size = " + data.length);
                    try {
                        fs.write(data);
                        fs.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mSource.send(data);
                }
            }
            try {
                fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void readChannelData(int channel){
        int TotalSize = 0;
        int currenSize = 0;  //
        int left = 0;
        int rsize = 0;
        int timeout_ms = 200;
        byte[] data;
        byte[] buffer = new byte[4096];
        int[] size = new int[1];
        size[0] = buffer.length;

        while(true){
            int ret = PPCS_APIs.PPCS_PktRecv(mHandleSession, (byte) channel, buffer, size, timeout_ms);
            rsize = size[0];
            Log.i(TAG, String.format("PPCS_PktRecv ret=%s, size=%d",ErrMsg.getErrorMessage(ret), rsize ));
            if(rsize > 0){
                //mFrameCallback.onFrame(buffer, 0, rsize);

            }
        }


    }

    public String receiveData(int channel) {
        int TotalSize = 0;
        int currenSize = 0;  //
        int left = 0;
        int rsize = 0;
        int timeout_ms = 200;
        byte[] data;
        byte[] buffer = new byte[1024];
        int[] size = new int[1];
        size[0] = buffer.length;
        int ret = PPCS_APIs.PPCS_Read(mHandleSession, (byte) channel, buffer, size, timeout_ms);
        if (ret != 0) {
            Log.i(TAG, "PPCS_Read, ret=" + ret + " , Error=" + ErrMsg.getErrorMessage(ret));
        }
        rsize = size[0];
        Log.i(TAG, "rsize = " + rsize + " buffer[0]" + buffer[0] + ", buffer[1]=" + buffer[1]);
        if (rsize > 8 && (buffer[0] == 35 && buffer[1] == 35)) {  // json data begin with ##xxxx$$
            TotalSize = DataUtil.byte4int(Arrays.copyOfRange(buffer, 2, 6));
            currenSize = 0;
            data = new byte[TotalSize];
        } else {
            Log.i(TAG, "Data invalid: " + new String(buffer));
            return null;
        }
        left = TotalSize - (rsize - 8);
        if (left <= 0) {
            System.arraycopy(buffer, 8, data, 0, TotalSize);
        }

        while (left > 0) {
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
