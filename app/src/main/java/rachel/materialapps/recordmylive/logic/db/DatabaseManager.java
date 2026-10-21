package rachel.materialapps.recordmylive.logic.db;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

public final class DatabaseManager {

    private static volatile AppDatabase INSTANCE;

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE concert ADD COLUMN ticketSaleDate TEXT");
            db.execSQL("ALTER TABLE concert ADD COLUMN ticketSaleTime TEXT");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS expense (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "concertId INTEGER NOT NULL, category TEXT NOT NULL, amountCents INTEGER NOT NULL, "
                    + "spentDate TEXT NOT NULL, note TEXT NOT NULL, "
                    + "FOREIGN KEY(concertId) REFERENCES concert(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_concertId ON expense (concertId)");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favorite_artist (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "name TEXT NOT NULL, matchName TEXT NOT NULL, createdAt INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_favorite_artist_matchName "
                    + "ON favorite_artist (matchName)");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorite_artist ADD COLUMN imageFile TEXT");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorite_artist ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0");
            // Preserve the existing newest-first order on upgrade.
            db.execSQL("UPDATE favorite_artist SET sortOrder = (SELECT COUNT(*) FROM favorite_artist AS other "
                    + "WHERE other.createdAt > favorite_artist.createdAt OR "
                    + "(other.createdAt = favorite_artist.createdAt AND other.id > favorite_artist.id))");
        }
    };

    private DatabaseManager() {
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "record_my_live.db"
                            )
                            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
