package com.shangyun.p2ptester.handler;

import android.util.Log;

import com.mid.lib.audio.ADTSDecoder;
import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.utils.ErrMsg;

public class ChannelStreamHandler implements ADTSDecoder.ADTSFrameHandler {
    private final String TAG = "ChannelStreamHandler";
    private int mChannel ;
    private int mSession;

    public ChannelStreamHandler(int session, int channel){
        mChannel = channel;
        mSession = session;
    }


    @Override
    public void handleFrame(byte[] data, int start, int end) {
        int rv = 0;
        int []wsize = new int[1];
        rv = PPCS_APIs.PPCS_Check_Buffer(mSession, (byte) mChannel, wsize, null);
        rv = PPCS_APIs.PPCS_Write(mSession, (byte) mChannel, data, end - start);
        Log.i(TAG, String.format("ThreadWrite CH=%d, ret=%d, msg=%s!!\n", mChannel, rv, ErrMsg.getErrorMessage(rv)));

    }
}
