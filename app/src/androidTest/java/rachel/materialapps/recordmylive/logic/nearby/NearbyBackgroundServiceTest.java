package rachel.materialapps.recordmylive.logic.nearby;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.ui.MainActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Real Android service and page lifecycle, with a fake radio transport (no GMS required). */
@RunWith(AndroidJUnit4.class)
public class NearbyBackgroundServiceTest {
    @Test public void backgroundServiceSurvivesPageAndActivityLifecycleAndNotificationCanStopIt() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences preferences = context.getSharedPreferences("nearby_preferences", Context.MODE_PRIVATE);
        boolean hadAlerts = preferences.contains("match_alerts");
        boolean oldAlerts = NearbyMatchNotifications.enabled(context);
        Field instance = NearbyDiscovery.class.getDeclaredField("instance");
        instance.setAccessible(true);
        AtomicReference<NearbyDiscovery> previous = new AtomicReference<>();
        AtomicReference<NearbyDiscovery> current = new AtomicReference<>();
        FakeTransport radio = new FakeTransport();
        main(() -> {
            preferences.edit().remove("match_alerts").commit();
            try {
                previous.set((NearbyDiscovery) instance.get(null));
                assertFalse(isForeground(context));
                current.set(new NearbyDiscovery(context, radio));
                instance.set(null, current.get());
            } catch (IllegalAccessException e) { throw new AssertionError(e); }
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nearby_page)).perform(click());
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_idle)));
            onView(withId(R.id.nearby_match_alerts)).check(matches(isNotChecked()));
            assertEquals(0, radio.starts);
            // Seed an active fake radio session; the actual service hosts it from here on.
            scenario.onActivity(activity -> {
                current.get().startSession("后台测试昵称");
                ContextCompat.startForegroundService(activity,
                        new Intent(activity, NearbyDiscoveryService.class).setAction(NearbyDiscoveryService.ACTION_START));
            });
            await(() -> isForeground(context));
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_active)));
            radioFound(current.get(), radio);
            await(() -> radio.loaded);
            main(() -> {
                current.get().setArtists(java.util.Arrays.asList(new NearbySession.Artist("Taylor Swift", null),
                        new NearbySession.Artist("司南", null)));
                radio.events.artists("nearby-test", new java.util.HashSet<>(java.util.Arrays.asList(
                        ArtistMatchProtocol.key("taylor  swift"), ArtistMatchProtocol.key("司 南  "))));
            });
            onView(withText(context.getString(R.string.nearby_match_count, 2))).perform(scrollTo(), click());
            onView(withText(context.getString(R.string.nearby_match_title, "附近用户"))).check(matches(isDisplayed()));
            onView(withText("Taylor Swift")).check(matches(isDisplayed()));
            onView(withText("司南")).check(matches(isDisplayed()));
            scenario.recreate();
            onView(withText(context.getString(R.string.nearby_match_title, "附近用户"))).check(matches(isDisplayed()));
            onView(withText(R.string.nearby_match_close)).perform(click());
            assertNull(matchNotification(context)); // default off, even when a match already exists
            assertTrue("Enable app notifications for this notification integration test", NearbyMatchNotifications.available(context));
            onView(withId(R.id.nearby_match_alerts)).perform(scrollTo(), click());
            await(() -> matchNotification(context) != null);
            android.service.notification.StatusBarNotification alert = matchNotification(context);
            assertEquals(context.getString(R.string.nearby_alert_title), alert.getNotification().extras.getString(android.app.Notification.EXTRA_TITLE));
            assertEquals(context.getString(R.string.nearby_alert_text), alert.getNotification().extras.getString(android.app.Notification.EXTRA_TEXT));
            assertEquals(android.app.Notification.VISIBILITY_PRIVATE, alert.getNotification().visibility);
            scenario.recreate();
            onView(withId(R.id.nearby_match_alerts)).check(matches(isChecked()));
            context.getSystemService(NotificationManager.class).cancel(alert.getId());
            main(() -> radio.events.artists("nearby-test", java.util.Collections.singleton(ArtistMatchProtocol.key("司南"))));
            assertNull(matchNotification(context)); // dismissing does not allow the same endpoint to alert again
            onView(withId(R.id.nearby_match_alerts)).perform(scrollTo(), click());
            assertFalse(NearbyMatchNotifications.enabled(context));
            main(() -> {
                radio.events.found("second-test", "第二位用户");
                radio.events.artists("second-test", java.util.Collections.singleton(ArtistMatchProtocol.key("司南")));
            });
            assertNull(matchNotification(context));
            onView(withId(R.id.nearby_match_alerts)).perform(scrollTo(), click());
            await(() -> matchNotification(context) != null); // enabling considers current, previously unalerted matches
            onView(withId(R.id.nearby_match_alerts)).perform(scrollTo(), click());
            await(() -> matchNotification(context) == null);
            onView(withId(R.id.nearby_match_alerts)).perform(scrollTo(), click());
            assertNull(matchNotification(context)); // toggling does not cause duplicate alerts
            main(() -> radio.events.lost("second-test"));
            main(() -> radio.events.artists("nearby-test", java.util.Collections.emptySet()));
            onView(withText(R.string.nearby_match_empty)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.nearby_count)).check(matches(withText(context.getString(R.string.nearby_count, 1))));
            onView(withId(R.id.my_page)).perform(click());
            scenario.close();
            main(() -> assertTrue(current.get().isRunning()));
            assertTrue(isForeground(context));
            assertEquals(0, radio.stops);
            main(() -> {
                radio.events.found("third-test", "后台附近用户");
                radio.events.artists("third-test", java.util.Collections.singleton(ArtistMatchProtocol.key("司南")));
            });
            await(() -> matchNotification(context) != null);
            try (ActivityScenario<MainActivity> resumed = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nearby_page)).perform(click());
            resumed.recreate();
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_active)));
            onView(withId(R.id.nearby_nickname)).check(matches(withText("后台测试昵称")));
            assertEquals(1, radio.starts);
            onView(withId(R.id.my_page)).perform(click());
            matchNotification(context).getNotification().contentIntent.send();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_active)));
            onView(withId(R.id.my_page)).perform(click());
            PendingIntent open = PendingIntent.getActivity(context, 2001, new Intent(context, MainActivity.class)
                            .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            assertNotNull(open);
            open.send();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_active)));
            PendingIntent stop = PendingIntent.getService(context, 2002,
                    new Intent(context, NearbyDiscoveryService.class).setAction(NearbyDiscoveryService.ACTION_STOP),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            assertNotNull(stop);
            stop.send();
            await(() -> !isForeground(context));
            onView(withId(R.id.nearby_toggle)).check(matches(withText(R.string.nearby_start)));
            main(() -> {
                assertEquals(NearbySession.Stage.IDLE, current.get().getState().getValue().stage);
                assertTrue(current.get().getState().getValue().peers.isEmpty());
            });
            assertTrue(radio.stops > 0);
            assertEquals(0, context.getSystemService(NotificationManager.class).getActiveNotifications().length);
            resumed.recreate();
            assertFalse(isForeground(context));
            assertEquals(1, radio.starts);
            }
        } finally {
            main(() -> current.get().stop());
            context.stopService(new Intent(context, NearbyDiscoveryService.class));
            await(() -> !isForeground(context));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            main(() -> {
                if (hadAlerts) NearbyMatchNotifications.setEnabled(context, oldAlerts);
                else preferences.edit().remove("match_alerts").commit();
                try { instance.set(null, previous.get()); }
                catch (IllegalAccessException e) { throw new AssertionError(e); }
            });
        }
    }

    private void radioFound(NearbyDiscovery discovery, FakeTransport radio) {
        main(() -> {
            radio.events.found("nearby-test", "附近用户");
            assertEquals(1, discovery.getState().getValue().peers.size());
        });
    }
    private static boolean isForeground(Context context) {
        for (ActivityManager.RunningServiceInfo info : context.getSystemService(ActivityManager.class).getRunningServices(100))
            if (info.service.getClassName().equals(NearbyDiscoveryService.class.getName())) return info.foreground;
        return false;
    }
    private static android.service.notification.StatusBarNotification matchNotification(Context context) {
        for (android.service.notification.StatusBarNotification notification :
                context.getSystemService(NotificationManager.class).getActiveNotifications())
            if (notification.getId() == 2003) return notification;
        return null;
    }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void await(BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50);
        assertTrue(condition.getAsBoolean());
    }
    private static class FakeTransport implements NearbySession.Transport {
        int starts, stops;
        volatile boolean loaded;
        @Override public void setArtists(java.util.Collection<String> keys) { loaded = true; }
        NearbySession.Events events;
        @Override public void start(String nickname, NearbySession.Events events, Consumer<Boolean> complete) {
            starts++;
            this.events = events;
            complete.accept(true);
        }
        @Override public void stop() { stops++; }
    }
}
