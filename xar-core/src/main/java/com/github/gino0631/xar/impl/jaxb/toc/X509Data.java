package com.github.gino0631.xar.impl.jaxb.toc;

import com.github.gino0631.xar.impl.jaxb.adapters.X509CertificateAdapter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.security.cert.X509Certificate;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class X509Data {
    @XmlElement(name = "X509Certificate", namespace = "http://www.w3.org/2000/09/xmldsig#")
    @XmlJavaTypeAdapter(X509CertificateAdapter.class)
    protected List<X509Certificate> certificates;

    public X509Data() {
    }

    public X509Data(List<X509Certificate> certificates) {
        this.certificates = certificates;
    }

    public List<X509Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<X509Certificate> certificates) {
        this.certificates = certificates;
    }
}
