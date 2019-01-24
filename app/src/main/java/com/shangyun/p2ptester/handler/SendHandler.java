package com.shangyun.p2ptester.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_Session;
import com.shangyun.p2ptester.utils.ErrMsg;
import com.shangyun.p2ptester.utils.FeiflyJson;

import java.util.ArrayList;
import java.util.List;

public class SendHandler extends Handler {
    public final static int MSG_SEND_JSON = 1;
    public final static int MSG_SEND_HANG_UP = 2;
    private final static String TAG = "SendHandler";
    private int mHandleSession = -1;
    private List<Handler> mNotifiers = new ArrayList<>();

    public SendHandler(Looper looper, int hanleSession) {
        super(looper);
        mHandleSession = hanleSession;
    }

    public void AddNotify(Handler handler) {
        mNotifiers.add(handler);
    }

    @Override
    public void handleMessage(Message msg) {
        FeiflyJson.Builder builder = new FeiflyJson.Builder();
        switch (msg.what) {
            case MSG_SEND_JSON:
                sendData(builder.build());
                break;
            case MSG_SEND_HANG_UP:
                builder.setChannels("1");
                builder.setOperation("hang up");
                sendData(builder.build());
                break;
        }
    }

    public void sendData(FeiflyJson fj) {
        int Check_ret;
        int ret;
        int channel = 1;
        try {
            channel = Integer.valueOf(fj.getChannels());
        } catch (Exception e) {
            Log.i(TAG, "channels: " + fj.getChannels() + ", " + e.getLocalizedMessage());
        }

        int[] wsize = new int[1];
        if (mHandleSession >= 0) {
            st_PPCS_Session SInfo = new st_PPCS_Session();
            if (PPCS_APIs.PPCS_Check(mHandleSession, SInfo) == PPCS_APIs.ERROR_PPCS_SUCCESSFUL) {
                String str = String.format("Remote Address=%s:%d", SInfo.getRemoteIP(), SInfo.getRemotePort());
                Log.i(TAG, str);
                byte[] buffer = fj.getData();
                Check_ret = PPCS_APIs.PPCS_Check_Buffer(mHandleSession, (byte) channel, wsize, null);
                if (0 > Check_ret) {
                    Log.i(TAG, "PPCS_Check_Buffer CH=" + channel + ", ret=" + Check_ret);
                } else {
                    Log.i(TAG, "PPCS_Check_Buffer pass, begin send data");
                    ret = PPCS_APIs.PPCS_Write(mHandleSession, (byte) channel, buffer, buffer.length);
                    if (0 > ret) {
                        if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret) {
                            Log.i(TAG, String.format("ThreadWrite CH=%d, ret=%d, Session Closed TimeOUT!!\n", channel, ret));
                        } else if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret) {
                            Log.i(TAG, String.format("ThreadWrite CH=%d, ret=%d, Session Remote Close!!\n", channel, ret));
                        } else {
                            Log.i(TAG, String.format("ThreadWrite CH=%d, ret=%d [%s]\n", channel, ret, ErrMsg.getErrorMessage(ret)));
                        }
                    } else {
                        for (Handler h : mNotifiers) {
                            h.sendMessage(h.obtainMessage(ReceiveHandler.MSG_RECEIVE_JSON));
                        }
                    }
                }
            } else {
                Log.i(TAG, "PPCS_Check fail");
            }
        }

    }
}
