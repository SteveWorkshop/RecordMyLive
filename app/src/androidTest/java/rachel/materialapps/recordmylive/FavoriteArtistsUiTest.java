package rachel.materialapps.recordmylive;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.logic.dao.FavoriteArtistDao;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;
import rachel.materialapps.recordmylive.ui.MainActivity;

@RunWith(AndroidJUnit4.class)
public class FavoriteArtistsUiTest {
    @Test public void separateEditorSupportsCreateRenameCancelDuplicateAndDelete() {
        String suffix = Long.toHexString(System.nanoTime());
        String name = "Taylor Swift QA " + suffix;
        String renamed = name + " edited";
        FavoriteArtistDao dao = DatabaseManager.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext()).favoriteArtistDao();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.my_page)).perform(click());
            onView(withId(R.id.favorite_artists)).perform(click());
            onView(withId(R.id.artist_input)).check(doesNotExist());
            onView(withId(R.id.add_artist)).perform(click());
            onView(withId(R.id.artist_input)).perform(scrollTo(), replaceText("  Taylor   Swift QA " + suffix + "  "), closeSoftKeyboard());
            scenario.recreate();
            onView(withId(R.id.artist_input)).check(matches(withText("  Taylor   Swift QA " + suffix + "  ")));
            onView(withId(R.id.action_save)).perform(click());
            awaitFavorite(dao, name, true);
            awaitList();
            onView(withText(name)).check(matches(isDisplayed()));
            onView(withText(name)).perform(click());
            awaitEditor();
            onView(withId(R.id.artist_input)).perform(scrollTo(), replaceText(renamed), closeSoftKeyboard());
            onView(withId(R.id.action_save)).perform(click());
            awaitFavorite(dao, renamed, true);
            awaitList();
            onView(withText(renamed)).perform(click());
            awaitEditor();
            onView(withId(R.id.artist_input)).perform(scrollTo(), replaceText("unsaved draft"), closeSoftKeyboard());
            pressBack();
            onView(withText(renamed)).check(matches(isDisplayed()));
            onView(withId(R.id.add_artist)).perform(click());
            onView(withId(R.id.artist_input)).perform(scrollTo(), replaceText(renamed.toLowerCase(java.util.Locale.ROOT)), closeSoftKeyboard());
            onView(withId(R.id.action_save)).perform(click());
            awaitEditor();
            onView(withText(R.string.favorite_artist_duplicate)).check(matches(isDisplayed()));
            pressBack();
            onView(allOf(withId(R.id.remove_artist), hasSibling(withText(renamed)))).perform(click());
            onView(withId(android.R.id.button2)).perform(click());
            onView(withText(renamed)).check(matches(isDisplayed()));
            pressBack();
            // New artists append after existing favorites, so this fixture may be outside the top three.
            assertPreviewMatchesOrder(dao);
            onView(withId(R.id.favorite_artists)).perform(click());
            onView(allOf(withId(R.id.remove_artist), hasSibling(withText(renamed)))).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            awaitFavorite(dao, renamed, false);
            pressBack();
            onView(withText(renamed)).check(doesNotExist());
        } finally {
            for (FavoriteArtistEntity artist : dao.getAllSync())
                if (artist.getName().startsWith(name)) dao.delete(artist.getId());
        }
    }
    private void assertPreviewMatchesOrder(FavoriteArtistDao dao) {
        java.util.List<FavoriteArtistEntity> artists = dao.getAllSync();
        onView(withId(R.id.favorite_artists_preview)).perform(new androidx.test.espresso.ViewAction() {
            @Override public org.hamcrest.Matcher<android.view.View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "verify preview follows the saved top three"; }
            @Override public void perform(androidx.test.espresso.UiController ui, android.view.View view) {
                ui.loopMainThreadForAtLeast(200);
                android.widget.LinearLayout preview = (android.widget.LinearLayout) view;
                org.junit.Assert.assertEquals(Math.min(3, artists.size()), preview.getChildCount());
                for (int i = 0; i < preview.getChildCount(); i++) {
                    android.widget.TextView name = preview.getChildAt(i).findViewById(R.id.name);
                    org.junit.Assert.assertEquals(artists.get(i).getName(), name.getText().toString());
                }
            }
        });
    }
    private void awaitFavorite(FavoriteArtistDao dao, String name, boolean expected) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        do {
            boolean present = false;
            for (FavoriteArtistEntity artist : dao.getAllSync())
                if (artist.getMatchName().equals(ArtistNameNormalizer.comparisonKey(name))) present = true;
            if (present == expected) return;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Favorite write did not complete");
    }
    private void awaitEditor() { awaitView(R.id.artist_input); }
    private void awaitList() { awaitView(R.id.add_artist); }
    private void awaitView(int id) {
        // Drive main-loop callbacks, including repository results and navigation.
        onView(isRoot()).perform(new androidx.test.espresso.ViewAction() {
            @Override public org.hamcrest.Matcher<android.view.View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "wait for artist screen"; }
            @Override public void perform(androidx.test.espresso.UiController ui, android.view.View view) {
                long deadline = SystemClock.uptimeMillis() + 5000;
                android.view.View target;
                do {
                    ui.loopMainThreadForAtLeast(50);
                    target = view.findViewById(id);
                } while ((target == null || !target.isEnabled()) && SystemClock.uptimeMillis() < deadline);
                ui.loopMainThreadUntilIdle();
            }
        });
    }
}
