package com.github.gino0631.xar.impl;

import com.github.gino0631.common.io.IoStreams;
import com.github.gino0631.xar.ChecksumAlgorithm;
import com.github.gino0631.xar.EncodingAlgorithm;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

final class XarOutputStream extends OutputStream {
    private final OutputStream outputStream;
    private final MessageDigest archivedChecksumMessageDigest;
    private final MessageDigest extractedChecksumMessageDigest;
    private final ChecksumConsumer archivedChecksumConsumer;
    private final ChecksumConsumer extractedChecksumConsumer;
    private final AtomicLong archivedSize = new AtomicLong();
    private final AtomicLong extractedSize = new AtomicLong();
    private boolean closed;

    @FunctionalInterface
    interface ChecksumConsumer extends Consumer<byte[]> {
    }

    XarOutputStream(OutputStream os, EncodingAlgorithm encodingAlgorithm,
                    ChecksumAlgorithm archivedChecksumAlgorithm, ChecksumConsumer archivedChecksumConsumer,
                    ChecksumAlgorithm extractedChecksumAlgorithm, ChecksumConsumer extractedChecksumConsumer) {

        this.archivedChecksumConsumer = archivedChecksumConsumer;
        this.extractedChecksumConsumer = extractedChecksumConsumer;

        os = IoStreams.count(os, archivedSize::addAndGet);

        // Archived data checksum calculator
        if (archivedChecksumAlgorithm != ChecksumAlgorithm.NONE) {
            archivedChecksumMessageDigest = archivedChecksumAlgorithm.getInstance();
            os = new DigestOutputStream(os, archivedChecksumMessageDigest);

        } else {
            archivedChecksumMessageDigest = null;
        }

        // Encoder
        if (encodingAlgorithm != EncodingAlgorithm.NONE) {
            os = encodingAlgorithm.getEncoder(os);
        }

        // Original data checksum calculator
        if (extractedChecksumAlgorithm != ChecksumAlgorithm.NONE) {
            extractedChecksumMessageDigest = extractedChecksumAlgorithm.getInstance();
            os = new DigestOutputStream(os, extractedChecksumMessageDigest);

        } else {
            extractedChecksumMessageDigest = null;
        }

        outputStream = IoStreams.count(os, extractedSize::addAndGet);
    }

    public long getArchivedSize() {
        return archivedSize.get();
    }

    public long getExtractedSize() {
        return extractedSize.get();
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            outputStream.close();
            closed = true;

            if (archivedChecksumConsumer != null) {
                archivedChecksumConsumer.accept(archivedChecksumMessageDigest.digest());
            }

            if (extractedChecksumConsumer != null) {
                extractedChecksumConsumer.accept(extractedChecksumMessageDigest.digest());
            }
        }
    }
}
