package rachel.materialapps.recordmylive.logic.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;

@Dao
public interface ConcertDao {

    @Insert
    long insert(ConcertEntity concert);

    @Update
    void update(ConcertEntity concert);

    @Delete
    void delete(ConcertEntity concert);

    @Query("SELECT * FROM concert ORDER BY date DESC,time DESC")
    LiveData<List<ConcertEntity>> getAll();

    @Query(" SELECT * FROM concert WHERE date=:date ORDER BY time ASC")
    LiveData<List<ConcertEntity>> getByDate(LocalDate date);

    @Query("SELECT * FROM concert WHERE date BETWEEN :start AND :end")
    LiveData<List<ConcertEntity>> getByMonth(LocalDate start, LocalDate end);

    @Query("SELECT * FROM concert WHERE favorite=1 ORDER BY date DESC")
    LiveData<List<ConcertEntity>> getFavorites();

    @Transaction
    @Query("SELECT * FROM concert WHERE id=:id")
    LiveData<ConcertWithSongs> getDetail(long id);


    @Query("SELECT COUNT(*) FROM concert WHERE date = :date")
    int countByDate(LocalDate date);

    @Transaction
    default void insertConcertWithSongs(ConcertEntity concert,
                                        List<ConcertSongEntity> songs,
                                        ConcertSongDao songDao) {

        long concertId = insert(concert);

        for (ConcertSongEntity song : songs) {
            song.setConcertId(concertId);
        }
        songDao.insert(songs);
    }
}