package com.mid.lib;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MyClass {
    public static  void main(String av[]){

        byte[] buff = {(byte) 0xff, (byte) 0xf1, 0x50, (byte) 0x80, 0x19 , (byte) 0xdf, (byte) 0xfc};
        int profile = ((buff [2] >> 6) & 0x3);
        int sample = (buff[2] &0x3c)>>2;
        int channel = (buff[2] &0x1)<<2 | (buff[3]>>6)&0x3;
        int frameLen = ((buff[3]&0x3) <<11) | ((buff[4] & 0xff) << 3) | (buff[5]>>5 & 0x7);
        String msg = String.format("profile=%d, sample=%d, channel=%d, frameLen=%d",
                profile, sample, channel, frameLen);
        System.out.println(msg);
//        DemoSink sink = new DemoSink();
//        DemoSource source = new DemoSource();
//        source.addSink(sink);
//        List<Thread> ths = new ArrayList<>(10);
//        ths.add(new Thread(source));
//        ths.add(new Thread(sink));
//        ths.forEach(t -> t.start());
//        InputStream in = null;
//        try {
//            int rv = 0;
//            byte[] buff = new byte[4096]; // 4k
//            in = new FileInputStream("MediaStarme");
//            while(true){
//                rv = in.read(buff);
//                if(rv <= 0){
//                    System.out.println("Data done , quit");
//                    source.stop();
//                    break;
//                }
//                source.send(Arrays.copyOfRange(buff, 0, rv));
//            }
//            in.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        ths.forEach(t -> {
//            try {
//                t.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//
//
    }
}
