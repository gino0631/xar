package com.github.gino0631.xar;

import com.github.gino0631.common.io.InputStreamSupplier;
import com.github.gino0631.common.io.Writable;
import com.github.gino0631.xar.spi.ServiceUtils;
import com.github.gino0631.xar.spi.XarArchiveSpi;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

/**
 * XAR archive representation.
 * <p>
 * Implementations of this interface are required to be thread-safe.
 */
public interface XarArchive extends Writable, Closeable {
    /**
     * XAR header representation.
     */
    interface Header {
        /**
         * Gets size of the header.
         *
         * @return size of the header
         */
        int getSize();

        /**
         * Gets length of a compressed TOC.
         *
         * @return length of a compressed TOC
         */
        long getTocLengthCompressed();

        /**
         * Gets length of an uncompressed TOC.
         *
         * @return length of an uncompressed TOC
         */
        long getTocLengthUncompressed();

        /**
         * Gets checksum algorithm.
         *
         * @return algorithm of a checksum computed over the compressed TOC
         */
        ChecksumAlgorithm getChecksumAlgorithm();
    }

    /**
     * XAR signature representation.
     */
    interface Signature {
        interface Timestamp {
            /**
             * Gets TSA's policy under which the response was produced.
             *
             * @return policy OID
             */
            String getPolicyOid();

            /**
             * Gets integer assigned by the TSA to each TimeStampToken. It is unique for each token issued by a given TSA.
             *
             * @return serial number of the token
             */
            BigInteger getSerialNumber();

            /**
             * Gets time at which the time-stamp token has been created by the TSA.
             *
             * @return UTC time
             */
            Date getTime();
        }

        /**
         * Gets type of the signature.
         *
         * @return type of the signature (usually, RSA or CMS)
         */
        String getType();

        /**
         * Gets certificates present in the signature.
         *
         * @return a list of certificates, starting with a signer certificate followed by intermediate certificates
         */
        List<X509Certificate> getCertificates();

        /**
         * Gets signature timestamp.
         *
         * @return signature timestamp, or {@code null} if no valid timestamp is present
         */
        Timestamp getTimestamp();

        /**
         * Verifies the signature.
         *
         * @return {@code true} if the signature was verified successfully, {@code false} otherwise
         */
        boolean verify();
    }

    /**
     * XAR entry representation.
     */
    interface Entry extends InputStreamSupplier {
        /**
         * Gets identifier of the entry.
         *
         * @return identifier of the entry
         */
        long getId();

        /**
         * Gets name of the entry.
         *
         * @return name of the entry
         */
        String getName();

        /**
         * Gets path of the entry.
         *
         * @return path of the entry
         */
        String getPath();

        /**
         * Tells whether or not this entry is a directory.
         *
         * @return {@code true} if this entry is a directory, {@code false} otherwise
         */
        boolean isDirectory();

        /**
         * Gets uncompressed size of the entry.
         *
         * @return uncompressed size of the entry, or {@code -1} if it is not specified (e.g. because the entry is a directory)
         */
        long getSize();

        /**
         * Gets compressed size of the entry.
         *
         * @return compressed size of the entry, or {@code -1} if it is not specified (e.g. because the entry is a directory)
         */
        long getCompressedSize();

        /**
         * Gets input stream of the entry.
         *
         * @return input stream, or {@code null} if the entry contains no data (e.g. because it is a directory)
         * @throws IOException if an I/O error occurs
         */
        InputStream newInputStream() throws IOException;

        /**
         * Gets entries contained in this entry.
         *
         * @return a list of child entries, or {@code null} if the entry has no children (e.g. because it is a file)
         */
        List<Entry> getEntries();
    }

    /**
     * Gets XAR header.
     *
     * @return XAR header representation
     */
    Header getHeader();

    /**
     * Gets XAR creation time.
     *
     * @return creation date and time
     */
    Date getCreationTime();

    /**
     * Gets XAR signature.
     *
     * @return XAR signature representation
     */
    Signature getSignature();

    /**
     * Gets XAR x-signature.
     *
     * @return XAR x-signature representation
     */
    Signature getXSignature();

    /**
     * Gets a list of entries present in the archive.
     *
     * @return unmodifiable list of entries
     */
    List<Entry> getEntries();

    /**
     * Writes the archive to the specified output stream.
     * <p>
     * Care must be taken not to write to the same file the archive was loaded from.
     *
     * @param output output stream to write to
     * @throws IOException if an I/O error occurs
     */
    void writeTo(OutputStream output) throws IOException;

    /**
     * Loads XAR archive from a file.
     *
     * @param file file to read from
     * @return XAR archive representation
     * @throws IOException if an I/O error occurs
     */
    static XarArchive load(Path file) throws IOException {
        return ServiceUtils.getImpl(XarArchiveSpi.class).load(file);
    }

    /**
     * Loads XAR archive from a supplier of input streams.
     *
     * @param streamSupplier supplier of streams to read from
     * @return XAR archive representation
     * @throws IOException if an I/O error occurs
     */
    static XarArchive load(InputStreamSupplier streamSupplier) throws IOException {
        return ServiceUtils.getImpl(XarArchiveSpi.class).load(streamSupplier);
    }
}
