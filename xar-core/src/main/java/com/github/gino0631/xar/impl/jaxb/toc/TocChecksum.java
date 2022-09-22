package com.github.gino0631.xar.impl.jaxb.toc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class TocChecksum {
    @XmlAttribute(name = "style")
    protected String style;

    @XmlElement(name = "offset", required = true)
    protected long offset;

    @XmlElement(name = "size", required = true)
    protected int size;

    public TocChecksum() {
    }

    public TocChecksum(String style, long offset, int size) {
        this.style = style;
        this.offset = offset;
        this.size = size;
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
}
