package rachel.materialapps.recordmylive.logic.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;

@Dao
public interface ConcertSongDao {

    @Insert
    long insert(ConcertSongEntity song);

    @Insert
    List<Long> insert(List<ConcertSongEntity> songs);

    @Update
    void update(ConcertSongEntity song);

    @Delete
    void delete(ConcertSongEntity song);

    @Query("DELETE FROM concert_song WHERE concertId=:concertId")
    void deleteAll(long concertId);

    @Query(" SELECT * FROM concert_song WHERE concertId=:concertId ORDER BY sortOrder")
    LiveData<List<ConcertSongEntity>> getSongs(long concertId);

}
