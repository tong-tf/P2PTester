package com.mid.lib;

import java.nio.ByteBuffer;

public class FFlyVideo {
    byte flags;
    long time_pts;
    int size;
    int spspps_size;


    ByteBuffer bf;
    ByteBuffer data;
    ByteBuffer spspps;
    int headerSize = 17;
    boolean ok = false;


    public  FFlyVideo(){
        bf = ByteBuffer.allocate(17);
        size = spspps_size =  0;
        data = spspps = null;
    }


    /**
     * 向ByteBuffer中插入一个字节并返回是否满
     * @param ch 要插入的字节
     * @return  true if full otherwise false
     */
    public boolean fillHeaderOrFull(byte ch){
        bf.put(ch);
        return bf.position() == bf.limit();
    }

    boolean isOk(){
        return ok;
    }
    public void init(){
        ok = true;
        bf.rewind();
        flags = bf.get();
        time_pts = bf.getLong();
        spspps_size = bf.getInt();
        size = bf.getInt();
        System.out.println(String.format("init %h %h %h %h", flags, time_pts, spspps_size, size));
        data = ByteBuffer.allocate(size);
        if(spspps_size != 0){
            spspps = ByteBuffer.allocate(spspps_size);
        }


    }

    public boolean fillDataOrFull(byte ch){
        if(size != 0){
            data.put(ch);
            size--;
        }else if(spspps_size != 0){
            spspps.put(ch);
            spspps_size--;
        }

        return size == 0 && spspps_size==0;
    }

    @Override
    public String toString() {
        return String.format("flags=%d, size=%d, spspps_size = %d\n",
                flags, data.limit(), spspps != null ? spspps.limit() :0);
    }
}

