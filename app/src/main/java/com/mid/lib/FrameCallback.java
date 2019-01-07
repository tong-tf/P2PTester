package com.mid.lib;

public interface FrameCallback {
    boolean onFrame(byte[] buf, int offset, int length);
}
