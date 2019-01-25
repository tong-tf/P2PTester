package com.shangyun.p2ptester.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mid.lib.FrameCallback;
import com.mid.lib.ISink;
import com.mid.lib.ISource;
import com.mid.lib.audio.AACDecoder;
import com.mid.lib.audio.ADTSDecoder;
import com.mid.lib.audio.PcmPlayer;
import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.utils.DataUtil;
import com.shangyun.p2ptester.utils.ErrMsg;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ReceiveHandler extends Handler {
    public final static int MSG_RECEIVE_JSON = 1;
    public final static int MSG_RECEIVE_VIDEO = 2;
    public final static int MSG_RECEIVE_HANGUP = 3;
    private final static String TAG = "ReceiveHandler";
    public int readTimeoutMs = 200; //
    private int mHandleSession = -1;
    private Thread mVideoThread;
    private Thread mDataThread;
    ISource mSource;

    public ReceiveHandler(Looper looper, int handleSession, ISource source) {
        super(looper);
        mHandleSession = handleSession;
        mSource = source;
    }

    @Override
    public void handleMessage(Message msg) {
        int channel = -1;
        switch (msg.what) {
            case MSG_RECEIVE_JSON:
                String out = receiveData(1);
                if (out != null) {
                    Log.i(TAG, "receiveData: " + out);
                    try {
                        JSONObject js = new JSONObject(out);
                        channel = js.getInt("channels");
                        Log.i(TAG, "channels is: " + channel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if(mVideoThread == null){
                    mVideoThread = new Thread(new VideoDataProcessor(mHandleSession, 2, mSource));
                    mVideoThread.start();
                }
                if(mDataThread == null){
                    mDataThread = new Thread(new DataProcessor(mHandleSession, 3));
                    mDataThread.start();
                }
                break;
            case MSG_RECEIVE_VIDEO:
                break;
            case MSG_RECEIVE_HANGUP:
                if(mVideoThread != null){
                    mVideoThread.interrupt();
                    mVideoThread = null;
                }
                if(mDataThread != null){
                    mDataThread.interrupt();
                    mDataThread = null;
                }
                break;
            default:
                break;
        }
    }


    public static final  class VideoDataProcessor implements  Runnable{
        private byte mChannel;
        private int mSession;
        private ISource mSource;
        int timeout_ms = 500;
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
            while (!Thread.interrupted()){
                size[0] = BUFFER_SIZE;
                int ret = PPCS_APIs.PPCS_Read(mSession, mChannel, buffer, size, timeout_ms);
                if (ret < 0) {
                    if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret) {
                        //updateStatus("Session Closed TimeOUT!!\n");
                        break;
                    } else if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret) {
                       // updateStatus("Session Remote Close!!\n");
                        break;
                    }else{
                        Log.i(TAG, String.format("VideoDataProcessor.PPCS_Read %d, %s", ret, ErrMsg.getErrorMessage(ret)));
                    }
                }else{
                    int recvSize = size[0];
                    if (recvSize <= 0){
                        continue;
                    }
                    if(recvSize != BUFFER_SIZE){
                        // TODO: less than need

                    }
                    byte[] data = Arrays.copyOfRange(buffer, 0, recvSize);
                    Log.i(TAG, "PPCS_PktRecv size = " + data.length);
                    mSource.send(data);
                }
            }
            if(mSource != null){
                mSource.stop();
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
        int retry = 5;
        int ret = 0;
        do {
            size[0] = buffer.length;
            ret = PPCS_APIs.PPCS_Read(mHandleSession, (byte) channel, buffer, size, timeout_ms);
            if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret || PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret
                    || retry==0) {
                Log.i(TAG, "PPCS_Read, ret=" + ret + " , Error=" + ErrMsg.getErrorMessage(ret));
                return null;
            }
            if (ret != 0) {
                Log.i(TAG, "PPCS_Read, ret=" + ret + " , Error=" + ErrMsg.getErrorMessage(ret));
            }
            rsize = size[0];
            retry -= 1;
            if(rsize > 0  || retry == 0) { // we fetch data or retry exceed.
                break;
            }
            Log.i(TAG, "rsize = " + rsize + " buffer[0] = " + buffer[0] + ", buffer[1]=" + buffer[1]);
        }while(ret == PPCS_APIs.ERROR_PPCS_TIME_OUT );

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
