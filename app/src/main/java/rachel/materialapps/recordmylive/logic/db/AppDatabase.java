package rachel.materialapps.recordmylive.logic.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import rachel.materialapps.recordmylive.logic.dao.ExpenseDao;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;

import rachel.materialapps.recordmylive.logic.dao.ConcertDao;
import rachel.materialapps.recordmylive.logic.dao.ConcertSongDao;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.handler.ArtistListConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.ConcertStatusConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.ConcertTypeConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.LocalDateConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.LocalTimeConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.PurchaseChannelConverter;

@Database(
        entities = {
                ConcertEntity.class,
                ConcertSongEntity.class,
                ExpenseEntity.class
        },
        version = 7,
        exportSchema = false
)
@TypeConverters({
        ConcertTypeConverter.class,
        PurchaseChannelConverter.class,
        ConcertStatusConverter.class,
        ArtistListConverter.class,
        LocalDateConverter.class,
        LocalTimeConverter.class
})
public abstract class AppDatabase extends RoomDatabase {

    public abstract ConcertDao concertDao();

    public abstract ConcertSongDao concertSongDao();
    public abstract ExpenseDao expenseDao();

}
