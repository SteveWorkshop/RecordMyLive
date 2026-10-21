package rachel.materialapps.recordmylive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.logic.db.AppDatabase;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;

@RunWith(AndroidJUnit4.class)
public class ExpensePersistenceTest {
    @Test
    public void sameCategoryAllowsMultipleRowsAndConcertDeletionCascades() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        try {
            ConcertEntity concert = concert();
            concert.setId(db.concertDao().insert(concert));
            ExpenseEntity first = expense(concert.getId(), 20000);
            first.setId(db.expenseDao().insert(first));
            ExpenseEntity second = expense(concert.getId(), 3500);
            second.setId(db.expenseDao().insert(second));
            assertEquals(2, db.expenseDao().getForConcertSync(concert.getId()).size());
            first.setAmountCents(18000);
            assertEquals(1, db.expenseDao().update(first));
            assertEquals(18000L, db.expenseDao().getForConcertSync(concert.getId()).get(0).getAmountCents());
            assertEquals(1, db.expenseDao().delete(second.getId(), concert.getId()));
            assertEquals(1, db.expenseDao().getForConcertSync(concert.getId()).size());
            assertEquals(1, db.concertDao().delete(concert));
            assertEquals(0, db.concertDao().countByDate(concert.getDate()));
            assertEquals(0, db.concertDao().delete(concert));
            assertTrue(db.expenseDao().getForConcertSync(concert.getId()).isEmpty());
        } finally {
            db.close();
        }
    }

    @Test
    public void migrationFromSixPreservesConcertAndCreatesValidExpenseSchema() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "expense-migration-test.db";
        context.deleteDatabase(name);
        try {
            // Reconstruct version 6 by removing the tables added since then.
            AppDatabase initial = Room.databaseBuilder(context, AppDatabase.class, name).build();
            try {
                initial.concertDao().insert(concert());
            } finally {
                initial.close();
            }
            try (SQLiteDatabase old = SQLiteDatabase.openDatabase(context.getDatabasePath(name).getPath(),
                    null, SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("DROP TABLE expense");
                old.execSQL("DROP TABLE favorite_artist");
                old.setVersion(6);
            }
            AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, name)
                    .addMigrations(DatabaseManager.MIGRATION_6_7, DatabaseManager.MIGRATION_7_8,
                            DatabaseManager.MIGRATION_8_9, DatabaseManager.MIGRATION_9_10).build();
            try {
                assertEquals(1, db.concertDao().countByDate(LocalDate.of(2026, 10, 21)));
                assertTrue(db.expenseDao().insert(expense(1, 12000)) > 0);
                assertEquals(1, db.expenseDao().getForConcertSync(1).size());
            } finally {
                db.close();
            }
        } finally {
            context.deleteDatabase(name);
        }
    }

    private ConcertEntity concert() {
        ConcertEntity concert = ConcertEntity.createEmpty();
        concert.setType(rachel.materialapps.recordmylive.logic.constants.ConcertType.CONCERT);
        concert.setName("费用测试");
        concert.setDate(LocalDate.of(2026, 10, 21));
        concert.setTicketSaleDate(LocalDate.of(2026, 9, 1));
        return concert;
    }

    private ExpenseEntity expense(long concertId, long cents) {
        ExpenseEntity expense = new ExpenseEntity();
        expense.setConcertId(concertId);
        expense.setCategory("TRANSPORT");
        expense.setAmountCents(cents);
        expense.setNote("高铁");
        return expense;
    }
}
