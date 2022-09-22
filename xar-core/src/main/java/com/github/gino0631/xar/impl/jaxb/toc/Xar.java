package com.github.gino0631.xar.impl.jaxb.toc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "xar")
public class Xar {
    @XmlElement(name = "toc")
    protected XarToc toc;

    public Xar() {
    }

    public Xar(XarToc toc) {
        this.toc = toc;
    }

    public XarToc getToc() {
        return toc;
    }

    public void setToc(XarToc toc) {
        this.toc = toc;
    }
}
