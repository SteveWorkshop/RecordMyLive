package rachel.materialapps.recordmylive.logic.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;

@Dao
public interface ExpenseDao {
    @Insert
    long insert(ExpenseEntity expense);
    @Update
    int update(ExpenseEntity expense);
    @Query("DELETE FROM expense WHERE id = :id AND concertId = :concertId")
    int delete(long id, long concertId);
    @Query("SELECT * FROM expense WHERE concertId = :concertId ORDER BY spentDate DESC, id DESC")
    LiveData<List<ExpenseEntity>> getForConcert(long concertId);
    @Query("SELECT * FROM expense")
    LiveData<List<ExpenseEntity>> getAll();
    @Query("SELECT * FROM expense WHERE concertId = :concertId ORDER BY id")
    List<ExpenseEntity> getForConcertSync(long concertId);
}
