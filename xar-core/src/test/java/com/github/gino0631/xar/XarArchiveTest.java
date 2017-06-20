package com.github.gino0631.xar;

import com.github.gino0631.common.io.IoStreams;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class XarArchiveTest {
    @Test
    public void testLoadXz() throws Exception {
        try (XarArchive xar = XarArchive.load(new File(getClass().getResource("/XZ.pkg").toURI()).toPath())) {
            XarArchive.Header header = xar.getHeader();
            assertEquals(28, header.getSize());
            assertEquals(ChecksumAlgorithm.SHA1, xar.getHeader().getChecksumAlgorithm());

            assertEquals("2014-09-29T18:34:31Z", xar.getCreationTime().toInstant().toString());

            assertNull(xar.getSignature());
            assertNull(xar.getXSignature());

            List<XarArchive.Entry> entries = xar.getEntries();
            assertEquals(3, entries.size());

            assertEntry(entries.get(0), 6, "local.pkg", true, -1, -1);
            assertEntry(entries.get(1), 2, "Resources", true, -1, -1);
            assertEntry(entries.get(2), 1, "Distribution", false, 591, 316);
        }
    }

    @Test
    public void testLoadProduct() throws Exception {
        try (XarArchive xar = XarArchive.load(new File(getClass().getResource("/Product.pkg").toURI()).toPath())) {
            XarArchive.Header header = xar.getHeader();
            assertEquals(28, header.getSize());
            assertEquals(ChecksumAlgorithm.SHA1, xar.getHeader().getChecksumAlgorithm());

            assertEquals("2017-05-07T14:46:13Z", xar.getCreationTime().toInstant().toString());

            XarArchive.Signature signature = xar.getSignature();
            assertNotNull(signature);
            assertEquals("RSA", signature.getType());
            assertEquals(2, signature.getCertificates().size());
            assertTrue(signature.verify());

            XarArchive.Signature xSignature = xar.getXSignature();
            assertNotNull(xSignature);
            assertEquals("CMS", xSignature.getType());
            assertEquals(2, xSignature.getCertificates().size());
            assertTrue(xSignature.getCertificates().equals(signature.getCertificates()));
            assertTrue(xSignature.verify());

            XarArchive.Signature.Timestamp timestamp = xSignature.getTimestamp();
            assertNotNull(timestamp);
            assertEquals("1.2.3.4.5.6", timestamp.getPolicyOid());
            assertEquals(new BigInteger("4399174063086540075"), timestamp.getSerialNumber());
            assertEquals("2017-05-07T14:46:26Z", timestamp.getTime().toInstant().toString());

            List<XarArchive.Entry> entries = xar.getEntries();
            assertEquals(2, entries.size());

            assertEntry(entries.get(0), 5, "Distribution", false, 994, 382);
            assertEntry(entries.get(1), 1, "com.test.my-test-app.pkg", true, -1, -1);

            entries = entries.get(1).getEntries();
            assertEquals(3, entries.size());

            assertEntry(entries.get(0), 2, "Bom", false, 35903, 733);
            assertEntry(entries.get(1), 3, "Payload", false, 598, 598);
            assertEntry(entries.get(2), 4, "PackageInfo", false, 891, 376);
        }
    }

    @Test
    public void testBuild() throws Exception {
        try (XarBuilder xarBuilder = XarBuilder.getInstance()) {
            XarBuilder.Container root = xarBuilder.getRoot();
            XarBuilder.Directory dir;
            XarBuilder.File file;

            file = root.addFile("file-0-0-100", EncodingAlgorithm.ZLIB, new ByteArrayInputStream(new byte[100]));
            assertEquals(1, file.getId());
            assertEquals(100, file.getSize());
            assertEquals(12, file.getCompressedSize());

            dir = root.addDirectory("dir-0");
            assertEquals(2, dir.getId());

            file = dir.addFile("file-1-0-100", EncodingAlgorithm.NONE, new ByteArrayInputStream(new byte[100]));
            assertEquals(3, file.getId());
            assertEquals(100, file.getSize());
            assertEquals(100, file.getCompressedSize());

            file = dir.addFile("file-1-1-200", EncodingAlgorithm.ZLIB, new ByteArrayInputStream(new byte[200]));
            assertEquals(4, file.getId());
            assertEquals(200, file.getSize());
            assertEquals(12, file.getCompressedSize());

            dir = dir.addDirectory("dir-1-0");
            assertEquals(5, dir.getId());

            try (XarArchive originalXar = setTestSigning(xarBuilder, false).build()) {
                testRead(originalXar.getEntries());

                Path xarFile = fileInTargetDir("test.xar").toPath();
                try {
                    try (OutputStream os = Files.newOutputStream(xarFile)) {
                        originalXar.writeTo(os);
                    }

                    try (XarArchive loadedXar = XarArchive.load(xarFile)) {
                        assertXarEquals(originalXar, loadedXar);
                        testRead(loadedXar.getEntries());
                    }

                } finally {
                    Files.deleteIfExists(xarFile);
                }
            }
        }
    }

    @Test
    @Ignore("Calls external services, so should be run manually")
    public void testSigning() throws Exception {
        try (XarBuilder xarBuilder = XarBuilder.getInstance()) {
            XarArchive xar = setTestSigning(xarBuilder, true).build();

            XarArchive.Signature.Timestamp timestamp = xar.getXSignature().getTimestamp();
            assertNotNull(timestamp);
            System.out.println("Timestamped at: " + timestamp.getTime().toInstant());
        }
    }

    private XarBuilder setTestSigning(XarBuilder xarBuilder, boolean timestamp) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(getClass().getResourceAsStream("/mycert.p12"), "test".toCharArray());
        PrivateKey privateKey = (PrivateKey) keystore.getKey("1", "test".toCharArray());
        Certificate[] certificateChain = keystore.getCertificateChain("1");
        List<X509Certificate> certificates = Arrays.asList(Arrays.copyOf(certificateChain, certificateChain.length, X509Certificate[].class));

        return xarBuilder.setSigning(privateKey, certificates, null, timestamp ? new URL("http://timestamp.apple.com/ts01") : null);
    }

    private static void assertEntry(XarArchive.Entry entry, long id, String name, boolean isDirectory, long size, long compressedSize) throws Exception {
        assertEquals(id, entry.getId());
        assertEquals(name, entry.getName());
        assertEquals(isDirectory, entry.isDirectory());
        assertEquals(size, entry.getSize());
        assertEquals(compressedSize, entry.getCompressedSize());

        if (size >= 0) {
            try (InputStream is = entry.newInputStream()) {
                assertEquals(size, IoStreams.exhaust(is));
            }
        }
    }

    private static void assertXarEquals(XarArchive expected, XarArchive actual) {
        assertEquals(expected.getCreationTime(), actual.getCreationTime());
    }

    private static void testRead(List<XarArchive.Entry> entries) throws IOException {
        if (entries != null) {
            for (XarArchive.Entry e : entries) {
                if (e.isDirectory()) {
                    testRead(e.getEntries());

                } else {
                    try (InputStream is = e.newInputStream()) {
                        IoStreams.exhaust(is);
                    }
                }
            }
        }
    }

    private static File fileInTargetDir(String name) {
        try {
            return new File(new File(XarArchiveTest.class.getResource("/").toURI()), "../" + name).getCanonicalFile();

        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
