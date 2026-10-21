package rachel.materialapps.recordmylive.ui.pages.nearby;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.ui.MainActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class NearbyUiTest {
    @Test public void discoveryIsOptInAndProfileEditorReturnsToIdle() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("local_profile", Context.MODE_PRIVATE);
        Map<String, ?> original = backup(context, prefs);
        prefs.edit().remove("nickname").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nearby_page)).perform(click());
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_idle)));
            onView(withId(R.id.nearby_toggle)).perform(scrollTo(), click());
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_name_required)));
            onView(withId(R.id.nearby_edit_profile)).perform(scrollTo(), click());
            onView(withId(R.id.nickname_input)).check(matches(isDisplayed()));
            pressBack();
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_idle)));
            prefs.edit().putString("nickname", "附近测试昵称").commit();
            scenario.recreate();
            onView(withId(R.id.nearby_nickname)).check(matches(withText("附近测试昵称")));
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_idle)));
        } finally { restore(context, prefs, original); }
    }

    @Test public void unavailablePlayServicesShowsActionableMessageWithoutStarting() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS);
        SharedPreferences prefs = context.getSharedPreferences("local_profile", Context.MODE_PRIVATE);
        Map<String, ?> original = backup(context, prefs);
        prefs.edit().putString("nickname", "附近测试昵称").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nearby_page)).perform(click());
            onView(withId(R.id.nearby_toggle)).perform(scrollTo(), click());
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_services_unavailable)));
            onView(withId(R.id.my_page)).perform(click());
            onView(withId(R.id.nearby_page)).perform(click());
            onView(withId(R.id.nearby_status)).check(matches(withText(R.string.nearby_idle)));
        } finally { restore(context, prefs, original); }
    }

    private Map<String, ?> backup(Context context, SharedPreferences prefs) {
        // A killed instrumentation process cannot execute finally. Recover on the next run.
        SharedPreferences backup = context.getSharedPreferences("nearby_ui_profile_backup", Context.MODE_PRIVATE);
        if (backup.getBoolean("pending", false)) {
            SharedPreferences.Editor recover = prefs.edit();
            if (backup.getBoolean("had_nickname", false)) recover.putString("nickname", backup.getString("nickname", ""));
            else recover.remove("nickname");
            recover.commit();
        }
        Map<String, ?> original = prefs.getAll();
        backup.edit().clear().putBoolean("pending", true).putBoolean("had_nickname", prefs.contains("nickname"))
                .putString("nickname", prefs.getString("nickname", "")).commit();
        return original;
    }

    private void restore(Context context, SharedPreferences prefs, Map<String, ?> original) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : original.entrySet()) editor.putString(entry.getKey(), (String) entry.getValue());
        editor.commit();
        context.getSharedPreferences("nearby_ui_profile_backup", Context.MODE_PRIVATE).edit().clear().commit();
    }
}
