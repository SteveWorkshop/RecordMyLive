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

    default long insert(ConcertEntity concert) {
        concert.requireDate();
        return insertValidated(concert);
    }

    @Insert
    long insertValidated(ConcertEntity concert);

    default int update(ConcertEntity concert) {
        concert.requireDate();
        return updateValidated(concert);
    }

    @Update
    int updateValidated(ConcertEntity concert);

    @Delete
    void delete(ConcertEntity concert);

    @Query("SELECT * FROM concert ORDER BY date DESC,time DESC")
    LiveData<List<ConcertEntity>> getAll();

    @Query(" SELECT * FROM concert WHERE date=:date ORDER BY time ASC")
    LiveData<List<ConcertEntity>> getByDate(LocalDate date);

    @Query("SELECT * FROM concert WHERE date BETWEEN :start AND :end")
    LiveData<List<ConcertEntity>> getByMonth(LocalDate start, LocalDate end);

    @Query("SELECT * FROM concert WHERE date >= :today ORDER BY date ASC, time ASC")
    LiveData<List<ConcertEntity>> getUpcoming(LocalDate today);

    @Query("SELECT * FROM concert WHERE favorite=1 ORDER BY date DESC")
    LiveData<List<ConcertEntity>> getFavorites();

    @Transaction
    @Query("SELECT * FROM concert WHERE id=:id")
    LiveData<ConcertWithSongs> getDetail(long id);


    @Query("SELECT COUNT(*) FROM concert WHERE date = :date")
    int countByDate(LocalDate date);

    @Transaction
    default boolean insertIfBelowDailyLimit(ConcertEntity concert, int dailyLimit) {
        if (countByDate(concert.getDate()) >= dailyLimit) {
            return false;
        }
        insert(concert);
        return true;
    }

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

    @Transaction
    default boolean saveConcertWithSongs(ConcertEntity concert,
                                         List<ConcertSongEntity> songs,
                                         ConcertSongDao songDao,
                                         int dailyLimit) {
        if (countByDate(concert.getDate()) >= dailyLimit) {
            return false;
        }
        long concertId = insert(concert);
        insertSongsForConcert(songs, concertId, songDao);
        return true;
    }

    @Transaction
    default boolean updateConcertWithSongs(ConcertEntity concert,
                                           List<ConcertSongEntity> songs,
                                           ConcertSongDao songDao) {
        if (update(concert) <= 0) {
            return false;
        }
        songDao.deleteAll(concert.getId());
        insertSongsForConcert(songs, concert.getId(), songDao);
        return true;
    }

    private static void insertSongsForConcert(List<ConcertSongEntity> songs,
                                             long concertId,
                                             ConcertSongDao songDao) {
        for (int i = 0; i < songs.size(); i++) {
            ConcertSongEntity song = songs.get(i);
            song.setId(0);
            song.setConcertId(concertId);
            song.setSortOrder(i + 1);
        }
        if (!songs.isEmpty()) {
            songDao.insert(songs);
        }
    }
}
