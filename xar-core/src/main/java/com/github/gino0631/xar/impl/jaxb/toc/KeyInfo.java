package com.github.gino0631.xar.impl.jaxb.toc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class KeyInfo {
    @XmlElement(name = "X509Data", namespace = "http://www.w3.org/2000/09/xmldsig#")
    protected X509Data x509Data;

    public KeyInfo() {
    }

    public KeyInfo(X509Data x509Data) {
        this.x509Data = x509Data;
    }

    public X509Data getX509Data() {
        return x509Data;
    }

    public void setX509Data(X509Data x509Data) {
        this.x509Data = x509Data;
    }
}
