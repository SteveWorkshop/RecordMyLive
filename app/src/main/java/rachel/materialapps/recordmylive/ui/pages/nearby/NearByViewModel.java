package rachel.materialapps.recordmylive.ui.pages.nearby;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import rachel.materialapps.recordmylive.logic.nearby.NearbyDiscovery;
import rachel.materialapps.recordmylive.logic.nearby.NearbySession;

public class NearByViewModel extends AndroidViewModel {
    private final NearbyDiscovery discovery;

    public NearByViewModel(@NonNull Application application) {
        super(application);
        discovery = NearbyDiscovery.get(application);
    }
    public LiveData<NearbySession.Snapshot> getState() { return discovery.getState(); }
    public void start() { discovery.requestStart(); }
    public void stop() { discovery.stop(); }
    public void showIssue(NearbySession.Issue issue) { discovery.showIssue(issue); }
    public void clearIssue() { discovery.clearIssue(); }
    public String getNickname(String savedNickname) {
        return discovery.isRunning() ? discovery.getBroadcastNickname() : savedNickname;
    }
}
