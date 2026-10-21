package rachel.materialapps.recordmylive;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.logic.db.AppDatabase;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.dao.FavoriteArtistDao;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;

@RunWith(AndroidJUnit4.class)
public class ArtistOrderPersistenceTest {
    @Test public void migrationRetainsPreviousOrderAndCustomOrderSurvivesReopen() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "artist-order-test.db";
        context.deleteDatabase(name);
        try {
            AppDatabase initial = Room.databaseBuilder(context, AppDatabase.class, name).build();
            long first, second, third;
            try {
                first = initial.favoriteArtistDao().insert(artist("First", 100));
                second = initial.favoriteArtistDao().insert(artist("Second", 200));
                third = initial.favoriteArtistDao().insert(artist("Third", 200));
            } finally { initial.close(); }
            try (SQLiteDatabase old = SQLiteDatabase.openDatabase(context.getDatabasePath(name).getPath(),
                    null, SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("CREATE TABLE favorites_v9 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + "name TEXT NOT NULL, matchName TEXT NOT NULL, createdAt INTEGER NOT NULL, imageFile TEXT)");
                old.execSQL("INSERT INTO favorites_v9 SELECT id, name, matchName, createdAt, imageFile FROM favorite_artist");
                old.execSQL("DROP TABLE favorite_artist");
                old.execSQL("ALTER TABLE favorites_v9 RENAME TO favorite_artist");
                old.execSQL("CREATE UNIQUE INDEX index_favorite_artist_matchName ON favorite_artist(matchName)");
                old.setVersion(9);
            }
            AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, name)
                    .addMigrations(DatabaseManager.MIGRATION_9_10).build();
            try {
                FavoriteArtistDao dao = db.favoriteArtistDao();
                assertOrder(dao, third, second, first);
                // Duplicates/deleted IDs are harmless; omitted current artists stay at the end.
                dao.reorder(Arrays.asList(first, first, 99999L, third));
                assertOrder(dao, first, third, second);
                FavoriteArtistEntity edited = dao.getById(third);
                edited.setName("Renamed");
                edited.setMatchName("renamed");
                edited.setImageFile("test.webp");
                dao.update(edited);
                assertOrder(dao, first, third, second);
                FavoriteArtistEntity added = artist("New", 300);
                added.setSortOrder(dao.nextSortOrder());
                long fourth = dao.insert(added);
                assertOrder(dao, first, third, second, fourth);
                dao.delete(first);
                dao.reorder(Arrays.asList(second, third, fourth));
                assertOrder(dao, second, third, fourth);
                assertEquals("test.webp", dao.getById(third).getImageFile());
            } finally { db.close(); }
            AppDatabase reopened = Room.databaseBuilder(context, AppDatabase.class, name).build();
            try {
                List<FavoriteArtistEntity> artists = reopened.favoriteArtistDao().getAllSync();
                assertEquals("Second", artists.get(0).getName());
                assertEquals("Renamed", artists.get(1).getName());
                assertEquals("New", artists.get(2).getName());
            } finally { reopened.close(); }
        } finally { context.deleteDatabase(name); }
    }
    private FavoriteArtistEntity artist(String name, long time) {
        FavoriteArtistEntity artist = new FavoriteArtistEntity();
        artist.setName(name);
        artist.setMatchName(name.toLowerCase(java.util.Locale.ROOT));
        artist.setCreatedAt(time);
        return artist;
    }
    private void assertOrder(FavoriteArtistDao dao, long... ids) {
        List<FavoriteArtistEntity> artists = dao.getAllSync();
        assertEquals(ids.length, artists.size());
        for (int i = 0; i < ids.length; i++) assertEquals(ids[i], artists.get(i).getId());
    }
}
