package com.shangyun.p2ptester.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.utils.DataUtil;
import com.shangyun.p2ptester.utils.ErrMsg;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class ReceiveHandler extends Handler {
    public final static int MSG_RECEIVE_JSON = 1;
    public final static int MSG_RECEIVE_VIDEO = 2;
    private final static String TAG = "rwtest";
    public int readTimeoutMs = 200; //
    private int mHandleSession = -1;

    public ReceiveHandler(Looper looper, int hanleSession) {
        super(looper);
        mHandleSession = hanleSession;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_RECEIVE_JSON:
                String out = receiveData(1);
                if (out != null) {
                    Log.i(TAG, "receiveData: " + out);
                    try {
                        JSONObject js = new JSONObject(out);
                        Log.i(TAG, "channels is: " + js.getInt("channels"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MSG_RECEIVE_VIDEO:

                break;
            default:
                break;
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
