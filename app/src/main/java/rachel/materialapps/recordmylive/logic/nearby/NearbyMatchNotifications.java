package rachel.materialapps.recordmylive.logic.nearby;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.ui.MainActivity;

/** Optional alert channel, independent of the silent foreground-service notification. */
public final class NearbyMatchNotifications {
    private static final String CHANNEL = "nearby_matches";
    private static final String KEY_ENABLED = "match_alerts";
    private static final int NOTIFICATION_ID = 2003;
    private final Context context;
    private final SharedPreferences preferences;
    private final MatchAlertTracker tracker = new MatchAlertTracker();
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("nearby_preferences", Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) { return preferences(context).getBoolean(KEY_ENABLED, false); }
    public static void setEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (!enabled) context.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
    }

    public static boolean available(Context context) {
        if (!NearbyPermissions.notificationGranted(context)
                || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;
        NotificationChannel channel = context.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);
        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    public static Intent settingsIntent(Context context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled())
            return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        return new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL);
    }

    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL, context.getString(R.string.nearby_alert_channel),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.nearby_alert_channel_description));
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public NearbyMatchNotifications(Context context, Supplier<NearbySession.Snapshot> state) {
        this.context = context;
        createChannel(context);
        preferences = preferences(context);
        listener = (prefs, key) -> {
            if (KEY_ENABLED.equals(key)) update(state.get());
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void update(NearbySession.Snapshot state) {
        if (state == null) return;
        if (state.stage == NearbySession.Stage.IDLE || state.stage == NearbySession.Stage.ERROR
                || state.stage == NearbySession.Stage.STOPPING) {
            tracker.clear();
            cancel();
            return;
        }
        if (!enabled(context)) { cancel(); return; }
        if (state.stage != NearbySession.Stage.ACTIVE || !available(context)) return;
        List<String> matches = new ArrayList<>();
        for (NearbySession.Peer peer : state.peers)
            if (peer.matched && !peer.commonArtists.isEmpty()) matches.add(peer.id);
        if (!tracker.take(matches)) return;
        Intent open = new Intent(context, MainActivity.class)
                .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(NearbyDiscoveryService.EXTRA_OPEN_NEARBY, true);
        PendingIntent content = PendingIntent.getActivity(context, NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            context.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,
                    new NotificationCompat.Builder(context, CHANNEL)
                            .setSmallIcon(R.drawable.baseline_diversity_3_24px)
                            .setContentTitle(context.getString(R.string.nearby_alert_title))
                            .setContentText(context.getString(R.string.nearby_alert_text))
                            .setContentIntent(content).setAutoCancel(true).setOnlyAlertOnce(true)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .setCategory(NotificationCompat.CATEGORY_SOCIAL).build());
        } catch (SecurityException ignored) { /* Permission may be revoked between checking and posting. */ }
    }

    private void cancel() { context.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID); }
    public void close() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
        tracker.clear();
        cancel();
    }
}
