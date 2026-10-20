package rachel.materialapps.recordmylive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import rachel.materialapps.recordmylive.logic.constants.AppConstants;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;
import rachel.materialapps.recordmylive.logic.dao.ConcertDao;
import rachel.materialapps.recordmylive.logic.db.AppDatabase;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;

@RunWith(AndroidJUnit4.class)
public class ConcertSaveTest {

    private AppDatabase database;
    private ConcertDao dao;
    private final LocalDate date = LocalDate.of(2026, 10, 19);

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AppDatabase.class).build();
        dao = database.concertDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertAndUpdateRejectMissingDate() {
        ConcertEntity draft = ConcertEntity.createEmpty();
        try {
            dao.insert(draft);
            fail("不应保存没有日期的演出");
        } catch (NullPointerException expected) {
            assertEquals("演出日期不能为空", expected.getMessage());
        }
        try {
            dao.update(draft);
            fail("不应更新为没有日期的演出");
        } catch (NullPointerException expected) {
            assertEquals("演出日期不能为空", expected.getMessage());
        }
    }

    @Test
    public void dailyLimitRejectsExtraConcertButAllowsAnotherDate() {
        for (int i = 0; i < AppConstants.MAX_CONCERT_PER_DAY; i++) {
            assertTrue(dao.insertIfBelowDailyLimit(concert(date), AppConstants.MAX_CONCERT_PER_DAY));
        }
        assertFalse(dao.insertIfBelowDailyLimit(concert(date), AppConstants.MAX_CONCERT_PER_DAY));
        assertEquals(AppConstants.MAX_CONCERT_PER_DAY, dao.countByDate(date));
        assertTrue(dao.insertIfBelowDailyLimit(concert(date.plusDays(1)), AppConstants.MAX_CONCERT_PER_DAY));
        assertEquals(1, dao.countByDate(date.plusDays(1)));
    }

    @Test
    public void concurrentSavesCannotExceedDailyLimit() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                results.add(executor.submit(() -> dao.insertIfBelowDailyLimit(
                        concert(date), AppConstants.MAX_CONCERT_PER_DAY)));
            }
            int inserted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    inserted++;
                }
            }
            assertEquals(AppConstants.MAX_CONCERT_PER_DAY, inserted);
            assertEquals(AppConstants.MAX_CONCERT_PER_DAY, dao.countByDate(date));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void updateChangesExistingConcertWithoutCreatingAnotherRow() {
        ConcertEntity original = concert(date);
        long id = dao.insert(original);
        ConcertEntity edited = concert(date.plusDays(1));
        edited.setId(id);
        edited.setName("编辑后的演出");

        assertEquals(1, dao.update(edited));
        assertEquals(1, dao.countByDate(date.plusDays(1)));
        assertEquals(0, dao.countByDate(date));
    }

    @Test
    public void saveAndUpdateConcertSongsAtomically() throws Exception {
        ConcertEntity concert = concert(date);
        List<ConcertSongEntity> firstSongs = new ArrayList<>();
        ConcertSongEntity first = new ConcertSongEntity("开场曲");
        ConcertSongEntity encore = new ConcertSongEntity("返场曲");
        encore.setEncore(true);
        firstSongs.add(first);
        firstSongs.add(encore);

        assertTrue(dao.saveConcertWithSongs(concert, firstSongs, database.concertSongDao(),
                AppConstants.MAX_CONCERT_PER_DAY));
        long concertId = concert.getId();
        assertEquals(2, database.concertSongDao().getSongsSync(concertId).size());

        List<ConcertSongEntity> replacement = new ArrayList<>();
        replacement.add(new ConcertSongEntity("新歌单第一首"));
        ConcertSongEntity last = new ConcertSongEntity("新歌单返场");
        last.setEncore(true);
        replacement.add(last);
        concert.setName("已编辑");
        assertTrue(dao.updateConcertWithSongs(concert, replacement, database.concertSongDao()));

        List<ConcertSongEntity> saved = database.concertSongDao().getSongsSync(concertId);
        assertEquals(2, saved.size());
        assertEquals("新歌单第一首", saved.get(0).getName());
        assertEquals(1, saved.get(0).getSortOrder());
        assertTrue(saved.get(1).isEncore());
        assertEquals(1, dao.countByDate(date));
    }

    private ConcertEntity concert(LocalDate date) {
        ConcertEntity concert = ConcertEntity.createEmpty();
        concert.setName("保存测试");
        concert.setType(ConcertType.CONCERT);
        concert.setDate(date);
        concert.setCity("上海");
        concert.setVenue("测试场馆");
        return concert;
    }
}
