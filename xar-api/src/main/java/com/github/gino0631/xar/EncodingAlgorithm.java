package com.github.gino0631.xar;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Encoding algorithm enumeration.
 */
public enum EncodingAlgorithm {
    NONE(MediaType.APPLICATION_OCTET_STREAM),
    ZLIB(MediaType.APPLICATION_GZIP);

    private final String type;

    static class MediaType {
        static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        static final String APPLICATION_GZIP = "application/x-gzip";
    }

    EncodingAlgorithm(String type) {
        this.type = type;
    }

    /**
     * Gets encoding algorithm by its type.
     *
     * @param type type identifier of the algorithm
     * @return a {@link EncodingAlgorithm}, or {@code null} if no algorithm corresponds to the specified type
     */
    public static EncodingAlgorithm byType(String type) {
        switch (type) {
            case MediaType.APPLICATION_OCTET_STREAM:
                return NONE;

            case MediaType.APPLICATION_GZIP:
                return ZLIB;

            default:
                return null;
        }
    }

    /**
     * Gets type of the algorithm.
     *
     * @return media (MIME) type identifier
     */
    public String getType() {
        return type;
    }

    /**
     * Gets decoder input stream.
     *
     * @param input source input stream
     * @return a decoder input stream which wraps the provided input stream
     */
    public InputStream getDecoder(InputStream input) {
        switch (this) {
            case NONE:
                return input;

            case ZLIB:
                return new InflaterInputStream(input);

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Gets encoder output stream.
     *
     * @param output target output stream
     * @return an encoder output stream which wraps the provided output stream
     */
    public OutputStream getEncoder(OutputStream output) {
        switch (this) {
            case NONE:
                return output;

            case ZLIB:
                return new DeflaterOutputStream(output);

            default:
                throw new IllegalStateException();
        }
    }
}
