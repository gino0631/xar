package com.github.gino0631.xar.impl.jaxb.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public final class X509CertificateAdapter extends XmlAdapter<String, X509Certificate> {
    private static final CertificateFactory CERTIFICATE_FACTORY;

    private static Base64.Encoder encoder;

    static {
        try {
            CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");

        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public X509Certificate unmarshal(String s) throws CertificateException {
        if (s != null) {
            ByteArrayInputStream is = new ByteArrayInputStream(Base64.getMimeDecoder().decode(s));
            return (X509Certificate) CERTIFICATE_FACTORY.generateCertificate(is);

        } else {
            return null;
        }
    }

    @Override
    public String marshal(X509Certificate certificate) throws CertificateException {
        if (encoder == null) {
            encoder = Base64.getMimeEncoder(72, new byte[]{'\n'});
        }

        return encoder.encodeToString(certificate.getEncoded());
    }
}
