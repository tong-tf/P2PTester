package com.mid.lib;

import com.shangyun.p2ptester.TestActivity;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DemoSink implements ISink, Runnable {
    BlockingQueue<FFlyVideo> mQu = new LinkedBlockingDeque<>();
    FFlyVideo sentinel = new FFlyVideo();
    TestActivity mTestActivity;

    public DemoSink(TestActivity test) {
        mTestActivity = test;
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
                boolean ok = false;
                if (!video.isOk()) {
                    break;
                }
                System.out.println(video);
                if (video.spspps != null) {
                    do {
                        ok = mTestActivity.onFrame(video.spspps.array(), 0, video.spspps.limit());
                    } while (!ok);
                }
                do {
                    ok = mTestActivity.onFrame(video.data.array(), 0, video.data.limit());
                } while (!ok);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
