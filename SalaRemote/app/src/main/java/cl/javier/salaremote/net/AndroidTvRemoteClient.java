package cl.javier.salaremote.net;

import android.content.Context;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.*;

public final class AndroidTvRemoteClient {
    public interface Listener {
        void onStatus(String status, boolean connected);
        void onError(String message);
    }

    private static final int PAIR_PORT = 6467;
    private static final int REMOTE_PORT = 6466;
    private static final int ACTIVE_FEATURES = 1 | 2 | 32 | 64 | 512;

    private final Context context;
    private final Listener listener;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Object sendLock = new Object();

    private volatile SSLSocket remoteSocket;
    private volatile OutputStream remoteOut;
    private SSLSocket pairingSocket;
    private X509Certificate pairingServerCert;
    private ClientIdentity identity;

    public AndroidTvRemoteClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void connect(String host, Runnable needsPairing, Runnable connected) {
        io.execute(() -> {
            closeRemote();
            try {
                ensureIdentity();
                SSLSocket socket = createSocket(host, REMOTE_PORT);
                remoteSocket = socket;
                remoteOut = socket.getOutputStream();
                postStatus("Conectando…", false);
                Thread reader = new Thread(() -> remoteReadLoop(socket, connected), "tv-remote-reader");
                reader.setDaemon(true);
                reader.start();
            } catch (SSLHandshakeException e) {
                postStatus("TV encontrada · falta vincular", false);
                needsPairing.run();
            } catch (Exception e) {
                postError("No fue posible conectar con la TV: " + shortMessage(e));
            }
        });
    }

    public void startPairing(String host, Runnable showPinDialog) {
        io.execute(() -> {
            closePairing();
            try {
                ensureIdentity();
                pairingSocket = createSocket(host, PAIR_PORT);
                java.security.cert.Certificate[] peer = pairingSocket.getSession().getPeerCertificates();
                pairingServerCert = (X509Certificate) peer[0];
                InputStream in = pairingSocket.getInputStream();
                OutputStream out = pairingSocket.getOutputStream();

                Proto.writeFrame(out, outer(Proto.fieldBytes(10,
                        Proto.concat(Proto.fieldString(1, "atvremote"), Proto.fieldString(2, "Sala Remote")))));

                boolean configured = false;
                long deadline = System.currentTimeMillis() + 15000;
                while (!configured && System.currentTimeMillis() < deadline) {
                    Map<Integer, List<Proto.Value>> m = Proto.parse(Proto.readFrame(in));
                    if (Proto.first(m, 11) != null) {
                        byte[] encoding = Proto.concat(Proto.fieldVarint(1, 3), Proto.fieldVarint(2, 6));
                        byte[] options = Proto.concat(Proto.fieldBytes(1, encoding), Proto.fieldVarint(3, 1));
                        Proto.writeFrame(out, outer(Proto.fieldBytes(20, options)));
                    } else if (Proto.first(m, 20) != null) {
                        byte[] encoding = Proto.concat(Proto.fieldVarint(1, 3), Proto.fieldVarint(2, 6));
                        byte[] config = Proto.concat(Proto.fieldBytes(1, encoding), Proto.fieldVarint(2, 1));
                        Proto.writeFrame(out, outer(Proto.fieldBytes(30, config)));
                    } else if (Proto.first(m, 31) != null) {
                        configured = true;
                    } else {
                        Proto.Value status = Proto.first(m, 2);
                        if (status != null && status.number != 200) throw new IOException("Pairing status " + status.number);
                    }
                }
                if (!configured) throw new IOException("La TV no completó el inicio de vinculación");
                postStatus("Ingresa el código mostrado en la TV", false);
                showPinDialog.run();
            } catch (Exception e) {
                closePairing();
                postError("No se pudo iniciar la vinculación: " + shortMessage(e));
            }
        });
    }

    public void finishPairing(String code, String host, Runnable paired) {
        io.execute(() -> {
            try {
                if (pairingSocket == null || pairingServerCert == null) throw new IOException("La sesión de vinculación expiró");
                String pin = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
                if (!pin.matches("[0-9A-F]{6}")) throw new IllegalArgumentException("El código debe tener 6 caracteres hexadecimales");
                byte[] secret = makeSecret(pin, identity.certificate(), pairingServerCert);
                Proto.writeFrame(pairingSocket.getOutputStream(), outer(Proto.fieldBytes(40, Proto.fieldBytes(1, secret))));
                InputStream in = pairingSocket.getInputStream();
                long deadline = System.currentTimeMillis() + 10000;
                boolean ok = false;
                while (!ok && System.currentTimeMillis() < deadline) {
                    Map<Integer, List<Proto.Value>> m = Proto.parse(Proto.readFrame(in));
                    Proto.Value status = Proto.first(m, 2);
                    if (status != null && status.number != 200) throw new IOException("Código rechazado por la TV");
                    if (Proto.first(m, 41) != null) ok = true;
                }
                if (!ok) throw new IOException("La TV no confirmó la vinculación");
                closePairing();
                postStatus("Vinculación completada", false);
                paired.run();
            } catch (Exception e) {
                postError("No se pudo completar la vinculación: " + shortMessage(e));
            }
        });
    }

