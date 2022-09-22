package com.github.gino0631.xar.impl.jaxb.toc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class DataEncoding {
    @XmlAttribute(name = "style", required = true)
    protected String style;

    public DataEncoding() {
    }

    public DataEncoding(String style) {
        this.style = style;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
