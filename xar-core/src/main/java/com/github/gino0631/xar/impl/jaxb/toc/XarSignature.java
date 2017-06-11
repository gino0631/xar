package com.github.gino0631.xar.impl.jaxb.toc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class XarSignature {
    public static final String RSA = "RSA";
    public static final String CMS = "CMS";

    @XmlAttribute(name = "style", required = true)
    protected String style;

    @XmlElement(name = "offset", required = true)
    protected long offset;

    @XmlElement(name = "size", required = true)
    protected int size;

    @XmlElement(name = "KeyInfo", namespace = "http://www.w3.org/2000/09/xmldsig#")
    protected KeyInfo keyInfo;

    public XarSignature() {
    }

    public XarSignature(String style, long offset, int size, KeyInfo keyInfo) {
        this.style = style;
        this.offset = offset;
        this.size = size;
        this.keyInfo = keyInfo;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public KeyInfo getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(KeyInfo keyInfo) {
        this.keyInfo = keyInfo;
    }
}
