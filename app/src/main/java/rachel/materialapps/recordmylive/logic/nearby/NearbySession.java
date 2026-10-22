package rachel.materialapps.recordmylive.logic.nearby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.function.Consumer;

/** Discovery state owned by the foreground service. All calls and callbacks run on the main thread. */
public final class NearbySession {
    public enum Stage { IDLE, STARTING, ACTIVE, STOPPING, ERROR }
    public enum Issue { NONE, NAME_REQUIRED, PERMISSION, SERVICES, BLUETOOTH, LOCATION, START_FAILED }
    public static final class Peer {
        public final String id, nickname;
        public final boolean matched, matchFailed;
        public final List<Artist> commonArtists;
        private final Set<String> keys;
        public Peer(String id, String nickname) { this(id, nickname, null, Collections.emptyList(), false, false); }
        private Peer(String id, String nickname, Set<String> keys, List<Artist> common, boolean loaded, boolean failed) {
            this.id = id; this.nickname = nickname; this.keys = keys;
            matched = keys != null && loaded;
            matchFailed = failed;
            commonArtists = Collections.unmodifiableList(common);
        }
    }
    public static final class Artist {
        public final String name, imageFile, key;
        public Artist(String name, String imageFile) {
            this.name = name; this.imageFile = imageFile; key = ArtistMatchProtocol.key(name);
        }
    }
    public static final class Snapshot {
        public final Stage stage;
        public final Issue issue;
        public final List<Peer> peers;
        private Snapshot(Stage stage, Issue issue, List<Peer> peers) {
            this.stage = stage;
            this.issue = issue;
            this.peers = Collections.unmodifiableList(peers);
        }
    }
    public interface Events {
        void found(String id, String nickname);
        void lost(String id);
        default void artists(String id, Set<String> keys) { }
    }
    public interface Transport {
        void start(String nickname, Events events, Consumer<Boolean> complete);
        void stop();
        default void setArtists(Collection<String> keys) { }
        default void clearArtists() { }
    }

    private final Transport transport;
    private final Consumer<Snapshot> observer;
    private final LinkedHashMap<String, Peer> peers = new LinkedHashMap<>();
    private List<Artist> artists = Collections.emptyList();
    private boolean artistsLoaded;

    public void setArtists(List<Artist> value) {
        artists = new ArrayList<>(value);
        artistsLoaded = true;
        List<String> keys = new ArrayList<>();
        for (Artist artist : artists) keys.add(artist.key);
        transport.setArtists(keys);
        peers.replaceAll((id, peer) -> match(peer.id, peer.nickname, peer.keys, peer.matchFailed));
        publish();
    }

    private Peer match(String id, String name, Set<String> keys, boolean failed) {
        List<Artist> common = new ArrayList<>();
        Set<String> added = new java.util.HashSet<>();
        if (keys != null) for (Artist artist : artists)
            if (keys.contains(artist.key) && added.add(artist.key)) common.add(artist);
        return new Peer(id, name, keys, common, artistsLoaded, failed);
    }
    private Stage stage = Stage.IDLE;
    private Issue issue = Issue.NONE;
    private boolean wanted, pending;
    private int generation;

    public NearbySession(Transport transport, Consumer<Snapshot> observer) {
        this.transport = transport;
        this.observer = observer;
        publish();
    }

    public void start(String nickname) {
        if (wanted || pending) return;
        wanted = pending = true;
        int token = ++generation;
        peers.clear();
        stage = Stage.STARTING;
        issue = Issue.NONE;
        publish();
        transport.start(nickname, new Events() {
            @Override public void found(String id, String name) {
                if (!wanted || token != generation) return;
                if (peers.size() >= 100 && !peers.containsKey(id)) return;
                Peer old = peers.get(id);
                peers.put(id, match(id, name, old == null ? null : old.keys, old != null && old.matchFailed));
                publish();
            }
            @Override public void artists(String id, Set<String> keys) {
                if (!wanted || token != generation || !peers.containsKey(id)) return;
                Peer old = peers.get(id);
                peers.put(id, match(id, old.nickname, keys == null ? null :
                        Collections.unmodifiableSet(new java.util.HashSet<>(keys)), keys == null));
                publish();
            }
            @Override public void lost(String id) {
                if (wanted && token == generation && peers.remove(id) != null) publish();
            }
        }, success -> {
            if (token != generation) return;
            pending = false;
            if (!wanted) {
                // A start task may finish after stop; stop again before allowing another start.
                transport.stop();
                stage = Stage.IDLE;
            } else if (!success) {
                wanted = false;
                transport.stop();
                peers.clear();
                stage = Stage.ERROR;
                issue = Issue.START_FAILED;
            } else stage = Stage.ACTIVE;
            publish();
        });
    }

    public void stop() {
        wanted = false;
        transport.stop();
        peers.clear();
        issue = Issue.NONE;
        stage = pending ? Stage.STOPPING : Stage.IDLE;
        publish();
    }

    public void showIssue(Issue value) {
        if (wanted || pending) return;
        stage = Stage.ERROR;
        issue = value;
        publish();
    }

    private void publish() { observer.accept(new Snapshot(stage, issue, new ArrayList<>(peers.values()))); }
}
