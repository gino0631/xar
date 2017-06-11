package com.github.gino0631.xar.impl.jaxb.toc;

import com.github.gino0631.xar.impl.jaxb.adapters.HexBinaryAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class DataChecksum {
    @XmlAttribute(name = "style", required = true)
    protected String style;

    @XmlValue()
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    protected byte[] value;

    public DataChecksum() {
    }

    public DataChecksum(String style, byte[] value) {
        this.style = style;
        this.value = value;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
