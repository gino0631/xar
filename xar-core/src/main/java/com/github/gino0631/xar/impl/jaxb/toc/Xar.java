package com.github.gino0631.xar.impl.jaxb.toc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
