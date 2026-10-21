package rachel.materialapps.recordmylive;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.logic.nearby.NearbySession;
import static org.junit.Assert.*;
import static rachel.materialapps.recordmylive.logic.nearby.NearbySession.Stage.*;

public class NearbySessionTest {
    @Test public void matchingWaitsForLocalDataAndUpdatesAfterEditsAndPeerChanges() {
        session.start("本人");
        transport.finish(true);
        transport.events().found("a", "附近用户");
        java.util.Set<String> keys = new java.util.HashSet<>();
        keys.add(rachel.materialapps.recordmylive.logic.nearby.ArtistMatchProtocol.key("taylor  swift"));
        transport.events().artists("a", keys);
        assertFalse(state.peers.get(0).matched);
        session.setArtists(java.util.Arrays.asList(new NearbySession.Artist("Taylor Swift", "local.webp"),
                new NearbySession.Artist("司南", null)));
        assertTrue(state.peers.get(0).matched);
        assertEquals(1, state.peers.get(0).commonArtists.size());
        assertEquals("local.webp", state.peers.get(0).commonArtists.get(0).imageFile);
        keys.clear(); // caller cannot mutate a published snapshot
        assertEquals(1, state.peers.get(0).commonArtists.size());
        NearbySession.Snapshot before = state;
        session.setArtists(java.util.Collections.emptyList());
        assertTrue(state.peers.get(0).commonArtists.isEmpty());
        assertEquals(1, before.peers.get(0).commonArtists.size());
        transport.events().artists("a", null);
        assertTrue(state.peers.get(0).matchFailed);
        transport.events().artists("a", java.util.Collections.emptySet());
        assertTrue(state.peers.get(0).matched);
        assertFalse(state.peers.get(0).matchFailed);
        session.stop();
        transport.events().artists("a", java.util.Collections.emptySet());
        assertTrue(state.peers.isEmpty());
        session.start("本人");
        transport.finish(true);
        transport.events().found("a", "附近用户");
        transport.events().artists("a", java.util.Collections.emptySet());
        assertFalse(state.peers.get(0).matched); // fresh session must await fresh local preferences
    }
    private final FakeTransport transport = new FakeTransport();
    private NearbySession.Snapshot state;
    private final NearbySession session = new NearbySession(transport, value -> state = value);

    @Test public void activeDiscoveryKeepsDistinctEndpointsWithTheSameNickname() {
        assertEquals(IDLE, state.stage);
        session.start("本人");
        assertEquals(STARTING, state.stage);
        transport.finish(true);
        assertEquals(ACTIVE, state.stage);
        transport.events().found("a", "小雨");
        transport.events().found("b", "小雨");
        transport.events().found("a", "新昵称");
        assertEquals(2, state.peers.size());
        assertEquals("新昵称", state.peers.get(0).nickname);
        transport.events().lost("a");
        assertEquals("b", state.peers.get(0).id);
        session.stop();
        assertEquals(IDLE, state.stage);
        assertTrue(state.peers.isEmpty());
        transport.events().found("late", "不会显示");
        assertTrue(state.peers.isEmpty());
    }

    @Test public void partialStartFailureStopsBothOperationsAndClearsPeers() {
        session.start("本人");
        transport.events().found("a", "小雨");
        transport.finish(false);
        assertEquals(ERROR, state.stage);
        assertEquals(NearbySession.Issue.START_FAILED, state.issue);
        assertTrue(state.peers.isEmpty());
        assertEquals(1, transport.stops);
        transport.events().found("b", "延迟事件");
        assertTrue(state.peers.isEmpty());
    }

    @Test public void stopWhileStartingWaitsForLateCompletionAndCleansUpAgain() {
        session.start("本人");
        session.stop();
        assertEquals(STOPPING, state.stage);
        session.start("不能提前重启");
        assertEquals(1, transport.callbacks.size());
        transport.events().found("late", "延迟事件");
        assertTrue(state.peers.isEmpty());
        transport.finish(true);
        assertEquals(IDLE, state.stage);
        assertEquals(2, transport.stops);
        session.start("现在可以重启");
        assertEquals(2, transport.callbacks.size());
    }

    @Test public void previousSessionCallbacksCannotChangeRestartedSession() {
        session.start("本人");
        NearbySession.Events oldEvents = transport.events();
        Consumer<Boolean> oldCompletion = transport.callbacks.get(0);
        transport.finish(true);
        session.stop();
        session.start("本人");
        oldEvents.found("old", "上轮用户");
        oldCompletion.accept(false);
        assertEquals(STARTING, state.stage);
        transport.events().found("new", "本轮用户");
        oldEvents.lost("new");
        transport.finish(true);
        assertEquals(ACTIVE, state.stage);
        assertEquals(1, state.peers.size());
        assertEquals("new", state.peers.get(0).id);
    }

    @Test public void snapshotsAreStableAndPeerCountIsBounded() {
        session.start("本人");
        transport.finish(true);
        for (int i = 0; i < 150; i++) transport.events().found("id" + i, "昵称");
        assertEquals(100, state.peers.size());
        NearbySession.Snapshot old = state;
        transport.events().lost("id0");
        assertEquals(100, old.peers.size());
        assertEquals(99, state.peers.size());
        transport.events().found("new", "昵称");
        assertEquals(100, state.peers.size());
    }

    private static class FakeTransport implements NearbySession.Transport {
        final List<NearbySession.Events> listeners = new ArrayList<>();
        final List<Consumer<Boolean>> callbacks = new ArrayList<>();
        int stops;
        @Override public void start(String name, NearbySession.Events events, Consumer<Boolean> complete) {
            listeners.add(events);
            callbacks.add(complete);
        }
        @Override public void stop() { stops++; }
        NearbySession.Events events() { return listeners.get(listeners.size() - 1); }
        void finish(boolean success) { callbacks.get(callbacks.size() - 1).accept(success); }
    }
}
