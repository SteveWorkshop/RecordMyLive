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
                            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
