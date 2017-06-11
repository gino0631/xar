package com.github.gino0631.xar.impl.jaxb.toc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class XarToc implements TocFileHolder {
    @XmlElement(name = "creation-time")
    protected XMLGregorianCalendar creationTime;

    @XmlElement(name = "checksum")
    protected TocChecksum checksum;

    @XmlElement(name = "signature")
    protected XarSignature signature;

    @XmlElement(name = "x-signature")
    protected XarSignature xSignature;

    @XmlElement(name = "file")
    protected List<TocFile> files;

    public XarToc() {
    }

    public XarToc(XMLGregorianCalendar creationTime, TocChecksum checksum) {
        this.creationTime = creationTime;
        this.checksum = checksum;
    }

    public XMLGregorianCalendar getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(XMLGregorianCalendar creationTime) {
        this.creationTime = creationTime;
    }

    public TocChecksum getChecksum() {
        return checksum;
    }

    public void setChecksum(TocChecksum checksum) {
        this.checksum = checksum;
    }

    public XarSignature getSignature() {
        return signature;
    }

    public void setSignature(XarSignature signature) {
        this.signature = signature;
    }

    public XarSignature getXSignature() {
        return xSignature;
    }

    public void setXSignature(XarSignature xSignature) {
        this.xSignature = xSignature;
    }

    public List<TocFile> getFiles() {
        return files;
    }

    public void setFiles(List<TocFile> files) {
        this.files = files;
    }
}
