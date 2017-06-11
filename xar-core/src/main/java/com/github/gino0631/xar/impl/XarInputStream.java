package com.github.gino0631.xar.impl;

import com.github.gino0631.xar.ChecksumAlgorithm;
import com.github.gino0631.xar.EncodingAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

final class XarInputStream extends InputStream {
    private final InputStream inputStream;
    private final MessageDigest archivedChecksumMessageDigest;
    private final MessageDigest extractedChecksumMessageDigest;
    private final ChecksumVerifier archivedChecksumVerifier;
    private final ChecksumVerifier extractedChecksumVerifier;
    private boolean closed;

    @FunctionalInterface
    interface ChecksumVerifier {
        void verify(byte[] checksum) throws IOException;
    }

    XarInputStream(InputStream is, long pos, long length, EncodingAlgorithm encodingAlgorithm,
                   ChecksumAlgorithm archivedChecksumAlgorithm, ChecksumVerifier archivedChecksumVerifier,
                   ChecksumAlgorithm extractedChecksumAlgorithm, ChecksumVerifier extractedChecksumVerifier) throws IOException {

        this.archivedChecksumVerifier = archivedChecksumVerifier;
        this.extractedChecksumVerifier = extractedChecksumVerifier;

        IoUtils.skip(is, pos);

        is = new LimitedInputStream(is, length);

        // Archived data checksum calculator
        if (archivedChecksumAlgorithm != ChecksumAlgorithm.NONE) {
            archivedChecksumMessageDigest = archivedChecksumAlgorithm.getInstance();
            is = new DigestInputStream(is, archivedChecksumMessageDigest);

        } else {
            archivedChecksumMessageDigest = null;
        }

        // Decoder
        if (encodingAlgorithm != EncodingAlgorithm.NONE) {
            is = encodingAlgorithm.getDecoder(is);
        }

        // Extracted data checksum calculator
        if (extractedChecksumAlgorithm != ChecksumAlgorithm.NONE) {
            extractedChecksumMessageDigest = extractedChecksumAlgorithm.getInstance();
            is = new DigestInputStream(is, extractedChecksumMessageDigest);

        } else {
            extractedChecksumMessageDigest = null;
        }

        inputStream = is;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return IoUtils.skipByReading(inputStream, n);   // because DigestInputStream.skip() does not update digest
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                if ((archivedChecksumVerifier != null) || (extractedChecksumVerifier != null)) {
                    if (available() > 0) {
                        IoUtils.skipAll(this);
                    }

                    if (archivedChecksumVerifier != null) {
                        archivedChecksumVerifier.verify(archivedChecksumMessageDigest.digest());
                    }

                    if (extractedChecksumVerifier != null) {
                        extractedChecksumVerifier.verify(extractedChecksumMessageDigest.digest());
                    }
                }

            } finally {
                closed = true;
                inputStream.close();
            }
        }
    }
}
