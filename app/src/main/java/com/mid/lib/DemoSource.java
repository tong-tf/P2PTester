package com.mid.lib;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DemoSource implements ISource, Runnable {
    public final int STATE_START_MARKER = 0;
    public final int STATE_FLAGS = 1;
    public final int STATE_DATA = 2;
    public final int START_MARKER_VALUE = 0x24;
    List<ISink> sinkers = new LinkedList<>();
    BlockingQueue<byte[]> mQue = new LinkedBlockingDeque<>();
    byte[] sentinel = new byte[0];
    int mState = STATE_START_MARKER;
    int leftMarker = 2;
    int totalMarker = 2;
    FFlyVideo video = null;
    int counter = 0;

    @Override
    public void addSink(ISink sink) {
        sinkers.add(sink);
    }

    @Override
    public void stop() {
        try {
            mQue.put(sentinel);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void parse(byte[] bs) {

        int len = bs.length;
        for (int i = 0; i < len; ) {
            counter++;
            byte ch = bs[i];
            switch (mState) {
                case STATE_START_MARKER:
                    if (ch == START_MARKER_VALUE) {
                        leftMarker--;
                        if (leftMarker == 0) {
                            mState = STATE_FLAGS;
                            video = new FFlyVideo();
                            System.out.println("counter for frame start = " + counter);
                        }
                    } else {
                        leftMarker = totalMarker;
                    }
                    i++;
                    break;
                case STATE_FLAGS:
                    if (video.fillHeaderOrFull(ch)) {
                        video.init();
                        mState = STATE_DATA;
                    }
                    i++;
                    break;
                case STATE_DATA:
                    if (video.fillDataOrFull(ch)) {
                        sinkers.forEach(s -> s.send(video));
                        System.out.println("counter for frame done  = " + counter);
                        mState = STATE_START_MARKER;
                        leftMarker = totalMarker;
                        video = null;
                    }
                    i++;
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    public void run() {
        counter = 0;
        while (true) {

            try {
                byte[] bs = mQue.take();
                if (bs == null || bs.length == 0) {
                    sinkers.forEach(s -> s.stop());
                    break;
                }
                parse(bs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(byte[] bs) {
        try {
            mQue.put(bs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static final class Header {

    }
}
