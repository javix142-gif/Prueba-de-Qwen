package cl.javier.salaremote.net;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TvDiscovery {
    public interface Listener {
        void onDevice(String name, String host);
        void onState(String text);
    }

    private static final String TYPE = "_androidtvremote2._tcp.";
    private final NsdManager nsd;
    private final WifiManager wifi;
    private final Listener listener;
    private NsdManager.DiscoveryListener discovery;
    private WifiManager.MulticastLock multicastLock;
    private final Map<String, String> seen = new LinkedHashMap<>();

    public TvDiscovery(Context context, Listener listener) {
        this.nsd = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.listener = listener;
    }

    public void start() {
        stop();
        seen.clear();
        try {
            multicastLock = wifi.createMulticastLock("SalaRemoteDiscovery");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        } catch (Exception ignored) {}

        discovery = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String serviceType) { listener.onState("Buscando TVs en tu Wi‑Fi…"); }
            @Override public void onServiceFound(NsdServiceInfo serviceInfo) {
                if (!serviceInfo.getServiceType().contains("androidtvremote2")) return;
                nsd.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}
                    @Override public void onServiceResolved(NsdServiceInfo resolved) {
                        InetAddress address = resolved.getHost();
                        if (address == null) return;
                        String host = address.getHostAddress();
                        if (host == null || seen.containsKey(host)) return;
                        String name = resolved.getServiceName();
                        seen.put(host, name);
                        listener.onDevice(name == null || name.isBlank() ? "Android TV" : name, host);
                        listener.onState("TV encontrada");
                    }
                });
            }
            @Override public void onServiceLost(NsdServiceInfo serviceInfo) {}
            @Override public void onDiscoveryStopped(String serviceType) {}
            @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) { listener.onState("No se pudo iniciar la búsqueda"); stop(); }
            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) { stop(); }
        };
        try { nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, discovery); }
        catch (Exception e) { listener.onState("Búsqueda no disponible"); }
    }

    public void stop() {
        if (discovery != null) {
            try { nsd.stopServiceDiscovery(discovery); } catch (Exception ignored) {}
            discovery = null;
        }
        if (multicastLock != null) {
            try { if (multicastLock.isHeld()) multicastLock.release(); } catch (Exception ignored) {}
            multicastLock = null;
        }
    }
}
