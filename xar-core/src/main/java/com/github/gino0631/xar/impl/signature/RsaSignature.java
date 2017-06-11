package com.github.gino0631.xar.impl.signature;

import org.bouncycastle.asn1.x509.DigestInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.util.List;

public final class RsaSignature extends AbstractSignature {
    private byte[] signatureBytes;
    private String digestAlgorithmOid;

    public RsaSignature(byte[] signatureBytes, String digestAlgorithmOid) {
        this.signatureBytes = signatureBytes;
        this.digestAlgorithmOid = digestAlgorithmOid;
    }

    @Override
    public boolean verify(byte[] digest, List<X509Certificate> certificates) {
        try {
            byte[] data = getDigestInfo(digest, digestAlgorithmOid);

            return doVerify(data, signatureBytes, "NONEwithRSA", certificates.get(0).getPublicKey());

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSize() {
        return signatureBytes.length;
    }

    @Override
    public void writeTo(OutputStream output) throws IOException {
        output.write(signatureBytes);
    }

    public static int estimateSize(X509Certificate certificate) {
        int bitLength = ((RSAKey) certificate.getPublicKey()).getModulus().bitLength();

        return (bitLength >>> 3) + (((bitLength & 0x7) != 0) ? 1 : 0);
    }

    public static RsaSignature sign(byte[] digest, String digestAlgorithmOid, PrivateKey privateKey, Provider provider) {
        try {
            byte[] data = getDigestInfo(digest, digestAlgorithmOid);
            byte[] signature = doSign(data, "NONEwithRSA", privateKey, provider);

            return new RsaSignature(signature, digestAlgorithmOid);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getDigestInfo(byte[] digest, String digestAlgorithmOid) {
        return getDerEncoded(new DigestInfo(getAlgorithmIdentifier(digestAlgorithmOid), digest));
    }
}
