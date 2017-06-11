package com.github.gino0631.xar.impl.signature;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cms.CMSSignatureEncryptionAlgorithmFinder;
import org.bouncycastle.cms.DefaultCMSSignatureEncryptionAlgorithmFinder;
import org.bouncycastle.operator.AlgorithmNameFinder;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class CmsSignature extends AbstractSignature {
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static final ASN1ObjectIdentifier ENCRYPTION_ALGORITHM_OID = PKCSObjectIdentifiers.rsaEncryption;

    private ContentInfo contentInfo;
    private SignerInfo signerInfo;
    private TimeStampToken timeStampToken;
    private byte[] encoded;

    public CmsSignature(byte[] signatureBytes) {
        this(getContentInfo(signatureBytes));
    }

    private CmsSignature(ContentInfo contentInfo) {
        this.contentInfo = contentInfo;
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.signedData)) {
            throw new IllegalStateException(MessageFormat.format("Unexpected content type {0}", contentInfo.getContentType()));
        }

        SignedData signedData = SignedData.getInstance(contentInfo.getContent());

        ASN1Set signerInfos = signedData.getSignerInfos();
        if (signerInfos.size() != 1) {
            throw new IllegalStateException(MessageFormat.format("Unexpected signer info count {0}", signerInfos.size()));
        }
        signerInfo = SignerInfo.getInstance(signerInfos.getObjectAt(0));

        ASN1Set unsignedAttrs = signerInfo.getUnauthenticatedAttributes();
        if (unsignedAttrs != null) {
            timeStampToken = getTimeStampToken(new AttributeTable(unsignedAttrs), signerInfo.getEncryptedDigest().getOctets());
        }
    }

    @Override
    public TimeStampToken getTimestamp() {
        return timeStampToken;
    }

    @Override
    public boolean verify(byte[] data, List<X509Certificate> certificates) {
        try {
            String sigAlgName = getSignatureAlgorithmName(signerInfo.getDigestAlgorithm(), signerInfo.getDigestEncryptionAlgorithm());
            byte[] signatureBytes = signerInfo.getEncryptedDigest().getOctets();

            return doVerify(data, signatureBytes, sigAlgName, certificates.get(0).getPublicKey());

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSize() {
        return getEncoded().length;
    }

    @Override
    public void writeTo(OutputStream output) throws IOException {
        output.write(getEncoded());
    }

    public static int estimateSize(String digestAlgorithmOid, List<X509Certificate> certificates, boolean withTimestamp) {
        byte[] signature = new byte[RsaSignature.estimateSize(certificates.get(0))];

        return roundSize(getDerEncoded(getContentInfo(signature, digestAlgorithmOid, certificates, null)).length +
                (withTimestamp ? 6 * 1024 : 0));
    }

    public static int roundSize(int size) {
        return ((size & 0x3ff) != 0) ? (size & ~0x3ff) + 0x400 : size;  // multiple of 1024
    }

    public static CmsSignature sign(byte[] data, String digestAlgorithmOid, PrivateKey privateKey, List<X509Certificate> certificates, Provider provider, URL tsa) throws IOException {
        try {
            AlgorithmIdentifier digestAlgorithmIdentifier = getAlgorithmIdentifier(digestAlgorithmOid);
            String sigAlgName = getSignatureAlgorithmName(digestAlgorithmIdentifier, getAlgorithmIdentifier(ENCRYPTION_ALGORITHM_OID));
            byte[] signature = doSign(data, sigAlgName, privateKey, provider);

            TimeStampToken timeStampToken = null;
            if (tsa != null) {
                byte[] digest = getDigest(signature, digestAlgorithmIdentifier);
                timeStampToken = doTimestamp(digestAlgorithmIdentifier.getAlgorithm(), digest, tsa);
            }

            return new CmsSignature(getContentInfo(signature, digestAlgorithmOid, certificates, timeStampToken));

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getEncoded() {
        if (encoded == null) {
            encoded = getDerEncoded(contentInfo);
        }

        return encoded;
    }

    private static ContentInfo getContentInfo(byte[] data) {
        try {
            return ContentInfo.getInstance(new ASN1InputStream(data).readObject());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ContentInfo getContentInfo(byte[] signature, String digestAlgorithmOid, List<X509Certificate> certificates, TimeStampToken timeStampToken) {
        Certificate[] certs = certificates.stream()
                .map(c -> Certificate.getInstance(getEncoded(c)))
                .collect(Collectors.toList())
                .toArray(new Certificate[certificates.size()]);

        AlgorithmIdentifier digestAlgorithmId = getAlgorithmIdentifier(digestAlgorithmOid);
        AlgorithmIdentifier encryptionAlgorithmId = getAlgorithmIdentifier(ENCRYPTION_ALGORITHM_OID);

        Attributes unsignedAttrs = null;
        if (timeStampToken != null) {
            Attribute tsAttr = new Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                    new DLSet(timeStampToken.toCMSSignedData().toASN1Structure()));
            unsignedAttrs = new AttributeTable(tsAttr).toASN1Structure();
        }

        SignerInfo signerInfo = new SignerInfo(
                new SignerIdentifier(new IssuerAndSerialNumber(certs[0])),
                digestAlgorithmId,
                null,
                encryptionAlgorithmId,
                new DEROctetString(signature),
                unsignedAttrs
        );

        SignedData signedData = new SignedData(
                new DERSet(digestAlgorithmId),
                new ContentInfo(CMSObjectIdentifiers.data, null),
                new DLSet(certs),
                null,
                new DERSet(signerInfo)
        );

        return new ContentInfo(CMSObjectIdentifiers.signedData, signedData);
    }

    /**
     * Gets time stamp token.
     * <p>
     * Based on {@link org.bouncycastle.tsp.TSPUtil#getSignatureTimestamps(org.bouncycastle.cms.SignerInformation, org.bouncycastle.operator.DigestCalculatorProvider)}}
     */
    private static TimeStampToken getTimeStampToken(AttributeTable attributeTable, byte[] signatureBytes) {
        ASN1EncodableVector tstAttrs = attributeTable.getAll(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);

        for (int i = 0; i < tstAttrs.size(); ++i) {
            Attribute tsAttr = (Attribute) tstAttrs.get(i);

            for (ASN1Encodable obj : tsAttr.getAttrValues()) {
                try {
                    ContentInfo contentInfo = ContentInfo.getInstance(obj);
                    TimeStampToken timeStampToken = new TimeStampToken(contentInfo);
                    TimeStampTokenInfo tstInfo = timeStampToken.getTimeStampInfo();

                    if (Arrays.equals(getDigest(signatureBytes, tstInfo.getHashAlgorithm()), tstInfo.getMessageImprintDigest())) {
                        return timeStampToken;

                    } else {
                        logger.warning("Incorrect digest in message imprint");
                    }

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Timestamp could not be parsed", e);
                }
            }
        }

        return null;
    }

    private static String getSignatureAlgorithmName(AlgorithmIdentifier digestAlgId, AlgorithmIdentifier encOrSigAlgId) {
        AlgorithmNameFinder algorithmNameFinder = new DefaultAlgorithmNameFinder();
        CMSSignatureEncryptionAlgorithmFinder encryptionAlgorithmFinder = new DefaultCMSSignatureEncryptionAlgorithmFinder();
        encOrSigAlgId = encryptionAlgorithmFinder.findEncryptionAlgorithm(encOrSigAlgId);

        return algorithmNameFinder.getAlgorithmName(digestAlgId) + "with" + algorithmNameFinder.getAlgorithmName(encOrSigAlgId);
    }
}
