package com.github.gino0631.xar.impl;

import java.io.IOException;
import java.io.OutputStream;

final class CounterOutputStream extends DelegatingOutputStream {
    private final Counter counter;

    @FunctionalInterface
    interface Counter {
        void add(long delta);
    }

    CounterOutputStream(OutputStream outputStream, Counter counter) {
        super(outputStream);
        this.counter = counter;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        counter.add(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        counter.add(len);
    }
}
