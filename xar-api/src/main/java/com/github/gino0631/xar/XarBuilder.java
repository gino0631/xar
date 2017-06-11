package com.github.gino0631.xar;

import com.github.gino0631.xar.spi.ServiceUtils;
import com.github.gino0631.xar.spi.XarBuilderSpi;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * XAR builder.
 */
public interface XarBuilder extends Closeable {
    /**
     * Generic XAR entry representation.
     */
    interface Entry {
        /**
         * Gets identifier of the entry.
         *
         * @return identifier of the entry
         */
        long getId();
    }

    /**
     * File container representation.
     */
    interface Container {
        /**
         * Adds a file to the container.
         *
         * @param name              name of the file to add
         * @param encodingAlgorithm encoding algorithm to use
         * @param input             input stream to read file data from
         * @return a representation of the file added
         * @throws IOException if an I/O error occurs
         */
        File addFile(String name, EncodingAlgorithm encodingAlgorithm, InputStream input) throws IOException;

        /**
         * Adds a directory to the container.
         *
         * @param name name of the directory to add
         * @return a representation of the directory added
         */
        Directory addDirectory(String name);
    }

    /**
     * File representation.
     */
    interface File extends Entry {
        /**
         * Gets uncompressed size of the file.
         *
         * @return uncompressed size of the file
         */
        long getSize();

        /**
         * Gets compressed size of the file.
         *
         * @return compressed size of the file
         */
        long getCompressedSize();
    }

    /**
     * Directory representation.
     */
    interface Directory extends Entry, Container {
    }

    /**
     * Gets root file container.
     *
     * @return file container
     */
    Container getRoot();

    /**
     * Sets signing parameters. Equivalent to calling {@link #setSigning(PrivateKey, List, Provider, URL)} with {@code tsa} set to {@code null}
     *
     * @param privateKey   private key to sign with
     * @param certificates certificates to include in the signature; the first one must correspond to the private key
     * @param provider     security provider to use, or {@code null} to select automatically
     * @return this builder
     */
    default XarBuilder setSigning(PrivateKey privateKey, List<X509Certificate> certificates, Provider provider) {
        return setSigning(privateKey, certificates, provider, null);
    }

    /**
     * Sets signing parameters.
     *
     * @param privateKey   private key to sign with
     * @param certificates certificates to include in the signature; the first one must correspond to the private key
     * @param provider     security provider to use, or {@code null} to select automatically
     * @param tsa          URL of Time Stamping Authority (TSA) to use for signature time stamping, or {@code null} to not time stamp
     * @return this builder
     */
    XarBuilder setSigning(PrivateKey privateKey, List<X509Certificate> certificates, Provider provider, URL tsa);

    /**
     * Builds XAR archive.
     *
     * @return a representation of the new XAR archive
     * @throws IOException if an I/O error occurs
     */
    XarArchive build() throws IOException;

    /**
     * Gets an instance of {@code XarBuilder}.
     *
     * @return a new instance of the builder
     */
    static XarBuilder getInstance() {
        return ServiceUtils.getImpl(XarBuilderSpi.class).getInstance();
    }
}
