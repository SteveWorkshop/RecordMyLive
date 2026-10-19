package rachel.materialapps.recordmylive.logic.db;

import android.content.Context;

import androidx.room.Room;

public final class DatabaseManager {

    private static volatile AppDatabase INSTANCE;

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
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}