package com.mid.lib;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DemoSink implements   ISink, Runnable {
    BlockingQueue<FFlyVideo> mQu = new LinkedBlockingDeque<>();
    FFlyVideo sentinel = new FFlyVideo();
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
        while (true){
            try {
                FFlyVideo video = mQu.take();
                if(!video.isOk()){
                    break;
                }
                System.out.println(video);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
