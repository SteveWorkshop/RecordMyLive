package rachel.materialapps.recordmylive.logic.nearby;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Session-only deduplication; no stable device identity or discovery history is stored. */
public final class MatchAlertTracker {
    private final Set<String> alerted = new HashSet<>();

    public boolean take(Collection<String> matches) {
        boolean fresh = false;
        for (String id : matches) {
            // Bound memory even if discovery is left running for a long time.
            if (alerted.size() < 1000 && alerted.add(id)) fresh = true;
        }
        return fresh;
    }

    public void clear() { alerted.clear(); }
}
