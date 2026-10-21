package rachel.materialapps.recordmylive.ui.pages.my;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.ui.MainActivity;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository;
import rachel.materialapps.recordmylive.logic.util.ProfileImages;

@RunWith(AndroidJUnit4.class)
public class ProfileEditorUiTest {
    @Test public void localProfileSurvivesRecreationAndCancelDoesNotChangeSavedAvatar() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("local_profile", Context.MODE_PRIVATE);
        Map<String, ?> original = prefs.getAll();
        prefs.edit().clear().commit(); // Keep the user's original image untouched while testing.
        ProfileRepository repo = new ProfileRepository(context);
        File source = File.createTempFile("profile-ui-", ".png", context.getCacheDir());
        File savedImage = null;
        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.CYAN);
        try (FileOutputStream output = new FileOutputStream(source)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally { bitmap.recycle(); }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.my_page)).perform(click());
            onView(withId(R.id.profile_nickname)).check(matches(withText(R.string.profile_unset)));
            onView(withId(R.id.profile_card)).perform(click());
            onView(withId(R.id.nickname_input)).perform(scrollTo(), replaceText("  小雨   Live  "), closeSoftKeyboard());
            scenario.onActivity(activity -> model(activity).chooseImage(Uri.fromFile(source)));
            awaitEnabled(R.id.choose_avatar);
            scenario.recreate();
            onView(withId(R.id.nickname_input)).check(matches(withText("  小雨   Live  ")));
            scenario.onActivity(activity -> assertTrue(model(activity).image().isFile()));
            onView(withId(R.id.action_save)).perform(click());
            awaitEnabled(R.id.profile_card);
            onView(withId(R.id.profile_nickname)).check(matches(withText("小雨 Live")));
            savedImage = ProfileImages.file(context, repo.getAvatarFile());
            assertNotNull(savedImage);
            assertTrue(savedImage.isFile());
            source.delete();
            scenario.recreate();
            onView(withId(R.id.profile_nickname)).check(matches(withText("小雨 Live")));
            onView(withId(R.id.profile_card)).perform(click());
            onView(withId(R.id.remove_avatar)).perform(scrollTo(), click());
            onView(withId(R.id.nickname_input)).perform(scrollTo(), replaceText("cancelled"), closeSoftKeyboard());
            pressBack();
            onView(withId(R.id.profile_nickname)).check(matches(withText("小雨 Live")));
            assertTrue(savedImage.isFile());
            assertEquals(savedImage.getName(), repo.getAvatarFile());
            onView(withId(R.id.profile_card)).perform(click());
            onView(withId(R.id.nickname_input)).perform(scrollTo(), replaceText("   "), closeSoftKeyboard());
            onView(withId(R.id.action_save)).perform(click());
            awaitEnabled(R.id.nickname_input);
            assertEquals("小雨 Live", repo.getNickname());
            onView(withId(R.id.nickname_input)).perform(replaceText("司 南 🎵"), closeSoftKeyboard());
            onView(withId(R.id.remove_avatar)).perform(scrollTo(), click());
            onView(withId(R.id.action_save)).perform(click());
            awaitEnabled(R.id.profile_card);
            onView(withId(R.id.profile_nickname)).check(matches(withText("司 南 🎵")));
            assertNull(repo.getAvatarFile());
            assertFalse(savedImage.exists());
        } finally {
            File image = ProfileImages.file(context, repo.getAvatarFile());
            if (image != null) image.delete();
            if (savedImage != null) savedImage.delete();
            source.delete();
            SharedPreferences.Editor restore = prefs.edit().clear();
            for (Map.Entry<String, ?> entry : original.entrySet())
                restore.putString(entry.getKey(), (String) entry.getValue());
            restore.commit();
        }
    }
    private ProfileEditorViewModel model(MainActivity activity) {
        return new ViewModelProvider(activity.getNavHostFragment().getChildFragmentManager()
                .getPrimaryNavigationFragment()).get(ProfileEditorViewModel.class);
    }
    private void awaitEnabled(int id) {
        onView(isRoot()).perform(new ViewAction() {
            @Override public org.hamcrest.Matcher<View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "wait for local profile operation"; }
            @Override public void perform(UiController ui, View view) {
                long deadline = SystemClock.uptimeMillis() + 5000;
                View target;
                do {
                    ui.loopMainThreadForAtLeast(50);
                    target = view.findViewById(id);
                } while ((target == null || !target.isEnabled()) && SystemClock.uptimeMillis() < deadline);
                assertNotNull(target);
                assertTrue(target.isEnabled());
            }
        });
    }
}
