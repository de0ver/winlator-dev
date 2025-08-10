package com.winlator.cmo.xserver;

public interface XLock extends AutoCloseable {
    @Override
    void close();
}
