package com.mid.lib;

import com.shangyun.p2ptester.TestActivity;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DemoSink implements ISink, Runnable {
    BlockingQueue<FFlyVideo> mQu = new LinkedBlockingDeque<>();
    FFlyVideo sentinel = new FFlyVideo();
    FrameCallback mCallback;

    public DemoSink(FrameCallback callback) {
        mCallback = callback;
    }

    @Override
    public void stop() {
        try {
            mQu.put(sentinel);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(FFlyVideo video) {
        try {
            mQu.put(video);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                FFlyVideo video = mQu.take();
                Error ok = Error.ERROR_NONE;
                if (!video.isOk()) {
                    //break;
                    continue;
                }
                System.out.println(video);
                if (video.spspps != null) {
                    do {
                        ok = mCallback.onFrame(video.spspps.array(), 0, video.spspps.limit());
                    } while (ok == Error.ERROR_RETRY);
                }
                do {
                    ok = mCallback.onFrame(video.data.array(), 0, video.data.limit());
                } while (ok == Error.ERROR_RETRY);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
