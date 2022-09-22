package com.github.gino0631.xar.impl;

import com.github.gino0631.common.io.InputStreamSupplier;
import com.github.gino0631.common.io.IoStreams;
import com.github.gino0631.xar.ChecksumAlgorithm;
import com.github.gino0631.xar.EncodingAlgorithm;
import com.github.gino0631.xar.XarArchive;
import com.github.gino0631.xar.impl.jaxb.ObjectFactory;
import com.github.gino0631.xar.impl.jaxb.toc.*;
import com.github.gino0631.xar.impl.signature.AbstractSignature;
import com.github.gino0631.xar.impl.signature.CmsSignature;
import com.github.gino0631.xar.impl.signature.RsaSignature;
import org.bouncycastle.tsp.TimeStampToken;

import jakarta.xml.bind.DatatypeConverter;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class XarArchiveImpl implements XarArchive {
    private InputStreamSupplier streamSupplier;
    private final Path tmpHeapFile;
    private final long heapOffset;
    private final HeaderImpl header;
    private final byte[] tocChecksum;
    private final XarToc toc;
    private final Path tocFile;
    private final SignatureImpl signature;
    private final SignatureImpl xSignature;
    private final List<Entry> entries;

    static final class HeaderImpl implements Header {
        private static final int XAR_HEADER_MAGIC = 0x78617221;
        private static final short XAR_HEADER_SIZE = 28;
        private static final short XAR_HEADER_VERSION = 1;

        private final int magic;
        private final short size;
        private final short version;
        private final long tocLengthCompressed;
        private final long tocLengthUncompressed;
        private final ChecksumAlgorithm checksumAlgorithm;

        HeaderImpl(DataInput input) throws IOException {
            magic = input.readInt();
            if (magic != XAR_HEADER_MAGIC) {
                throw new IllegalArgumentException(MessageFormat.format("Bad file magic (expected 0x{0}, got 0x{1})",
                        Integer.toHexString(XAR_HEADER_MAGIC), Integer.toHexString(magic)));
            }

            size = input.readShort();

            version = input.readShort();
            if (version != XAR_HEADER_VERSION) {
                throw new IllegalArgumentException(MessageFormat.format("Bad file version (expected {0}, got {1})",
                        XAR_HEADER_VERSION, version));
            }

            tocLengthCompressed = input.readLong();
            tocLengthUncompressed = input.readLong();
            checksumAlgorithm = readChecksumAlgorithm(input);
        }

        HeaderImpl(long tocLengthCompressed, long tocLengthUncompressed, ChecksumAlgorithm checksumAlgorithm) {
            this.tocLengthCompressed = tocLengthCompressed;
            this.tocLengthUncompressed = tocLengthUncompressed;
            this.checksumAlgorithm = checksumAlgorithm;

            magic = XAR_HEADER_MAGIC;
            size = XAR_HEADER_SIZE;
            version = XAR_HEADER_VERSION;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public long getTocLengthCompressed() {
            return tocLengthCompressed;
        }

        @Override
        public long getTocLengthUncompressed() {
            return tocLengthUncompressed;
        }

        @Override
        public ChecksumAlgorithm getChecksumAlgorithm() {
            return checksumAlgorithm;
        }

        void writeTo(DataOutput output) throws IOException {
            output.writeInt(magic);
            output.writeShort(size);
            output.writeShort(version);
            output.writeLong(tocLengthCompressed);
            output.writeLong(tocLengthUncompressed);
            writeChecksumAlgorithm(checksumAlgorithm, output);
        }

        private static ChecksumAlgorithm readChecksumAlgorithm(DataInput input) throws IOException {
            int cksumAlg = input.readInt();

            switch (cksumAlg) {
                case 0:
                    return ChecksumAlgorithm.NONE;

                case 1:
                    return ChecksumAlgorithm.SHA1;

                case 2:
                    return ChecksumAlgorithm.MD5;

                default:
                    throw new IllegalArgumentException(MessageFormat.format("Unsupported checksum algorithm {0}", cksumAlg));
            }
        }

        private static void writeChecksumAlgorithm(ChecksumAlgorithm alg, DataOutput output) throws IOException {
            output.writeInt(alg.ordinal());
        }
    }

    static final class TimestampImpl implements Signature.Timestamp {
        private final TimeStampToken timeStampToken;

        TimestampImpl(TimeStampToken timeStampToken) {
            this.timeStampToken = timeStampToken;
        }

        static TimestampImpl getInstance(TimeStampToken timeStampToken) {
            return (timeStampToken != null) ? new TimestampImpl(timeStampToken) : null;
        }

        @Override
        public String getPolicyOid() {
            return timeStampToken.getTimeStampInfo().getPolicy().getId();
        }

        @Override
        public BigInteger getSerialNumber() {
            return timeStampToken.getTimeStampInfo().getSerialNumber();
        }

        @Override
        public Date getTime() {
            return timeStampToken.getTimeStampInfo().getGenTime();
        }
    }

    final class SignatureImpl implements Signature {
        private final XarSignature xarSignature;
        private final Supplier<byte[]> signatureDataSupplier;
        private final List<X509Certificate> certificates;
        private AbstractSignature signature;
        private Timestamp timestamp;

        SignatureImpl(XarSignature xarSignature, Supplier<byte[]> signatureDataSupplier) throws IOException {
            this(xarSignature, signatureDataSupplier, null);
        }

        SignatureImpl(XarSignature xarSignature, AbstractSignature signature) {
            this(xarSignature, null, signature);
        }

        private SignatureImpl(XarSignature xarSignature, Supplier<byte[]> signatureDataSupplier, AbstractSignature signature) {
            this.xarSignature = xarSignature;
            this.signatureDataSupplier = signatureDataSupplier;

            setSignature(signature);

            certificates = Collections.unmodifiableList(xarSignature.getKeyInfo().getX509Data().getCertificates());
        }

        private void setSignature(AbstractSignature signature) {
            this.signature = signature;

            if (signature != null) {
                timestamp = TimestampImpl.getInstance(signature.getTimestamp());
            }
        }

        @Override
        public String getType() {
            return xarSignature.getStyle();
        }

        @Override
        public List<X509Certificate> getCertificates() {
            return certificates;
        }

        @Override
        public Timestamp getTimestamp() {
            ensureInitialized();

            return timestamp;
        }

        @Override
        public boolean verify() {
            ensureInitialized();

            return signature.verify(tocChecksum, certificates);
        }

        private void writeTo(OutputStream output) throws IOException {
            int padSize = xarSignature.getSize() - signature.getSize();
            if (padSize < 0) {
                throw new IllegalStateException(MessageFormat.format("Signature is bigger ({0}) than the space reserved ({1})", signature.getSize(), xarSignature.getSize()));
            }

            signature.writeTo(output);

            for (int i = 0; i < padSize; i++) {
                output.write(0);
            }
        }

        private void ensureInitialized() {
            if (signature == null) {
                switch (getType()) {
                    case XarSignature.RSA:
                        String checksumType = toc.getChecksum().getStyle();
                        ChecksumAlgorithm checksumAlgorithm = (checksumType != null) ? ChecksumAlgorithm.byType(checksumType) : ChecksumAlgorithm.SHA1;
                        Objects.requireNonNull(checksumAlgorithm, () -> MessageFormat.format("Unknown checksum type {0}", checksumType));

                        setSignature(new RsaSignature(signatureDataSupplier.get(), checksumAlgorithm.getOid()));
                        break;

                    case XarSignature.CMS:
                        setSignature(new CmsSignature(signatureDataSupplier.get()));
                        break;

                    default:
                        throw new UnsupportedOperationException(MessageFormat.format("Unsupported signature type {0}", getType()));
                }
            }
        }
    }

    final class EntryImpl implements Entry {
        private final TocFile tocFile;
        private final Entry parent;
        private final List<Entry> entries;

        EntryImpl(TocFile tocFile, Entry parent) {
            this.tocFile = tocFile;
            this.parent = parent;

            entries = convert(this, tocFile.getFiles());
        }

        @Override
        public long getId() {
            return tocFile.getId();
        }

        @Override
        public String getName() {
            return tocFile.getName();
        }

        @Override
        public String getPath() {
            return ((parent != null) ? parent.getPath() : "") + "/" + getName();
        }

        @Override
        public boolean isDirectory() {
            return (tocFile.getType() == FileType.DIRECTORY);
        }

        @Override
        public long getSize() {
            FileData data = tocFile.getData();

            return (data != null) ? data.getSize() : -1;
        }

        @Override
        public long getCompressedSize() {
            FileData data = tocFile.getData();

            return (data != null) ? data.getLength() : -1;
        }

        @Override
        public InputStream newInputStream() throws IOException {
            checkNotClosed();

            FileData data = tocFile.getData();

            if (data != null) {
                String encStyle = data.getEncoding().getStyle();
                EncodingAlgorithm encodingAlgorithm = EncodingAlgorithm.byType(encStyle);
                if (encodingAlgorithm == null) {
                    throw new IllegalArgumentException(MessageFormat.format("Unknown encoding algorithm {0}", encStyle));
                }

                DataChecksum archivedChecksum = data.getArchivedChecksum();
                DataChecksum extractedChecksum = data.getExtractedChecksum();

                return new XarInputStream(streamSupplier.newInputStream(), heapOffset + data.getOffset(), data.getLength(), encodingAlgorithm,
                        ChecksumAlgorithm.byType(archivedChecksum.getStyle()), (c) -> verifyChecksum(archivedChecksum.getValue(), c),
                        ChecksumAlgorithm.byType(extractedChecksum.getStyle()), (c) -> verifyChecksum(extractedChecksum.getValue(), c));

            } else {
                return null;
            }
        }

        @Override
        public List<Entry> getEntries() {
            return entries;
        }
    }

    XarArchiveImpl(Path file) throws IOException {
        this(InputStreamSupplier.of(file));
    }

    XarArchiveImpl(InputStreamSupplier streamSupplier) throws IOException {
        this.streamSupplier = streamSupplier;

        tmpHeapFile = null;
        tocFile = null;

        try (InputStream fis = streamSupplier.newInputStream()) {
            DataInputStream dis = new DataInputStream(fis);
            header = new HeaderImpl(dis);
        }

        heapOffset = header.getSize() + header.getTocLengthCompressed();

        // Read ToC
        {
            ChecksumHolder computedChecksumHolder = new ChecksumHolder();

            try (XarInputStream xis = new XarInputStream(streamSupplier.newInputStream(), header.getSize(), header.getTocLengthCompressed(), EncodingAlgorithm.ZLIB,
                    header.getChecksumAlgorithm(), computedChecksumHolder,
                    ChecksumAlgorithm.NONE, null)) {

                toc = ObjectFactory.unmarshal(xis).getToc();
            }

            TocChecksum checksumInfo = toc.getChecksum();
            tocChecksum = read(heapOffset + checksumInfo.getOffset(), checksumInfo.getSize());

            verifyChecksum(tocChecksum, computedChecksumHolder.getChecksum());
        }

        signature = convert(toc.getSignature());
        xSignature = convert(toc.getXSignature());

        entries = convert(null, toc.getFiles());
    }

    XarArchiveImpl(long tocLengthCompressed, long tocLengthUncompressed, ChecksumAlgorithm checksumAlgorithm, byte[] tocChecksum, XarToc toc, Path tocFile,
                   Path heapFile, long heapOffset, RsaSignature rsaSignature, CmsSignature cmsSignature) throws IOException {
        this.tocChecksum = tocChecksum;
        this.toc = toc;
        this.tocFile = tocFile;
        this.heapOffset = heapOffset;

        tmpHeapFile = heapFile;
        streamSupplier = InputStreamSupplier.of(heapFile);
        header = new HeaderImpl(tocLengthCompressed, tocLengthUncompressed, checksumAlgorithm);

        signature = convert(toc.getSignature(), rsaSignature);
        xSignature = convert(toc.getXSignature(), cmsSignature);

        entries = convert(null, toc.getFiles());
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public Date getCreationTime() {
        return toDateUtc(toc.getCreationTime());
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public Signature getXSignature() {
        return xSignature;
    }

    @Override
    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public void writeTo(OutputStream output) throws IOException {
        checkNotClosed();

        if (heapOffset > 0) {
            IoStreams.copy(streamSupplier.newInputStream(), output);

        } else {
            // Write header
            DataOutputStream dos = new DataOutputStream(output);
            header.writeTo(dos);
            dos.flush();

            // Write ToC and checksum
            Files.copy(tocFile, output);

            // Write signatures (if any)
            if (signature != null) {
                signature.writeTo(output);
            }

            if (xSignature != null) {
                xSignature.writeTo(output);
            }

            // Write heap
            IoStreams.copy(streamSupplier.newInputStream(), output);
        }
    }

    @Override
    public void close() {
        if (streamSupplier != null) {
            XarBuilderImpl.deleteTempFile(tocFile);
            XarBuilderImpl.deleteTempFile(tmpHeapFile);

            streamSupplier = null;
        }
    }

    private void checkNotClosed() {
        if (streamSupplier == null) {
            throw new IllegalStateException("The archive is closed");
        }
    }

    private byte[] read(long pos, int cnt) throws IOException {
        byte[] buf = new byte[cnt];

        try (InputStream is = streamSupplier.newInputStream()) {
            IoStreams.skip(is, pos);
            DataInputStream dis = new DataInputStream(is);
            dis.readFully(buf);
        }

        return buf;
    }

    private SignatureImpl convert(XarSignature xarSignature) throws IOException {
        return (xarSignature != null) ? new SignatureImpl(xarSignature, () -> {
            try {
                return read(heapOffset + xarSignature.getOffset(), xarSignature.getSize());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }) : null;
    }

    private SignatureImpl convert(XarSignature xarSignature, AbstractSignature signature) throws IOException {
        return (xarSignature != null) ? new SignatureImpl(xarSignature, signature) : null;
    }

    private List<Entry> convert(Entry parent, List<TocFile> children) {
        return ((children != null) && (children.size() > 0)) ? Collections.unmodifiableList(children.stream()
                .map(f -> new EntryImpl(f, parent))
                .collect(Collectors.toCollection(() -> new ArrayList<>(children.size())))) : null;
    }

    private static void verifyChecksum(byte[] expected, byte[] actual) throws IOException {
        if (!Arrays.equals(expected, actual)) {
            throw new IOException(MessageFormat.format("Bad checksum: expected 0x{0}, actual 0x{1}",
                    DatatypeConverter.printHexBinary(expected), DatatypeConverter.printHexBinary(actual)));
        }
    }

    private static Date toDateUtc(XMLGregorianCalendar cal) {
        return (cal != null) ? cal.toGregorianCalendar(TimeZone.getTimeZone(ZoneOffset.UTC), null, null).getTime() : null;
    }
}
