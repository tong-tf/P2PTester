package com.shangyun.p2ptester.handler;

import android.util.Log;

import com.mid.lib.audio.ADTSDecoder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileFrameHandler implements ADTSDecoder.ADTSFrameHandler {
    private final static String TAG = "FileFrameHandler";
    private OutputStream fout;
    private int count = 0;   // how many data we receive

    public FileFrameHandler(String name){
        count = 0;
        try {
            fout = new FileOutputStream(name);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
            fout = null;
        }

    }


    @Override
    public void handleFrame(byte[] data, int start, int end) {
        if(fout != null){
            try {
                fout.write(data, start , end-start);
                fout.flush();
                count += end -start;
                Log.i(TAG, "handleFrame, total byte: " + count);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        if(fout != null){
            try {
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
