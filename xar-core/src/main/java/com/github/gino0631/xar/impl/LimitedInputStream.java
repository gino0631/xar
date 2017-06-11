package com.github.gino0631.xar.impl;

import java.io.IOException;
import java.io.InputStream;

final class LimitedInputStream extends DelegatingInputStream {
    private long available;

    LimitedInputStream(InputStream inputStream, long available) {
        super(inputStream);
        this.available = available;
    }

    @Override
    public int read() throws IOException {
        if (available > 0) {
            int b = super.read();
            if (b >= 0) {
                available--;
                return b;
            }
        }

        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (available > 0) {
            len = Math.min(len, available());
            len = super.read(b, off, len);
            available -= len;

            return len;

        } else {
            return -1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        n = Math.min(n, available);

        if (n > 0) {
            n = super.skip(n);
            available -= n;
            return n;

        } else {
            return 0;
        }
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(available, Integer.MAX_VALUE);
    }
}
