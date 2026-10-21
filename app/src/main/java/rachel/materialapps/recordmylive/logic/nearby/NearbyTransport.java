package rachel.materialapps.recordmylive.logic.nearby;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.*;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository;

/** Connections exist only during explicitly enabled discovery. No remote files accepted. */
public final class NearbyTransport implements NearbySession.Transport {
    private final ConnectionsClient client;
    private final String serviceId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Link> links = new HashMap<>();
    private Collection<String> artistKeys = Collections.emptySet();
    private NearbySession.Events events;
    private boolean running, artistsLoaded;
    private int generation;
    private String endpointName;
    private static final class Link { boolean connected, received; }

    public NearbyTransport(Context context) {
        client = Nearby.getConnectionsClient(context.getApplicationContext());
        serviceId = context.getPackageName() + ".nearby.v2";
    }

    @Override public void setArtists(Collection<String> keys) {
        artistKeys = new LinkedHashSet<>(keys);
        artistsLoaded = true;
        if (running) for (String id : new ArrayList<>(links.keySet()))
            if (links.get(id).connected) send(id);
    }

    @Override public void clearArtists() {
        artistKeys = Collections.emptySet();
        artistsLoaded = false;
    }

    private static String nickname(String endpoint) {
        if (endpoint == null || !endpoint.matches("g2:[0-9a-f]{4}:[\\s\\S]+")
                || endpoint.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 128) return null;
        String name = ProfileRepository.normalizeNickname(endpoint.substring(8));
        return ProfileRepository.isValidNickname(name) ? name : null;
    }

    @Override public void start(String name, NearbySession.Events listener, Consumer<Boolean> complete) {
        running = true;
        events = listener;
        int token = ++generation;
        endpointName = String.format(Locale.ROOT, "g2:%04x:", new java.security.SecureRandom().nextInt(65536)) + name;
        ConnectionLifecycleCallback lifecycle = new ConnectionLifecycleCallback() {
            @Override public void onConnectionInitiated(String id, ConnectionInfo info) {
                if (!current(token)) return;
                String peerName = nickname(info.getEndpointName());
                if (peerName == null || (links.size() >= 100 && !links.containsKey(id))) {
                    try { client.rejectConnection(id); } catch (RuntimeException ignored) { }
                    return;
                }
                add(id, peerName, token);
                try {
                    client.acceptConnection(id, new PayloadCallback() {
                        @Override public void onPayloadReceived(String endpoint, Payload payload) {
                            if (!current(token) || !links.containsKey(endpoint)) return;
                            if (payload.getType() != Payload.Type.BYTES) {
                                try { client.cancelPayload(payload.getId()); } catch (RuntimeException ignored) { }
                                fail(endpoint);
                                return;
                            }
                            try {
                                Set<String> keys = ArtistMatchProtocol.decode(payload.asBytes());
                                links.get(endpoint).received = true;
                                events.artists(endpoint, keys);
                            } catch (IllegalArgumentException e) { fail(endpoint); }
                        }
                        @Override public void onPayloadTransferUpdate(String endpoint, PayloadTransferUpdate update) {
                            if (current(token) && (update.getStatus() == PayloadTransferUpdate.Status.FAILURE
                                    || update.getStatus() == PayloadTransferUpdate.Status.CANCELED)) fail(endpoint);
                        }
                    }).addOnFailureListener(error -> { if (current(token)) fail(id); });
                } catch (RuntimeException e) { fail(id); }
            }
            @Override public void onConnectionResult(String id, ConnectionResolution result) {
                if (!current(token) || !links.containsKey(id)) return;
                if (!result.getStatus().isSuccess()) { fail(id); return; }
                links.get(id).connected = true;
                if (artistsLoaded) send(id);
            }
            @Override public void onDisconnected(String id) {
                if (!current(token)) return;
                if (links.remove(id) != null) events.lost(id);
            }
        };
        Task<Void> advertising;
        Task<Void> discovery;
        try {
            advertising = client.startAdvertising(endpointName, serviceId, lifecycle,
                    new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build());
        } catch (RuntimeException e) { advertising = Tasks.forException(e); }
        try {
            discovery = client.startDiscovery(serviceId, new EndpointDiscoveryCallback() {
                @Override public void onEndpointFound(String id, DiscoveredEndpointInfo info) {
                    if (!current(token) || !serviceId.equals(info.getServiceId())) return;
                    String peerName = nickname(info.getEndpointName());
                    if (peerName == null || links.containsKey(id) || links.size() >= 100) return;
                    add(id, peerName, token);
                    // Both peers agree who initiates, even with identical nicknames.
                    if (endpointName.compareTo(info.getEndpointName()) <= 0) {
                        try { client.requestConnection(endpointName, id, lifecycle); }
                        catch (RuntimeException e) { fail(id); }
                    }
                }
                @Override public void onEndpointLost(String id) {
                    if (!current(token)) return;
                    Link link = links.get(id);
                    // Discovery can report lost during an established connection.
                    if (link == null || !link.connected) { links.remove(id); events.lost(id); }
                }
            }, new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build());
        } catch (RuntimeException e) { discovery = Tasks.forException(e); }
        Task<Void> advertiseTask = advertising, discoverTask = discovery;
        advertising.addOnCompleteListener(result -> { if (!running) stop(); });
        discovery.addOnCompleteListener(result -> { if (!running) stop(); });
        Tasks.whenAllComplete(advertising, discovery).addOnCompleteListener(
                result -> complete.accept(advertiseTask.isSuccessful() && discoverTask.isSuccessful()));
    }

    private boolean current(int token) { return running && generation == token; }

    private void add(String id, String name, int token) {
        if (!links.containsKey(id)) {
            Link link = new Link();
            links.put(id, link);
            handler.postDelayed(() -> {
                if (current(token) && links.get(id) == link && !link.received) fail(id);
            }, 20000);
        }
        events.found(id, name);
    }

    private void send(String id) {
        int token = generation;
        try {
            client.sendPayload(id, Payload.fromBytes(ArtistMatchProtocol.encode(artistKeys)))
                    .addOnFailureListener(error -> { if (current(token)) fail(id); });
        } catch (RuntimeException e) { fail(id); }
    }

    private void fail(String id) {
        Link link = links.get(id);
        if (link == null) return;
        link.connected = false;
        events.artists(id, null);
        try { client.disconnectFromEndpoint(id); } catch (RuntimeException ignored) { }
    }

    @Override public void stop() {
        running = false;
        links.clear();
        handler.removeCallbacksAndMessages(null);
        try { client.stopAdvertising(); } catch (RuntimeException ignored) { }
        try { client.stopDiscovery(); } catch (RuntimeException ignored) { }
        try { client.stopAllEndpoints(); } catch (RuntimeException ignored) { }
    }
}
