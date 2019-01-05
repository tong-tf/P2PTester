package com.mid.lib;

public interface ISource {
    public void addSink(ISink sink);

    public void stop();
}
