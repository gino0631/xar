package com.github.gino0631.xar.impl;

import java.io.IOException;

final class ChecksumHolder implements XarInputStream.ChecksumVerifier, XarOutputStream.ChecksumConsumer {
    private byte[] checksum;

    @Override
    public void verify(byte[] checksum) throws IOException {
        this.checksum = checksum;
    }

    @Override
    public void accept(byte[] checksum) {
        this.checksum = checksum;
    }

    byte[] getChecksum() {
        return checksum;
    }
}
