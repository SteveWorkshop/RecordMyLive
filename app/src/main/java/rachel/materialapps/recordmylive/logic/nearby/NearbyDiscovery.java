package rachel.materialapps.recordmylive.logic.nearby;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.function.Consumer;

/** Process-local state shared by the service and any number of page instances. Never persisted. */
public final class NearbyDiscovery {
    private static NearbyDiscovery instance;
    private final Context context;
    private final MutableLiveData<NearbySession.Snapshot> state = new MutableLiveData<>();
    private final NearbySession session;
    private String broadcastNickname = "";

    public static synchronized NearbyDiscovery get(Context context) {
        if (instance == null) instance = new NearbyDiscovery(context.getApplicationContext(), null);
        return instance;
    }

    NearbyDiscovery(Context context, NearbySession.Transport suppliedTransport) {
        this.context = context.getApplicationContext();
        NearbySession.Transport transport = suppliedTransport != null ? suppliedTransport : new NearbySession.Transport() {
            private NearbyTransport delegate;
            private java.util.Collection<String> keys;
            @Override public void clearArtists() {
                keys = null;
                if (delegate != null) delegate.clearArtists();
            }
            @Override public void setArtists(java.util.Collection<String> value) {
                keys = new java.util.ArrayList<>(value);
                if (delegate != null) delegate.setArtists(keys);
            }
            @Override public void start(String nickname, NearbySession.Events events, Consumer<Boolean> complete) {
                try {
                    if (delegate == null) delegate = new NearbyTransport(NearbyDiscovery.this.context);
                    if (keys != null) delegate.setArtists(keys);
                    delegate.start(nickname, events, complete);
                } catch (RuntimeException e) { complete.accept(false); }
            }
            @Override public void stop() { if (delegate != null) delegate.stop(); }
        };
        session = new NearbySession(transport, state::setValue);
    }

    public LiveData<NearbySession.Snapshot> getState() { return state; }
    /** Recheck observers after returning from notification permission/channel settings. */
    public void refreshState() { state.setValue(state.getValue()); }
    public void setArtists(java.util.List<NearbySession.Artist> artists) { session.setArtists(artists); }
    public String getBroadcastNickname() { return broadcastNickname; }
    public boolean isRunning() {
        NearbySession.Stage stage = state.getValue().stage;
        return stage == NearbySession.Stage.STARTING || stage == NearbySession.Stage.ACTIVE;
    }

    public void requestStart() {
        if (isRunning() || state.getValue().stage == NearbySession.Stage.STOPPING) return;
        try {
            ContextCompat.startForegroundService(context, new Intent(context, NearbyDiscoveryService.class)
                    .setAction(NearbyDiscoveryService.ACTION_START));
        } catch (RuntimeException e) { showIssue(NearbySession.Issue.START_FAILED); }
    }

    // Only the foreground service starts transport operations.
    void startSession(String nickname) {
        if (isRunning() || state.getValue().stage == NearbySession.Stage.STOPPING) return;
        broadcastNickname = nickname;
        session.start(nickname);
    }

    public void stop() { session.stop(); }
    public void showIssue(NearbySession.Issue issue) { session.showIssue(issue); }
    public void clearIssue() {
        if (state.getValue().stage == NearbySession.Stage.ERROR) session.stop();
    }
}
