package com.github.gino0631.xar.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Input stream supplier.
 */
@FunctionalInterface
public interface InputStreamSupplier {
    /**
     * Opens a new input stream.
     *
     * @return an input stream
     * @throws IOException if an I/O error occurs
     */
    InputStream newInputStream() throws IOException;

    /**
     * Constructs an instance of {@code InputStreamSupplier} based on a file.
     *
     * @param file file
     * @return an instance of {@code InputStreamSupplier}
     */
    static InputStreamSupplier of(Path file) {
        return () -> Files.newInputStream(file);
    }
}
