package rachel.materialapps.recordmylive;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import rachel.materialapps.recordmylive.logic.nearby.MatchAlertTracker;
import static org.junit.Assert.*;

public class MatchAlertTrackerTest {
    @Test public void deduplicatesRepeatedMatchesDeparturesAndReturnsUntilSessionEnds() {
        MatchAlertTracker tracker = new MatchAlertTracker();
        assertFalse(tracker.take(Collections.emptyList()));
        assertTrue(tracker.take(Arrays.asList("a", "a", "b")));
        assertFalse(tracker.take(Collections.singletonList("a")));
        assertFalse(tracker.take(Collections.emptyList()));
        assertFalse(tracker.take(Arrays.asList("a", "b")));
        assertTrue(tracker.take(Arrays.asList("a", "c")));
        tracker.clear();
        assertTrue(tracker.take(Collections.singletonList("a")));
    }

    @Test public void boundsMemoryDuringLongSessionsAndResetsForNextSession() {
        MatchAlertTracker tracker = new MatchAlertTracker();
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 2000; i++) many.add("id" + i);
        assertTrue(tracker.take(many));
        assertFalse(tracker.take(many));
        tracker.clear();
        assertTrue(tracker.take(Collections.singletonList("id1999")));
    }
}
