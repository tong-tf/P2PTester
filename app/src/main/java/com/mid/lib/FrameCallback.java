package com.mid.lib;

public interface FrameCallback {
    Error onFrame(byte[] buf, int offset, int length);
}
