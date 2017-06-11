package com.github.gino0631.xar.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IoUtils {
    private IoUtils() {
    }

    public static long skipAll(InputStream is) throws IOException {
        return skip(is, Long.MAX_VALUE);
    }

    public static long skip(InputStream is, long count) throws IOException {
        long total = 0;
        long cur;

        while ((total < count) && ((cur = is.skip(count - total)) > 0)) {
            total += cur;
        }

        return total;
    }

    public static long skipAllByReading(InputStream is) throws IOException {
        return skipByReading(is, Long.MAX_VALUE);
    }

    public static long skipByReading(InputStream is, long count) throws IOException {
        long total = 0;
        long cur;
        byte[] buf = new byte[(int) Math.min(count, 4096)];

        while ((total < count) && ((cur = is.read(buf, 0, (int) Math.min(buf.length, count - total))) >= 0)) {
            total += cur;
        }

        return total;
    }

    /**
     * Reads all bytes from an input stream and writes them to an output stream.
     * Copied from java.nio.file.Files.
     */
    public static long copy(InputStream source, OutputStream sink) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[8192];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    public static Path createTempFile(String prefix) throws IOException {
        Path file = Files.createTempFile(prefix, null);
        file.toFile().deleteOnExit();
        return file;
    }

    public static void deleteSilently(Path file) {
        if (file != null) {
            try {
                Files.delete(file);

            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();

            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static InputStream closeProtect(InputStream is) {
        return new DelegatingInputStream(is) {
            @Override
            public void close() throws IOException {
                // Ignore
            }
        };
    }

    public static OutputStream closeProtect(OutputStream os) {
        return new DelegatingOutputStream(os) {
            @Override
            public void close() throws IOException {
                // Just flush
                flush();
            }
        };
    }
}
