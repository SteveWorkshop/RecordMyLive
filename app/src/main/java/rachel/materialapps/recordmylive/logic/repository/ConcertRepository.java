package rachel.materialapps.recordmylive.logic.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import rachel.materialapps.recordmylive.logic.constants.AppConstants;
import rachel.materialapps.recordmylive.logic.dao.ConcertDao;
import rachel.materialapps.recordmylive.logic.dao.ConcertSongDao;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;

public class ConcertRepository {

    public enum SaveResult {
        SUCCESS, DAILY_LIMIT_REACHED, ERROR
    }

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

    public void saveConcert(ConcertEntity entity, List<ConcertSongEntity> songs, Consumer<SaveResult> callback) {
        executor.execute(() -> {
            SaveResult result;
            try {
                boolean inserted = concertDao.saveConcertWithSongs(
                        entity, songs, songDao, AppConstants.MAX_CONCERT_PER_DAY);
                result = inserted ? SaveResult.SUCCESS : SaveResult.DAILY_LIMIT_REACHED;
            } catch (Exception e) {
                Log.e(TAG, "保存演出失败", e);
                result = SaveResult.ERROR;
            }
            SaveResult saveResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(saveResult));
        });
    }

    public void updateConcert(ConcertEntity entity) {
        executor.execute(() -> concertDao.update(entity));
    }

    public void updateConcert(ConcertEntity entity, List<ConcertSongEntity> songs, Consumer<Boolean> callback) {
        executor.execute(() -> {
            boolean updated;
            try {
                updated = concertDao.updateConcertWithSongs(entity, songs, songDao);
            } catch (Exception e) {
                Log.e(TAG, "更新演出失败", e);
                updated = false;
            }
            boolean result = updated;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        });
    }

    public void deleteConcert(ConcertEntity entity) {
        deleteConcert(entity, success -> {});
    }

    public void deleteConcert(ConcertEntity entity, Consumer<Boolean> callback) {
        executor.execute(() -> {
            boolean deleted;
            try {
                deleted = concertDao.delete(entity) == 1;
            } catch (Exception e) {
                Log.e(TAG, "删除演出失败", e);
                deleted = false;
            }
            boolean result = deleted;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        });
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

    public LiveData<List<ConcertEntity>> getUpcoming(LocalDate today) {
        return concertDao.getUpcoming(today);
    }

    public int countByDate(LocalDate date) {
        return concertDao.countByDate(date);
    }
}
