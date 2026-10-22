package rachel.materialapps.recordmylive.logic.nearby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.LiveData;
import java.util.ArrayList;
import java.util.List;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository;
import rachel.materialapps.recordmylive.ui.MainActivity;

/** User-started connected-device foreground service. No restart or boot receiver. */
public class NearbyDiscoveryService extends Service {
    public static final String ACTION_START = "rachel.materialapps.recordmylive.nearby.START";
    public static final String ACTION_STOP = "rachel.materialapps.recordmylive.nearby.STOP";
    public static final String EXTRA_OPEN_NEARBY = "open_nearby";
    private static final String CHANNEL = "nearby_discovery";
    private static final int NOTIFICATION_ID = 2001;
    private NearbyDiscovery discovery;
    private NearbyMatchNotifications matchNotifications;
    private boolean observing;
    private LiveData<List<FavoriteArtistEntity>> favoriteArtists;
    private final Observer<List<FavoriteArtistEntity>> artistObserver = value -> {
        discovery.preloadFavorites(value);
    };
    private final Observer<NearbySession.Snapshot> observer = state -> {
        matchNotifications.update(state);
        if (state.stage == NearbySession.Stage.IDLE || state.stage == NearbySession.Stage.ERROR) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        } else {
            getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,
                    notification(state.stage == NearbySession.Stage.STOPPING));
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        discovery = NearbyDiscovery.get(this);
        matchNotifications = new NearbyMatchNotifications(this, () -> discovery.getState().getValue());
        favoriteArtists = new FavoriteArtistRepository(this).getAll();
        NotificationChannel channel = new NotificationChannel(CHANNEL, getString(R.string.nearby_notification_channel),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.nearby_notification_channel_description));
        channel.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !ACTION_START.equals(intent.getAction())) {
            discovery.stop();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(false),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } catch (RuntimeException e) {
            discovery.stop();
            discovery.showIssue(NearbySession.Issue.START_FAILED);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!discovery.isRunning()) {
            String nickname = new ProfileRepository(this).getNickname();
            if (!ProfileRepository.isValidNickname(nickname)) discovery.showIssue(NearbySession.Issue.NAME_REQUIRED);
            else if (!NearbyPermissions.granted(this)) discovery.showIssue(NearbySession.Issue.PERMISSION);
            else discovery.startSession(nickname);
        }
        favoriteArtists.removeObserver(artistObserver);
        favoriteArtists.observeForever(artistObserver);
        if (!observing) {
            observing = true;
            discovery.getState().observeForever(observer);
        }
        return START_NOT_STICKY;
    }

    private Notification notification(boolean stopping) {
        Intent open = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_OPEN_NEARBY, true);
        PendingIntent content = PendingIntent.getActivity(this, 2001, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2002,
                new Intent(this, NearbyDiscoveryService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.baseline_nearby_24px)
                .setContentTitle(getString(stopping ? R.string.nearby_stopping : R.string.nearby_notification_title))
                .setContentText(getString(R.string.nearby_notification_text))
                .setContentIntent(content)
                .addAction(0, getString(R.string.nearby_stop), stop)
                .setOngoing(true).setSilent(true).setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override public void onDestroy() {
        matchNotifications.close();
        favoriteArtists.removeObserver(artistObserver);
        discovery.getState().removeObserver(observer);
        // Keep a startup error visible, but never leave a running session without its service.
        if (discovery.isRunning() || discovery.getState().getValue().stage == NearbySession.Stage.STOPPING)
            discovery.stop();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
