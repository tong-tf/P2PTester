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
        DemoSink sink = new DemoSink();
        DemoSource source = new DemoSource();
        source.addSink(sink);
        List<Thread> ths = new ArrayList<>(10);
        ths.add(new Thread(source));
        ths.add(new Thread(sink));
        ths.forEach(t -> t.start());
        InputStream in = null;
        try {
            int rv = 0;
            byte[] buff = new byte[4096]; // 4k
            in = new FileInputStream("MediaStarme");
            while(true){
                rv = in.read(buff);
                if(rv <= 0){
                    System.out.println("Data done , quit");
                    source.stop();
                    break;
                }
                source.send(Arrays.copyOfRange(buff, 0, rv));
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ths.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


    }
}
