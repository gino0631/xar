package com.github.gino0631.xar.impl.jaxb.toc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class FileData {
    @XmlElement(name = "length")
    protected long length;

    @XmlElement(name = "offset")
    protected long offset;

    @XmlElement(name = "size")
    protected long size;

    @XmlElement(name = "encoding")
    protected DataEncoding encoding;

    @XmlElement(name = "archived-checksum")
    protected DataChecksum archivedChecksum;

    @XmlElement(name = "extracted-checksum")
    protected DataChecksum extractedChecksum;

    public FileData() {
    }

    public FileData(long length, long offset, long size, DataEncoding encoding, DataChecksum archivedChecksum, DataChecksum extractedChecksum) {
        this.length = length;
        this.offset = offset;
        this.size = size;
        this.encoding = encoding;
        this.archivedChecksum = archivedChecksum;
        this.extractedChecksum = extractedChecksum;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public DataEncoding getEncoding() {
        return encoding;
    }

    public void setEncoding(DataEncoding encoding) {
        this.encoding = encoding;
    }

    public DataChecksum getArchivedChecksum() {
        return archivedChecksum;
    }

    public void setArchivedChecksum(DataChecksum archivedChecksum) {
        this.archivedChecksum = archivedChecksum;
    }

    public DataChecksum getExtractedChecksum() {
        return extractedChecksum;
    }

    public void setExtractedChecksum(DataChecksum extractedChecksum) {
        this.extractedChecksum = extractedChecksum;
    }
}
