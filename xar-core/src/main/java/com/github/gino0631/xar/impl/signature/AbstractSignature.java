package com.github.gino0631.xar.impl.signature;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDefaultDigestProvider;
import org.bouncycastle.tsp.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.List;

public abstract class AbstractSignature {
    public TimeStampToken getTimestamp() {
        return null;
    }

    public abstract boolean verify(byte[] data, List<X509Certificate> certificates);

    public abstract int getSize();

    public abstract void writeTo(OutputStream output) throws IOException;

    static byte[] doSign(byte[] data, String sigAlgName, PrivateKey privateKey, Provider provider) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = (provider != null) ? Signature.getInstance(sigAlgName, provider) : Signature.getInstance(sigAlgName);
        sig.initSign(privateKey);
        sig.update(data);

        return sig.sign();
    }

    static boolean doVerify(byte[] data, byte[] signature, String sigAlgName, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance(sigAlgName);
        sig.initVerify(publicKey);
        sig.update(data);

        return sig.verify(signature);
    }

    static TimeStampToken doTimestamp(ASN1ObjectIdentifier digestAlgorithm, byte[] digest, URL tsaUrl) throws IOException {
        // Generate time stamp request
        TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
        tsqGenerator.setCertReq(true);
        TimeStampRequest request = tsqGenerator.generate(digestAlgorithm, digest);
        byte[] requestBytes = request.getEncoded();

        // Call TSA
        HttpURLConnection conn = (HttpURLConnection) tsaUrl.openConnection();
        conn.setConnectTimeout(7_000);
        conn.setReadTimeout(10_000);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/timestamp-query");

        try (OutputStream out = conn.getOutputStream()) {
            out.write(requestBytes);
        }

        // Get time stamp response
        try (InputStream in = conn.getInputStream()) {
            if (!"application/timestamp-reply".equalsIgnoreCase(conn.getContentType())) {
                throw new IOException(MessageFormat.format("Bad response MIME type {0}", conn.getContentType()));
            }

            try {
                TimeStampResponse response = new TimeStampResponse(in);
                response.validate(request);
                TimeStampToken timeStampToken = response.getTimeStampToken();

                if (timeStampToken != null) {
                    return timeStampToken;

                } else {
                    throw new IOException(MessageFormat.format("Timestamp request rejected with status {0} ({1}), {2}",
                            response.getStatus(), response.getStatusString(), response.getFailInfo()));
                }

            } catch (TSPException e) {
                throw new IOException(e);
            }
        }
    }

    static byte[] getDigest(byte[] data, AlgorithmIdentifier algorithmIdentifier) {
        try {
            ExtendedDigest digest = BcDefaultDigestProvider.INSTANCE.get(algorithmIdentifier);
            digest.update(data, 0, data.length);

            byte[] result = new byte[digest.getDigestSize()];
            digest.doFinal(result, 0);

            return result;

        } catch (OperatorCreationException e) {
            throw new IllegalArgumentException(MessageFormat.format("Unknown digest algorithm {0}", algorithmIdentifier.getAlgorithm()));
        }
    }

    static byte[] getEncoded(X509Certificate certificate) {
        try {
            return certificate.getEncoded();

        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] getDerEncoded(ASN1Object obj) {
        try {
            return obj.getEncoded(ASN1Encoding.DER);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static AlgorithmIdentifier getAlgorithmIdentifier(String algorithmOid) {
        return getAlgorithmIdentifier(new ASN1ObjectIdentifier(algorithmOid));
    }

    static AlgorithmIdentifier getAlgorithmIdentifier(ASN1ObjectIdentifier algorithmOid) {
        return new AlgorithmIdentifier(algorithmOid, DERNull.INSTANCE);
    }
}