    public void sendKey(int keyCode) {
        io.execute(() -> {
            try {
                OutputStream out = remoteOut;
                if (out == null) throw new IOException("No hay una TV conectada");
                byte[] inject = Proto.concat(Proto.fieldVarint(1, keyCode), Proto.fieldVarint(2, 3));
                synchronized (sendLock) { Proto.writeFrame(out, Proto.fieldBytes(10, inject)); }
            } catch (Exception e) { postError("No se pudo enviar el comando"); }
        });
    }

    public void disconnect() { io.execute(this::closeRemote); }

    private void remoteReadLoop(SSLSocket socket, Runnable connected) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            boolean readyNotified = false;
            while (!socket.isClosed()) {
                byte[] frame = Proto.readFrame(in);
                Map<Integer, List<Proto.Value>> m = Proto.parse(frame);
                if (Proto.first(m, 1) != null) {
                    byte[] deviceInfo = Proto.concat(Proto.fieldVarint(3, 1), Proto.fieldString(4, "1"), Proto.fieldString(5, "cl.javier.salaremote"), Proto.fieldString(6, "1.0.0"));
                    byte[] cfg = Proto.concat(Proto.fieldVarint(1, ACTIVE_FEATURES), Proto.fieldBytes(2, deviceInfo));
                    synchronized (sendLock) { Proto.writeFrame(out, Proto.fieldBytes(1, cfg)); }
                } else if (Proto.first(m, 2) != null) {
                    synchronized (sendLock) { Proto.writeFrame(out, Proto.fieldBytes(2, Proto.fieldVarint(1, ACTIVE_FEATURES))); }
                } else if (Proto.first(m, 8) != null) {
                    Proto.Value ping = Proto.first(m, 8);
                    long val = 1;
                    if (ping != null && ping.bytes != null) {
                        Proto.Value v = Proto.first(Proto.parse(ping.bytes), 1);
                        if (v != null) val = v.number;
                    }
                    synchronized (sendLock) { Proto.writeFrame(out, Proto.fieldBytes(9, Proto.fieldVarint(1, val))); }
                } else if (Proto.first(m, 40) != null && !readyNotified) {
                    readyNotified = true;
                    postStatus("Conectado", true);
                    connected.run();
                }
            }
        } catch (Exception e) {
            if (remoteSocket == socket) { closeRemote(); postStatus("Desconectado", false); }
        }
    }

    private SSLSocket createSocket(String host, int port) throws Exception {
        SSLContext ssl = buildSslContext();
        SSLSocket s = (SSLSocket) ssl.getSocketFactory().createSocket();
        s.connect(new InetSocketAddress(host, port), 5000);
        s.setSoTimeout(16000);
        s.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
        s.startHandshake();
        return s;
    }

    private SSLContext buildSslContext() throws Exception {
        KeyStore ks = identity.asKeyStore();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, identity.password());
        TrustManager[] trust = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }};
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), trust, new SecureRandom());
        return ctx;
    }

    private void ensureIdentity() throws Exception { if (identity == null) identity = ClientIdentity.loadOrCreate(context); }
    private static byte[] outer(byte[] payload) { return Proto.concat(Proto.fieldVarint(1, 2), Proto.fieldVarint(2, 200), payload); }

    private static byte[] makeSecret(String pin, X509Certificate client, X509Certificate server) throws Exception {
        RSAPublicKey c = (RSAPublicKey) client.getPublicKey();
        RSAPublicKey s = (RSAPublicKey) server.getPublicKey();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(unsigned(c.getModulus())); sha.update(unsigned(c.getPublicExponent()));
        sha.update(unsigned(s.getModulus())); sha.update(unsigned(s.getPublicExponent()));
        sha.update(hexToBytes(pin.substring(2)));
        byte[] result = sha.digest();
        if ((result[0] & 0xff) != Integer.parseInt(pin.substring(0, 2), 16)) throw new IllegalArgumentException("Código incorrecto");
        return result;
    }

    private static byte[] unsigned(BigInteger n) {
        byte[] b = n.toByteArray();
        return b.length > 1 && b[0] == 0 ? Arrays.copyOfRange(b, 1, b.length) : b;
    }
    private static byte[] hexToBytes(String hex) {
        if ((hex.length() & 1) != 0) hex = "0" + hex;
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte)Integer.parseInt(hex.substring(i*2, i*2+2), 16);
        return out;
    }
    private void closeRemote() { SSLSocket s = remoteSocket; remoteSocket = null; remoteOut = null; if (s != null) try { s.close(); } catch (IOException ignored) {} }
    private void closePairing() { SSLSocket s = pairingSocket; pairingSocket = null; pairingServerCert = null; if (s != null) try { s.close(); } catch (IOException ignored) {} }
    private void postStatus(String text, boolean connected) { listener.onStatus(text, connected); }
    private void postError(String text) { listener.onError(text); }
    private static String shortMessage(Throwable t) { String m = t.getMessage(); return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m; }
}
