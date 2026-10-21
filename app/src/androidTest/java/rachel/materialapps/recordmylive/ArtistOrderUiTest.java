package rachel.materialapps.recordmylive;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.UiController;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.action.Press;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.logic.dao.FavoriteArtistDao;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.ui.MainActivity;

@RunWith(AndroidJUnit4.class)
public class ArtistOrderUiTest {
    @Test public void draggingPersistsAndUpdatesTopThree() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FavoriteArtistDao dao = DatabaseManager.getInstance(context).favoriteArtistDao();
        List<Long> original = new ArrayList<>();
        for (FavoriteArtistEntity artist : dao.getAllSync()) original.add(artist.getId());
        List<Long> added = new ArrayList<>();
        String prefix = "Order QA " + System.nanoTime();
        try {
            for (int i = 0; i < 4; i++) {
                FavoriteArtistEntity artist = new FavoriteArtistEntity();
                artist.setName(prefix + " " + i);
                artist.setMatchName(artist.getName());
                artist.setSortOrder(dao.nextSortOrder());
                added.add(dao.insert(artist));
            }
            List<Long> order = new ArrayList<>(added);
            order.addAll(original);
            dao.reorder(order);
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                onView(withId(R.id.my_page)).perform(click());
                onView(withId(R.id.favorite_artists)).perform(click());
                onView(withId(R.id.artist_list)).perform(new ViewAction() {
                    @Override public org.hamcrest.Matcher<View> getConstraints() { return isDisplayed(); }
                    @Override public String getDescription() { return "drag first artist below second"; }
                    @Override public void perform(UiController ui, View view) {
                        RecyclerView recycler = (RecyclerView) view;
                        ui.loopMainThreadForAtLeast(200);
                        View first = recycler.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.drag_handle);
                        View second = recycler.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.drag_handle);
                        int[] a = new int[2], b = new int[2];
                        first.getLocationOnScreen(a);
                        second.getLocationOnScreen(b);
                        new GeneralSwipeAction(Swipe.SLOW,
                                v -> new float[]{a[0] + first.getWidth() / 2f, a[1] + first.getHeight() / 2f},
                                v -> new float[]{b[0] + second.getWidth() / 2f,
                                        b[1] + second.getHeight() / 2f + recycler.findViewHolderForAdapterPosition(1).itemView.getHeight() / 2f},
                                Press.FINGER).perform(ui, first);
                        ui.loopMainThreadForAtLeast(700);
                    }
                });
                long deadline = SystemClock.uptimeMillis() + 5000;
                while (dao.getAllSync().get(0).getId() != added.get(1) && SystemClock.uptimeMillis() < deadline)
                    SystemClock.sleep(50);
                assertEquals(added.get(1).longValue(), dao.getAllSync().get(0).getId());
                scenario.recreate();
                onView(withId(R.id.artist_list)).perform(new ViewAction() {
                    @Override public org.hamcrest.Matcher<View> getConstraints() { return isDisplayed(); }
                    @Override public String getDescription() { return "verify restored list order"; }
                    @Override public void perform(UiController ui, View view) {
                        ui.loopMainThreadForAtLeast(200);
                        RecyclerView recycler = (RecyclerView) view;
                        android.widget.TextView name = recycler.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.artist_name);
                        assertEquals(prefix + " 1", name.getText().toString());
                    }
                });
                pressBack();
                onView(withId(R.id.favorite_artists_preview)).perform(new ViewAction() {
                    @Override public org.hamcrest.Matcher<View> getConstraints() { return isDisplayed(); }
                    @Override public String getDescription() { return "verify top three follow custom order"; }
                    @Override public void perform(UiController ui, View view) {
                        ui.loopMainThreadForAtLeast(200);
                        android.widget.LinearLayout preview = (android.widget.LinearLayout) view;
                        assertEquals(3, preview.getChildCount());
                        int[] expected = {1, 0, 2};
                        for (int i = 0; i < 3; i++) {
                            android.widget.TextView name = preview.getChildAt(i).findViewById(R.id.name);
                            assertEquals(prefix + " " + expected[i], name.getText().toString());
                        }
                    }
                });
            }
        } finally {
            for (Long id : added) dao.delete(id);
            dao.reorder(original);
        }
    }
}
