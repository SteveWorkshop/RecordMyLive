package rachel.materialapps.recordmylive.logic.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rachel.materialapps.recordmylive.logic.dao.ConcertDao;
import rachel.materialapps.recordmylive.logic.dao.ConcertSongDao;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;

public class ConcertRepository {

    private static final String TAG = "ConcertRepository";

    private final ConcertDao concertDao;
    private final ConcertSongDao songDao;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public ConcertRepository(Context context) {

        var db = DatabaseManager.getInstance(context);

        concertDao = db.concertDao();
        songDao = db.concertSongDao();

    }

    public LiveData<List<ConcertEntity>> getAllConcerts() {
        return concertDao.getAll();
    }

    public LiveData<List<ConcertEntity>> getConcertsByDate(LocalDate date) {
        return concertDao.getByDate(date);
    }

    public LiveData<ConcertWithSongs> getConcertDetail(long id) {
        return concertDao.getDetail(id);
    }

    public void insertConcert(ConcertEntity entity) {
        executor.execute(() -> concertDao.insert(entity));
    }

    public void updateConcert(ConcertEntity entity) {
        executor.execute(() -> concertDao.update(entity));
    }

    public void deleteConcert(ConcertEntity entity) {
        executor.execute(() -> concertDao.delete(entity));
    }

    public void insertSongs(List<ConcertSongEntity> songs) {
        executor.execute(() -> songDao.insert(songs));
    }

    public LiveData<List<ConcertSongEntity>> getSongs(long concertId) {
        return songDao.getSongs(concertId);
    }

    public LiveData<List<ConcertEntity>> getByMonth(LocalDate start, LocalDate end) {
        Log.d(TAG, "getByMonth: 触发DB查询");
        return concertDao.getByMonth(start, end);
    }

    public int countByDate(LocalDate date) {
        return concertDao.countByDate(date);
    }
}