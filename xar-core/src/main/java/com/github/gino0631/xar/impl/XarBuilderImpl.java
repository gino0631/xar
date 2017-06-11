package com.github.gino0631.xar.impl;

import com.github.gino0631.xar.ChecksumAlgorithm;
import com.github.gino0631.xar.EncodingAlgorithm;
import com.github.gino0631.xar.XarArchive;
import com.github.gino0631.xar.XarBuilder;
import com.github.gino0631.xar.impl.jaxb.ObjectFactory;
import com.github.gino0631.xar.impl.jaxb.toc.*;
import com.github.gino0631.xar.impl.signature.CmsSignature;
import com.github.gino0631.xar.impl.signature.RsaSignature;

import javax.xml.ws.Holder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class XarBuilderImpl implements XarBuilder {
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private final ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.SHA1;
    private final AtomicLong id = new AtomicLong();
    private final XarToc toc;
    private final Path heapFile;
    private final SeekableByteChannel outputChannel;
    private final Container root;
    private long pos;
    private PrivateKey signingPrivateKey;
    private List<X509Certificate> signingCertificates;
    private Provider signingProvider;
    private URL signingTsa;

    class EntryImpl implements Entry {
        final TocFile tocFile;

        EntryImpl(TocFile tocFile) {
            this.tocFile = tocFile;
        }

        @Override
        public long getId() {
            return tocFile.getId();
        }
    }

    class FileImpl extends EntryImpl implements File {
        FileImpl(String name, FileData data) {
            super(new TocFile(id.incrementAndGet(), name, FileType.FILE, data));
        }

        @Override
        public long getSize() {
            return tocFile.getData().getSize();
        }

        @Override
        public long getCompressedSize() {
            return tocFile.getData().getLength();
        }
    }

    class DirectoryImpl extends EntryImpl implements Directory {
        DirectoryImpl(String name) {
            super(new TocFile(id.incrementAndGet(), name, FileType.DIRECTORY));
        }

        @Override
        public File addFile(String name, EncodingAlgorithm encodingAlgorithm, InputStream input) throws IOException {
            return doAddFile(tocFile, name, encodingAlgorithm, input);
        }

        @Override
        public Directory addDirectory(String name) {
            return doAddDirectory(tocFile, name);
        }
    }

    XarBuilderImpl() throws IOException {
        toc = new XarToc(ObjectFactory.newXmlGregorianCalendar(),
                new TocChecksum(checksumAlgorithm.getType(), 0, checksumAlgorithm.getChecksumSize()));

        heapFile = IoUtils.createTempFile("xar-heap-");
        outputChannel = Files.newByteChannel(heapFile, StandardOpenOption.WRITE);

        root = new Container() {
            @Override
            public File addFile(String name, EncodingAlgorithm encodingAlgorithm, InputStream input) throws IOException {
                return doAddFile(toc, name, encodingAlgorithm, input);
            }

            @Override
            public Directory addDirectory(String name) {
                return doAddDirectory(toc, name);
            }
        };
    }

    private synchronized File doAddFile(TocFileHolder parent, String name, EncodingAlgorithm encodingAlgorithm, InputStream input) throws IOException {
        checkNotClosed();
        Objects.requireNonNull(name);
        Objects.requireNonNull(input);

        if (encodingAlgorithm == null) {
            encodingAlgorithm = EncodingAlgorithm.NONE;
        }

        FileImpl file = new FileImpl(name, writeData(input, encodingAlgorithm));
        addTo(file.tocFile, parent);

        return file;
    }

    private synchronized Directory doAddDirectory(TocFileHolder parent, String name) {
        checkNotClosed();
        Objects.requireNonNull(name);

        DirectoryImpl directory = new DirectoryImpl(name);
        addTo(directory.tocFile, parent);

        return directory;
    }

    @Override
    public Container getRoot() {
        return root;
    }

    @Override
    public synchronized XarBuilder setSigning(PrivateKey privateKey, List<X509Certificate> certificates, Provider provider, URL tsa) {
        checkNotClosed();
        Objects.requireNonNull(privateKey);
        Objects.requireNonNull(certificates);

        this.signingPrivateKey = privateKey;
        this.signingCertificates = certificates;
        this.signingProvider = provider;
        this.signingTsa = tsa;

        return this;
    }

    @Override
    public synchronized XarArchive build() throws IOException {
        checkNotClosed();

        XarArchive xarArchive = null;
        Path tocFile = null;

        try {
            outputChannel.close();

            tocFile = IoUtils.createTempFile("xar-toc-");
            long tocLengthCompressed;
            long tocLengthUncompressed;
            byte[] tocChecksum;

            long heapOffset = toc.getChecksum().getSize();

            KeyInfo keyInfo = null;
            RsaSignature rsaSignature = null;
            CmsSignature cmsSignature = null;
            int rsaSignatureSize = 0;
            int cmsSignatureSize = 0;

            if (signingPrivateKey != null) {
                keyInfo = new KeyInfo(new X509Data(signingCertificates));

                // Estimate signature sizes
                rsaSignatureSize = RsaSignature.estimateSize(signingCertificates.get(0));
                cmsSignatureSize = CmsSignature.estimateSize(checksumAlgorithm.getOid(), signingCertificates, (signingTsa != null));
            }

            boolean retrySigning;
            do {
                retrySigning = false;

                if (signingPrivateKey != null) {
                    long offs = toc.getChecksum().getSize();

                    toc.setSignature(new XarSignature(XarSignature.RSA, offs, rsaSignatureSize, keyInfo));
                    offs += rsaSignatureSize;

                    toc.setXSignature(new XarSignature(XarSignature.CMS, offs, cmsSignatureSize, keyInfo));
                    offs += cmsSignatureSize;

                    // Make room for signatures
                    if (heapOffset != offs) {
                        shiftFileData(toc, offs - heapOffset);
                        heapOffset = offs;
                    }
                }

                // Serialize ToC
                try (OutputStream fos = Files.newOutputStream(tocFile)) {
                    Holder<byte[]> computedChecksum = new Holder<>();

                    XarOutputStream xos = new XarOutputStream(IoUtils.closeProtect(fos), EncodingAlgorithm.ZLIB,
                            checksumAlgorithm, (c) -> computedChecksum.value = c,
                            ChecksumAlgorithm.NONE, null);

                    try (OutputStream os = xos) {
                        ObjectFactory.marshal(new Xar(toc), os);
                    }

                    tocLengthCompressed = xos.getArchivedSize();
                    tocLengthUncompressed = xos.getExtractedSize();

                    if (computedChecksum.value.length != toc.getChecksum().getSize()) {
                        throw new IllegalStateException();
                    }

                    fos.write(tocChecksum = computedChecksum.value);
                }

                if (signingPrivateKey != null) {
                    int signatureSize;

                    // Try CMS signing first as it is more likely to fail
                    cmsSignature = CmsSignature.sign(tocChecksum, checksumAlgorithm.getOid(), signingPrivateKey, signingCertificates, signingProvider, signingTsa);
                    if ((signatureSize = cmsSignature.getSize()) > cmsSignatureSize) {
                        logRetrying(signatureSize, cmsSignatureSize);
                        cmsSignatureSize = CmsSignature.roundSize(signatureSize);
                        retrySigning = true;
                    }

                    if (!retrySigning) {
                        rsaSignature = RsaSignature.sign(tocChecksum, checksumAlgorithm.getOid(), signingPrivateKey, signingProvider);
                        if ((signatureSize = rsaSignature.getSize()) > rsaSignatureSize) { // very unlikely
                            logRetrying(signatureSize, rsaSignatureSize);
                            rsaSignatureSize = signatureSize;
                            retrySigning = true;
                        }
                    }
                }
            } while (retrySigning);

            xarArchive = new XarArchiveImpl(tocLengthCompressed, tocLengthUncompressed, checksumAlgorithm, tocChecksum, toc, tocFile,
                    heapFile, -heapOffset, rsaSignature, cmsSignature);

            return xarArchive;

        } finally {
            if (xarArchive == null) {
                // Something went wrong - clean up now
                IoUtils.deleteSilently(heapFile);
                IoUtils.deleteSilently(tocFile);
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (outputChannel.isOpen()) {
            outputChannel.close();

            IoUtils.deleteSilently(heapFile);
        }
    }

    private void checkNotClosed() {
        if (!outputChannel.isOpen()) {
            throw new IllegalStateException("The builder is closed");
        }
    }

    private FileData writeData(InputStream input, EncodingAlgorithm encodingAlgorithm) throws IOException {
        Holder<byte[]> archivedChecksum = new Holder<>();
        Holder<byte[]> extractedChecksum = new Holder<>();

        outputChannel.position(pos);

        XarOutputStream xos = new XarOutputStream(IoUtils.closeProtect(Channels.newOutputStream(outputChannel)), encodingAlgorithm,
                checksumAlgorithm, (c) -> archivedChecksum.value = c,
                checksumAlgorithm, (c) -> extractedChecksum.value = c);

        try (OutputStream os = xos) {
            IoUtils.copy(input, os);
        }

        verifyChecksumSize(archivedChecksum.value, checksumAlgorithm);
        verifyChecksumSize(extractedChecksum.value, checksumAlgorithm);

        long bytesWritten = xos.getArchivedSize();

        FileData fileData = new FileData(bytesWritten, pos + toc.getChecksum().getSize(), xos.getExtractedSize(),
                new DataEncoding(encodingAlgorithm.getType()),
                new DataChecksum(checksumAlgorithm.getType(), archivedChecksum.value),
                new DataChecksum(checksumAlgorithm.getType(), extractedChecksum.value));

        pos += bytesWritten;

        return fileData;
    }

    private static void logRetrying(int actualSize, int estimatedSize) {
        logger.warning(() -> MessageFormat.format("Retrying signing because actual signature size {0} is larger than estimated {1}",
                actualSize, estimatedSize));
    }

    private static void shiftFileData(TocFileHolder tocFileHolder, long offset) {
        List<TocFile> files = tocFileHolder.getFiles();

        if (files != null) {
            for (TocFile f : files) {
                FileData data = f.getData();
                if (data != null) {
                    data.setOffset(data.getOffset() + offset);
                }

                shiftFileData(f, offset);
            }
        }
    }

    private static void addTo(TocFile tocFile, TocFileHolder tocFileHolder) {
        List<TocFile> tocFiles = tocFileHolder.getFiles();
        if (tocFiles == null) {
            tocFiles = new ArrayList<>();
            tocFileHolder.setFiles(tocFiles);
        }

        tocFiles.add(tocFile);
    }

    private static void verifyChecksumSize(byte[] checksum, ChecksumAlgorithm checksumAlgorithm) {
        if (checksum.length != checksumAlgorithm.getChecksumSize()) {
            throw new IllegalStateException();
        }
    }
}
