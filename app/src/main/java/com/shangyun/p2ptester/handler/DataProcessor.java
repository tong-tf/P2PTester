package com.shangyun.p2ptester.handler;

import android.util.Log;

import com.mid.lib.ISource;
import com.mid.lib.audio.AACDecoder;
import com.mid.lib.audio.ADTSDecoder;
import com.mid.lib.audio.PcmPlayer;
import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.utils.ErrMsg;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class DataProcessor implements Runnable{
    public final static String TAG  = "audio";
    private final int BUFFER_SIZE = 1024;
    private byte mChannel;
    private int mSession;
    private Callback mCallback;
    private OutputStream fout;
    public DataProcessor(int session, int channel){
        mChannel = (byte)channel;
        mSession = session;
    }

    public static interface Callback {
        public void onData(byte[] data);
    }


    @Override
    public void run() {
        try {
            fout = new FileOutputStream("/sdcard/audio.aac");
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        int[] size = new int[1];
        size[0] = BUFFER_SIZE;
        ADTSDecoder adtsDecoder = new ADTSDecoder();
        AACDecoder aacDecoder = new AACDecoder();
        ChannelStreamHandler channelStreamHandler = new ChannelStreamHandler(mSession, 4);
        FileFrameHandler fileFrameHandler = new FileFrameHandler("/sdcard/test.aac");
        PcmPlayer player = new PcmPlayer();
        player.init();
        adtsDecoder.addHandler(aacDecoder);
    //    adtsDecoder.addHandler(channelStreamHandler);
        adtsDecoder.addHandler(fileFrameHandler);
        aacDecoder.setCallback(player);
        aacDecoder.startDecode();
        Log.i(TAG, "DataProcessor.run enter");
        while (!Thread.interrupted()){
            size[0] = BUFFER_SIZE;
            int ret = PPCS_APIs.PPCS_Read(mSession, mChannel, buffer, size, 0xFFFFFFF);
            Log.i(TAG, String.format("PPCS_Read channel=%d, rv=%d, msg=%s", mChannel, ret, ErrMsg.getErrorMessage(ret)));
            if (ret < 0) {
                if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret) {
                    break;
                } else if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret) {
                    break;
                }
            }else{
                int recvSize = size[0];
                if(recvSize != BUFFER_SIZE){
                    // TODO: less than need
                }
                Log.i(TAG, "DataProcessor: recvSize=" + recvSize);
                byte[] data = Arrays.copyOfRange(buffer, 0, recvSize);
                try {
                    if(fout != null){
                        fout.write(data);
                        fout.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(mCallback != null){
                    mCallback.onData(data);
                }
                adtsDecoder.process(data);
            }
        }
        if(player != null){
            player.release();
        }
        try {
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileFrameHandler.stop();
    }
}
