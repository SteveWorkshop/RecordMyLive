package rachel.materialapps.recordmylive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.logic.db.AppDatabase;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;

@RunWith(AndroidJUnit4.class)
public class FavoriteArtistPersistenceTest {
    @Test
    public void normalizedNamesAreUniqueAndFavoritesAreIndependentOfConcerts() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        try {
            long id = db.favoriteArtistDao().insert(artist(" Taylor   Swift "));
            assertTrue(id > 0);
            assertEquals(-1L, db.favoriteArtistDao().insert(artist("taylor swift")));
            assertTrue(db.favoriteArtistDao().insert(artist("司 南  ")) > 0);
            assertEquals(-1L, db.favoriteArtistDao().insert(artist("司南")));
            assertEquals(2, db.favoriteArtistDao().getAllSync().size());
            ConcertEntity concert = ConcertEntity.createEmpty();
            concert.setType(rachel.materialapps.recordmylive.logic.constants.ConcertType.CONCERT);
            concert.setDate(LocalDate.of(2026, 10, 21));
            concert.setArtists(Collections.singletonList("Taylor Swift"));
            concert.setId(db.concertDao().insert(concert));
            assertEquals(1, db.favoriteArtistDao().delete(id));
            assertEquals(1, db.concertDao().countByDate(concert.getDate()));
            db.concertDao().delete(concert);
            assertEquals(1, db.favoriteArtistDao().getAllSync().size());
        } finally {
            db.close();
        }
    }

    @Test
    public void nearbyMatchSelectionIsOptInAndLimitedToTwenty() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        try {
            java.util.List<Long> ids = new java.util.ArrayList<>();
            for (int i = 0; i < 21; i++) {
                FavoriteArtistEntity artist = artist("nearby selection " + i);
                ids.add(db.favoriteArtistDao().insert(artist));
                assertFalse(artist.isNearbyMatchSelected());
            }
            for (int i = 0; i < 20; i++)
                assertTrue(db.favoriteArtistDao().selectForNearbyMatch(ids.get(i), true, 20));
            assertFalse(db.favoriteArtistDao().selectForNearbyMatch(ids.get(20), true, 20));
            assertEquals(20, db.favoriteArtistDao().getNearbyMatchSelectedCount());
            assertTrue(db.favoriteArtistDao().selectForNearbyMatch(ids.get(0), false, 20));
            assertTrue(db.favoriteArtistDao().selectForNearbyMatch(ids.get(20), true, 20));
            assertEquals(20, db.favoriteArtistDao().getNearbyMatchSelectedCount());
        } finally {
            db.close();
        }
    }

    @Test
    public void migrationFromSevenPreservesExistingTablesAndFavoritesSurviveReopening() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "favorite-artist-migration-test.db";
        context.deleteDatabase(name);
        LocalDate date = LocalDate.of(2026, 10, 21);
        try {
            AppDatabase initial = Room.databaseBuilder(context, AppDatabase.class, name).build();
            try {
                ConcertEntity concert = ConcertEntity.createEmpty();
                concert.setType(rachel.materialapps.recordmylive.logic.constants.ConcertType.CONCERT);
                concert.setName("原有演出");
                concert.setDate(date);
                concert.setArtists(Collections.singletonList("司 南"));
                long concertId = initial.concertDao().insert(concert);
                ExpenseEntity expense = new ExpenseEntity();
                expense.setConcertId(concertId);
                expense.setAmountCents(12345);
                initial.expenseDao().insert(expense);
            } finally {
                initial.close();
            }
            try (SQLiteDatabase old = SQLiteDatabase.openDatabase(context.getDatabasePath(name).getPath(),
                    null, SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("DROP TABLE favorite_artist");
                old.setVersion(7);
            }
            AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, name)
                    .addMigrations(DatabaseManager.MIGRATION_7_8, DatabaseManager.MIGRATION_8_9,
                            DatabaseManager.MIGRATION_9_10, DatabaseManager.MIGRATION_10_11).build();
            try {
                assertEquals(1, db.concertDao().countByDate(date));
                assertEquals(12345L, db.expenseDao().getForConcertSync(1).get(0).getAmountCents());
                try (android.database.Cursor cursor = db.getOpenHelper().getReadableDatabase()
                        .query("SELECT artists FROM concert WHERE id = 1")) {
                    assertTrue(cursor.moveToFirst());
                    assertTrue(cursor.getString(0).contains("司 南"));
                }
                assertTrue(db.favoriteArtistDao().getAllSync().isEmpty());
                FavoriteArtistEntity migrated = artist(" Taylor   Swift ");
                assertTrue(db.favoriteArtistDao().insert(migrated) > 0);
                assertFalse(db.favoriteArtistDao().getAllSync().get(0).isNearbyMatchSelected());
            } finally {
                db.close();
            }
            AppDatabase reopened = Room.databaseBuilder(context, AppDatabase.class, name).build();
            try {
                assertEquals("Taylor Swift", reopened.favoriteArtistDao().getAllSync().get(0).getName());
                assertEquals(1, reopened.concertDao().countByDate(date));
            } finally {
                reopened.close();
            }
        } finally {
            context.deleteDatabase(name);
        }
    }

    @Test
    public void migrationFromEightKeepsFavoriteIdentityAndStartsWithoutImage() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "favorite-image-migration-test.db";
        context.deleteDatabase(name);
        try {
            AppDatabase initial = Room.databaseBuilder(context, AppDatabase.class, name).build();
            long id;
            try {
                FavoriteArtistEntity favorite = artist("Taylor Swift");
                favorite.setCreatedAt(12345);
                id = initial.favoriteArtistDao().insert(favorite);
            } finally { initial.close(); }
            try (SQLiteDatabase old = SQLiteDatabase.openDatabase(context.getDatabasePath(name).getPath(),
                    null, SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("CREATE TABLE favorites_v8 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + "name TEXT NOT NULL, matchName TEXT NOT NULL, createdAt INTEGER NOT NULL)");
                old.execSQL("INSERT INTO favorites_v8 SELECT id, name, matchName, createdAt FROM favorite_artist");
                old.execSQL("DROP TABLE favorite_artist");
                old.execSQL("ALTER TABLE favorites_v8 RENAME TO favorite_artist");
                old.execSQL("CREATE UNIQUE INDEX index_favorite_artist_matchName ON favorite_artist(matchName)");
                old.setVersion(8);
            }
            AppDatabase migrated = Room.databaseBuilder(context, AppDatabase.class, name)
                    .addMigrations(DatabaseManager.MIGRATION_8_9, DatabaseManager.MIGRATION_9_10,
                            DatabaseManager.MIGRATION_10_11).build();
            try {
                FavoriteArtistEntity favorite = migrated.favoriteArtistDao().getById(id);
                assertEquals("Taylor Swift", favorite.getName());
                assertEquals(12345, favorite.getCreatedAt());
                org.junit.Assert.assertNull(favorite.getImageFile());
            } finally { migrated.close(); }
        } finally { context.deleteDatabase(name); }
    }

    private FavoriteArtistEntity artist(String input) {
        FavoriteArtistEntity artist = new FavoriteArtistEntity();
        artist.setName(ArtistNameNormalizer.normalize(input));
        artist.setMatchName(ArtistNameNormalizer.comparisonKey(input));
        artist.setCreatedAt(System.currentTimeMillis());
        return artist;
    }
}
