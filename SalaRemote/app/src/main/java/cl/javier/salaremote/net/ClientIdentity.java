package cl.javier.salaremote.net;

import android.content.Context;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

final class ClientIdentity {
    private static final char[] PASSWORD = "SalaRemoteLocal".toCharArray();
    private final PrivateKey privateKey;
    private final X509Certificate certificate;

    private ClientIdentity(PrivateKey privateKey, X509Certificate certificate) {
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    static ClientIdentity loadOrCreate(Context context) throws Exception {
        File file = new File(context.getFilesDir(), "tv_remote_identity.p12");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        if (file.isFile()) {
            try (InputStream in = new FileInputStream(file)) { ks.load(in, PASSWORD); }
            Key key = ks.getKey("client", PASSWORD);
            java.security.cert.Certificate cert = ks.getCertificate("client");
            if (key instanceof PrivateKey && cert instanceof X509Certificate) {
                return new ClientIdentity((PrivateKey) key, (X509Certificate) cert);
            }
        }

        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 24L * 60 * 60 * 1000);
        Date notAfter = new Date(now + 3650L * 24 * 60 * 60 * 1000);
        X500Name subject = new X500Name("CN=Sala Remote");
        BigInteger serial = new BigInteger(128, new SecureRandom()).abs();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, pair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);
        cert.checkValidity(new Date());
        cert.verify(pair.getPublic());

        ks.load(null, PASSWORD);
        ks.setKeyEntry("client", pair.getPrivate(), PASSWORD, new java.security.cert.Certificate[]{cert});
        try (OutputStream out = new FileOutputStream(file)) { ks.store(out, PASSWORD); }
        return new ClientIdentity(pair.getPrivate(), cert);
    }

    KeyStore asKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, PASSWORD);
        ks.setKeyEntry("client", privateKey, PASSWORD, new java.security.cert.Certificate[]{certificate});
        return ks;
    }

    char[] password() { return PASSWORD; }
    X509Certificate certificate() { return certificate; }
}
