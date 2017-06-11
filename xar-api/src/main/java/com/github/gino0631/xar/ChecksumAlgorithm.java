package com.github.gino0631.xar;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

/**
 * Checksum algorithm enumeration.
 */
public enum ChecksumAlgorithm {
    /**
     * 0, none.
     */
    NONE(0, null),

    /**
     * 1, sha1.
     */
    SHA1(20, "1.3.14.3.2.26"),

    /**
     * 2, md5.
     */
    MD5(16, "1.2.840.113549.2.5");

    private final int checksumSize;
    private final String oid;

    ChecksumAlgorithm(int checksumSize, String oid) {
        this.checksumSize = checksumSize;
        this.oid = oid;
    }

    /**
     * Gets checksum algorithm by its type.
     *
     * @param type type identifier of the algorithm
     * @return a {@link ChecksumAlgorithm}, or {@code null} if no algorithm corresponds to the specified type
     */
    public static ChecksumAlgorithm byType(String type) {
        for (ChecksumAlgorithm a : ChecksumAlgorithm.values()) {
            if (a.name().equalsIgnoreCase(type)) {
                return a;
            }
        }

        return null;
    }

    /**
     * Gets type of the algorithm.
     *
     * @return lowercase type identifier
     */
    public String getType() {
        return name().toLowerCase();
    }

    /**
     * Gets size of the checksum produced by the algorithm.
     *
     * @return size of the checksum produced by the algorithm
     */
    public int getChecksumSize() {
        return checksumSize;
    }

    /**
     * Gets OID of the algorithm.
     *
     * @return OID (object identifier) of the algorithm
     */
    public String getOid() {
        return oid;
    }

    /**
     * Gets implementation of this algorithm.
     *
     * @return an instance of {@link MessageDigest}, or {@code null} if the algorithm is {@link #NONE}
     */
    public MessageDigest getInstance() {
        try {
            switch (this) {
                case NONE:
                    return null;

                case SHA1:
                    return MessageDigest.getInstance("SHA-1");

                case MD5:
                    return MessageDigest.getInstance("MD5");

                default:
                    throw new IllegalStateException();
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(MessageFormat.format("Implementation of {0} is not available", getType()), e);
        }
    }
}
