package rachel.materialapps.recordmylive.ui.pages.my;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.ui.MainActivity;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.util.ArtistImages;

@RunWith(AndroidJUnit4.class)
public class ArtistEditorUiTest {
    @Test public void imageDraftSurvivesRecreationAndCancelKeepsSavedImage() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "Avatar UI " + System.nanoTime();
        File source = File.createTempFile("avatar-ui-", ".png", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.CYAN);
        try (FileOutputStream output = new FileOutputStream(source)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally { bitmap.recycle(); }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.my_page)).perform(click());
            onView(withId(R.id.favorite_artists)).perform(click());
            onView(withId(R.id.add_artist)).perform(click());
            onView(withId(R.id.artist_input)).perform(scrollTo(), replaceText(name), closeSoftKeyboard());
            // Exercise the same callback as the system picker, without depending on a device's documents UI.
            scenario.onActivity(activity -> model(activity).chooseImage(Uri.fromFile(source)));
            awaitEnabled(R.id.choose_image);
            scenario.recreate();
            scenario.onActivity(activity -> assertTrue(model(activity).image().isFile()));
            onView(withId(R.id.action_save)).perform(click());
            awaitEnabled(R.id.add_artist);
            FavoriteArtistEntity saved = find(context, name);
            assertNotNull(saved);
            File original = ArtistImages.file(context, saved.getImageFile());
            assertTrue(original.isFile());
            onView(withText(name)).perform(click());
            awaitEnabled(R.id.artist_input);
            onView(withId(R.id.remove_image)).perform(scrollTo(), click());
            pressBack();
            assertEquals(saved.getImageFile(), find(context, name).getImageFile());
            assertTrue(original.isFile());
            onView(withText(name)).perform(click());
            awaitEnabled(R.id.artist_input);
            onView(withId(R.id.remove_image)).perform(scrollTo(), click());
            onView(withId(R.id.action_save)).perform(click());
            awaitEnabled(R.id.add_artist);
            assertNull(find(context, name).getImageFile());
            assertFalse(original.exists());
        } finally {
            source.delete();
            FavoriteArtistEntity artist = find(context, name);
            if (artist != null) {
                File image = ArtistImages.file(context, artist.getImageFile());
                DatabaseManager.getInstance(context).favoriteArtistDao().delete(artist.getId());
                if (image != null) image.delete();
            }
        }
    }
    private ArtistEditorViewModel model(MainActivity activity) {
        return new ViewModelProvider(activity.getNavHostFragment().getChildFragmentManager()
                .getPrimaryNavigationFragment()).get(ArtistEditorViewModel.class);
    }
    private FavoriteArtistEntity find(Context context, String name) {
        for (FavoriteArtistEntity artist : DatabaseManager.getInstance(context).favoriteArtistDao().getAllSync())
            if (artist.getName().equals(name)) return artist;
        return null;
    }
    private void awaitEnabled(int id) {
        onView(isRoot()).perform(new androidx.test.espresso.ViewAction() {
            @Override public org.hamcrest.Matcher<android.view.View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "wait for image operation"; }
            @Override public void perform(androidx.test.espresso.UiController ui, android.view.View view) {
                long deadline = SystemClock.uptimeMillis() + 5000;
                android.view.View target;
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
